package s;

/**
 * Run with either: <br/>
 * `./ch.ensnert.app test` or<br/>
 * `java -cp ch.ensnert.app.jar s.Test`
 */
public final class Test
{
	@SuppressWarnings("unused")
	private static final String DB_FILE = "./db_test.csv";

	@SuppressWarnings("EmptyMethod")
	public static void main(String[] args)
	{
		// run : com.azure:azure-core-http-netty com.azure:azure-identity com.azure:azure-core ==
		// run : com.azure:azure-core com.azure:azure-core-http-netty com.azure:azure-identity ==
		// run : com.azure:azure-identity com.azure:azure-core-http-netty com.azure:azure-core ==
		// run : com.azure:azure-identity com.azure:azure-core com.azure:azure-core-http-netty ==
		// Output.setVerbose(true);

		// ArtifactVersionMatrix.main(args);

	}
}
