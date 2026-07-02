package ch.ensnert.impl;

import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;
import org.eclipse.aether.version.VersionRange;

import java.util.Comparator;


/**
 * @author ensnerT (2026) - no AI was used
 */
@SuppressWarnings("unused")
public final class VersionComparator
{
	private static final Comparator<String> COMPARATOR = Comparator.nullsFirst(Comparator.comparing(VersionComparator::parseVersion, Comparator.nullsFirst(Version::compareTo)));

	private VersionComparator()
	{
	}

	public static Comparator<String> getComparator()
	{
		return COMPARATOR;
	}

	private static final GenericVersionScheme genericVersionScheme = new GenericVersionScheme();

	public static Version parseVersion(String version)
	{
		try
		{
			return genericVersionScheme.parseVersion(version);
		}
		catch (InvalidVersionSpecificationException e)
		{
			return null;
		}
	}

	public static VersionRange parseRange(String version)
	{
		try
		{
			return genericVersionScheme.parseVersionRange(version);
		}
		catch (InvalidVersionSpecificationException e)
		{
			return null;
		}
	}

	/**
	 * Returns the index of the changed base: <br/>
	 * 1.0.0 vs 2.0.0 == 0 <br/>
	 * 1.0.0 vs 1.1.0 == 1 <br/>
	 * 1.0.0 vs 1.0.1 == 2 <br/>
	 * 1.0.0 vs 1.0.0 == 3 <br/>
	 * 1.0.0.0 vs 1.0.0.0 == 4 (no changes, the index is bigger than the amount of "." or version places) <br/>
	 *
	 * @return first changed index of split '.' (0 to {amount of '.'+1}), if 100% the same, returns {amount of '.' + 2}
	 */
	public static int changedLevel(String base, String comp)
	{
		String[] split = base.split("\\.");
		String[] compSplit = comp.split("\\.");

		int maxCheck = Math.min(split.length, compSplit.length);
		for (int i = 0; i < maxCheck; i++)
			if (!split[i].equals(compSplit[i]))
				return i;
		return maxCheck; // do not try to index this part!
	}

	public static boolean isSame(String base, String version)
	{
		return COMPARATOR.compare(base, version) == 0;
	}

	public static boolean isSameOrNewer(String base, String version)
	{
		return COMPARATOR.compare(base, version) <= 0;
	}

	public static boolean isNewer(String base, String version)
	{
		return COMPARATOR.compare(base, version) < 0;
	}

	public static boolean isSameOrOlder(String base, String version)
	{
		return COMPARATOR.compare(base, version) >= 0;
	}

	public static boolean isOlder(String base, String version)
	{
		return COMPARATOR.compare(base, version) > 0;
	}

	public static int compare(String base, String version)
	{
		return COMPARATOR.compare(base, version);
	}
}
