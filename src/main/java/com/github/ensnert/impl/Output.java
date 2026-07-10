package com.github.ensnert.impl;

import java.io.OutputStream;
import java.io.PrintStream;


@SuppressWarnings("unused")
public final class Output
{
	public static final PrintStream NULL_WRITER = new PrintStream(OutputStream.nullOutputStream());

	static PrintStream out = NULL_WRITER;
	static PrintStream err = NULL_WRITER;

	static boolean verbose = false;
	static boolean generic = true;

	// region Streams
	public static void setOut(PrintStream out)
	{
		Output.out = out;
	}

	public static void setErr(PrintStream err)
	{
		Output.err = err;
	}
	// endregion Streams

	// region Getter & Setter
	public static boolean isVerbose()
	{
		return verbose;
	}

	public static void setVerbose(boolean verbose)
	{
		Output.verbose = verbose;
	}

	public static boolean isGeneric()
	{
		return generic;
	}

	public static void setGeneric(boolean generic)
	{
		Output.generic = generic;
	}
	// endregion Getter & Setter

	// region Loggers
	public static void verbose(String message)
	{
		if (isVerbose())
			out.println(message);
	}

	public static void verbose(String format, Object... args)
	{
		verbose(String.format(format, args));
	}

	public static void generic(String message)
	{
		if (isGeneric())
			out.println(message);
	}

	public static void generic(String format, Object... args)
	{
		generic(String.format(format, args));
	}

	public static void error(String message)
	{
		err.println(message);
	}

	public static void error(String format, Object... args)
	{
		error(String.format(format, args));
	}
	// endregion Loggers

}
