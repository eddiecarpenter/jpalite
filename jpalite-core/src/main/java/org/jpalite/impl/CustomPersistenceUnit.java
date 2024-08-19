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

package org.jpalite.impl;

import org.jpalite.JPALitePersistenceUnit;
import org.jpalite.impl.providers.JPALitePersistenceProviderImpl;
import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import jakarta.persistence.spi.ClassTransformer;
import jakarta.persistence.spi.PersistenceUnitTransactionType;
import lombok.Setter;

import javax.sql.DataSource;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

@Setter
public class CustomPersistenceUnit implements JPALitePersistenceUnit
{
	private final String persistenceUnitName;
	private final Properties properties;

	private Boolean multiTenantMode = false;
	private String dataSourceName;
	private String cacheRegionPrefix;
	private String cacheClient;
	private String cacheProvider;
	private String cacheConfig;
	private CacheFormat cacheFormat;
	private String providerClass = JPALitePersistenceProviderImpl.class.getName();
	private PersistenceUnitTransactionType transactionType = PersistenceUnitTransactionType.RESOURCE_LOCAL;
	private ValidationMode validationMode = ValidationMode.NONE;
	private SharedCacheMode sharedCacheMode = SharedCacheMode.ENABLE_SELECTIVE;

	public CustomPersistenceUnit(String persistenceUnitName)
	{
		this.persistenceUnitName = persistenceUnitName;
		properties = new Properties();
	}

	@Override
	public String getDataSourceName()
	{
		return dataSourceName;
	}

	@Override
	public String getCacheRegionPrefix()
	{
		return cacheRegionPrefix;
	}

	@Override
	public String getCacheProvider()
	{
		return cacheProvider;
	}

	@Override
	public String getCacheClient()
	{
		return cacheClient;
	}

	@Override
	public String getCacheConfig()
	{
		return cacheConfig;
	}

	@Override
	public CacheFormat getCacheFormat()
	{
		return cacheFormat;
	}

	@Override
	public Boolean getMultiTenantMode()
	{
		return multiTenantMode;
	}

	@Override
	public String getPersistenceUnitName()
	{
		return persistenceUnitName;
	}

	@Override
	public String getPersistenceProviderClassName()
	{
		return providerClass;
	}

	@Override
	public PersistenceUnitTransactionType getTransactionType()
	{
		return transactionType;
	}

	@Override
	public DataSource getJtaDataSource()
	{
		return null;
	}

	@Override
	public DataSource getNonJtaDataSource()
	{
		return null;
	}

	@Override
	public List<String> getMappingFileNames()
	{
		return Collections.emptyList();
	}

	@Override
	public List<URL> getJarFileUrls()
	{
		return Collections.emptyList();
	}

	@Override
	public URL getPersistenceUnitRootUrl()
	{
		throw new UnsupportedOperationException("Method not supported");
	}

	@Override
	public List<String> getManagedClassNames()
	{
		//All classes are managed
		return Collections.emptyList();
	}

	@Override
	public boolean excludeUnlistedClasses()
	{
		return false;
	}

	@Override
	public SharedCacheMode getSharedCacheMode()
	{
		return sharedCacheMode;
	}

	@Override
	public ValidationMode getValidationMode()
	{
		return validationMode;
	}

	@Override
	public Properties getProperties()
	{
		return properties;
	}

	@Override
	public String getPersistenceXMLSchemaVersion()
	{
		return "3.0";
	}

	@Override
	public ClassLoader getClassLoader()
	{
		return null;
	}

	@Override
	public void addTransformer(ClassTransformer transformer)
	{
		//Silently ignore this command
	}

	@Override
	public ClassLoader getNewTempClassLoader()
	{
		return null;
	}
}
