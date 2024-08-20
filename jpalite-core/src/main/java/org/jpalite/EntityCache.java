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
import jakarta.persistence.Cache;
import jakarta.transaction.SystemException;

import java.time.Instant;

public interface EntityCache extends Cache
{
	/**
	 * Create a new transaction and associate it with the current thread.
	 *
	 * @throws jakarta.transaction.SystemException Thrown if the transaction manager
	 *                                             encounters an unexpected error condition.
	 */
	public void begin() throws SystemException;

	/**
	 * Complete the transaction associated with the current thread. When this
	 * method completes, the thread is no longer associated with a transaction.
	 *
	 * @throws SystemException Thrown if the transaction manager
	 *                         encounters an unexpected error condition.
	 */
	void commit() throws SystemException;

	/**
	 * Roll back the transaction associated with the current thread. When this
	 * method completes, the thread is no longer associated with a
	 * transaction.
	 *
	 * @throws SystemException Thrown if the transaction manager
	 *                         encounters an unexpected error condition.
	 */
	void rollback() throws SystemException;

	/**
	 * Search the cache for an entity using the primary key.
	 *
	 * @param entityType The class type of the entity
	 * @param primaryKey The primary key
	 * @return the entity or null if not found
	 */
	<T> T find(Class<T> entityType, Object primaryKey);

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
	void replace(JPAEntity entity);


	/**
	 * Return the time for when an entity-type was last updated
	 *
	 * @param entityType The entity type
	 * @return The instance when the entity was last updated
	 */
	@Nonnull
	<T> Instant getLastModified(Class<T> entityType);

	/**
	 * Return the time for when an entity-type was last updated
	 *
	 * @param entityType The entity type
	 * @return The time since epoch the entity was updated
	 * @deprecated Replaced by {{@link #getLastModified(Class)}}
	 */
	@Deprecated(forRemoval = true, since = "3.0.0")
	default <T> long lastModified(Class<T> entityType)
	{
		return getLastModified(entityType).toEpochMilli();
	}
}
