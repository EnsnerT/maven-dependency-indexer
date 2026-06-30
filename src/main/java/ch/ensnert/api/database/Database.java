package ch.ensnert.api.database;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 * created with AI
 * @param <T>
 */
public abstract class Database<T>
{
	protected final Class<T> type;
	protected final List<T> storage = new ArrayList<>();
	protected IndexStrategy<T> indexStrategy = null;

	public Database(Class<T> type, IndexStrategy<T> strategy)
	{
		this.type = type;
		this.indexStrategy = strategy;
	}

	public void setIndexStrategy(IndexStrategy<T> strategy)
	{
		this.indexStrategy = strategy;
		if (indexStrategy != null)
			this.indexStrategy.rebuildIndex(storage);
	}

	public String createKey(T record)
	{
		if (indexStrategy == null)
			return null;
		return indexStrategy.createKey(record);
	}

	public boolean hasKey(String key)
	{
		if (indexStrategy == null)
			return false;
		return indexStrategy.hasKey(key);
	}

	public List<T> getByKey(String key)
	{
		if (indexStrategy == null)
			return new ArrayList<>();
		return indexStrategy.getByKey(key);
	}

	public void batchInsert(Collection<T> newRecords)
	{
		storage.addAll(newRecords);
		if (indexStrategy != null)
			indexStrategy.rebuildIndex(storage);
	}

	public List<T> cloneData()
	{
		return new ArrayList<>(storage);
	}

	public abstract void load(String filePath, String separator) throws Exception;

	public abstract void store(String filePath, String separator) throws Exception;
}
