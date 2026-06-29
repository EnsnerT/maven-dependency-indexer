package s;

import ch.ensnert.api.database.csv.CsvDatabase;
import ch.ensnert.impl.database.indexstrategies.MavenCoordinateIndex;
import ch.ensnert.impl.database.types.DependencyData;
import ch.ensnert.impl.VersionComparator;

import java.util.Collections;
import java.util.List;


/**
 * Run with either: <br/>
 * `./ch.ensnert.app test` or<br/>
 * `java -cp ch.ensnert.app.jar s.Test`
 */
public final class Test
{
	private static final String DB_FILE = "./db.csv";

	public static void main(String[] args)
	{
		// run : com.azure:azure-core-http-netty com.azure:azure-identity com.azure:azure-core ==
		// run : com.azure:azure-core com.azure:azure-core-http-netty com.azure:azure-identity ==
		// run : com.azure:azure-identity com.azure:azure-core-http-netty com.azure:azure-core ==
		// run : com.azure:azure-identity com.azure:azure-core com.azure:azure-core-http-netty ==

		if (true)
			return;
		String[] vs = { "2.0.0", "1.1.0", "1.0.1", "1.0.0" };

		for (String v : vs)
		{
			int i = VersionComparator.changedLevel("1.0.0", v);
			System.out.println("Version changed: 1.0.0 vs " + v + " : " + i);

		}
		if (true)
			return;

		var db = new CsvDatabase<>(DependencyData.class, new MavenCoordinateIndex());

		try
		{
			db.load(DB_FILE, ",");

			String coordinate = "org.apache.kafka:kafka_2.12:3.9.0";

			DependencyData data = new DependencyData("org.apache.kafka", "kafka_2.12", "", "", "3.9.0", "org.apache.kafka", "kafka-clients", "jar", "", "",
					"3.9.0", "false", "");
			String key = db.createKey(data);
			if (db.hasKey(key))
			{
				System.out.println("Entry Exists!");

				List<DependencyData> byKey = db.getByKey(key);
				System.out.println(byKey.size());
				byKey.forEach(record ->
				{
					System.out.println(record.toString());
				});

			}
			else
			{

				List<DependencyData> dataList = Collections.singletonList(data);

				db.batchInsert(dataList);

				db.store(DB_FILE, ",");
			}

		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}

	}
}
