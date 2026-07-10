package com.github.ensnert.impl.database.indexstrategies;

import com.github.ensnert.api.database.IndexStrategy;
import com.github.ensnert.impl.database.types.DependencyData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Created by AI - Modified by ensnerT
 */
public final class MavenCoordinateIndex implements IndexStrategy<DependencyData>
{
	private final Map<String, List<DependencyData>> index = new HashMap<>();

	@Override
	public void rebuildIndex(Collection<DependencyData> records)
	{
		index.clear();
		for (DependencyData record : records){
			String key = createKey(record);
			index.computeIfAbsent(key, k -> new ArrayList<>()).add(record);
		}
	}

	@Override
	public boolean hasKey(String key) {
		return index.containsKey(normalizeKey(key));
	}

	@Override
	public List<DependencyData> getByKey(String key) {
		return index.getOrDefault(normalizeKey(key), Collections.emptyList());
	}

	/**
	 * @return "g:a:c:v"
	 */
	@Override
	public String createKey(DependencyData r)
	{
		String classifier = (r.classifier() == null || r.classifier().isBlank()) ? "" : r.classifier();
		return String.format("%s:%s:%s:%s", r.groupId(), r.artifactId(), classifier, r.version());
	}

	/**
	 * @param key "g:a:v" | "g:a:c:v"
	 * @return "g:a:c:v" -> any unknown == "" -> ::
	 */
	private String normalizeKey(String key) {
		String[] parts = key.split(":");
		if (parts.length == 3) { // g:a:v -> g:a::v
			return parts[0] + ":" + parts[1] + "::" + parts[2];
		}
		return key;
	}
}
