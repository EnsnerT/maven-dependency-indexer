package com.github.ensnert.impl.data;

import com.github.ensnert.api.Index;
import com.github.ensnert.api.Indexable;


public record VersionKey(String source_version, String target_version) implements Indexable
{
	@Override
	public Index asIndex()
	{
		return new Index(source_version, target_version);
	}
}
