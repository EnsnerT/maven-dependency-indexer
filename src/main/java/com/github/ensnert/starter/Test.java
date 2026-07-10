package com.github.ensnert.starter;

/**
 * Run with either: <br/>
 * `./com.github.ensnert.app test` or<br/>
 * `java -cp com.github.ensnert.app.jar com.github.ensnert.starter.Test`
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

		ClassLoader loader = Test.class.getClassLoader();
		while (loader != null)
		{
			System.out.println(loader);
			loader = loader.getParent();
		}

		System.out.println("current Thread Loader:");
		System.out.println(Thread.currentThread().getContextClassLoader());

	}
}
