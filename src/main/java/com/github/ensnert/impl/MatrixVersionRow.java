package com.github.ensnert.impl;

import java.util.ArrayList;


final class MatrixVersionRow extends ArrayList<String> implements Comparable<MatrixVersionRow>
{
	@Override
	public int compareTo(MatrixVersionRow o)
	{
		if (o == null)
		{
			return -1;
		}
		// then just compare the first non empty entry
		int min = Math.min(size(), o.size());
		for (int i = 0; i < min; i++)
		{
			if (get(i).isEmpty() || o.get(i).isEmpty())
				continue;

			int compare = VersionComparator.compare(get(i), o.get(i));
			if (compare != 0)
				return compare;
		}

		return 0;
	}
}
