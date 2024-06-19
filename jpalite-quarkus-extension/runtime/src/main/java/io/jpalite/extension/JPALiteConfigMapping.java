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

package io.jpalite.extension;

import io.jpalite.JPALiteEntityManager;
import io.jpalite.impl.CacheFormat;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.*;
import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import jakarta.persistence.spi.PersistenceUnitTransactionType;
import org.eclipse.microprofile.config.spi.Converter;

import java.util.Map;

@ConfigMapping(prefix = "jpalite.persistenceUnit")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface JPALiteConfigMapping
{
	/**
	 * The default persistenceUnit
	 *
	 * @return The default value
	 */
	@WithParentName
	PersistenceUnitConfig defaultPersistenceUnit();

	/**
	 * The defined units
	 *
	 * @return Map of units
	 */
	@ConfigDocSection
	@ConfigDocMapKey("name")
	@WithParentName
	Map<String, PersistenceUnitConfig> namedPersistenceUnits();

	interface PersistenceUnitConfig
	{
		/**
		 * Specifies a valid datasource that will be associated with the persistence unit.
		 * The default value points to the default datasource.
		 */
		@WithDefault("<default>")
		@WithName("datasource-name")
		String datasourceName();

		/**
		 * The region prefix to use (second level caching)
		 */
		@WithDefault("<default>")
		@WithName("cache-region-prefix")
		String cacheRegionPrefix();

		/**
		 * Retrieves the cache client (infinispan-client, redis-client, etc) to be used by the cache provider.
		 * Care should be taken to reference a client that is compatible with the cache provider specified.
		 *
		 * @return The cache client name as specified in the persistence unit configuration, or the default value if not set.
		 * @see PersistenceUnitConfig#cacheClient()
		 */
		@WithDefault("<default>")
		@WithName("cache-client")
		String cacheClient();

		/**
		 * The name of the 2nd level cache provider that is to be used.
		 */
		@WithDefault("io.jpalite.impl.caching.JPALiteInfinispanCache")
		@WithName("cache-provider")
		String cacheProvider();

		/**
		 * The cache configuration to use when create a new cache
		 */
		@WithDefault("<distributed-cache mode=\"ASYNC\" statistics=\"true\">" +
				"  <encoding media-type=\"application/x-protostream\"/>" +
				"  <memory max-count=\"1000000\" when-full=\"REMOVE\"/>" +
				"</distributed-cache>")
		@WithName("cache-config")
		String cacheConfig();

		/**
		 * The cache format to use when storing cache entities
		 */
		@WithDefault("JSON")
		@WithName("cache-format")
		@WithConverter(CacheFormatConverter.class)
		CacheFormat cacheFormat();

		/**
		 * Set to TRUE if the persistence unit reference a multi-tenant data source
		 */
		@WithDefault("FALSE")
		@WithName("multi-tenant")
		Boolean multiTenant();

		/**
		 * The cache share mode
		 */
		@WithDefault("ENABLE_SELECTIVE")
		@WithConverter(SharedCacheModeConverter.class)
		@WithName("shared-cache-mode")
		SharedCacheMode sharedCacheMode();

		/**
		 * Additional properties associated with the persistence unit.
		 * See {@link JPALiteEntityManager} for a list of valid properties.
		 */
		Map<String, String> properties();

		/**
		 * Set the transaction type value for the persistence unit.
		 */
		@WithName("transaction-type")
		@WithDefault("RESOURCE_LOCAL")
		@WithConverter(PersistenceUnitTransactionTypeConverter.class)
		PersistenceUnitTransactionType transactionType();

		/**
		 * The validation mode
		 */
		@WithName("validation-mode")
		@WithDefault("NONE")
		@WithConverter(ValidationModeConverter.class)
		ValidationMode validationMode();
	}//PersistenceUnitConfig

	class PersistenceUnitTransactionTypeConverter implements Converter<PersistenceUnitTransactionType>
	{
		@Override
		public PersistenceUnitTransactionType convert(String value) throws IllegalArgumentException, NullPointerException
		{
			return PersistenceUnitTransactionType.valueOf(value.toUpperCase());
		}
	}//PersistenceUnitTransactionTypeConverter

	class SharedCacheModeConverter implements Converter<SharedCacheMode>
	{
		@Override
		public SharedCacheMode convert(String value) throws IllegalArgumentException, NullPointerException
		{
			return SharedCacheMode.valueOf(value.toUpperCase());
		}
	}

	class CacheFormatConverter implements Converter<CacheFormat>
	{
		@Override
		public CacheFormat convert(String value) throws IllegalArgumentException, NullPointerException
		{
			return CacheFormat.valueOf(value.toUpperCase());
		}
	}

	class ValidationModeConverter implements Converter<ValidationMode>
	{
		@Override
		public ValidationMode convert(String value) throws IllegalArgumentException, NullPointerException
		{
			return ValidationMode.valueOf(value.toUpperCase());
		}
	}

	default PersistenceUnitConfig getPersistenceUnit(String persistenceUnitName)
	{
		if ("<default>".equals(persistenceUnitName)) {
			return defaultPersistenceUnit();
		}//if

		return namedPersistenceUnits().get(persistenceUnitName);
	}//getPersistenceUnit
}//JPAConfigMapping
