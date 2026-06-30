package ch.ensnert.impl.database.types;

import ch.ensnert.api.database.Column;


public record DependencyData(
		// @formatter:off
		@Column(value = "groupId", id = 0, index = true) String groupId,
		@Column(value = "artifactId", id = 1, index = true) String artifactId,
		@Column(value = "type", id = 2, index = true) String type,
		@Column(value = "classifier", id = 3, index = true) String classifier,
		@Column(value = "version", id = 4, index = true) String version,
		@Column(value = "depGroupId", id = 5) String depGroupId,
		@Column(value = "depArtifactId", id = 6) String depArtifactId,
		@Column(value = "depType", id = 7) String depType,
		@Column(value = "depClassifier", id = 8) String depClassifier,
		@Column(value = "depScope", id = 9) String depScope,
		@Column(value = "depVersion", id = 10) String depVersion,
		@Column(value = "depOptional", id = 11) String depOptional,
		@Column(value = "depExclusionList", id = 12) String depExclusionList)
	// @formatter:on
{
	public String getKeyArtifact()
	{
		String out = "";
		out += groupId() + ":" + artifactId();

		if (type() != null)
		{
			out += ":" + type();
			if (classifier() != null)
			{
				out += ":" + classifier();
			}
		}

		out += ":" + version();
		return out;
	}

	public String getKeyDependency()
	{
		String out = "";
		out += depGroupId() + ":" + depArtifactId();

		if (depType() != null)
		{
			out += ":" + depType();
			if (depClassifier() != null)
			{
				out += ":" + depClassifier();
			}
		}

		out += ":" + depVersion();
		return out;
	}
}
