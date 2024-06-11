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

import java.util.function.Consumer;

public interface EntityLocalCache
{
	/**
	 * Mark all the entities in the cache as DETACHED and clear the cache
	 */
	void clear();

	/**
	 * Search the cache for an entity using the primary key. Null will be returned if the entity not found or is
	 * REMOVED.
	 *
	 * @param entityType The class type of the entity
	 * @param primaryKey The primary key
	 * @return the entity or null if not found or if the entity is REMOVED
	 */
	<T> T find(Class<T> entityType, Object primaryKey);

	/**
	 * Search the cache for an entity using the primary key. If the entity not found and or has a status of REMOVED null
	 * will be returned.
	 *
	 * @param entityType     The class type of the entity
	 * @param primaryKey     The primary key
	 * @param checkIfRemoved if true throw an {@link IllegalArgumentException} if the entity has a status of REMOVED.
	 * @return the entity or null if not found
	 * @throws IllegalArgumentException if the entity has a status of REMOVED.
	 */
	<T> T find(Class<T> entityType, Object primaryKey, boolean checkIfRemoved);

	/**
	 * Search the cache for all entity of type entityType and performs an action for each element found.
	 *
	 * @param entityType The entity class type
	 * @param action     a <a href="package-summary.html#NonInterference"> non-interfering</a> action to perform on the
	 *                   elements
	 */
	<T> void foreachType(Class<T> entityType, Consumer<T> action);

	/**
	 * Performs an action for each element of this stream.
	 *
	 * @param action a <a href="package-summary.html#NonInterference"> non-interfering</a> action to perform on the
	 *               elements
	 */
	void foreach(Consumer<Object> action);

	/**
	 * Attach an entity to the cache and mark the entity as ATTACHED. If there is no active transaction the entity will
	 * not be attached and the entity will be marked as DETACHED.
	 *
	 * @param entity The entity to attach
	 */
	void manage(JPAEntity entity);

	/**
	 * Detach an entity from the cache and mark the entity as DETACHED
	 *
	 * @param entity the entity to detach
	 */
	void detach(JPAEntity entity);

	/**
	 * Check if the given entity is current attached to the cache
	 *
	 * @param entity The entity to check
	 * @return true if attached
	 */
	boolean contains(JPAEntity entity);
}
