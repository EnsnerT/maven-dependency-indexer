package ch.ensnert.impl;

import ch.ensnert.api.Index;
import ch.ensnert.api.Matrix;
import ch.ensnert.api.Table;
import ch.ensnert.impl.data.LinkKey;
import ch.ensnert.impl.data.VersionKey;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Node : an Artifact Key or an Dependency Key. (can be both) <br/>
 * Version 2 2026-07-02
 *
 * @author ensnerT (2026) - no AI was used
 */
public final class ArtifactVersionMatrix implements Matrix
{
	/// { node } ;; yes, i could use a Set, but the set will lose its order given from the Commandline.
	ArrayList<String> nodes = new ArrayList<>();

	/// { { artifact, dependency } }
	HashSet<LinkKey> relations = new HashSet<>();

	/// for { Map { masterKey , X } }, X must be the highest artifact.
	private static final String MASTER = "@";

	/// { Index(node, nodeVersion), List { hashIndex } }
	public final HashMap<Index, List<Integer>> index = new HashMap<>();
	/// { hashIndex, Map {node, version} }
	public final HashMap<Integer, HashMap<String, String>> versions = new HashMap<>();

	Integer lastIndex = 0;

	@Override
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

	@Override
	public Set<LinkKey> getRelations()
	{
		return new HashSet<>(relations);
	}

	/**
	 * @param keepPartialVersions {@code true} to keep nodes, that dont link to every version
	 * @return String{ [headers], [versions...] }
	 */
	@Override
	public String[][] makeView(boolean keepPartialVersions)
	{
		ArrayList<String> keys = new ArrayList<>(nodes);
		ArrayList<String[]> result = new ArrayList<>();
		result.add(keys.toArray(new String[] {})); /// add headers

		ArrayList<MatrixVersionRow> rows = new ArrayList<>();

		skipPartial:
		for (HashMap<String, String> value : versions.values())
		{
			MatrixVersionRow row = new MatrixVersionRow();
			for (String key : keys)
			{
				String entry = value.getOrDefault(key, "");
				if (entry.isEmpty() && !keepPartialVersions)
					continue skipPartial;
				row.add(entry);
			}
			rows.add(row);
		}
		rows.sort(MatrixVersionRow::compareTo);
		for (MatrixVersionRow row : rows)
			result.add(row.toArray(new String[] {}));

		return result.toArray(new String[0][]);
	}

	// region Index Creations

	/**
	 * Asumption: the versions are added by groups of "source".
	 *
	 */
	@Override
	public void addVersion(String source, String sourceVersion, String target, String targetVersion)
	{
		/*
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

		relations.add(new LinkKey(source, target));

		Index artifactKey = new Index(source, sourceVersion);
		List<Integer> artifactIndex = index.get(artifactKey);

		Index dependencyKey = new Index(target, targetVersion);
		List<Integer> dependencyIndex = index.get(dependencyKey);

		if (isNotEmpty(artifactIndex) && isNotEmpty(dependencyIndex))
		{
			artifactAndDependencyAreIndexed(source, sourceVersion, target, targetVersion);
		}
		else if (isNotEmpty(artifactIndex))
		{
			// case: there is a mapping for artifact only (case: index > this)
			// -> you want every entry to be
			// this entry can not expand children
			artifactIsIndexed(source, sourceVersion, target, targetVersion);

		}
		else if (isNotEmpty(dependencyIndex))
		{
			// case: there is a mapping for dependency only (case: this > index)
			// this entry can expand the children.
			dependencyIsIndexed(source, sourceVersion, target, targetVersion);
		}
		else
		{
			// case: there is no mapping for this yet.
			/// -> create complete new entry
			neitherIsIndexed(source, sourceVersion, target, targetVersion);
		}
	}

	private void neitherIsIndexed(String source, String sourceVersion, String target, String targetVersion)
	{
		Index art = new VersionKey(source, sourceVersion).asIndex();
		Index dep = new VersionKey(target, targetVersion).asIndex();
		Integer indexEntry = createIndexEntry();
		HashMap<String, String> map = versions.get(indexEntry);
		map.put(source, sourceVersion);
		map.put(target, targetVersion);
		setMaster(indexEntry, List.of(source));
		addIndex(art, indexEntry);
		addIndex(dep, indexEntry);
	}

	private void artifactIsIndexed(String source, String sourceVersion, String target, String targetVersion)
	{
		Index art = new VersionKey(source, sourceVersion).asIndex();
		Index dep = new VersionKey(target, targetVersion).asIndex();

		ArrayList<Integer> indexes = new ArrayList<>(Set.copyOf(index.get(art)));
		indexes.sort(Integer::compareTo);
		for (Integer i : indexes)
		{
			versions.get(i).put(target, targetVersion);
		}

		index.put(dep, indexes);
		// @ must not be updated, since the @ does not change for {?->A->B} {?,A,B}[@?]
	}

	private void dependencyIsIndexed(String source, String sourceVersion, String target, String targetVersion)
	{
		Index art = new VersionKey(source, sourceVersion).asIndex();
		Index dep = new VersionKey(target, targetVersion).asIndex();

		ArrayList<Integer> indexes = new ArrayList<>(Set.copyOf(index.get(dep)));
		indexes.sort(Integer::compareTo);

		// Output.verbose("Start Idexes: " + versions.size());
		boolean foundEmptyInstance = false;
		Integer sampleFoundFullInstance = null;

		// if you find {?,B} -> if A is not present : add A to it (flag run as FOUNDEMPTYINSTANCE); if A is present: wait for the end and check if flag FOUNDEMPTYINSTANCE is false, if it is, add a new entry.

		for (Integer i : indexes) // every distinct instance of
		{
			HashMap<String, String> map = versions.get(i);
			// test if not already set, If set, copy it!
			List<String> master = getMaster(i);
			if (map.containsKey(source)) // the presearcher would have picked a different method, if the versions are the same.
			{
				if (master.contains(source) || sampleFoundFullInstance == null)
					sampleFoundFullInstance = i;
			}
			else
			{
				foundEmptyInstance = true;
				if (master.contains(target))
				{
					// @ is B - change it to A
					ArrayList<String> newMaster = new ArrayList<>(master);
					newMaster.set(newMaster.indexOf(target), source);
					setMaster(i, newMaster);
				}
				map.put(source, sourceVersion); // add A
				addIndex(art, i);
			}
		}

		if (!foundEmptyInstance)
		{
			Map<String, String> map = getByIndex(sampleFoundFullInstance);
			List<String> sampleMaster = getMaster(sampleFoundFullInstance);
			if (sampleMaster.contains(source))
			{
				// @ is A already - keep it - dupplicate the entry
				Integer nextIndexEntry = createIndexEntry();
				Map<String, String> newMap = getByIndex(nextIndexEntry);

				newMap.putAll(map); // copy the entry and change the A
				newMap.put(source, sourceVersion); // overwrite A

				createIndexesFromNewEntry(nextIndexEntry);
			}
			else
			{
				// @ is unknown - keep it - merge the info
				map.merge(source, "," + sourceVersion, String::concat); // join A1,A2
				addIndex(art, sampleFoundFullInstance);
			}
		}

		// Output.verbose("End Idexes: " + versions.size() + " = " + String.join(", ", versions.keySet().stream().map(String::valueOf).toList()));

	}

	private void artifactAndDependencyAreIndexed(String source, String sourceVersion, String target, String targetVersion)
	{
		Index art = new VersionKey(source, sourceVersion).asIndex();
		Index dep = new VersionKey(target, targetVersion).asIndex();

		Set<Integer> dropableIndexes = new HashSet<>();

		// 1. create temporary copy of found indexes.
		List<Integer> artIndexes = new ArrayList<>(Set.copyOf(index.get(art)));
		artIndexes.sort(Integer::compareTo);
		List<Integer> depIndexes = new ArrayList<>(Set.copyOf(index.get(dep)));
		depIndexes.sort(Integer::compareTo);

		// 2. iterate through ?->B indexes
		for (Integer i : depIndexes)
		{
			Map<String, String> map = getByIndex(i);
			ArrayList<String> s = extractVersionsFromMap(map, source);

			if (isNotEmpty(s) && s.contains(sourceVersion))
			{
				// dupplicate - remove it - skip it!
				artIndexes.remove(i);
				continue;
			}

			// is checked later...
			Output.verbose("Checking later %s -> %s.", source + ":" + sourceVersion, target + ":" + targetVersion);
			// here is my issue:
			//  dep: {A1,B2}, {A2,B2}, {A3,B2} but what i need to know is, IS there always "A*" and is there a "A1" (sourceVersion) present at all?
			//
		}

		/// if {art}@? == A then #{1} else #{1,n}
		// expectable: {Z1,A1}@Z, {Z2,A1}@Z, {Z3,A1}@Z
		for (Integer i : artIndexes)
		{
			Map<String, String> artMap = getByIndex(i);
			ArrayList<String> s = extractVersionsFromMap(artMap, target);

			// there is an instance of b
			if (isNotEmpty(s) && s.contains(targetVersion))
			{
				// should not happen
				Output.verbose("Index has been found to be broken! %1$s -> %2$s; %2$s is not linked back to %1$s!", source + ":" + sourceVersion,
						target + ":" + targetVersion);
				// is the same - skip it!
				continue;
			}

			// The merge only appends. it does not modify old entries. so you need to delete the old entries afterwards and recalculate the indexes for these parts again.
			dropableIndexes.add(i);

			// Output.verbose("Joined %d dependent by %d artifacts with the usage of %s:%s->%s:%s!", depIndexes.size(), artIndexes.size(), source,
			// 		sourceVersion, target, targetVersion);

			// ok now the master merge part!
			if (depIndexes.size() * artIndexes.size() > 100)
			{
				Output.error("Data size Exploded!");

				Output.error("Joined %d dependent by %d artifacts with the usage of %s:%s->%s:%s!", depIndexes.size(), artIndexes.size(), source,
						sourceVersion, target, targetVersion);
				this.printState();

				break;
			}
			// todo : merge {A,B}@A + {C,D}@C where B->C ! current output: {A,B,C,D}@A+C but just A needs to stay, since @C gets pushed upwards
			// todo : merge {A,B1+2}@A + {C,D1+2} where B1 -> C

			/// if {dep}@? == B then #{1} else #{1,n}
			// expectable: {Y1,B1}@Y, {Y2,B1}@Y, {Y3,B1}@Y
			for (Integer j : depIndexes) // can be {1,n} but only if {dep}@? != B, else {1}  B@B
			{
				dropableIndexes.add(j);

				Map<String, String> depMap = getByIndex(j);

				HashMap<String, String> resultMap = new HashMap<>(artMap);
				resultMap.putAll(depMap);

				List<String> master = getMaster(artMap);
				master.addAll(getMaster(depMap));
				setMaster(resultMap, new ArrayList<>(Set.copyOf(master)));

				Integer indexEntry = createIndexEntry();
				versions.put(indexEntry, resultMap);
				createIndexesFromNewEntry(indexEntry);

				/// artMasters == i == @A+p1+p2 ( {Z1,A1,P1}@Z+P, {Z2,A1,P1}@Z+P, {Z3,A1,P2}@Z+P )
				/// depMasters == j == @B+q1+q2 ( {Y1,B1,Q2}@Y+Q, {Y2,B1,Q3}@Y+Q, {Y3,B1,Q3}@Y+Q )
				/* results:
				- A is higher, but does not have @ A -> copy A, overlay with B, merge the @ list
				- {Z1,A1,P1}@Z+P ++ {Y1,B1,Q2}@Y+Q -> {Z1,A1,P1,Y1,B1,Q2}@Z+P+Y+Q
				- {Z1,A1,P1}@Z+P ++ {Y2,B1,Q3}@Y+Q -> {Z1,A1,P1,Y2,B1,Q3}@Z+P+Y+Q
				- {Z1,A1,P1}@Z+P ++ {Y3,B1,Q3}@Y+Q -> {Z1,A1,P1,Y3,B1,Q3}@Z+P+Y+Q

				- {Z2,A1,P1}@Z+P ++ {Y1,B1,Q2}@Y+Q -> {Z2,A1,P1,Y1,B1,Q2}@Z+P+Y+Q
				- {Z2,A1,P1}@Z+P ++ {Y2,B1,Q3}@Y+Q -> {Z2,A1,P1,Y2,B1,Q3}@Z+P+Y+Q
				- {Z2,A1,P1}@Z+P ++ {Y3,B1,Q3}@Y+Q -> {Z2,A1,P1,Y3,B1,Q3}@Z+P+Y+Q

				- {Z3,A1,P2}@Z+P ++ {Y1,B1,Q2}@Y+Q -> {Z3,A1,P2,Y1,B1,Q2}@Z+P+Y+Q
				- {Z3,A1,P2}@Z+P ++ {Y2,B1,Q3}@Y+Q -> {Z3,A1,P2,Y2,B1,Q3}@Z+P+Y+Q
				- {Z3,A1,P2}@Z+P ++ {Y3,B1,Q3}@Y+Q -> {Z3,A1,P2,Y3,B1,Q3}@Z+P+Y+Q

				- A is higher, and A is @ -> copy A, overlay B, merge
				*/

			}
		}
		dropableIndexes.forEach(this::dropAndCleanupIndex);
	}

	// endregion Index Creations

	// region Verbose Utility

	public void printState()
	{
		Output.verbose("----------------------------------------");
		HashMap<Integer, ArrayList<String>> v = new HashMap<>();

		for (Map.Entry<Index, List<Integer>> e : index.entrySet())
		{
			for (Integer i : e.getValue())
			{
				v.compute(i, (k, v1) -> v1 != null ? v1 : new ArrayList<>()).add(e.getKey().toString());
			}
		}
		Table indexTable = new Table(2);
		indexTable.addCol("Id").addCol("Indexes").endRow();
		for (Map.Entry<Integer, ArrayList<String>> e : v.entrySet())
		{
			indexTable.addCol(String.valueOf(e.getKey())).addCol(String.join(",", e.getValue())).endRow();
		}
		Output.verbose(Table.TableConfig.TAB_STOPS().render(indexTable, Table.TableConfig.OutlineType.COLUMNS));

		v.clear();
		Table versionTable = new Table(2);
		versionTable.addCol("Id").addCol("Versions").endRow();
		for (Map.Entry<Integer, HashMap<String, String>> e : versions.entrySet())
		{
			for (Map.Entry<String, String> i : e.getValue().entrySet())
			{
				v.compute(e.getKey(), (k, v1) -> v1 != null ? v1 : new ArrayList<>()).add(i.getKey() + ":" + i.getValue());
			}
		}
		for (Map.Entry<Integer, ArrayList<String>> e : v.entrySet())
		{
			versionTable.addCol(String.valueOf(e.getKey())).addCol(String.join(" ", e.getValue())).endRow();
		}
		Output.verbose(Table.TableConfig.TAB_STOPS().render(versionTable, Table.TableConfig.OutlineType.COLUMNS));
	}
	// endregion Verbose Utility

	// region Utility

	private static boolean isNotEmpty(List<?> list)
	{
		return list != null && !list.isEmpty();
	}

	private static boolean isEmpty(String chars)
	{
		return chars == null || chars.isEmpty();
	}

	private static ArrayList<String> extractVersionsFromMap(Map<String, String> map, String source)
	{
		String ss = map.get(source);
		ArrayList<String> s;
		if (isEmpty(ss))
			s = new ArrayList<>();
		else
			s = new ArrayList<>(Set.of(ss.split(",")));
		return s;
	}

	private void createIndexesFromNewEntry(Integer target)
	{
		for (Map.Entry<String, String> entry : getByIndex(target).entrySet())
		{
			String key = entry.getKey();
			String value = entry.getValue();
			if (MASTER.equals(key))
				continue;
			if (value.contains(","))
				for (String s : value.split(","))
				{
					addIndex(new VersionKey(key, s).asIndex(), target);
				}
			else
				addIndex(new VersionKey(key, value).asIndex(), target);
		}
	}

	private void addIndex(Index art, Integer indexEntry)
	{
		index.compute(art, (i, o) -> o != null ? o : new ArrayList<>()).add(indexEntry);
	}

	private void dropAndCleanupIndex(Integer indexEntry)
	{
		ArrayList<Index> droppableIndexes = new ArrayList<>();
		index.forEach((k, v) ->
		{
			if (v.remove(indexEntry))
				if (v.isEmpty())
					droppableIndexes.add(k);
		});
		for (Index emptyIndex : droppableIndexes)
			index.remove(emptyIndex);
		versions.remove(indexEntry);
	}

	private Map<String, String> getByIndex(Integer index)
	{
		return versions.compute(index, (i, o) -> o != null ? o : new HashMap<>());
	}

	private Integer createIndexEntry()
	{
		Integer version = lastIndex++;
		versions.put(version, new HashMap<>());
		return version;
	}

	private List<String> getMaster(Integer indexEntry)
	{
		if (versions.containsKey(indexEntry))
			return getMaster(versions.get(indexEntry));
		return new ArrayList<>();
	}

	private List<String> getMaster(Map<String, String> map)
	{
		if (map.containsKey(MASTER))
			return new ArrayList<>(Arrays.asList(map.get(MASTER).split(",")));
		return new ArrayList<>();
	}

	private void setMaster(Integer indexEntry, List<String> masters)
	{
		if (versions.containsKey(indexEntry))
			setMaster(versions.get(indexEntry), masters);
	}

	private void setMaster(Map<String, String> map, List<String> masters)
	{
		map.put(MASTER, String.join(",", masters));
	}

	// endregion Utility

	@Override
	@SuppressWarnings("MethodDoesntCallSuperMethod")
	public Object clone()
	{
		ArtifactVersionMatrix clone = new ArtifactVersionMatrix();
		index.forEach((k, v) -> clone.index.put(k, new ArrayList<>(v)));
		clone.relations = new HashSet<>(relations);
		versions.forEach((k, v) -> clone.versions.put(k, new HashMap<>(v)));
		clone.lastIndex = lastIndex;

		return clone;
	}
}
