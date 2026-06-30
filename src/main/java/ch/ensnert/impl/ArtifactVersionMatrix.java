package ch.ensnert.impl;

import ch.ensnert.api.Index;
import ch.ensnert.impl.data.LinkKey;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;


/**
 * Node : an Artifact Key or an Dependency Key. (can be both) <br/>
 *
 * @author ensnerT (2026) - no AI was used
 */
public final class ArtifactVersionMatrix
{
	/// { node } ;; yes, i could use a Set, but the set will lose its order given from the Commandline.
	ArrayList<String> nodes = new ArrayList<>();

	/// { { artifact, dependency } }
	HashSet<LinkKey> relations = new HashSet<>();

	/// { Index(node, nodeVersion), hashIndex }
	HashMap<Index, Integer> index = new HashMap<>();
	/// { hashIndex, Map {node, version} }
	public HashMap<Integer, HashMap<String, String>> versions = new HashMap<>();

	Integer lastIndex = 0;

	public void notifyRelation(String artifact, String dependency)
	{
		relations.add(new LinkKey(artifact, dependency));
	}

	public void notifyRelation(LinkKey relation)
	{
		relations.add(relation);
	}

	public Set<LinkKey> getRelations()
	{
		return new HashSet<>(relations);
	}

	public void addNodes(String... nodes)
	{
		for (String node : nodes)
		{
			if (!this.nodes.contains(node))
			{
				this.nodes.add(node);
			}
		}
	}

	/**
	 * Asumption: the versions are added by groups of "source".
	 *
	 * @param source
	 * @param sourceVersion
	 * @param target
	 * @param targetVersion
	 */
	public void addVersion(String source, String sourceVersion, String target, String targetVersion)
	{
		/**
		 * todo
		 *  problem:
		 *   Version A:1-3 uses B:1, this currently is only displayed as A:3 uses B:1 since B:1 is a backwards reference key.
		 *  idea:
		 *   If the versions added is by group, you can tell each Artifact distinct while the Dependencies are Dupplicates (lists of indexes)
		 *   therefore inserting for dependency of a version, the index list needs to be expanded and then each item will be inserted.
		 *  for addition:
		 *   If the artifact is not yet existing, but the dependency is, check if that dependency has the current artifact already set.
		 *   If it does, copy the whole entry and overwrite the current artifact. Also modify the indexes refering to the OLD entry to also point to the current, except if the key is the current artifact.
		 *  for merging: if there is a entry above and below who have multiple links: copy from both sides
		 *   example: A1 [1-99] uses B2 [1-10] and B2[11-20] then i come as B2 [1-10] = C3[1-10] and B2[11-20] = C4[11-20]
		 *  issue:
		 *   can a dependenyVersion be in the same matrix twice? yes! A:1.0.0 and A:1.0.1 uses B:1.0.0
		 *   can a artifactVersion be in the same matrix twice? yes! since B might be a dependency of artifact A andusing it in 2 different versions.
		 *   can every line be in the same matrix twice? no! since at least one version must have changed, to be an entry.
		 *   does every matrix line need to connect to at least 1 other node? no! it may be, that A:1.0.0 to A:1.2.9 uses B:* but higher is the version not used.
		 *   -> currently i search for nodes that have artifact and dependency searched for, so A:1.2.10 will not be shown anymore, IF there is nothing connecting from or to A
		 *   i could try to do 2 Lists: 1st list is intended for artifacts, 2nd list is for dependency; if a artifact matches an other artifact, it binds. if it finds a dependency, it matches. and voila, a list of dependents.
		 *   !! new Problem : A and Z do not know eachother, yet A:1-3 uses B:1 and Z:1-4 uses B:1. How do i display that? A does not match with Z with a unique version.
		 * */

		LinkKey e = new LinkKey(source, target);
		relations.add(e);

		Index artifactKey = new Index(source, sourceVersion);
		Integer artifactIndex = index.get(artifactKey);

		Index dependencyKey = new Index(target, targetVersion);
		Integer dependencyIndex = index.get(dependencyKey);

		if (artifactIndex != null && dependencyIndex != null)
		{
			// 2 different indexes hit! we need to merge the instances...
			HashMap<String, String> compute = versions.compute(artifactIndex, (index, entry) -> entry != null ? entry : new HashMap<>());
			if (!compute.getOrDefault(source, "").equals(sourceVersion) || !compute.getOrDefault(target, "").equals(targetVersion))
			{
				Output.verbose("WARNING! Unmatching entries found! %s:%s to %s or %s:%s to %s", source, sourceVersion,
						compute.getOrDefault(source, sourceVersion), target, targetVersion, compute.getOrDefault(target, targetVersion));
			}

			compute.putAll(Map.of(source, sourceVersion, target, targetVersion));

			mergeIndexes(artifactIndex, dependencyIndex);
		}
		else if (artifactIndex != null)
		{
			// the Artifact was found! add the Dependency as entry
			HashMap<String, String> compute = versions.compute(artifactIndex, (index, entry) -> entry != null ? entry : new HashMap<>());
			if (!compute.getOrDefault(target, "").isEmpty())
			{
				Output.verbose("WARNING A! Dupplicate entry found for %s:%s at %s",target, targetVersion, artifactKey);
			}
			compute.put(target, targetVersion);
			index.put(dependencyKey, artifactIndex);
			if (!nodes.contains(target))
				nodes.add(target);
		}
		else if (dependencyIndex != null)
		{
			// the Dependency was found! add the Artifac as entry
			HashMap<String, String> compute = versions.compute(dependencyIndex, (index, entry) -> entry != null ? entry : new HashMap<>());
			if (!compute.getOrDefault(source, "").isEmpty())
			{
				Output.verbose("WARNING B! Dupplicate entry found for %s:%s at %s",source, sourceVersion, dependencyKey);
			}
			compute.put(source, sourceVersion);
			index.put(artifactKey, dependencyIndex);
			if (!nodes.contains(source))
				nodes.add(source);
		}
		else
		{
			// neither the dependency nor the artifact was found. Add both keys and create a new version entry.
			Integer id = lastIndex++;
			versions.compute(id, (index, entry) -> new HashMap<>()).putAll(Map.of(source, sourceVersion, target, targetVersion));
			index.put(artifactKey, id);
			if (!nodes.contains(source))
				nodes.add(source);
			index.put(dependencyKey, id);
			if (!nodes.contains(target))
				nodes.add(target);
		}
	}

	private void mergeIndexes(Integer source, Integer target)
	{
		if (Objects.equals(source, target))
			return;

		ArrayList<Index> updateIndexes = new ArrayList<>();
		for (Map.Entry<Index, Integer> indexIntegerEntry : index.entrySet())
		{
			if (Objects.equals(indexIntegerEntry.getValue(), target))
			{
				updateIndexes.add(indexIntegerEntry.getKey());
			}
		}
		for (Index updateIndex : updateIndexes)
		{
			index.put(updateIndex, source);
		}
		versions.get(source).putAll(versions.remove(target));
	}

	/**
	 * @param keepPartialVersions {@code true} to keep nodes, that dont link to every version
	 * @return String{ [headers], [versions...] }
	 */
	public String[][] makeView(boolean keepPartialVersions)
	{
		ArrayList<String> keys = new ArrayList<>(nodes);
		ArrayList<String[]> result = new ArrayList<>();
		result.add(keys.toArray(new String[] {})); /// add headers

		ArrayList<VersionRow> rows = new ArrayList<>();

		skipPartial:
		for (HashMap<String, String> value : versions.values())
		{
			VersionRow row = new VersionRow();
			for (String key : keys)
			{
				String entry = value.getOrDefault(key, "");
				if (entry.isEmpty() && !keepPartialVersions)
					continue skipPartial;
				row.add(entry);
			}
			rows.add(row);
		}
		rows.sort(VersionRow::compareTo);
		for (VersionRow row : rows)
			result.add(row.toArray(new String[] {}));

		return result.toArray(new String[0][]);
	}

	public static void main(String[] args)
	{
		HashMap<String, HashMap<String, String>> data = new HashMap<>();

		data.compute("azure-identity", (r, s) -> s == null ? new HashMap<>() : s).put("1.0.13", "14.0.2");

		String theAzureIdentityVersion = data.get("azure-identity").get("1.0.13");

		ArtifactVersionMatrix matrix = new ArtifactVersionMatrix();
		matrix.addVersion("azure-identity", "1.0.13", "azure-core", "14.0.2");

	}

	private static final class VersionRow extends ArrayList<String> implements Comparable<VersionRow>
	{
		@Override
		public int compareTo(VersionRow o)
		{
			if (o == null)
			{
				return -1;
			}
			// then just compare the first non empty entry
			int min = Math.min(size(), o.size());
			for (int i = 0; i < min; i++)
			{
				if (get(i).isEmpty() || o.get(i).isEmpty())
					continue;

				return VersionComparator.compare(get(i), o.get(i));
			}

			return 0;
		}
	}
}
