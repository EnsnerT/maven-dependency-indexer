package ch.ensnert.impl.data;

import ch.ensnert.api.Index;
import ch.ensnert.api.Indexable;


public record LinkKey(String source, String target) implements Indexable
{
	public Index asIndex()
	{
		return new Index(source, target);
	}
}
