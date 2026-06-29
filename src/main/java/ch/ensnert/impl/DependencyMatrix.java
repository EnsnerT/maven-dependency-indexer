package ch.ensnert.impl;


import ch.ensnert.impl.data.LinkKey;

import java.util.HashMap;
import java.util.Set;


@Deprecated(forRemoval = true)
public class DependencyMatrix
{
	private final HashMap<LinkKey, HashMap<String, String>> data = new HashMap<>();

	public Set<LinkKey> getLinks()
	{
		return data.keySet();
	}

	public HashMap<String, String> getLink(LinkKey link)
	{
		if (!data.containsKey(link))
		{
			data.put(link, new HashMap<>());
		}
		return data.get(link);
	}
}
