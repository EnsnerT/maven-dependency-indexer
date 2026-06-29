package ch.ensnert.api;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;
import java.util.StringJoiner;


public record Index(Serializable... cache)
{
	@Override
	public Serializable[] cache()
	{
		return Arrays.copyOf(cache, cache.length);
	}

	public boolean same(Serializable... other)
	{
		if (this.cache == null || other == null)
		{
			return this.cache == other;
		}
		if (this.cache.length != other.length)
			return false;
		for (int i = 0; i < this.cache.length; i++)
		{
			if (!Objects.equals(this.cache[i], other[i]))
				return false;
		}
		return true;
	}

	@Override
	public boolean equals(Object o)
	{
		if (!(o instanceof Index that))
			return false;
		return Objects.deepEquals(cache, that.cache);
	}

	@Override
	public int hashCode()
	{
		return Arrays.hashCode(cache);
	}

	@Override
	public String toString()
	{
		// StringBuilder stringBuilder = new StringBuilder();
		StringJoiner stringJoiner = new StringJoiner(", ", Index.class.getSimpleName() + "{", "}");
		// stringBuilder.append("Index {");
		for (Serializable serializable : cache())
		{
			stringJoiner.add(serializable.toString());
		}
		// stringBuilder.append("}");
		return stringJoiner.toString();
	}
}
