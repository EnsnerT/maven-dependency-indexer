package com.github.ensnert.api;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.function.Function;


@SuppressWarnings("UnusedReturnValue")
public final class SaveStated<T>
{
	private T currentState;
	private final Function<T, T> cloner;

	final HashMap<Serializable, T> saveStates = new HashMap<>();

	SaveStated(T state)
	{
		this(state, null);
	}

	SaveStated(T state, Function<T, T> cloner)
	{
		this.currentState = state;
		this.cloner = cloner;
	}

	public static <U> SaveStated<U> of(U state)
	{
		return new SaveStated<>(state);
	}

	public static <U> SaveStated<U> of(U state, Function<U, U> cloner)
	{
		return new SaveStated<>(state, cloner);
	}

	public T get()
	{
		return currentState;
	}

	@SuppressWarnings("unchecked")
	// i dont know why constructors()[0]:Constructor<?> and clone():Object must be that unspecific. a clone can not be something else than <? extends T> nor can a constructor create something else than T; yes super() exists but it still is <? super T>
	private static <U> U clone(U state, Function<U, U> cloner) throws CloneNotSupportedException
	{
		try
		{
			if (cloner != null)
			{
				return cloner.apply(state);
			}
		}
		catch (Exception e)
		{
			CloneNotSupportedException cloneNotSupportedException = new CloneNotSupportedException(e.getMessage());
			cloneNotSupportedException.initCause(e);
			throw cloneNotSupportedException;
		}

		try
		{
			Method clone = state.getClass().getMethod("clone");
			try
			{
				clone.setAccessible(true);
			}
			catch (Exception ignored)
			{
			}
			Object result = clone.invoke(state);
			if (state.getClass().isInstance(result))
				return (U) result;
			return (U) result;

		}
		catch (NoSuchMethodException e)
		{
			CloneNotSupportedException cloneNotSupportedException = new CloneNotSupportedException("No \"clone\" method found");
			cloneNotSupportedException.initCause(e);
			throw cloneNotSupportedException;
		}
		catch (InvocationTargetException e)
		{
			CloneNotSupportedException cloneNotSupportedException = new CloneNotSupportedException("\"clone\" had a issue: " + e.getMessage());
			cloneNotSupportedException.initCause(e);
			throw cloneNotSupportedException;
		}
		catch (IllegalAccessException e)
		{
			CloneNotSupportedException cloneNotSupportedException = new CloneNotSupportedException("Can not access: \"clone\"");
			cloneNotSupportedException.initCause(e);
			throw cloneNotSupportedException;
		}
	}

	public SaveStated<T> save(Serializable stateIdentifier) throws CloneNotSupportedException
	{
		saveStates.put(stateIdentifier, clone(currentState, this.cloner));
		return this;
	}

	public SaveStated<T> trySave(Serializable stateIdentifier)
	{
		try
		{
			return save(stateIdentifier);
		}
		catch (CloneNotSupportedException e)
		{
			throw new RuntimeException(e);
		}
	}

	public SaveStated<T> revert(Serializable stateIdentifier)
	{
		if (saveStates.containsKey(stateIdentifier))
		{
			try
			{
				currentState = clone(saveStates.get(stateIdentifier), this.cloner);
			}
			catch (CloneNotSupportedException e)
			{
				throw new RuntimeException(e); /// this sould be noticed earlier!
			}
			return this;
		}
		return this;
	}

	public SaveStated<T> drop(Serializable stateIdentifier)
	{
		saveStates.remove(stateIdentifier);
		return this;
	}

	public SaveStated<T> dropAll()
	{
		saveStates.clear();
		return this;
	}

	@Override
	@SuppressWarnings("MethodDoesntCallSuperMethod")
	public Object clone() throws CloneNotSupportedException
	{
		SaveStated<T> clone = SaveStated.of(clone(currentState, cloner), cloner);
		clone.saveStates.clear();
		clone.saveStates.putAll(saveStates);

		return clone;
	}
}
