package ch.ensnert.api.database;

import ch.ensnert.api.database.csv.CsvUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public abstract class Database<T>
{
	// private static Map<Class<?>, Database<?>> INSTANCES = new HashMap<Class<?>, Database<?>>();

	protected final Class<T> type;
	protected final List<T> storage = new ArrayList<>();
	protected IndexStrategy<T> indexStrategy = null;

	public Database(Class<T> type, IndexStrategy<T> strategy)
	{
		this.type = type;
		this.indexStrategy = strategy;
		// INSTANCES.put(type, this);
	}

	// Erlaubt das Austauschen der Index-Logik zur Laufzeit
	public void setIndexStrategy(IndexStrategy<T> strategy)
	{
		this.indexStrategy = strategy;
		this.indexStrategy.rebuildIndex(storage);
	}


	public String createKey(T record)
	{
		return indexStrategy.createKey(record);
	}

	public boolean hasKey(String key)
	{
		return indexStrategy.hasKey(key);
	}

	public List<T> getByKey(String key)
	{
		return indexStrategy.getByKey(key);
	}

	public void batchInsert(Collection<T> newRecords)
	{
		storage.addAll(newRecords);
		indexStrategy.rebuildIndex(storage); // Aktualisiert den Index
	}

	public List<T> cloneData()
	{
		return new ArrayList<>(storage);
	}

	public abstract void load(String filePath, String separator) throws Exception;

	public abstract void store(String filePath, String separator) throws Exception;
}
