package com.github.ensnert.impl.data;

import com.github.ensnert.impl.VersionComparator;

import java.util.StringJoiner;


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

	public String getAnnotations()
	{
		StringJoiner sj = new StringJoiner(", ");

		if (this.isCurrent())
			sj.add("current");
		if (this.isRecommended())
			sj.add("last patch");
		if (this.isSnapshot())
			sj.add("snapshot");
		if (this.isLatest())
			sj.add("latest");

		return String.valueOf(sj);
	}

}
