package ch.ensnert.impl.data;

import ch.ensnert.impl.VersionComparator;


public record AnalyzedVersion(String version, boolean isNewer, boolean isOlder, boolean isCurrent, boolean isRecommended, boolean isSnapshot, boolean isLatest)
		implements Comparable<AnalyzedVersion>
{
	public AnalyzedVersion log()
	{
		System.out.printf("%s, current %B, latest %B, older %B, newer %b, recommended %B, snapshot %B %n", version, isCurrent, isLatest, isOlder, isNewer,
				isRecommended, isSnapshot);
		return this;
	}

	@Override
	public int compareTo(AnalyzedVersion o)
	{
		return VersionComparator.compare(this.version, o.version);
	}

}
