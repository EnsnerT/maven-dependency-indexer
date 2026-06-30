package ch.ensnert.api.database.csv;

import ch.ensnert.api.database.Column;
import ch.ensnert.api.database.Database;
import ch.ensnert.api.database.IndexStrategy;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;


public class CsvDatabase<T> extends Database<T>
{
	public CsvDatabase(Class<T> type, IndexStrategy<T> strategy)
	{
		super(type, strategy);
	}

	/**
	 * @param filePath path to the database csv (file does not need to exist yet)
	 * @param separator csv column seperator
	 * @throws IOException thrown, when issues with the database file arise
	 * @throws InvocationTargetException thrown, if issues arise when constructing 'T'
	 * @throws InstantiationException thrown, if 'T' is an abstract class
	 * @throws IllegalAccessException thrown, if the constructor is non-public
	 * @throws ClassCastException thrown, if the constructor is not creating 'T' (one must show me an instance, where this is not the case!)
	 */
	@Override
	public void load(String filePath, String separator) throws IOException, InvocationTargetException, InstantiationException, IllegalAccessException
	{
		storage.clear();
		storage.addAll(CsvUtils.load(filePath, separator, type, indexStrategy));
		indexStrategy.rebuildIndex(storage);
	}

	/**
	 * @param filePath path to the database csv
	 * @param separator csv column seperator
	 * @throws IOException thrown, when issues with the database file arise
	 * @throws NoSuchMethodException thrown, when no {@link Column} annotated method or entry was found
	 * @throws InvocationTargetException thrown, if issues arise when executing getters of 'T'
	 * @throws IllegalAccessException thrown, if getters are annotated with {@link Column} but are <u><b>non public</b></u>
	 * */
	@Override
	public void store(String filePath, String separator) throws IOException, InvocationTargetException, NoSuchMethodException, IllegalAccessException
	{
		CsvUtils.store(filePath, separator, type, storage);
	}
}
