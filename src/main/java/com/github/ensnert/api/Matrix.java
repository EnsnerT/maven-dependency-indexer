package com.github.ensnert.api;

import com.github.ensnert.impl.data.LinkKey;

import java.util.Set;


public interface Matrix
{
	Set<LinkKey> getRelations();

	/**
	 * @param nodes nodes and their order the output should be in. if left empty, the output is permitted to make its own choice
	 */
	void addNodes(String... nodes);

	/**
	 * Add a Relation between Artifact {com:A:1.0} and Dependency {com:B:1.0}
	 */
	void addVersion(String artifact, String artifactVersion, String dependency, String dependencyVersion);

	/**
	 * Create a Matrix View <br/>
	 * - {@code String[0][]} is always the headers of the matrix
	 *
	 */
	String[][] makeView(boolean keepPartialVersions);
}
