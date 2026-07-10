package com.github.ensnert.impl;

import com.github.ensnert.api.Index;
import com.github.ensnert.impl.data.AnalyzedVersion;
import com.github.ensnert.impl.database.types.DependencyData;
import com.github.ensnert.system.Holder;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.aether.installation.InstallRequest;
import org.eclipse.aether.metadata.DefaultMetadata;
import org.eclipse.aether.metadata.Metadata;
import org.eclipse.aether.repository.LocalArtifactRequest;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.MetadataRequest;
import org.eclipse.aether.resolution.MetadataResult;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.version.Version;
import org.eclipse.aether.version.VersionRange;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * @author ensnerT (2026) - AI was used for Class Research of "org.eclipse.aether.*" and "org.apache.maven.artifact.*" and "org.apache.maven.model.*"
 */
public final class Pom
{
	private final Holder holder;
	@SuppressWarnings({ "FieldMayBeFinal" }) // because the artifact may change in the future
	private Artifact artifact;
	private Model model;
	private DependencyData artifactId;
	private ArrayList<String> artifactVersions = null;
	private Index storeKey_artifactVersions = null;

	private static final Pattern COORDINATE = Pattern.compile("([^: ]+):([^: ]+)(:([^: ]*)(:([^: ]+))?)?:([^: ]*)");

	public static Artifact toArtifact(String coordinate)
	{
		if (coordinate.matches("[^: ]+:[^: ]"))
		{
			coordinate = coordinate + ":";
		}

		Matcher matcher = COORDINATE.matcher(coordinate);
		if (!matcher.matches())
		{
			matcher = COORDINATE.matcher(coordinate + ":");
		}
		if (matcher.matches())
		{
			String group = matcher.group(1);
			String artifactId = matcher.group(2);
			String extension = get(matcher.group(4), "jar");
			String classifier = get(matcher.group(6), null);
			String version = get(matcher.group(7), "0");

			DefaultArtifactHandler artifactHandler = new DefaultArtifactHandler(extension);
			artifactHandler.setExtension(extension);
			artifactHandler.setLanguage(null);
			artifactHandler.setAddedToClasspath(false);
			artifactHandler.setIncludesDependencies(false);

			return new DefaultArtifact(group, artifactId, version, null, extension, classifier, artifactHandler);
		}
		throw new RuntimeException("Invalid coordinate: " + coordinate);
	}

	private static String get(String v1, String v2) {return v1 != null && !v1.isEmpty() ? v1 : v2;}

	// region Constructors

	@SuppressWarnings("unused") // might be of use for later as "POM-Scanning"
	public Pom(Holder holder, Artifact artifact)
	{
		this.holder = holder;
		this.artifact = new DefaultArtifact(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), null, "pom", artifact.getClassifier(),
				artifact.getArtifactHandler());
	}

	public Pom(Holder holder, String coordinate)
	{
		this.holder = holder;

		Artifact art1;
		try
		{

			art1 = toArtifact(coordinate);
		}
		catch (Exception e)
		{
			throw new RuntimeException("Coordinate not parseable correclty! please try to set : '{groupid}:{artifact}:{...}:' (with an empty version part)", e);
		}

		// this.artifact = new DefaultArtifact(art1.getGroupId(), art1.getArtifactId(), art1.getClassifier(), "pom", art1.getVersion());
		this.artifact = new DefaultArtifact(art1.getGroupId(), art1.getArtifactId(), art1.getVersion(), null, "pom", art1.getClassifier(),
				art1.getArtifactHandler());
	}

	// endregion Constructors

	// region Getters

	public String getGroupId()
	{
		return artifact.getGroupId();
	}

	public String getArtifactId()
	{
		return artifact.getArtifactId();
	}

	public String getVersion()
	{
		if ("0".equals(artifact.getVersion()))
			return null;
		return artifact.getVersion();
	}

	public Optional<String> version()
	{
		return Optional.ofNullable(getVersion());
	}

	public String getClassifier()
	{
		return artifact.getClassifier();
	}

	public String getExtension()
	{
		return artifact.getType();
	}

	// endregion Getters

	// region Setters

	public void setArtifactVersion(String version)
	{
		if (!VersionComparator.isSame(this.artifact.getVersion(), version))
		{
			this.artifact.setVersion(version);
			this.artifactId = null;
			this.model = null;
			this.storeKey_artifactVersions = null;
			this.artifactVersions = null;
		}
	}

	// endregion Setters

	// region Cached Attributes

	public DependencyData getKey()
	{
		if (this.artifactId == null)
		{
			this.artifactId = new DependencyData(getGroupId(), getArtifactId(), getExtension(), getClassifier(), getVersion(), null, null, null, null, null,
					null, null, null);
		}

		return this.artifactId;
	}

	public String getCoordinate()
	{
		StringBuilder sb = new StringBuilder();

		sb.append(artifact.getGroupId()).append(":").append(artifact.getArtifactId());

		if (!Set.of("", "jar").contains(artifact.getType()))
		{
			sb.append(":").append(artifact.getType());
			if (artifact.getClassifier() != null && !Objects.equals("", artifact.getClassifier()))
			{
				sb.append(":").append(artifact.getClassifier());
			}
			sb.append(":");
			if (getVersion() != null)
				sb.append(getVersion());
		}
		else
		{
			if (getVersion() != null)
				sb.append(":").append(getVersion());
		}
		return sb.toString();
	}

	// endregion Cached Attributes

	/**
	 * This Method Resolves the Full POM file from Remote with all Plugins, Dependencies and Parents.
	 *
	 * @return Completly Resolved Model of the given pom.xml.
	 * @implNote if you change anything within the Structure of Pom Class, which will affect the PomCompilation output, then please clear the "model" property.
	 */
	public Model resolveModel()
	{
		if (this.model == null)
		{
			Model result;
			ArtifactRequest artifactRequest = new ArtifactRequest();
			artifactRequest.setArtifact(new org.eclipse.aether.artifact.DefaultArtifact(getGroupId(), getArtifactId(), getClassifier(), "pom", getVersion()));
			artifactRequest.setRepositories(holder.getRemoteRepositories());

			File file;
			try
			{
				ArtifactResult artifactResult = holder.getRepositorySystem().resolveArtifact(holder.getRepositorySystemSession(), artifactRequest);
				file = artifactResult.getArtifact().getFile();
			}
			catch (ArtifactResolutionException e)
			{
				throw new RuntimeException(e);
			}

			ModelBuildingRequest request = new DefaultModelBuildingRequest();
			request.setProcessPlugins(true); // Matches standard mvn help:effective-pom output
			// request.setPomFile(pomFile);
			request.setPomFile(file);
			request.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
			request.setSystemProperties(System.getProperties());
			request.setModelResolver(holder.getModelResolver());

			try
			{
				result = holder.getModelBuilder().build(request).getEffectiveModel();
			}
			catch (ModelBuildingException e)
			{
				throw new RuntimeException(e);
			}
			this.model = result;
		}
		return this.model;
	}

	@SuppressWarnings({ "unused", "all" }) // intended for future ; incomplete
	public HashMap<Version, Model> resolveVersionRangeOffline()
	{
		List<String> versions = findVersions(true, false);

		VersionRange range = VersionComparator.parseRange(this.artifact.getVersion());
		HashMap<Version, Model> result = new HashMap<>();
		for (String v : versions)
		{
			Version currentVersion = VersionComparator.parseVersion(v);
			boolean b = range.containsVersion(currentVersion);
			if (b)
			{
				this.setArtifactVersion(v);
				this.resolveModel();

				result.put(currentVersion, this.model);
			}
		}
		return result;
	}

	@SuppressWarnings("unused") // intended for future ; incomplete
	public void resolveVersionRangesOnline()
	{
		VersionRangeRequest request = new VersionRangeRequest(
				new org.eclipse.aether.artifact.DefaultArtifact(getGroupId(), getArtifactId(), getClassifier(), "pom", getVersion()),
				holder.getRemoteRepositories(), null);

		try
		{
			VersionRangeResult result = holder.getRepositorySystem().resolveVersionRange(holder.getRepositorySystemSession(), request);
			for (Version v : result.getVersions())
			{
				Output.verbose(" - " + v.toString());
			}
		}
		catch (VersionRangeResolutionException e)
		{
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unused") // not used for now, used later.
	private List<String> findVersions()
	{
		if (this.artifactVersions == null)
		{
			return new ArrayList<>();
		}
		return this.artifactVersions;
	}

	/**
	 * @return List of all Versions found on the Central Repository by Artifact and Group
	 */
	public List<String> findVersions(boolean release, boolean snapshot)
	{
		if (this.artifactVersions == null || this.storeKey_artifactVersions == null || !this.storeKey_artifactVersions.same(release, snapshot))
		{
			MetadataRequest metadataRequest = new MetadataRequest();
			Metadata.Nature nature = Metadata.Nature.RELEASE;

			if (snapshot)
			{
				if (release)
				{
					nature = Metadata.Nature.RELEASE_OR_SNAPSHOT;
				}
				else
				{
					nature = Metadata.Nature.SNAPSHOT;
				}
			}

			metadataRequest.setMetadata(new DefaultMetadata(this.artifact.getGroupId(), this.artifact.getArtifactId(), "maven-metadata.xml", nature));
			metadataRequest.setRepository(holder.getRemoteRepositories().get(0));

			List<MetadataResult> results = holder.getRepositorySystem()
												   .resolveMetadata(holder.getRepositorySystemSession(), Collections.singletonList(metadataRequest));

			if (results == null || results.isEmpty())
			{
				Output.error("No metadata found for:" + metadataRequest);
				return new ArrayList<>();
			}

			Set<String> versions = new HashSet<>();

			for (MetadataResult result : results)
			{
				Exception exception = result.getException();

				if (exception != null)
				{
					Output.error(exception.getLocalizedMessage());
					continue;
				}

				try (XmlStreamReader reader = ReaderFactory.newXmlReader(result.getMetadata().getFile()))
				{
					versions.addAll(new MetadataXpp3Reader().read(reader, false).getVersioning().getVersions());
				}
				catch (IOException | XmlPullParserException e)
				{
					Output.error(e.getLocalizedMessage());
				}
			}

			storeKey_artifactVersions = new Index(release, snapshot);
			artifactVersions = new ArrayList<>(versions);
			artifactVersions.sort(VersionComparator.getComparator());
		}
		return artifactVersions;
	}

	public List<AnalyzedVersion> analyzeVersions(List<String> versions)
	{
		Optional<String> currentVersion = Optional.ofNullable(getVersion() == null || getVersion().isEmpty() ? null : getVersion());

		List<AnalyzedVersion> analyzedVersions = new ArrayList<>();

		for (int i = 0; i < versions.size(); i++)
		{
			String v = versions.get(i);
			Optional<String> nextV = Optional.ofNullable(versions.size() - 1 > i ? versions.get(i + 1) : null); // Index out of bounds at i == 126
			boolean isNewer = currentVersion.isEmpty() || VersionComparator.isNewer(currentVersion.get(), v);
			boolean isOlder = currentVersion.isEmpty() || VersionComparator.isOlder(currentVersion.get(), v);
			boolean isLatest = nextV.isEmpty();
			boolean isRecommended = isLatest || VersionComparator.changedLevel(v, nextV.get()) < 2;
			boolean isCurrent = currentVersion.isPresent() && currentVersion.get().equals(v);

			// on the last iteration, if the current add the
			analyzedVersions.add(new AnalyzedVersion(v, isNewer, isOlder, isCurrent, isRecommended, ArtifactUtils.isSnapshot(v), isLatest));
		}

		return analyzedVersions;
	}

	@SuppressWarnings({ "unused" })
	public void installVersion()
	{
		InstallRequest installRequest = new InstallRequest();
		org.eclipse.aether.artifact.DefaultArtifact jar = new org.eclipse.aether.artifact.DefaultArtifact(getGroupId(), getArtifactId(), getClassifier(), "jar",
				getVersion());
		installRequest.addArtifact(jar);
		// holder.getRepositorySystem().install(holder.getRepositorySystemSession(), installRequest);

		ArtifactResult artifactResult;
		LocalArtifactRequest localArtifactRequest = new LocalArtifactRequest();
		try
		{
			artifactResult = holder.getRepositorySystem()
									 .resolveArtifact(holder.getRepositorySystemSession(), new ArtifactRequest(jar, holder.getRemoteRepositories(), null));
		}
		catch (ArtifactResolutionException e)
		{
			throw new RuntimeException(e);
		}

		artifactResult.isResolved();
		boolean isDownloaded = artifactResult.getLocalArtifactResult().getFile() != null;
	}
}
