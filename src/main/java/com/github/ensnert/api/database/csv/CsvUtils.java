package com.github.ensnert.api.database.csv;

import com.github.ensnert.api.database.Column;
import com.github.ensnert.api.database.IndexStrategy;
import com.github.ensnert.impl.Output;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.RecordComponent;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;


public final class CsvUtils
{
	private CsvUtils()
	{
	}

	/**
	 * @param filePath Path to the Database File (existence: optional)
	 * @param separator CSV Column Seperator
	 * @param type Class reference to T (constructable)
	 * @param indexer Instance of an Indexer
	 * @param <T> Type of the output Object
	 * @return list of loaded Objects 'T' or an empty list, if the file does not exist or is empty
	 * @throws IOException thrown, when issues with the database file arise
	 * @throws InvocationTargetException thrown, if issues arise when constructing 'T'
	 * @throws InstantiationException thrown, if 'T' is an abstract class
	 * @throws IllegalAccessException thrown, if the constructor is non-public
	 * @throws ClassCastException thrown, if the constructor is not creating 'T' (one must show me an instance, where this is not the case!)
	 */
	@SuppressWarnings("unchecked")
	public static <T> List<T> load(String filePath, String separator, Class<T> type, IndexStrategy<T> indexer)
			throws IOException, InvocationTargetException, InstantiationException, IllegalAccessException, ClassCastException
	{
		// * Check if File Exists
		Path path = Paths.get(filePath);
		if (!path.toFile().exists())
			return new ArrayList<>();

		// * Check if File has any Content
		List<String> lines = Files.readAllLines(path, Charset.defaultCharset());
		if (lines.size() <= 1) // ignore the file, if only headers are present
			return new ArrayList<>();

		// * Read Headers
		String[] headers = lines.get(0).split(separator);
		Map<String, Integer> headerMap = new HashMap<>();
		for (int i = 0; i < headers.length; i++)
			headerMap.put(headers[i].trim(), i);

		// Option 1: T is a Record -> RecordComponents are also a Constuctor
		// Option 2: T is a General Class -> Constructor need the Paramters to have also these Annotation

		Constructor<T> constructor = (Constructor<T>) type.getConstructors()[0];
		Parameter[] parameters = constructor.getParameters();

		List<T> records = new ArrayList<>();
		for (int i = 1; i < lines.size(); i++)
		{
			String[] fields = lines.get(i).split(separator, -1);
			Object[] args = new Object[parameters.length];

			for (int p = 0; p < parameters.length; p++)
			{
				var annotation = parameters[p].getAnnotation(Column.class); // todo - in the future the #id could be used
				if (annotation != null)
				{
					String colName = annotation.value();
					Integer colIdx = headerMap.get(colName);
					args[p] = (colIdx != null && colIdx < fields.length) ? fields[colIdx].trim() : "";
				}
				else
				{
					Output.verbose("[WARN] Parameter " + parameters[p] + " for constructor of class " + type.getName() + " has no Column annotation!");
					args[p] = null;
				}
			}
			records.add(constructor.newInstance(args));
		}

		if (indexer != null)
			indexer.rebuildIndex(records);

		return records;
	}

	/**
	 * @param filePath path to CSV file
	 * @param separator column seperator
	 * @param type Output Type
	 * @param records &lt;out&gt; collection of storable entries
	 * @param <T> type of Storing Entries
	 * @throws IOException thrown, when issues with the database file arise
	 * @throws NoSuchMethodException thrown, when no {@link Column} annotated method or entry was found
	 * @throws InvocationTargetException thrown, if issues arise when executing getters of 'T'
	 * @throws IllegalAccessException thrown, if getters are annotated with {@link Column} but are <u><b>non public</b></u>
	 */
	public static <T> void store(String filePath, String separator, Class<T> type, Collection<T> records)
			throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException
	{
		// Option 1: T is a Record -> use RecordComponents

		Method[] fields = null;
		RecordComponent[] recordComponents = type.getRecordComponents();
		if (recordComponents != null && recordComponents.length > 0)
		{
			fields = new Method[recordComponents.length];
			for (int i = 0; i < recordComponents.length; i++)
			{
				fields[i] = recordComponents[i].getAccessor();
			}
		}

		// Option 2: T is a General Class -> use Methods
		if (fields == null)
		{
			Method[] methods = type.getMethods();
			ArrayList<Method> e = new ArrayList<>();
			for (Method method : methods)
			{
				// getter with a CsvColumnAnnotation
				if (method.isAnnotationPresent(Column.class) && method.getParameterCount() == 0)
				{
					e.add(method);
				}
			}
			e.sort(Comparator.comparingInt(p -> p.getAnnotation(Column.class).id()));

			fields = e.toArray(new Method[0]);
		}

		StringJoiner headerJoiner = new StringJoiner(separator);

		for (Method rc : fields)
		{
			Column ann = rc.getAnnotation(Column.class);

			if (ann != null)
			{
				headerJoiner.add(ann.value());
			}
			else
			{
				throw new NoSuchMethodException("@Column annotation is not present for " + type.getName() + "#" + rc.getName());
			}
		}

		try (PrintWriter writer = new PrintWriter(new FileWriter(filePath, false)))
		{
			writer.println(headerJoiner);
			for (T record : records)
			{
				StringJoiner rowJoiner = new StringJoiner(separator);
				for (Method rc : fields)
				{
					Object val = rc.invoke(record);

					rowJoiner.add(val == null ? "" : val.toString());
				}
				writer.println(rowJoiner);
			}
		}
	}

}
