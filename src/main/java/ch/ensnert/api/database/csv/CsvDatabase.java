package ch.ensnert.api.database.csv;

import ch.ensnert.api.database.Database;
import ch.ensnert.api.database.IndexStrategy;


public class CsvDatabase<T> extends Database<T>
{
	public CsvDatabase(Class<T> type, IndexStrategy<T> strategy)
	{
		super(type, strategy);
	}

	@Override
	public void load(String filePath, String separator) throws Exception
	{
		storage.clear();
		storage.addAll(CsvUtils.load(filePath, separator, type, indexStrategy));
		indexStrategy.rebuildIndex(storage);
	}

	@Override
	public void store(String filePath, String separator) throws Exception
	{
		CsvUtils.store(filePath, separator, type, storage);
	}
}
