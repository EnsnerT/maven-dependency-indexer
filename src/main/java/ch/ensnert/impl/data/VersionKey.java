package ch.ensnert.impl.data;

import ch.ensnert.api.Index;
import ch.ensnert.api.Indexable;


public record VersionKey(String source_version, String target_version) implements Indexable
{
	@Override
	public Index asIndex()
	{
		return new Index(source_version, target_version);
	}
}
