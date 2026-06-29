package ch.ensnert.impl.data;

import ch.ensnert.api.Index;
import ch.ensnert.api.Indexable;


public final class LinkKey implements Indexable
{
	private String source;
	private String target;

	public LinkKey(String source, String target)
	{
		this.source = source;
		this.target = target;
	}

	public String getSource()
	{
		return source;
	}

	public String getTarget()
	{
		return target;
	}

	public Index asIndex()
	{
		return new Index(source, target);
	}
}
