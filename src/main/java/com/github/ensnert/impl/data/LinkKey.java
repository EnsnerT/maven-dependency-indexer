package com.github.ensnert.impl.data;

import com.github.ensnert.api.Index;
import com.github.ensnert.api.Indexable;


public record LinkKey(String source, String target) implements Indexable
{
	public Index asIndex()
	{
		return new Index(source, target);
	}
}
