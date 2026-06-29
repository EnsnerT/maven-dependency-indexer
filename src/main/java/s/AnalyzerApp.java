package s;

import ch.ensnert.App;
import ch.ensnert.api.database.Database;
import ch.ensnert.api.database.csv.CsvDatabase;
import ch.ensnert.impl.ArtifactVersionMatrix;
import ch.ensnert.impl.database.indexstrategies.MavenCoordinateIndex;
import ch.ensnert.impl.database.types.DependencyData;
import ch.ensnert.impl.data.AnalyzedVersion;
import ch.ensnert.system.Holder;
import ch.ensnert.api.Index;
import ch.ensnert.impl.Pom;
import ch.ensnert.api.Table;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.FileReader;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;


@Named // makes the class integrate into the plexus lifecycle
public final class AnalyzerApp implements App
{

	public Holder holder;
	private static Database<DependencyData> db;
	private static final String DB_FILE = "./db.csv";

	@Inject()
	public AnalyzerApp(Holder holder)
	{
		if (holder != null)
			this.holder = holder;
	}

	/**
	 * <b> Note! this main method is Gone and has been replaced with the {@link PlexusLoader} </b>
	 */
	public static void main(String[] args) throws Exception
	{

	}

	public static final class Params
	{
		public Mode mode;
		public ArrayList<String> coordinates = new ArrayList<>();
		public boolean moreThenNEntries;
		public boolean version_newer;
		public boolean version_older;
		public boolean version_snapshot;
		public boolean version_release;
		public boolean version_all;
		public String[] originalArguments = new String[0];

		public Params()
		{
			this.mode = Mode.HELP;
			this.moreThenNEntries = false;
			this.version_newer = false;
			this.version_older = false;
			this.version_snapshot = false;
			this.version_release = true;
			this.version_all = false;
		}

		public enum Mode
		{
			HELP,
			INDEX,
			VERSION,
			RANGE,
			FIND
		}
	}

	@Override
	public void run(String[] args) throws Exception
	{
		Params runningParameter = new Params();

		if (args != null && args.length > 0)
		{
			runningParameter.originalArguments = args;
			int i = 0;
			while (i < args.length)
			{
				String arg = args[i++];
				if (Set.of("help", "-h", "--help", "-?").contains(arg))
				{
					command_help(runningParameter);
					return;
				}
				else if (Set.of("index").contains(arg))
				{
					runningParameter.mode = Params.Mode.INDEX;
				}
				else if (Set.of("range").contains(arg))
				{
					runningParameter.mode = Params.Mode.RANGE;
				}
				else if (Set.of("find").contains(arg))
				{
					runningParameter.mode = Params.Mode.FIND;
				}
				else if (Set.of("--version", "-v", "version", "versions").contains(arg))
				{
					runningParameter.mode = Params.Mode.VERSION;
				}
				else if (Set.of("--all", "-a").contains(arg))
				{
					runningParameter.version_all = true;
				}
				else if (Set.of("--snapshot", "--snapshots", "-s").contains(arg))
				{
					runningParameter.version_snapshot = true;
					runningParameter.version_release = false;
				}
				else if (Set.of("--release", "--releases", "-r").contains(arg))
				{
					runningParameter.version_release = true;
				}
				else if (Set.of("--new", "-n").contains(arg))
				{
					runningParameter.version_newer = true;
				}
				else if (Set.of("--old", "-o").contains(arg))
				{
					runningParameter.version_older = true;
				}
				else if (Set.of("--yes").contains(arg))
				{
					runningParameter.moreThenNEntries = true;
				}
				else if (arg.contains(":"))
				{
					runningParameter.coordinates.add(arg);
				}
			}

		}
		else
		{
			command_help(runningParameter);
			return;
		}

		switch (runningParameter.mode)
		{
			case HELP ->
			{
				command_help(runningParameter);
			}
			case INDEX ->
			{
				command_index(runningParameter);
			}
			case VERSION ->
			{
				command_version(runningParameter);
			}
			case RANGE ->
			{
				command_range(runningParameter);
			}
			case FIND ->
			{
				command_find(runningParameter);
			}
		}

	}

	public void command_help(Params runningParameter)
	{
		PrintStream output = System.out;
		output.println("Usage: ");
		// todo te - tell what the format for : {artifact} is!
		if (runningParameter.mode == Params.Mode.HELP)
		{
			output.println("\t-h | help | * Show this help");
			output.println("\tindex -h | index help | * index an artifact");
			output.println("\tversion -h | version help | * show versions of an artifact");
			output.println("\trange -h | range help | * resolve and index a range of versions for an artifact");
			output.println("\tfind -h | find help | * scrape your indexed database for a dependency matrix");
		}
		if (runningParameter.mode == Params.Mode.INDEX)
		{
			output.println("\tindex {artifact+version} | index a certain version of the artifact");
			output.println("\tindex {artifact+version} --all / -a | use all versions, not just the \"recommended\" (only with --new or --old)");
			output.println(
					"\tindex {artifact+version} --new / -n | \"recommended\" the newest patches for each generation (like x.y.z the x.y is the generation)");
			output.println("\tindex {artifact+version} --old / -o | \"recommended\" the best patches before the {version} for each generation");
			output.println("\tindex {artifact+version} --shapshot / --snapshots / -s | only snapshots in the versions ; overwrites release");
			output.println("\tindex {artifact+version} --release / --releases / -r | include release versions (only with -s -r)");
			output.println("\t * you could remember the arguments as \"sonar\" Shapshot, Old, New, All, Release");
			output.println("\tindex {artifact+version} {...} version {...} | will turn the command from an 'index' to an 'version'.");
		}

		if (runningParameter.mode == Params.Mode.VERSION)
		{
			output.println("\tversion {artifact} | show \"recommended\" versions");
			output.println("\tversion {artifact} --all / -a | show all versions");
			output.println("\tversion {artifact+version} --new / -n | show newer versions than {version}");
			output.println("\tversion {artifact+version} --old / -n | show older versions than {version}");
			output.println("\tversion {artifact+version} --shapshot / --snapshots / -s | only snapshots in the versions ; excludes releases");
			output.println("\tversion {artifact+version} --release / --releases / -r | include release versions (only with -s -r)");
			output.println("\t * you could remember the arguments as \"sonar\" Shapshot, Old, New, All, Release");
			output.println("\tversion {artifact+version} {...} index {...} | will turn the command from an 'version' to an 'index'.");
		}

		if (runningParameter.mode == Params.Mode.RANGE)
		{
			output.println(" * this command is not yet implemented! ");
		}

		if (runningParameter.mode == Params.Mode.FIND)
		{
			output.println("\tfind {artifact} | show all indexes by given key");
			output.println("\tfind {artifact_A} {artifact_B} | show all indexes by given key");
			output.println(" * this command is not yet fully implemented! ");
		}
	}

	public void command_index(Params runningParameter)
	{
		try
		{
			if (runningParameter.coordinates.isEmpty())
				throw new RuntimeException("Please specify a coordinate for operation!");

			database_init();

			Pom pom = new Pom(holder, runningParameter.coordinates.get(0));
			List<AnalyzedVersion> filteredVersions = applyParameteredFilter(runningParameter, pom);

			System.out.println(pom.getCoordinate() + " -> ");

			Pom indexingPom = new Pom(holder, runningParameter.coordinates.get(0));

			for (AnalyzedVersion fv : filteredVersions)
			{
				StringJoiner sj = new StringJoiner(", ");
				if (fv.isCurrent())
					sj.add("current");
				if (fv.isRecommended())
					sj.add("last patch");
				if (fv.isSnapshot() && runningParameter.version_release && runningParameter.version_snapshot)
					sj.add("snapshot");
				if (fv.isLatest())
					sj.add("latest");

				if (sj.length() > 0)
					System.out.println(" - Indexing : " + fv.version() + " (" + sj.toString() + ")");
				else
					System.out.println(" - Indexing : " + fv.version());

				{
					indexingPom.setArtifactVersion(fv.version());

					String key = db.createKey(indexingPom.getKey());
					if (!db.hasKey(key))
					{
						Model model = indexingPom.resolveModel();
						List<Dependency> deps = model.getDependencies();
						Collection<DependencyData> dd = new ArrayList<>();

						for (Dependency dep : deps)
							dd.add(fromFull(model, dep));

						System.out.println(" \\ - Found " + dd.size() + " dependencies.");
						db.batchInsert(dd);
					}
					else
					{
						System.out.println(" \\ -- Artifact Version : \"" + indexingPom.getVersion() + "\" already present in Database; Skiping indexing -- ");
					}
				}
			}

			database_save();
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}

	}

	public void command_version(Params runningParameter)
	{
		try
		{
			if (runningParameter.coordinates.isEmpty())
				throw new RuntimeException("Please specify a coordinate for operation!");

			Pom pom = new Pom(holder, runningParameter.coordinates.get(0));
			List<AnalyzedVersion> filteredVersions = applyParameteredFilter(runningParameter, pom);

			System.out.println(pom.getCoordinate() + " -> ");

			for (AnalyzedVersion fv : filteredVersions)
			{
				StringJoiner sj = new StringJoiner(", ");
				if (fv.isCurrent())
					sj.add("current");
				if (fv.isRecommended())
					sj.add("last patch");
				if (fv.isSnapshot() && runningParameter.version_release && runningParameter.version_snapshot)
					sj.add("snapshot");
				if (fv.isLatest())
					sj.add("latest");

				if (sj.length() > 0)
					System.out.println(" - " + fv.version() + " (" + sj.toString() + ")");
				else
					System.out.println(" - " + fv.version());
			}

		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	public void command_range(Params runningParameter)
	{
		throw new UnsupportedOperationException("Command not yet implemented");
	}

	public void command_find(Params runningParameter)
	{
		if (runningParameter.coordinates.size() == 1)
		{
			Table tableSimpleLookup = new Table(4);
			tableSimpleLookup.addCol("Artifact").addCol("Version").addCol("Dependency").addCol("Version").endRow();

			// if you find 1; show exactly this entry
			try
			{
				database_init();

				List<DependencyData> dataList = db.cloneData();

				Result result = searchDependencies(runningParameter.coordinates.get(0), dataList);

				for (DependencyData data : result.matchingAsArtifact())
				{
					tableSimpleLookup.addCol(Table.ColoredCol.of(data.groupId() + ":" + data.artifactId(), Table.ColoredCol.LIGHT_YELLOW))
							.addCol(Table.ColoredCol.of(data.version(), Table.ColoredCol.LIGHT_YELLOW))
							.addCol(data.depGroupId() + ":" + data.depArtifactId())
							.addCol(data.depVersion())
							.endRow();
				}
				for (DependencyData data : result.matchingAsDependency())
				{
					tableSimpleLookup.addCol(data.groupId() + ":" + data.artifactId())
							.addCol(data.version())
							.addCol(Table.ColoredCol.of(data.depGroupId() + ":" + data.depArtifactId(), Table.ColoredCol.LIGHT_YELLOW))
							.addCol(Table.ColoredCol.of(data.depVersion(), Table.ColoredCol.LIGHT_YELLOW))
							.endRow();
				}
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}

			System.out.println(Table.TableConfig.SIMPLE_LINES().render(tableSimpleLookup, Table.TableConfig.OutlineType.FULL));
		}
		else if (runningParameter.coordinates.size() > 1)
		{
			for (String coordinate : runningParameter.coordinates)
			{
				if (coordinate.contains("*"))
				{
					System.err.println("For Matrix search, you can not use a wildcard coordinate!");
					return;
				}
			}

			/// Set{ Index { artifact, dependency } }
			HashSet<Index> relations = new HashSet<>();

			ArtifactVersionMatrix matrix = new ArtifactVersionMatrix();

			matrix.addNodes(runningParameter.coordinates.toArray(new String[0])); /// to keep the order.

			try
			{
				database_init();

				List<DependencyData> dataList = db.cloneData();

				// i want to match the versions between each one.

				// 1. search for coordinate 1
				// 2. search in the merged list for coordinate [!1]
				// 3. all matches are end results and are recorded!

				HashMap<String, Result> mapResult = new HashMap<>();

				for (String coordinateA : runningParameter.coordinates)
				{
					Result resultsA = searchDependencies(coordinateA, dataList);

					ArrayList<DependencyData> previewAllNodes = new ArrayList<>();
					ArrayList<String> otherCoordinates = new ArrayList<>(runningParameter.coordinates);
					otherCoordinates.remove(coordinateA);

					for (String coordinateB : otherCoordinates)
					{
						Result resultsB = searchDependencies(coordinateB, resultsA.allMatchings());

						List<DependencyData> intersections = resultsB.allMatchings();
						previewAllNodes.addAll(intersections);

						if (!intersections.isEmpty())
						{
							// add a relation between the two coordinate
							relations.add(new Index(coordinateA, coordinateB)); // todo te - this relation might not be in the correct order!

							for (DependencyData data : intersections)
							{
								matrix.addVersion(data.groupId() + ":" + data.artifactId(), data.version(), data.depGroupId() + ":" + data.depArtifactId(),
										data.depVersion());
							}
						}
					}

					/// DEBUG : Preview All Dependencies
					if (false)
					{
						System.out.println(" - " + coordinateA + " has found:");
						printResult(new Result(previewAllNodes, new ArrayList<>(), previewAllNodes));
					}
				}


				/// References ( Bonds )
				if (false)
				{
					Table f1 = new Table(2);
					f1.setName(Table.Col.of("Bonds").setAlign(Table.Align.CENTER));
					f1.addCol("Artifact").addCol("Dependency").endRow();

					for (Index referenceKey : relations)
					{
						Serializable[] cache = referenceKey.cache();
						String key = cache[0].toString();
						String value = cache[1].toString();
						f1.addCol(key).addCol(value).endRow();
					}
					System.out.println(Table.TableConfig.SIMPLE_LINES().render(f1, Table.TableConfig.OutlineType.FULL));
				}

				/// second iteration of "-a" (now can choose between them)
				// if (false)
				{
					String[][] strings = matrix.makeView(runningParameter.version_all);
					Table f3 = new Table(strings[0].length);
					boolean firstRow = true;
					row:
					for (String[] string : strings)
					{
						boolean isColored = !firstRow;
						for (String s : string)
							if (s.isEmpty())
								isColored = false; /// mark a full entry as {colored}

						for (String entry : string)
							if (isColored && runningParameter.version_all) /// only color the row, if it is "-a" and {colored}
								f3.addCol(Table.ColoredCol.of(entry, Table.ColoredCol.LIGHT_GREEN));
							else
								f3.addCol(entry); /// otherwise keep it plain

						f3.endRow();
						firstRow = false;
					}
					System.out.println(Table.TableConfig.SIMPLE_LINES().render(f3, Table.TableConfig.OutlineType.FULL));
				}

			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}

		}

		// for a 2 instance table, you just search every entry, that matches **both**.
		// example: `find com.azure:azure-core com.azure:azure-identity` -> azure-identity && azure-core as dep

		// for >= 3 entries you make a column for each Artifact, if the artifact matches with another artifact, paint it yellow, if all of them match, paint it green.
		// example: `find com.azure:azure-core com.azure:azure-core-http-netty com.azure:azure-identity` should find:
		// - azure core as dep 1.58.1
		// - azure identity as artifact 1.18.4
		// - azure core netty as artifact 1.16.5

		// throw new UnsupportedOperationException("Command not yet implemented");
	}

	private static void printResult(Result value)
	{
		if (!value.matchingAsArtifact().isEmpty())
		{
			Table f1 = new Table(4);
			f1.setName(Table.Col.of("As Artifact").setAlign(Table.Align.CENTER));
			f1.addCol("Artifact").addCol("Version").addCol("Dependency").addCol("Version").endRow();
			for (DependencyData data : value.matchingAsArtifact())
			{
				f1.addCol(data.groupId() + ":" + data.artifactId())
						.addCol(data.version())
						.addCol(data.depGroupId() + ":" + data.depArtifactId())
						.addCol(data.depVersion())
						.endRow();
			}
			System.out.println(Table.TableConfig.SIMPLE_LINES().render(f1, Table.TableConfig.OutlineType.FULL));
		}
		if (!value.matchingAsDependency().isEmpty())
		{
			Table f1 = new Table(4);
			f1.setName(Table.Col.of("As Dependency").setAlign(Table.Align.CENTER));
			f1.addCol("Artifact").addCol("Version").addCol("Dependency").addCol("Version").endRow();
			for (DependencyData data : value.matchingAsDependency())
			{
				f1.addCol(data.groupId() + ":" + data.artifactId())
						.addCol(data.version())
						.addCol(data.depGroupId() + ":" + data.depArtifactId())
						.addCol(data.depVersion())
						.endRow();
			}
			System.out.println(Table.TableConfig.SIMPLE_LINES().render(f1, Table.TableConfig.OutlineType.FULL));
		}
	}

	private Result searchDependencies(String searchingCoordinate, List<DependencyData> dataList)
	{
		Pom pom = new Pom(holder, searchingCoordinate);
		String groupId = pom.getGroupId();
		String artifactId = pom.getArtifactId();
		Optional<String> version = pom.version();
		String type = pom.getExtension();
		String classifier = pom.getClassifier();

		List<DependencyData> matchingAsArtifact = new ArrayList<>();
		List<DependencyData> matchingAsDependency = new ArrayList<>();

		for (DependencyData data : dataList)
		{
			// if (!groupId.equalsIgnoreCase(data.groupId()) && !groupId.equalsIgnoreCase(data.depGroupId()))
			if (!groupId.equals("*") && !groupId.equalsIgnoreCase(data.groupId()) && !groupId.equalsIgnoreCase(data.depGroupId()))
				continue;
			if (!artifactId.equalsIgnoreCase(data.artifactId()) && !artifactId.equalsIgnoreCase(data.depArtifactId()))
				continue;

			// if ((groupId.equalsIgnoreCase(data.groupId())) && artifactId.equalsIgnoreCase(data.artifactId()))
			if ((groupId.equals("*") || groupId.equalsIgnoreCase(data.groupId())) && artifactId.equalsIgnoreCase(data.artifactId()))
			{
				// main
				if (version.isPresent())
				{
					if (!version.get().equalsIgnoreCase(data.version()))
					{
						continue;
					}
				}

				matchingAsArtifact.add(data);
			}
			// else if ((groupId.equalsIgnoreCase(data.depGroupId())) && artifactId.equalsIgnoreCase(data.depArtifactId()))
			else if ((groupId.equals("*") || groupId.equalsIgnoreCase(data.depGroupId())) && artifactId.equalsIgnoreCase(data.depArtifactId()))
			{
				// dependency
				if (version.isPresent())
				{
					if (!version.get().equalsIgnoreCase(data.depVersion()))
					{
						continue;
					}
				}
				// if (classifier != null && !classifier.isEmpty() && classifier.equalsIgnoreCase(data.depClassifier()))
				// 	continue;
				// if (type != null && !type.isEmpty() && type.equalsIgnoreCase(data.depType()))
				// 	continue;
				matchingAsDependency.add(data);
			}
			else
				continue;
		}
		ArrayList<DependencyData> allMatchings = new ArrayList<>(matchingAsArtifact);
		allMatchings.addAll(matchingAsDependency);
		return new Result(matchingAsArtifact, matchingAsDependency, allMatchings);
	}

	private static List<AnalyzedVersion> applyParameteredFilter(Params runningParameter, Pom pom)
	{
		List<AnalyzedVersion> filteredVersions = new ArrayList<>();

		Optional<String> version = pom.version();
		List<String> versions = pom.findVersions(runningParameter.version_release, runningParameter.version_snapshot);
		List<AnalyzedVersion> analyzedVersions = pom.analyzeVersions(versions);

		for (AnalyzedVersion analyzedVersion : analyzedVersions)
		{
			// either you tell --all or it is recommended
			boolean keep = runningParameter.version_all || analyzedVersion.isRecommended() || analyzedVersion.isCurrent();

			if (version.isPresent())
			{
				// if its old and you want old
				if (analyzedVersion.isOlder())
					keep &= runningParameter.version_older;
				// if its newer and you want newer
				if (analyzedVersion.isNewer())
					keep &= runningParameter.version_newer;
			}

			if (keep)
				filteredVersions.add(analyzedVersion);
		}
		return filteredVersions;
	}

	private record Result(List<DependencyData> matchingAsArtifact, List<DependencyData> matchingAsDependency, List<DependencyData> allMatchings) {}

	// public void inst(String[] args) throws Exception
	/* {
		String coordinate = null;

		if (args != null && args.length > 0)
		{
			int i = 0;

			// region Test
			if (args[i].equals("test"))
			{
				Test.main(Arrays.copyOfRange(args, ++i, args.length));
				return;
			}
			// endregion Test

			// region CLI Parser
			while (i < args.length)
			{
				if (args[i].equals("-h") || args[i].equals("-?") || args[i].equals("--help") || args[i].equals("--?"))
				{
					System.out.println("Usage: ");
					System.out.println(" - Single Scan | `<groupId>:<artifactId>:<version>` to add a Complete Version to the Database.");
					System.out.println(" - Range Scan  |`-r <groupId>:<artifactId>:<versionRange>` to add a Complete Range of Version to the Database.");
					System.out.println(" - Range Scan  | `--range <groupId>:<artifactId>:<versionRange>`");
					System.out.println(" - Sniffing    | `-l <groupId>:<artifactId>` Lookup Versions for specifed Group and Artifacts");
					System.out.println(" - Sniffing    | `-v <groupId>:<artifactId>`");
					System.out.println(" - Sniffing    | `--list <groupId>:<artifactId>`");
					System.out.println(" - Sniffing & Range Scan | `-s` also map Snapshots");
					System.out.println(" - *           | `-n` do not add entry to the Database.");
					return;
				}
				else if (args[i].equals("-r") || args[i].equals("--range"))
				{
					// n <= x  `[n,)`
					// n <= x < m  `[n,m)`
					Params.mode = Params.Mode.SCAN_RANGE;
				}
				else if (args[i].equals("-l") || args[i].equals("-v") || args[i].equals("--list"))
				{
					Params.mode = Params.Mode.SNIFF;
				}
				else if (args[i].equals("-s"))
				{
					Params.withSnapshot = true;
				}
				else if (args[i].equals("--yes"))
				{
					Params.didConfirm = true;
				}
				else if (args[i].contains(":"))
				{
					coordinate = args[i];
				}
				i++;
			}
			// endregion CLI Parser
		}

		if (coordinate == null)
		{
			throw new RuntimeException("Please specify a coordinate for operation, or use -h or -? for a list of operations!");
		}

		switch (Params.mode)
		{
			case SCAN:
			{
				database_init();
				Pom pom = new Pom(holder, coordinate);

				String key = db.createKey(pom.getKey());
				System.out.println(key + ":");
				if (!db.hasKey(key))
				{

					Model model = pom.getPom();
					List<Dependency> deps = model.getDependencies();
					Collection<DependencyData> dd = new ArrayList<>();
					for (Dependency dep : deps)
					{
						DependencyData data = new DependencyData(model.getGroupId(), model.getArtifactId(), model.getPackaging(), "", model.getVersion(),
								dep.getGroupId(), dep.getArtifactId(), dep.getType(), dep.getClassifier(), dep.getScope(), dep.getVersion(), dep.getOptional(),
								String.join(".", dep.getExclusions().stream().map(Main::getKey).toList()));

						System.out.println(" - " + getDependencyKey(data));

						dd.add(data);
					}
					db.batchInsert(dd);

					database_save();
				}
				else
				{
					System.out.println(" -- Loaded through Cache DB -- ");
					List<DependencyData> entries = db.getByKey(key);

					for (DependencyData dd : entries)
					{
						System.out.println(" - " + getDependencyKey(dd));
					}
				}
				// exit
			}
			break;
			case SCAN_RANGE:
			{
				// database_init();
				Pom pom = new Pom(holder, coordinate);
				pom.resolveVersionRangesOnline();
			}
			break;
			case SNIFF:
			{
				Pom pom = new Pom(holder, coordinate);
				List<String> versions = pom.findVersions();

				List<String> latestVersions = pom.getLatestVersions();
				String latestVersion = pom.getLatestVersion();
				String currentVersion = pom.getVersion();

				int olderVersionCount = 0;
				ArrayList<String> recommended = new ArrayList<>();
				for (String version : versions)
				{
					boolean isCurrentVersion = VersionComparator.isSame(currentVersion, version);
					boolean isSameOrNewer = VersionComparator.isSameOrNewer(currentVersion, version);
					boolean isLatestPatch = latestVersions.contains(version);
					boolean isNewest = VersionComparator.isSame(latestVersion, version);

					StringJoiner tags = new StringJoiner(", ");
					if (isCurrentVersion)
						tags.add("current");
					if (isLatestPatch)
					{
						recommended.add(version);
						tags.add("recommended");
					}
					if (isNewest)
						tags.add("latest");

					if (!isSameOrNewer)
					{
						olderVersionCount++;
						continue;
					}

					if (isCurrentVersion)
						System.out.println(" = Skipped " + olderVersionCount + " Older Versions =");

					System.out.println(" - " + version + (tags.length() > 0 ? (" (" + tags + ")") : "")); // - <version> | - <version> (<tags>)
				}
				if (!Params.didConfirm)
				{
					System.out.println(" Found " + recommended.size() + " Recommendations!");
					System.out.println(" " + String.join(" / ", recommended));
					System.out.println(" ");
					System.out.println(" To Index all Recommendations, use the same command and append '--yes'.");
					break;
				}
				System.out.println(" ============ Indexing " + recommended.size() + " Recommendations ============ ");

				database_init();
				ArrayList<DependencyData> batchInsert = new ArrayList<>();
				for (String recVersion : recommended)
				{
					pom.setArtifactVersion(recVersion);

					String key = db.createKey(pom.getKey());
					// System.out.println("[Debug] Checking key -> " + key + " for version " + recVersion);
					if (!db.hasKey(key))
					{
						System.out.println("[Info] Loading '" + key + "'!");
						Model model = pom.getPom();

						for (Dependency dep : model.getDependencies())
						{
							DependencyData data = fromFull(model, dep);
							batchInsert.add(data);
						}
					}
					else
					{
						System.out.println("[Info] Skipping Recommendations " + key + " due to index already exists!");
					}
				}

				System.out.println("[Info] Inserting Recommendations of " + batchInsert.size() + " Direct Dependencies!");
				db.batchInsert(batchInsert);

				database_save();
			}
			break;
		}
	} */

	private static void database_init() throws Exception
	{
		db = new CsvDatabase<>(DependencyData.class, new MavenCoordinateIndex());

		db.load(DB_FILE, ",");
	}

	private static void database_save() throws Exception
	{
		db.store(DB_FILE, ",");
	}

	public static DependencyData fromFull(Model model, Dependency dep)
	{
		return new DependencyData(model.getGroupId(), model.getArtifactId(), model.getPackaging(), "", model.getVersion(), dep.getGroupId(),
				dep.getArtifactId(), dep.getType(), dep.getClassifier(), dep.getScope(), dep.getVersion(), dep.getOptional(),
				String.join(".", dep.getExclusions().stream().map(AnalyzerApp::getKey).toList()));
	}

	public static String getKey(Dependency dependency)
	{
		String out = "";
		out += dependency.getGroupId() + ":" + dependency.getArtifactId();

		if (dependency.getType() != null)
		{
			out += ":" + dependency.getType();
			if (dependency.getClassifier() != null)
			{
				out += ":" + dependency.getClassifier();
			}
		}

		out += ":" + dependency.getVersion();
		return out;
	}

	public static String getDependencyKey(DependencyData dependency)
	{
		String out = "";
		out += dependency.depGroupId() + ":" + dependency.depArtifactId();

		if (dependency.depType() != null)
		{
			out += ":" + dependency.depType();
			if (dependency.depClassifier() != null)
			{
				out += ":" + dependency.depClassifier();
			}
		}

		out += ":" + dependency.depVersion();
		return out;
	}

	public static String getKey(Exclusion dependency)
	{
		return dependency.getGroupId() + ":" + dependency.getArtifactId();
	}

	@Deprecated
	public static void getVersion(String pom, String id) throws Exception
	{
		FileReader reader = new FileReader(pom);

		MavenXpp3Reader xpp3Reader = new MavenXpp3Reader();
		Model model = xpp3Reader.read(reader);

		for (Dependency dep : model.getDependencies())
		{
			System.out.println(dep.getGroupId() + ":" + dep.getArtifactId() + ":" + dep.getVersion() + " (" + dep.getClassifier() + "," + dep.getScope() + ")");
		}
	}
}