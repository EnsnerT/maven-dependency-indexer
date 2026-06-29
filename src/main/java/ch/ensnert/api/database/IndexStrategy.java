package ch.ensnert.api.database;

import java.util.Collection;
import java.util.List;


public interface IndexStrategy<T>
{
	void rebuildIndex(Collection<T> records);

	boolean hasKey(String key);

	List<T> getByKey(String key);

	String createKey(T object);
}
