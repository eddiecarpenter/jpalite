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

package org.jpalite.impl.providers;

import org.jpalite.JPAEntity;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.spi.LoadState;
import jakarta.persistence.spi.PersistenceProvider;
import jakarta.persistence.spi.PersistenceUnitInfo;
import jakarta.persistence.spi.ProviderUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class JPALitePersistenceProviderImpl implements PersistenceProvider
{
	private static final String NOT_SUPPORTED = "Not supported in the current implementation";

	private final Map<String, JPALiteEntityManagerFactoryImpl> factory = new ConcurrentHashMap<>();

	@Override
	public EntityManagerFactory createEntityManagerFactory(String name, Map properties)
	{
		return factory.computeIfAbsent(name, JPALiteEntityManagerFactoryImpl::new);
	}//createEntityManagerFactory

	@Override
	public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo info, Map properties)
	{
		throw new UnsupportedOperationException(NOT_SUPPORTED);
	}//createEntityManagerFactory

	@Override
	public void generateSchema(PersistenceUnitInfo info, Map properties)
	{
		throw new UnsupportedOperationException(NOT_SUPPORTED);
	}//generateSchema

	@Override
	public boolean generateSchema(String persistenceUnitName, Map properties)
	{
		throw new UnsupportedOperationException(NOT_SUPPORTED);
	}

	@Override
	public ProviderUtil getProviderUtil()
	{
		return new ProviderUtil()
		{
			@Override
			public LoadState isLoadedWithoutReference(Object entity, String attributeName)
			{
				if (entity instanceof JPAEntity) {
					return LoadState.LOADED;
				}//if

				return LoadState.UNKNOWN;
			}

			@Override
			public LoadState isLoadedWithReference(Object entity, String attributeName)
			{
				if (entity instanceof JPAEntity jpaEntity) {
					return jpaEntity._loadState();
				}//if

				return LoadState.UNKNOWN;
			}

			@Override
			public LoadState isLoaded(Object entity)
			{
				if (entity instanceof JPAEntity jpaEntity) {
					return jpaEntity._loadState();
				}//if

				return LoadState.UNKNOWN;
			}
		};
	}
}
