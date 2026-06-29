package ch.ensnert.impl;

import ch.ensnert.api.Index;
import ch.ensnert.impl.data.LinkKey;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;


/**
 * Node : an Artifact Key or an Dependency Key. (can be both) <br/>
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

	public void addVersion(String source, String sourceVersion, String target, String targetVersion)
	{
		LinkKey e = new LinkKey(source, target);
		relations.add(e);

		Index artifactKey = new Index(source, sourceVersion);
		Integer artifactIndex = index.get(artifactKey);

		Index dependencyKey = new Index(target, targetVersion);
		Integer dependencyIndex = index.get(dependencyKey);

		if (artifactIndex != null && dependencyIndex != null)
		{
			// 2 different indexes hit! we need to merge the instances...
			versions.compute(artifactIndex, (index, entry) -> entry != null ? entry : new HashMap<>())
					.putAll(Map.of(source, sourceVersion, target, targetVersion));

			mergeIndexes(artifactIndex, dependencyIndex);
		}
		else if (artifactIndex != null)
		{
			// the Artifact was found! add the Dependency as entry
			versions.compute(artifactIndex, (index, entry) -> entry != null ? entry : new HashMap<>()).put(target, targetVersion);
			index.put(dependencyKey, artifactIndex);
			if (!nodes.contains(target))
				nodes.add(target);
		}
		else if (dependencyIndex != null)
		{
			// the Dependency was found! add the Artifac as entry
			versions.compute(dependencyIndex, (index, entry) -> entry != null ? entry : new HashMap<>()).put(source, sourceVersion);
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
