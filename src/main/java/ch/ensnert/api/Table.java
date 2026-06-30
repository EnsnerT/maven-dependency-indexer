package ch.ensnert.api;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.StringJoiner;


/**
 * @author ensnerT (2025)
 */
public final class Table
{
	private static String getColor(String a)
	{
		return (char) 0x1b + "[" + a + "m";
	}

	private static String applyColor(String i, String c)
	{
		return getColor(c) + i + getColor("");
	}

	int colNumbers = 1;
	boolean hasHeader = false;
	Col name = null;
	LinkedList<Row> rows = new LinkedList<>();

	public Table()
	{
	}

	public Table(int colNumbers)
	{
		this.setColSize(colNumbers);
	}

	public Table setName(Col name)
	{
		this.name = name;
		return this;
	}

	public Table setColSize(int number)
	{
		if (number < 1)
		{
			throw new java.lang.IllegalArgumentException("Size can not be less than 1");
		}
		this.colNumbers = number;
		return this;
	}

	public Table addCol(String value)
	{
		return addCol(Col.of(value));
	}

	public Table addCol(Table.Col column)
	{

		int rownum = this.rows.size() - 1;
		if (rownum < 0)
		{
			this.rows.add(Row.from(column));
		}
		else
		{
			Row row = this.rows.get(rownum);
			if (row == null || row.size() >= this.colNumbers)
			{
				this.rows.add(Row.from(column));
			}
			else
			{
				row.add(column);
			}
		}
		return this;
	}

	public void setAlign(int colindex, Align align)
	{
		for (Row row : this.rows)
		{
			row.setAlign(colindex, align);
		}
	}

	public Table endRow()
	{
		int rownum = this.rows.size() - 1;
		if (rownum >= 0)
		{
			Row row = this.rows.get(rownum);
			if (row != null && row.size() < this.colNumbers)
			{
				for (int i = row.size(); i < this.colNumbers; i++)
				{
					row.add("");
				}
			}
		}
		return this;
	}

	public static class TableConfig
	{
		public String render(Table t, OutlineType chosen)
		{
			if (!this.supportings.contains(chosen))
				throw new IllegalArgumentException(chosen.name() + " is not supported!");

			// prerender
			int[] colSizes = new int[0];
			for (Row row : t.rows)
				colSizes = row.columnLengths(t.colNumbers, colSizes);

			boolean hasNameShown = t.name != null && t.name.length() > 0 && chosen.outside;

			// rendering
			StringBuilder sb = new StringBuilder();
			StringJoiner joiner;

			// line : Name Top
			if (hasNameShown)
			{
				sb.append(mapping.get(Directions.from(Directions.RIGHT, Directions.BOTTOM)));

				joiner = new StringJoiner(mapping.get(Directions.from(Directions.RIGHT, Directions.LEFT)));

				for (int i = 0; i < colSizes.length; i++)
				{
					joiner.add(mapping.get(Directions.from(Directions.RIGHT, Directions.LEFT)).repeat(colSizes[i]));
				}
				sb.append(joiner);

				sb.append(mapping.get(Directions.from(Directions.BOTTOM, Directions.LEFT)));

				sb.append("\n");

				// line : Name content

				// do first row
				// if (chosen.outside)
				sb.append(mapping.get(Directions.from(Directions.TOP, Directions.BOTTOM)));

				int totalColSize = 0;
				int i1 = chosen.columns ? mapping.getOrDefault(Directions.from(Directions.TOP, Directions.BOTTOM), " ").length() : 1;
				for (int i = 0; i < colSizes.length; i++)
				{
					totalColSize += colSizes[i];
				}
				totalColSize = totalColSize + (i1 * Math.max(0, colSizes.length - 1));

				sb.append(t.name.print(totalColSize));

				// sb.append(t.rows.get(0).print(colSizes, chosen.columns ? mapping.getOrDefault(Directions.from(Directions.TOP,Directions.BOTTOM)," ") : " "));

				// if (chosen.outside)
				sb.append(mapping.get(Directions.from(Directions.TOP, Directions.BOTTOM)));

				sb.append("\n");
			}

			// line : Header Top
			if (chosen.outside)
			{
				// do first row
				if (hasNameShown)
					sb.append(mapping.get(Directions.from(Directions.TOP, Directions.RIGHT, Directions.BOTTOM)));
				else
					sb.append(mapping.get(Directions.from(Directions.RIGHT, Directions.BOTTOM)));

				joiner = new StringJoiner(mapping.get(chosen.columns ?
															  Directions.from(Directions.RIGHT, Directions.BOTTOM, Directions.LEFT) :
															  Directions.from(Directions.RIGHT, Directions.LEFT)));

				for (int i = 0; i < colSizes.length; i++)
				{
					joiner.add(mapping.get(Directions.from(Directions.RIGHT, Directions.LEFT)).repeat(colSizes[i]));
				}
				sb.append(joiner);

				if (hasNameShown)
					sb.append(mapping.get(Directions.from(Directions.TOP, Directions.BOTTOM, Directions.LEFT)));
				else
					sb.append(mapping.get(Directions.from(Directions.BOTTOM, Directions.LEFT)));

				sb.append("\n");
			}

			// line : Header content
			if (chosen.header)
			{
				// do first row
				if (chosen.outside)
					sb.append(mapping.get(Directions.from(Directions.TOP, Directions.BOTTOM)));

				sb.append(t.rows.get(0).print(colSizes, chosen.columns ? mapping.getOrDefault(Directions.from(Directions.TOP, Directions.BOTTOM), " ") : " "));

				if (chosen.outside)
					sb.append(mapping.get(Directions.from(Directions.TOP, Directions.BOTTOM)));
				sb.append("\n");
			}

			// line : Header & body Seperator
			if (chosen.header)
			{
				if (chosen.outside)
					sb.append(mapping.get(Directions.from(Directions.TOP, Directions.RIGHT, Directions.BOTTOM)));

				joiner = new StringJoiner(chosen.columns ?
												  mapping.getOrDefault(Directions.from(Directions.TOP, Directions.RIGHT, Directions.BOTTOM, Directions.LEFT),
														  " ") :
												  " ");

				for (int i = 0; i < colSizes.length; i++)
				{
					joiner.add(mapping.getOrDefault(Directions.from(Directions.RIGHT, Directions.LEFT), " ").repeat(colSizes[i]));
				}
				sb.append(joiner);

				if (chosen.outside)
					sb.append(mapping.get(Directions.from(Directions.TOP, Directions.BOTTOM, Directions.LEFT)));
				sb.append("\n");
			}
			// table body
			{
				String colsep = chosen.columns ? mapping.getOrDefault(Directions.from(Directions.TOP, Directions.BOTTOM), " ") : " ";

				int start = 0;
				if (chosen.header)
				{
					start = 1;
				}
				for (; start < t.rows.size(); start++)
				{
					if (chosen.outside)
						sb.append(colsep);

					sb.append(t.rows.get(start).print(colSizes, colsep));

					if (chosen.outside)
						sb.append(colsep);
					sb.append("\n");
				}
			}

			if (chosen.outside)
			{
				sb.append(mapping.getOrDefault(Directions.from(Directions.TOP, Directions.RIGHT), " "));

				joiner = new StringJoiner(mapping.get(chosen.columns ?
															  Directions.from(Directions.TOP, Directions.RIGHT, Directions.LEFT) :
															  Directions.from(Directions.RIGHT, Directions.LEFT)));

				for (int i = 0; i < colSizes.length; i++)
				{
					joiner.add(mapping.get(Directions.from(Directions.RIGHT, Directions.LEFT)).repeat(colSizes[i]));
				}
				sb.append(joiner);

				sb.append(mapping.getOrDefault(Directions.from(Directions.TOP, Directions.LEFT), " "));
			}

			return sb.toString();
		}

		boolean bordered = false;

		enum Directions
		{
			TOP(1 << 0),
			BOTTOM(1 << 1),
			LEFT(1 << 2),
			RIGHT(1 << 3);

			final int i;

			Directions(int i)
			{
				this.i = i;
			}

			static Integer from(Directions... d)
			{
				int i = 0;
				for (Directions directions : d)
				{
					i |= directions.i;
				}
				return i;
			}
		}

		public enum OutlineType
		{
			NONE(false, false, false),
			COLUMNS(false, false, true),
			HEADER_COLUMNS(false, true, true),
			FULL(true, true, true);

			boolean outside = false;
			boolean header = false;
			boolean columns = false;

			OutlineType(boolean outside, boolean header, boolean columns)
			{
				this.outside = outside;
				this.header = header;
				this.columns = columns;
			}

			static OutlineType[] ALL = OutlineType.values();
		}

		private static TableConfig NO_LINES;
		private static TableConfig SIMPLE_LINES;
		private static TableConfig DOUBLE_LINES;
		private static TableConfig ASCII_LINES;
		private static TableConfig TAB_STOPS;

		HashMap<Integer, String> mapping;
		HashSet<OutlineType> supportings;

		public TableConfig(boolean bordered, OutlineType... supports)
		{
			this.bordered = bordered;
			this.mapping = new HashMap<>();
			this.supportings = new HashSet<>(Arrays.asList(supports));
		}

		TableConfig add(String part, Directions... d)
		{
			mapping.put(Directions.from(d), part);
			return this;
		}

		public static TableConfig NO_LINES()
		{
			if (NO_LINES == null)
			{
				NO_LINES = new TableConfig(false, OutlineType.COLUMNS, OutlineType.NONE).add(" ", Directions.TOP, Directions.BOTTOM);
			}

			return NO_LINES;
		}

		public static TableConfig SIMPLE_LINES()
		{
			if (SIMPLE_LINES == null)
			{
				SIMPLE_LINES = new TableConfig(true, OutlineType.ALL).add("\u2500", Directions.LEFT, Directions.RIGHT)
									   .add("\u2502", Directions.TOP, Directions.BOTTOM)
									   .add("\u250c", Directions.BOTTOM, Directions.RIGHT)
									   .add("\u2510", Directions.BOTTOM, Directions.LEFT)
									   .add("\u2514", Directions.TOP, Directions.RIGHT)
									   .add("\u2518", Directions.TOP, Directions.LEFT)
									   .add("\u251c", Directions.TOP, Directions.BOTTOM, Directions.RIGHT)
									   .add("\u2524", Directions.TOP, Directions.BOTTOM, Directions.LEFT)
									   .add("\u252c", Directions.BOTTOM, Directions.LEFT, Directions.RIGHT)
									   .add("\u2534", Directions.TOP, Directions.LEFT, Directions.RIGHT)
									   .add("\u253c", Directions.TOP, Directions.BOTTOM, Directions.LEFT, Directions.RIGHT);
			}
			return SIMPLE_LINES;
		}

		public static TableConfig DOUBLE_LINES()
		{
			if (DOUBLE_LINES == null)
			{
				DOUBLE_LINES = new TableConfig(true, OutlineType.ALL).add("\u2550", Directions.LEFT, Directions.RIGHT)
									   .add("\u2551", Directions.TOP, Directions.BOTTOM)
									   .add("\u2554", Directions.BOTTOM, Directions.RIGHT)
									   .add("\u2557", Directions.BOTTOM, Directions.LEFT)
									   .add("\u255a", Directions.TOP, Directions.RIGHT)
									   .add("\u255d", Directions.TOP, Directions.LEFT)
									   .add("\u2560", Directions.TOP, Directions.BOTTOM, Directions.RIGHT)
									   .add("\u2563", Directions.TOP, Directions.BOTTOM, Directions.LEFT)
									   .add("\u2566", Directions.BOTTOM, Directions.LEFT, Directions.RIGHT)
									   .add("\u2569", Directions.TOP, Directions.LEFT, Directions.RIGHT)
									   .add("\u256c", Directions.TOP, Directions.BOTTOM, Directions.LEFT, Directions.RIGHT);
			}
			return DOUBLE_LINES;
		}

		public static TableConfig ASCII_LINES()
		{
			if (ASCII_LINES == null)
			{
				ASCII_LINES = new TableConfig(true, OutlineType.ALL).add("-", Directions.LEFT, Directions.RIGHT)
									  .add("\u007c", Directions.TOP, Directions.BOTTOM)
									  .add("+", Directions.TOP, Directions.LEFT)
									  .add("+", Directions.TOP, Directions.RIGHT)
									  .add("+", Directions.BOTTOM, Directions.LEFT)
									  .add("+", Directions.BOTTOM, Directions.RIGHT)
									  .add("\u007c", Directions.TOP, Directions.BOTTOM, Directions.LEFT)
									  .add("\u007c", Directions.TOP, Directions.BOTTOM, Directions.RIGHT)
									  .add("-", Directions.TOP, Directions.LEFT, Directions.RIGHT)
									  .add("-", Directions.BOTTOM, Directions.LEFT, Directions.RIGHT)
									  .add("+", Directions.TOP, Directions.BOTTOM, Directions.LEFT, Directions.RIGHT);
			}

			return ASCII_LINES;
		}

		public static TableConfig TAB_STOPS()
		{
			if (TAB_STOPS == null)
			{
				TAB_STOPS = new TableConfig(false, OutlineType.COLUMNS)
									.add("\t", Directions.LEFT, Directions.RIGHT)
									.add("\t", Directions.TOP, Directions.BOTTOM);
			}
			return TAB_STOPS;
		}
	}

	private static class Row
	{
		LinkedList<Col> cols = new LinkedList<>();

		public String print(int[] lengths, String inbetween)
		{
			StringJoiner joiner = new StringJoiner(inbetween);
			for (int i = 0; i < cols.size() && i < lengths.length; i++)
			{
				joiner.add(cols.get(i).print(lengths[i]));
			}
			return joiner.toString();
		}

		int size()
		{
			return cols.size();
		}

		int length()
		{
			int i = 0;
			for (Col col : cols)
			{
				i += col.length() + 1;
			}
			return Math.max(0, i - 1);
		}

		int[] columnLengths(int count, int[] p)
		{
			if (p.length < count)
				p = new int[count];

			int[] i = new int[count];
			for (int i1 = 0; i1 < count && i1 < cols.size(); i1++)
			{
				i[i1] = Math.max(cols.get(i1).length(), p[i1]);
			}
			return i;
		}

		void add(String value)
		{
			this.cols.add(Col.of(value));
		}

		void add(Col value)
		{
			this.cols.add(value);
		}

		void setAlign(int colindex, Align align)
		{
			if (cols.size() <= colindex)
			{
				return;
			}
			cols.get(colindex).setAlign(align);
		}

		static Row from(Col firstValue)
		{
			Row row = new Row();
			row.cols.add(firstValue);
			return row;
		}
	}

	public static class Col
	{
		String value;
		Align align = Align.LEFT;

		public Col(String p)
		{
			value = p;
		}

		String print(int length)
		{
			if (align == Align.LEFT || align == Align.RIGHT)
			{
				String padding = padding(length - length());
				if (align == Align.LEFT)
					return value + padding;
				else
					return padding + value;
			}
			else
			{
				int padding = length - length();
				int padL = padding / 2, padR = padding / 2;
				if (padding % 2 == 1)
				{
					padR++;
				}
				return padding(padL) + value + padding(padR);
			}
		}

		static String padding(int length)
		{
			return " ".repeat(Math.max(0, length));
		}

		public static Col of(String p)
		{
			return new Col(p);
		}

		public Col setAlign(Align align)
		{
			this.align = align;
			return this;
		}

		int length()
		{
			if (value == null)
				return 0;
			return value.length();
		}
	}

	public static class ColoredCol extends Col
	{
		String color;

		public ColoredCol(String p, String color)
		{
			super(p);
			this.color = color;
		}

		@Override
		String print(int length)
		{
			return applyColor(super.print(length), color);
		}

		public static Col of(String p, String color)
		{
			return new ColoredCol(p, color);
		}

		public static final String COLOR_RESET = "0";
		public static final String BLACK = "30";
		public static final String RED = "31";
		public static final String GREEN = "32";
		public static final String YELLOW = "33";
		public static final String BLUE = "34";
		public static final String MAGENTA = "35";
		public static final String CYAN = "36";
		public static final String WHITE = "37";
		public static final String LIGHT_BLACK = "90";
		public static final String LIGHT_RED = "91";
		public static final String LIGHT_GREEN = "92";
		public static final String LIGHT_YELLOW = "93";
		public static final String LIGHT_BLUE = "94";
		public static final String LIGHT_MAGENTA = "95";
		public static final String LIGHT_CYAN = "96";
		public static final String LIGHT_WHITE = "97";
	}

	public enum Align
	{
		LEFT,
		CENTER,
		RIGHT;
	}
}