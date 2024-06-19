package io.jpalite;

import jakarta.persistence.PersistenceException;

public class PersistenceUnitNotFoundException extends PersistenceException
{
	public PersistenceUnitNotFoundException()
	{
		super();
	}

	public PersistenceUnitNotFoundException(String message)
	{
		super(message);
	}

	public PersistenceUnitNotFoundException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public PersistenceUnitNotFoundException(Throwable cause)
	{
		super(cause);
	}
}
