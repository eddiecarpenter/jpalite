/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.jpalite;

import jakarta.annotation.Nonnull;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.SynchronizationType;
import jakarta.persistence.TransactionRequiredException;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Map;

public interface PersistenceContext extends EntityTransaction
{
	/**
	 * The jpalite.persistence.jta hint is used to signal the transaction management under JTA control.
	 */
	String PERSISTENCE_JTA_MANAGED = "jpalite.persistence.jta";

	/**
	 * The method is used to retrieve the persistence unit used to create the context
	 *
	 * @return The persistence unit
	 */
	JPALitePersistenceUnit getPersistenceUnit();

	/**
	 * Open a new connection. If already open stack will be maintained to keep track of the number of times the
	 * connection has been opened with closing it. The cursor name is null current thread name is used.
	 *
	 * @param connectionName The connection name
	 * @return The connection
	 */
	@Nonnull
	Connection getConnection(String connectionName);

	/**
	 * Close the connection, if the connection was opened previously the open stack will be popped. If forced the open
	 * stack will be flushed and the connection will be closed.
	 */
	void close();

	/**
	 * Release the connection regardless of the number of times it was opened
	 */
	void release();

	/**
	 * Return true if the persistence context has be released from the database pool
	 *
	 * @return true if released
	 */
	boolean isReleased();

	/**
	 * Register a transaction listener. The listener will be called whenever a new transaction started, committed of
	 * rolled back
	 *
	 * @param listener The transaction listener to add
	 */
	void addTransactionListener(EntityTransactionListener listener);

	/**
	 * Removes a transaction listener from the persistence context.
	 *
	 * @param listener The transaction listener to remove
	 */
	void removeTransactionListener(EntityTransactionListener listener);

	/**
	 * Return number of times the same a transaction has been started on the same connection
	 *
	 * @return The transaction depth
	 */
	int getTransactionDepth();

	/**
	 * Called by the database undertow to report the query that was execute
	 *
	 * @param lastQuery The query
	 */
	void setLastQuery(String lastQuery);

	/**
	 * Get the last executed query
	 *
	 * @return The query
	 */
	String getLastQuery();

	/**
	 * Return the open level the connection is at
	 *
	 * @return the open level
	 */
	int getOpenLevel();

	/**
	 * Get the current cursor name that will be used
	 *
	 * @return The cursor name
	 */
	String getConnectionName();

	/**
	 * Set the cursor name to be used when request a connection
	 *
	 * @param connectionName the cursor name
	 */
	void setConnectionName(String connectionName);

	/**
	 * Return the Level 1 cache instance of {@link EntityLocalCache} linked to the context.
	 *
	 * @return the cache
	 */
	EntityLocalCache l1Cache();

	/**
	 * Return the Level 2 cache instance of {@link EntityLocalCache} linked to the context.
	 *
	 * @return the cache
	 */
	EntityCache l2Cache();

	/**
	 * Map the ResultSet to the given entity and the entity to the persistence context
	 *
	 * @param entity    The entity
	 * @param resultSet the {@link ResultSet}
	 * @return The entity
	 */
	<X> X mapResultSet(@Nonnull X entity, ResultSet resultSet);

	/**
	 * Map the ResultSet to the given entity and the entity to the persistence context
	 *
	 * @param entity    The entity
	 * @param colPrefix Only used column from the result set that starts with colPrefix
	 * @param resultSet the {@link ResultSet}
	 * @return The entity
	 */
	<X> X mapResultSet(@Nonnull X entity, String colPrefix, ResultSet resultSet);

	/**
	 * Synchronize the persistence context to the underlying database.
	 *
	 * @throws TransactionRequiredException if there is no transaction or if the entity manager has not been joined to
	 *                                      the current transaction
	 * @throws PersistenceException         if the flush fails
	 */
	void flush();

	/**
	 * Synchronize the entity to the underlying database.
	 *
	 * @throws TransactionRequiredException if there is no transaction or if the entity manager has not been joined to
	 *                                      the current transaction
	 * @throws PersistenceException         if the flush fails
	 * @throws IllegalStateException        if the connection is not open
	 */
	void flushEntity(@Nonnull JPAEntity entity);

	/**
	 * Synchronize the all entity of a given type belonging to the persistence context to the underlying database.
	 *
	 * @throws TransactionRequiredException if there is no transaction or if the entity manager has not been joined to
	 *                                      the current transaction
	 * @throws PersistenceException         if the flush fails
	 * @throws IllegalStateException        if the connection is not open
	 */
	void flushOnType(Class<?> entityClass);

	/**
	 * Return the resource-level <code>EntityTransaction</code> object. The <code>EntityTransaction</code> instance may
	 * be used serially to begin and commit multiple transactions.
	 *
	 * @return EntityTransaction instance
	 * @throws IllegalStateException if invoked on a JTA entity manager
	 */
	EntityTransaction getTransaction();

	/**
	 * Return an object of the specified type to allow access to the provider-specific API.
	 *
	 * @param cls the class of the object to be returned.
	 * @return an instance of the specified class
	 * @throws IllegalArgumentException if the cls is not unwrapped
	 */
	<T> T unwrap(Class<T> cls);

	/**
	 * Enable the context to detect and join a JTA transaction
	 */
	void setAutoJoinTransaction();

	/**
	 * return the current configured JTA status
	 *
	 * @return {@link SynchronizationType}
	 */
	boolean isAutoJoinTransaction();

	/**
	 * Indicate to the persistence context that a JTA transaction is active and join the persistence context to it.
	 * <p>This method should be called on a JTA application managed entity manager that was created outside the scope
	 * of the active transaction or on an entity manager of type
	 * <code>SynchronizationType.UNSYNCHRONIZED</code> to associate it with the current JTA transaction.
	 *
	 * @throws TransactionRequiredException if there is no transaction
	 */
	void joinTransaction();

	/**
	 * Determine whether the persistence context is joined to the current transaction. Returns false if the entity
	 * manager is not joined to the current transaction or if no transaction is active
	 *
	 * @return boolean
	 */
	boolean isJoinedToTransaction();

	/**
	 * Set a persistance context property or hint. If a vendor-specific property or hint is not recognized, it is
	 * silently ignored.
	 *
	 * @param name  name of property or hint
	 * @param value value for property or hint
	 * @throws IllegalArgumentException if the second argument is not valid for the implementation
	 */
	void setProperty(String name, Object value);

	/**
	 * Get the properties and hints and associated values that are in effect for the persistence context. Changing the
	 * contents of the map does not change the configuration in effect.
	 *
	 * @return map of properties and hints in effect for persistence context
	 */
	Map<String, Object> getProperties();

	/**
	 * This method is called by the transaction
	 * manager after the transaction is committed or rolled back.
	 *
	 * @param status The status of the transaction completion.
	 */
	void afterCompletion(int status);
}
