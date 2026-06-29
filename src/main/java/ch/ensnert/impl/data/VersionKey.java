package ch.ensnert.impl.data;

import ch.ensnert.api.Index;
import ch.ensnert.api.Indexable;


public final class VersionKey implements Indexable
{
	private String source_version;
	private String target_version;

	public VersionKey(String source_version, String target_version)
	{
		this.source_version = source_version;
		this.target_version = target_version;
	}

	public String getSource_version()
	{
		return source_version;
	}

	public String getTarget_version()
	{
		return target_version;
	}

	@Override
	public Index asIndex()
	{
		return new Index(source_version, target_version);
	}
}
