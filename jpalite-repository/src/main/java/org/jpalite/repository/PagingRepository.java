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

package org.jpalite.repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * The PagingRepository is part of the JPALite Repository generation feature.
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
 * public interface MyRepository implements PagingRepository<MyEntity> {
 * }
 *
 * }</pre>
 * <p>
 *
 * @param <E> The Entity class the paging repository controls
 */
public interface PagingRepository<E> extends RepositoryBase
{
	/**
	 * Retrieve ALL the entities in the repository.
	 * <p>
	 * NOTE: This method should NOT be used on a repository that has a large number of entries
	 * </p>
	 *
	 * @return A list of the all the entities
	 */
	default List<E> findAll()
	{
		return findAll(Pageable.unpaged(), Filter.noFilter(), Collections.emptyMap());
	}//findAll

	default List<E> findAll(Map<String, Object> hints)
	{
		return findAll(Pageable.unpaged(), Filter.noFilter(), hints);
	}//findAll

	/**
	 * Retrieve ALL the entities in the repository using a pageable control
	 * <p>
	 * NOTE: Take care not to specify a page size that cannot fit in memory
	 * </p>
	 *
	 * @return A list of the entities based on the pageable control
	 */
	default List<E> findAll(Pageable pageable)
	{
		return findAll(pageable, Filter.noFilter(), Collections.emptyMap());
	}//findAll

	default List<E> findAll(Pageable pageable, Map<String, Object> hints)
	{
		return findAll(pageable, Filter.noFilter(), hints);
	}//findAll

	/**
	 * Retrieve ALL the entities in the repository using a filter control
	 * <p>
	 * NOTE: This method should NOT be used on a repository that has a large number of entries
	 * </p>
	 *
	 * @param filter The filter control
	 * @return A list of the all the filtered entities
	 */
	default List<E> findAll(Filter filter)
	{
		return findAll(Pageable.unpaged(), filter, Collections.emptyMap());
	}//findAll

	default List<E> findAll(Filter filter, Map<String, Object> hints)
	{
		return findAll(Pageable.unpaged(), filter, hints);
	}//findAll

	/**
	 * Retrieve ALL the entities in the repository order by the Sort control
	 * <p>
	 * NOTE: This method should NOT be used on a repository that has a large number of entries
	 * </p>
	 *
	 * @param sort The sort control
	 * @return A list of the all the entities
	 */
	default List<E> findAll(Sort sort)
	{
		return findAll(Pageable.of(sort), Filter.noFilter(), Collections.emptyMap());
	}//findAll

	default List<E> findAll(Sort sort, Map<String, Object> hints)
	{
		return findAll(Pageable.of(sort), Filter.noFilter(), hints);
	}//findAll

	/**
	 * Retrieve the entities from repository using a pageable and filter control
	 * <p>
	 * NOTE: This is the recommend method to retrieve entity from a large repository. Take care not to specify a page
	 * size that cannot fit in memory
	 * </p>
	 *
	 * @param pageable The pageable control
	 * @param filter   The filter control
	 * @return A list of the all the filtered entities using the pageable control to limit the number entities returned
	 */
	default List<E> findAll(Pageable pageable, Filter filter)
	{
		return findAll(pageable, filter, Collections.emptyMap());
	}//findAll

	List<E> findAll(Pageable pageable, Filter filter, Map<String, Object> hints);


	default long count(Filter filter)
	{
		return count(filter, Collections.emptyMap());
	}

	/**
	 * Return the number of entities found in the repository.
	 *
	 * @return The number of entities
	 */
	default long count()
	{
		return count(Filter.noFilter(), Collections.emptyMap());
	}

	default long count(Map<String, Object> hints)
	{
		return count(Filter.noFilter(), hints);
	}

	/**
	 * Return the number of entities found in the repository using the filter control
	 *
	 * @param filter The filter control
	 * @return The number of entities
	 */
	long count(Filter filter, Map<String, Object> hints);
}//PagingRepository
