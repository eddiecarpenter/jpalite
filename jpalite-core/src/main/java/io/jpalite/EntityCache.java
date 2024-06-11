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

package io.jpalite;

import jakarta.annotation.Nonnull;
import jakarta.persistence.Cache;
import jakarta.transaction.*;

import java.util.List;

public interface EntityCache extends Cache
{
	/**
	 * Create a new transaction and associate it with the current thread.
	 *
	 * @throws jakarta.transaction.NotSupportedException Thrown if the thread is already
	 *                                                   associated with a transaction and the Transaction Manager
	 *                                                   implementation does not support nested transactions.
	 * @throws jakarta.transaction.SystemException       Thrown if the transaction manager
	 *                                                   encounters an unexpected error condition.
	 */
	public void begin() throws NotSupportedException, SystemException;

	/**
	 * Complete the transaction associated with the current thread. When this
	 * method completes, the thread is no longer associated with a transaction.
	 *
	 * @throws jakarta.transaction.RollbackException          Thrown to indicate that
	 *                                                        the transaction has been rolled back rather than committed.
	 * @throws jakarta.transaction.HeuristicMixedException    Thrown to indicate that a heuristic
	 *                                                        decision was made and that some relevant updates have been committed
	 *                                                        while others have been rolled back.
	 * @throws jakarta.transaction.HeuristicRollbackException Thrown to indicate that a
	 *                                                        heuristic decision was made and that all relevant updates have been
	 *                                                        rolled back.
	 * @throws SecurityException                              Thrown to indicate that the thread is
	 *                                                        not allowed to commit the transaction.
	 * @throws IllegalStateException                          Thrown if the current thread is
	 *                                                        not associated with a transaction.
	 * @throws SystemException                                Thrown if the transaction manager
	 *                                                        encounters an unexpected error condition.
	 */
	void commit() throws RollbackException,
						 HeuristicMixedException, HeuristicRollbackException, SecurityException,
						 IllegalStateException, SystemException;

	/**
	 * Roll back the transaction associated with the current thread. When this
	 * method completes, the thread is no longer associated with a
	 * transaction.
	 *
	 * @throws SecurityException     Thrown to indicate that the thread is
	 *                               not allowed to roll back the transaction.
	 * @throws IllegalStateException Thrown if the current thread is
	 *                               not associated with a transaction.
	 * @throws SystemException       Thrown if the transaction manager
	 *                               encounters an unexpected error condition.
	 */
	void rollback() throws IllegalStateException, SecurityException,
						   SystemException;

	/**
	 * Search the cache for an entity using the primary key.
	 *
	 * @param entityType The class type of the entity
	 * @param primaryKey The primary key
	 * @return the entity or null if not found
	 */
	<T> T find(Class<T> entityType, Object primaryKey);

	/**
	 * Search for the entity in the cache using the where clause
	 *
	 * @param entityType
	 * @param query
	 * @param <T>
	 * @return
	 */
	@Nonnull
	<T> List<T> search(Class<T> entityType, String query);

	/**
	 * Add an entity to the cache.
	 *
	 * @param entity The entity to attach
	 */
	void add(JPAEntity entity);

	/**
	 * Add or update an entity to the cache. The last modified timestamp will also be updated
	 *
	 * @param entity The entity to update or add
	 */
	void update(JPAEntity entity);

	/**
	 * Detach an entity from the cache and mark the entity as DETACHED
	 *
	 * @param entity the entity to detach
	 */
	void remove(JPAEntity entity);

	/**
	 * Return the time for when an entity-type was last updated
	 *
	 * @param entityType The entity type
	 * @param <T>
	 * @return The time since epoch the entity was updated
	 */
	<T> long lastModified(Class<T> entityType);
}
