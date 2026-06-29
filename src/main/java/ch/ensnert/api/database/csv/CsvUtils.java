package ch.ensnert.api.database.csv;

import ch.ensnert.api.database.Column;
import ch.ensnert.api.database.IndexStrategy;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.RecordComponent;
import java.nio.charset.Charset;
import java.nio.file.Files;
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
	CsvUtils()
	{
	}

	public static List<String> generateHeader(Class<?> clazz)
	{
		HashMap<Integer, String> name = new HashMap<>();
		// HashMap<Integer, String> index = new HashMap<>();
		AnnotatedElement[] elements;

		if (clazz.isRecord())
		{
			elements = clazz.getConstructors()[0].getParameters();
		}
		else
		{
			elements = clazz.getMethods();
		}

		for (AnnotatedElement method : elements)
		{
			Column[] annotationsByType = method.getAnnotationsByType(Column.class);
			if (annotationsByType.length == 0)
				continue;
			for (Column column : annotationsByType)
			{
				name.putIfAbsent(column.id(), column.value());
				// if (column.index())
				// 	index.putIfAbsent(column.id(), column.value());
			}
		}

		// List<String> indexes = index.entrySet().stream().sorted(Comparator.comparingInt(Map.Entry::getKey)).map(Map.Entry::getValue).toList();
		return name.entrySet().stream().sorted(Comparator.comparingInt(Map.Entry::getKey)).map(Map.Entry::getValue).toList();
	}

	// public record ModelMetadata(String header, String index){}

	public static <T> List<T> load(String filePath, String separator, Class<T> type, IndexStrategy<T> indexer) throws Exception
	{
		List<String> lines;
		try
		{
			lines = Files.readAllLines(Paths.get(filePath), Charset.defaultCharset());
			if (lines.isEmpty())
				return new ArrayList<>();
		}
		catch (IOException e)
		{
			e.printStackTrace(System.err);
			return new ArrayList<>(); /// maybe file does not exists
		}

		String[] headers = lines.get(0).split(separator);
		Map<String, Integer> headerMap = new HashMap<>();
		for (int i = 0; i < headers.length; i++)
		{
			headerMap.put(headers[i].trim(), i);
		}

		// Option 1: Records
		// Option 2: General Class

		Constructor<?> constructor = type.getConstructors()[0];
		Parameter[] parameters = constructor.getParameters();

		List<T> records = new ArrayList<>();
		for (int i = 1; i < lines.size(); i++)
		{
			String[] fields = lines.get(i).split(separator, -1);
			Object[] args = new Object[parameters.length];

			for (int p = 0; p < parameters.length; p++)
			{
				var annotation = parameters[p].getAnnotation(Column.class); // ignore Id for now
				if (annotation == null)
				{
					System.err.println("[Error] can not find annotation for "+parameters[p]);
					args[p] = null;
				} else {
					String colName = annotation.value();
					Integer colIdx = headerMap.get(colName);
					args[p] = (colIdx != null && colIdx < fields.length) ? fields[colIdx].trim() : "";
				}
			}
			records.add((T) constructor.newInstance(args));
		}

		if (indexer != null)
			indexer.rebuildIndex(records);

		return records;
	}

	public static <T> void store(String filePath, String separator, Class<T> type, Collection<T> records) throws Exception
	{
		// Option 1: Records

		Method[] fields = null;
		RecordComponent[] recordComponents = type.getRecordComponents();
		if (recordComponents != null && recordComponents.length > 0) {
			fields = new Method[recordComponents.length];
			for (int i = 0; i < recordComponents.length; i++)
			{
				fields[i] = recordComponents[i].getAccessor();
			}
		}

		// Option 2: Method
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

			if (ann == null)
			{
				System.err.println("[ERROR] Can not find Annotation Element for : " + rc);
			}
			else
			{
				headerJoiner.add(ann.value());
			}
		}

		try (PrintWriter writer = new PrintWriter(new FileWriter(filePath, false)))
		{
			writer.println(headerJoiner.toString());
			for (T record : records)
			{
				StringJoiner rowJoiner = new StringJoiner(separator);
				for (Method rc : fields)
				{
					Object val = rc.invoke(record);

					rowJoiner.add(val == null ? "" : val.toString());
				}
				writer.println(rowJoiner.toString());
			}
		}
	}

}
