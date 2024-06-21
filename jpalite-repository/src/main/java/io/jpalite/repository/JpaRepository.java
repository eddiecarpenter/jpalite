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

package io.jpalite.repository;

import jakarta.persistence.LockModeType;

/**
 * The JpaRepository is part of the JPALite Repository generation feature.
 * <p>The implementation of the interface
 * <b>must not</b> be coded and is generated by the JPALite Repository Generator
 * <p><b>Remember to attach the interface
 * to the Repository Generator using the @Repository annotation!</b>
 * <p>
 * <p>
 * Usage Example:
 * <pre>{@code
 *
 * @Repository
 * public interface MyRepository implements JpaRepository<MyEntity, Long> {
 * }
 *
 * }</pre>
 * <p>
 *
 * @param <E> The Entity class type
 * @param <I> The Primary Key class type
 */
public interface JpaRepository<E, I> extends RepositoryBase
{
	/**
	 * Save an entity. If the entity is new it will be persis. If it is an existing entity it will be merged
	 *
	 * @param entity The entity to save
	 */
	void save(E entity);

	/**
	 * Persist the entity if it is not attached
	 *
	 * @param entity The entity to persist
	 */
	void persist(E entity);

	/**
	 * Lock the current entity from being updated by another thread
	 *
	 * @param entity       The entity
	 * @param lockModeType The locktype
	 */
	void lock(E entity, LockModeType lockModeType);

	/**
	 * Merge entity into the current context.
	 *
	 * @param entity The entity
	 * @return The replaced entity
	 */
	E merge(E entity);

	/**
	 * Replace the current entity with the value from storage
	 *
	 * @param entity the entity to refresh
	 */
	void refresh(E entity);

	/**
	 * Search for an entity given it's primary key
	 *
	 * @param id The primary key
	 * @return The retrieved entity or null if not found
	 */
	E findById(I id);

	E findById(I id, LockModeType lockModeType);

	/**
	 * Create a reference to an existing entity given the primary key
	 *
	 * @param id The primary key
	 * @return The entity
	 */
	E getReference(I id);

	/**
	 * Delete an attached entity from the repository.
	 *
	 * @param entity
	 */
	void delete(E entity);
}
