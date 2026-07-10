package com.github.ensnert.starter;

import com.github.ensnert.App;
import com.github.ensnert.api.Matrix;
import com.github.ensnert.api.Table;
import com.github.ensnert.api.database.csv.CsvDatabase;
import com.github.ensnert.impl.ArtifactVersionMatrix;
import com.github.ensnert.impl.Output;
import com.github.ensnert.impl.Pom;
import com.github.ensnert.impl.data.AnalyzedVersion;
import com.github.ensnert.impl.data.LinkKey;
import com.github.ensnert.impl.database.indexstrategies.MavenCoordinateIndex;
import com.github.ensnert.impl.database.types.DependencyData;
import com.github.ensnert.system.Holder;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;


/**
 * <h2>Application Manager</h2> <br/>
 * This class handles the whole Application. <br/>
 * All Commands go to {@link #run(String[])}. <br/>
 * Each type of command then splits into {@code command_{type}(Params)} to process. <br/>
 *
 * @author ensnerT (2026) - no AI was used
 */
@Named() // makes the class integrate into the plexus lifecycle
public final class AnalyzerApp implements App
{
	// region Fields

	public Holder holder;
	private static CsvDatabase<DependencyData> db;
	private static final String DB_FILE = "./db.csv";

	// endregion Fields

	// region Helper / Container / Classes

	private record Result(List<DependencyData> matchingAsArtifact, List<DependencyData> matchingAsDependency, List<DependencyData> allMatchings) {}

	public static final class Params
	{
		public Mode mode;
		public final ArrayList<String> coordinates = new ArrayList<>();
		public boolean moreThenNEntries;
		public boolean version_newer;
		public boolean version_older;
		public boolean version_snapshot;
		public boolean version_release;
		public boolean version_all;
		public boolean table_format_tab;
		public boolean table_nocolor;
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
			this.table_format_tab = false;
			this.table_nocolor = false;
		}

		// todo -> this could be modified to contain the command resolvers.
		public enum Mode
		{
			HELP,
			INDEX,
			VERSION,
			RANGE,
			FIND
		}
	}

	// endregion Helper / Container / Classes

	@Inject()
	public AnalyzerApp(Holder holder)
	{
		if (holder != null)
			this.holder = holder;
	}

	@SuppressWarnings({ "SuspiciousListRemoveInLoop", "RedundantCollectionOperation" })
	@Override
	public void run(String[] args)
	{
		// region Shorthand Expander

		/// this Area makes from '-abc' the expanded command '-a -b -c'
		if (args != null && args.length > 0)
		{
			ArrayList<String> preArgs = new ArrayList<>(args.length);
			preArgs.addAll(Arrays.asList(args));

			for (int i = 0; i < preArgs.size(); i++)
			{
				String s = preArgs.get(i);
				/// is '-ab' or longer but not '--ab' or 'ab'
				if (s.startsWith("-") && !s.startsWith("--") && s.length() > 2)
				{
					preArgs.remove(i); /// remove self, then add the others back

					for (char c : s.toCharArray())
						if ('a' <= c && c <= 'z' || 'A' <= c && c <= 'Z')
							preArgs.add(i, "-" + c);
				}
			}
			args = preArgs.toArray(String[]::new);
		}

		// endregion Shorthand Expander

		// region Parameter Parser
		Params runningParameter = new Params();

		if (args != null && args.length > 0)
		{
			runningParameter.originalArguments = args;
			int i = 0;
			while (i < args.length)
			{
				String arg = args[i++];
				// todo -> this could be modified to work with switch cases.
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
				else if (Set.of("--version", "version", "versions").contains(arg))
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
				else if (Set.of("--verbose", "-v").contains(arg))
				{
					Output.setVerbose(true);
				}
				else if (Set.of("--tab", "-t").contains(arg))
				{
					runningParameter.table_format_tab = true;
				}
				else if (Set.of("--no-color", "--no-colour", "-c").contains(arg))
				{
					runningParameter.table_nocolor = true;
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

		// endregion Parameter Parser

		switch (runningParameter.mode)
		{
			case HELP -> command_help(runningParameter);
			case INDEX -> command_index(runningParameter);
			case VERSION -> command_version(runningParameter);
			case RANGE -> command_range(runningParameter);
			case FIND -> command_find(runningParameter);
		}

	}

	// region Commands

	public void command_help(Params runningParameter)
	{
		Output.generic("Usage: ");

		if (runningParameter.mode == Params.Mode.HELP)
		{
			Output.generic("\t-h | help | * Show this help");
			Output.generic("\tindex -h | index help | * index an artifact");
			Output.generic("\tversion -h | version help | * show versions of an artifact");
			Output.generic("\trange -h | range help | * resolve and index a range of versions for an artifact");
			Output.generic("\tfind -h | find help | * scrape your indexed database to create a Version matrix");
			Output.generic("Generic Paramters: ");
			Output.generic("\t-v / --verbose | * Output more Context (or more help)");
			Output.generic("\t-t / --tab | * change the Table's formats to '\\t' separated");
			Output.generic("\t-c / --no-color / --no-colour | * prevent colorfull Output");
		}
		if (runningParameter.mode == Params.Mode.INDEX)
		{
			Output.generic("\tindex {artifact+version} | index a certain version of the artifact");
			Output.generic("\tindex {artifact+version} --all / -a | use all versions, not just the \"recommended\" (only with --new or --old)");
			Output.generic("\tindex {artifact+version} --new / -n | \"recommended\" the newest patches for each generation (x.y.z -> x.y is the generation)");
			Output.generic("\tindex {artifact+version} --old / -o | \"recommended\" the best patches before the {version} for each generation");
			Output.generic("\tindex {artifact+version} --shapshot / --snapshots / -s | only snapshots in the versions ; overwrites release");
			Output.generic("\tindex {artifact+version} --release / --releases / -r | include release versions (only with -s -r)");
			Output.generic("\t * you could remember the arguments as \"sonar\" Shapshot, Old, New, All, Release");
			Output.generic("\tindex {artifact+version} {...} version {...} | will turn the command from an 'index' to an 'version'.");
			Output.generic("Formats: ");
			Output.generic("\t{artifact} = {groupId}:{artifactId} - Example: 'com.any:alpha'");
			Output.generic("\t{artifact+version} = {groupId}:{artifactId}:{version} - Example: 'com.any:alpha:1.0.0'");
		}

		if (runningParameter.mode == Params.Mode.VERSION)
		{
			Output.generic("\tversion {artifact} | show \"recommended\" versions");
			Output.generic("\tversion {artifact} --all / -a | show all versions");
			Output.generic("\tversion {artifact+version} --new / -n | show newer versions than {version}");
			Output.generic("\tversion {artifact+version} --old / -n | show older versions than {version}");
			Output.generic("\tversion {artifact+version} --shapshot / --snapshots / -s | only snapshots in the versions ; excludes releases");
			Output.generic("\tversion {artifact+version} --release / --releases / -r | include release versions (only with -s -r)");
			Output.generic("\t * you could remember the arguments as \"sonar\" Shapshot, Old, New, All, Release");
			Output.generic("\tversion {artifact+version} {...} index {...} | will turn the command from an 'version' to an 'index'.");
			Output.generic("Formats: ");
			Output.generic("\t{artifact} = {groupId}:{artifactId} - Example: 'com.any:alpha'");
			Output.generic("\t{artifact+version} = {groupId}:{artifactId}:{version} - Example: 'com.any:alpha:1.0.0'");
		}

		if (runningParameter.mode == Params.Mode.RANGE)
		{
			Output.generic(" * this command is not yet implemented! ");
		}

		if (runningParameter.mode == Params.Mode.FIND)
		{
			Output.generic("\tfind {artifact*} | show all indexes by given key");
			Output.generic("\tfind {artifact} {artifact} | show intersecting versions of given artifacts");
			Output.generic("\tfind {artifact} {artifact} -a | show all versions of given artifacts");
			Output.generic("\tfind {artifact} {artifact+version} -a | search for a specific version of an artifact");
			Output.generic("Formats: ");
			Output.generic("\t{artifact} = {groupId}:{artifactId} - Example: 'com.any:alpha'");
			Output.generic("\t{artifact*} = *:{artifactId} - Example: '*:alpha'");
			Output.generic("\t{artifact+version} = {groupId}:{artifactId}:{version} - Example: 'com.any:alpha:1.0.0'");
			Output.generic("Filter Options: ");
			Output.generic("\t-T | exclude \"test\" scopes");
			Output.generic("\t-O | exclude optional dependencies");
			Output.verbose("Verbose Outputs (only visible with \"-v\": ");
			Output.verbose("\t---findings | output for each match, its complete found table [DEBUG] ");
			Output.verbose("\t---bonds | outputs the found relations ");
		}
	}

	public void command_index(Params runningParameter)
	{
		try
		{
			if (runningParameter.coordinates.isEmpty())
				throw new RuntimeException("Please specify a coordinate for operation!");

			database_init();

			for (String coordinate : runningParameter.coordinates)
			{
				// String coordinate = runningParameter.coordinates.get(0);
				Pom pom = new Pom(holder, coordinate);
				List<AnalyzedVersion> filteredVersions = applyParameteredFilter(runningParameter, pom);

				Output.verbose(pom.getCoordinate() + " -> ");

				Pom indexingPom = new Pom(holder, coordinate);

				for (AnalyzedVersion fv : filteredVersions)
				{
					String sj = fv.getAnnotations();

					if (!sj.isEmpty())
						Output.generic(" - Indexing : " + fv.version() + " (" + sj + ")");
					else
						Output.generic(" - Indexing : " + fv.version());

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

							Output.verbose(" \\ - Found " + dd.size() + " dependencies.");
							db.batchInsert(dd);
						}
						else
						{
							Output.verbose(" \\ -- Artifact Version : \"" + indexingPom.getVersion() + "\" already present in Database; Skiping indexing -- ");
						}
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

			Output.verbose(pom.getCoordinate() + " -> ");

			for (AnalyzedVersion fv : filteredVersions)
			{
				String sj = fv.getAnnotations();

				if (!sj.isEmpty())
					Output.generic(" - " + fv.version() + " (" + sj + ")");
				else
					Output.generic(" - " + fv.version());
			}
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings({"UnusedParameters"})
	public void command_range(Params runningParameter)
	{
		throw new UnsupportedOperationException("Command not yet implemented");
	}

	public void command_find(Params runningParameter)
	{
		Set<String> originalArguments = new HashSet<>(Set.of(runningParameter.originalArguments));
		originalArguments.retainAll(List.of("--no-test", "-T"));
		boolean hideTest = !originalArguments.isEmpty();
		originalArguments = new HashSet<>(Set.of(runningParameter.originalArguments));
		originalArguments.retainAll(List.of("--no-optional", "-O"));
		boolean hideOptional = !originalArguments.isEmpty();

		if (runningParameter.coordinates.size() == 1)
		{
			Table tableSimpleLookup = new Table(4);
			tableSimpleLookup.addCol("Artifact").addCol("Version").addCol("Dependency").addCol("Version").endRow();

			// if you find 1; show exactly this entry
			try
			{
				database_init();

				List<DependencyData> dataList = db.cloneData();
				Set<String> ignoredScopes = Set.of("test");

				Result result = searchDependencies(runningParameter.coordinates.get(0), dataList, hideTest, hideOptional);

				for (DependencyData data : result.matchingAsArtifact())
				{
					boolean isTest = ignoredScopes.contains(data.depScope());
					if (isTest && hideTest)
						continue;

					String value = data.groupId() + ":" + data.artifactId();
					if (runningParameter.table_nocolor)
					{
						tableSimpleLookup.addCol(value);
						tableSimpleLookup.addCol(data.version());
					}
					else
					{
						tableSimpleLookup.addCol(Table.ColoredCol.of(value, Table.ColoredCol.LIGHT_YELLOW));
						tableSimpleLookup.addCol(Table.ColoredCol.of(data.version(), Table.ColoredCol.LIGHT_YELLOW));
					}

					String depValue = data.depGroupId() + ":" + data.depArtifactId() + (isTest ? " (" + data.depScope() + ")" : "");
					tableSimpleLookup.addCol(depValue).addCol(data.depVersion()).endRow();
				}
				for (DependencyData data : result.matchingAsDependency())
				{
					boolean isTest = ignoredScopes.contains(data.depScope());
					if (isTest && hideTest)
						continue;

					tableSimpleLookup.addCol(data.groupId() + ":" + data.artifactId()).addCol(data.version());

					String value = data.depGroupId() + ":" + data.depArtifactId() + (isTest ? " (" + data.depScope() + ")" : "");
					if (runningParameter.table_nocolor)
					{
						tableSimpleLookup.addCol(value);
						tableSimpleLookup.addCol(data.depVersion());
					}
					else
					{
						tableSimpleLookup.addCol(Table.ColoredCol.of(value, Table.ColoredCol.LIGHT_YELLOW));
						tableSimpleLookup.addCol(Table.ColoredCol.of(data.depVersion(), Table.ColoredCol.LIGHT_YELLOW));
					}
					tableSimpleLookup.endRow();
				}
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}

			printTable(tableSimpleLookup, runningParameter, true);
		}
		else if (runningParameter.coordinates.size() > 1)
		{
			for (String coordinate : runningParameter.coordinates)
			{
				if (coordinate.contains("*"))
				{
					Output.error("For Matrix search, you can not use a wildcard coordinate!");
					return;
				}
			}

			/// Set{ Index { artifact, dependency } }
			// HashSet<Index> relations = new HashSet<>();

			Matrix matrix = new ArtifactVersionMatrix();

			// 2. add coordinates to keep the order
			matrix.addNodes(new ArrayList<>(runningParameter.coordinates).stream()
									.map(a -> String.join(":", Arrays.copyOfRange(a.split(":"), 0, 2)))
									.toArray(String[]::new));

			try
			{
				database_init();

				List<DependencyData> dataList = db.cloneData();

				// i want to match the versions between each one.

				// 1. search for coordinate 1
				// 2. search in the merged list for coordinate [!1]
				// 3. all matches are end results and are recorded!

				for (String coordinateA : runningParameter.coordinates)
				{
					Result resultsA = searchDependencies(coordinateA, dataList, hideTest, hideOptional);

					ArrayList<DependencyData> previewAllNodes = new ArrayList<>();
					ArrayList<String> otherCoordinates = new ArrayList<>(runningParameter.coordinates);
					otherCoordinates.remove(coordinateA);

					for (String coordinateB : otherCoordinates)
					{
						Result resultsB = searchDependencies(coordinateB, resultsA.allMatchings(), hideTest, hideOptional);

						List<DependencyData> intersections = resultsB.allMatchings();
						previewAllNodes.addAll(intersections);

						if (!intersections.isEmpty())
						{
							for (DependencyData data : intersections)
							{
								matrix.addVersion(data.groupId() + ":" + data.artifactId(), data.version(), data.depGroupId() + ":" + data.depArtifactId(),
										data.depVersion());
							}
						}
					}

					/// DEBUG : Preview All Dependencies
					// region Findings '---findings -v'
					if (Set.of(runningParameter.originalArguments).contains("---findings"))
					{
						Output.verbose(" - " + coordinateA + " has found:");
						printResult(new Result(previewAllNodes, new ArrayList<>(), previewAllNodes), runningParameter);
					}
					// endregion Findings '---findings -v'
				}

				// region References ( Bonds ) '---bonds -v'
				if (Set.of(runningParameter.originalArguments).contains("---bonds") && Output.isVerbose())
				{
					Table f1 = new Table(2);
					f1.setName(Table.Col.of("Bonds").setAlign(Table.Align.CENTER));
					f1.addCol("Artifact").addCol("Dependency").endRow();

					for (LinkKey referenceKey : matrix.getRelations())
					{
						Serializable[] cache = referenceKey.asIndex().cache();
						String key = cache[0].toString();
						String value = cache[1].toString();
						f1.addCol(key).addCol(value).endRow();
					}
					printTable(f1, runningParameter, false);
				}
				// endregion References ( Bonds ) '---bonds'

				/// second iteration of "-a" (now can choose between them)
				// if (false)
				{
					String[][] strings = matrix.makeView(runningParameter.version_all);
					Table f3 = new Table(strings[0].length);
					boolean firstRow = true;

					for (String[] string : strings)
					{
						boolean isColored = !firstRow;
						for (String s : string)
							if (s.isEmpty())
							{
								isColored = false; /// mark a full entry as {colored}
								break;
							}

						for (String entry : string)
							if (isColored && runningParameter.version_all && !runningParameter.table_nocolor)
							/// only color the row, if  {colored} and '-a' and !'-c'
								f3.addCol(Table.ColoredCol.of(entry, Table.ColoredCol.LIGHT_GREEN));
							else
								f3.addCol(entry); /// otherwise keep it plain

						f3.endRow();
						firstRow = false;
					}
					printTable(f3, runningParameter, true);
				}

			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		}
	}

	// endregion Commands

	// region Utility Methods

	private static void printTable(Table table, Params runningParameter, boolean isGeneric)
	{
		String render;
		if (runningParameter.table_format_tab)
			render = Table.TableConfig.TAB_STOPS().render(table, Table.TableConfig.OutlineType.COLUMNS);
		else
			render = Table.TableConfig.SIMPLE_LINES().render(table, Table.TableConfig.OutlineType.FULL);

		// '\n' put into println will make 2 lines ; here we prevent this
		if (render.charAt(render.length() - 1) == '\n') // todo - OS dependent
			render = render.substring(0, render.length() - 1);

		if (isGeneric)
			Output.generic(render);
		else
			Output.verbose(render);
	}

	private static void printResult(Result value, Params runningParameter)
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
			printTable(f1, runningParameter, false);
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
			printTable(f1, runningParameter, false);
		}
	}

	private Result searchDependencies(String searchingCoordinate, List<DependencyData> dataList, boolean hideTestScope, boolean hideOptional)
	{
		Pom pom = new Pom(holder, searchingCoordinate);
		String groupId = pom.getGroupId();
		String artifactId = pom.getArtifactId();
		Optional<String> version = pom.version();
		// String type = pom.getExtension();
		// String classifier = pom.getClassifier();

		List<DependencyData> matchingAsArtifact = new ArrayList<>();
		List<DependencyData> matchingAsDependency = new ArrayList<>();

		for (DependencyData data : dataList)
		{
			if (data.depType().equals("test-jar")) // remove all test-jars
				continue;
			if (!groupId.equals("*") && !groupId.equalsIgnoreCase(data.groupId()) && !groupId.equalsIgnoreCase(data.depGroupId()))
				continue;
			if (!artifactId.equalsIgnoreCase(data.artifactId()) && !artifactId.equalsIgnoreCase(data.depArtifactId()))
				continue;

			if (hideTestScope && "test".equals(data.depScope()))
				continue;
			if (hideOptional && !data.depOptional().isEmpty() && Boolean.parseBoolean(data.depOptional()))
				continue;

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

	// endregion Utility Methods

	// region Database

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
		// @formatter:off
		return new DependencyData(
				model.getGroupId(),
				model.getArtifactId(),
				model.getPackaging(),
				"",
				model.getVersion(),
				dep.getGroupId(),
				dep.getArtifactId(),
				dep.getType(),
				dep.getClassifier(),
				dep.getScope(),
				dep.getVersion(),
				dep.getOptional(),
				String.join(".",
						dep.getExclusions().stream().map(
								exclusion -> exclusion.getGroupId() + ":" + exclusion.getArtifactId()
						).toList())
		);
		// @formatter:on
	}

	// endregion Database

}