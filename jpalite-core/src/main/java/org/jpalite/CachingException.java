package org.jpalite;

import jakarta.persistence.PersistenceException;

public class CachingException extends PersistenceException
{
	public CachingException()
	{
		super();
	}

	public CachingException(String message)
	{
		super(message);
	}

	public CachingException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public CachingException(Throwable cause)
	{
		super(cause);
	}
}
