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

import io.jpalite.impl.CacheFormat;
import jakarta.persistence.spi.PersistenceUnitInfo;

public interface JPALitePersistenceUnit extends PersistenceUnitInfo
{
	/**
	 * The name of the datasource to associate with persistence unit
	 *
	 * @return The datasource name
	 */
	String getDataSourceName();

	/**
	 * The cache region prefix. If null or blank no prefix will be applied
	 *
	 * @return The prefix
	 */
	String getCacheRegionPrefix();

	/**
	 * Retrieves the cache provider for the persistence unit.
	 *
	 * @return The cache provider as a string
	 */
	String getCacheProvider();

	/**
	 * Gets the cache configuration for the persistence unit.
	 *
	 * @return The cache configuration as a string
	 */
	String getCacheConfig();


	/**
	 * Gets the cache client for the persistence unit.
	 *
	 * @return The cache client as a string
	 */
	String getCacheClient();

	/**
	 * Retrieves the cache format for the persistence unit.
	 *
	 * @return The cache format for the persistence unit. The possible values are:
	 * - {@link CacheFormat#BINARY}: Indicates that the cache format is binary.
	 * - {@link CacheFormat#JSON}: Indicates that the cache format is JSON.
	 */
	CacheFormat getCacheFormat();

	Boolean getMultiTenantMode();
}
