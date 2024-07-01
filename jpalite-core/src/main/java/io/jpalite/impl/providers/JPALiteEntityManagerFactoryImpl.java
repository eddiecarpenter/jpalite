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

package io.jpalite.impl.providers;

import io.jpalite.PersistenceContext;
import io.jpalite.*;
import io.jpalite.impl.JPAConfig;
import io.jpalite.impl.JPALiteEntityManagerImpl;
import io.jpalite.impl.caching.EntityCacheImpl;
import io.jpalite.impl.db.DatabasePoolFactory;
import jakarta.persistence.*;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.metamodel.Metamodel;
import lombok.extern.slf4j.Slf4j;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;

import static io.jpalite.JPALiteEntityManager.PERSISTENCE_QUERY_LOG_SLOWTIME;
import static io.jpalite.JPALiteEntityManager.PERSISTENCE_SHOW_SQL;
import static io.jpalite.PersistenceContext.PERSISTENCE_JTA_MANAGED;

@SuppressWarnings("unchecked")
@Slf4j
public class JPALiteEntityManagerFactoryImpl implements EntityManagerFactory
{
	private static final String NOT_SUPPORTED = "Not supported by current implementation";
	private final long defaultSlowQueryTime = JPAConfig.getValue("jpalite.slowQueryTime", 500L);
	private final boolean defaultShowQueries = JPAConfig.getValue("jpalite.showQueries", false);
	private final String persistenceUnitName;
	private boolean openFactory;

	public JPALiteEntityManagerFactoryImpl(String persistenceUnitName)
	{
		this.persistenceUnitName = persistenceUnitName;
		openFactory = true;

		LOG.info("Building the Entity Manager Factory for EntityManager named {}", persistenceUnitName);
	}

	@Override
	public EntityManager createEntityManager()
	{
		return entityManagerBuilder(SynchronizationType.UNSYNCHRONIZED, Collections.emptyMap());
	}

	@Override
	public EntityManager createEntityManager(Map map)
	{
		return entityManagerBuilder(SynchronizationType.UNSYNCHRONIZED, map);
	}

	@Override
	public EntityManager createEntityManager(SynchronizationType synchronizationType)
	{
		return entityManagerBuilder(synchronizationType, Collections.emptyMap());
	}

	@Override
	public EntityManager createEntityManager(SynchronizationType pSynchronizationType, Map map)
	{
		return entityManagerBuilder(pSynchronizationType, map);
	}

	private JPALitePersistenceUnit getPersistenceUnit()
	{
		ServiceLoader<PersistenceUnitProvider> loader = ServiceLoader.load(PersistenceUnitProvider.class);
		for (PersistenceUnitProvider persistenceUnitProvider : loader) {
			JPALitePersistenceUnit persistenceUnit = persistenceUnitProvider.getPersistenceUnit(persistenceUnitName);
			if (persistenceUnit != null) {
				if (persistenceUnit.getMultiTenantMode().equals(Boolean.TRUE)) {
					ServiceLoader<MultiTenantProvider> multiTenantLoader = ServiceLoader.load(MultiTenantProvider.class);
					for (MultiTenantProvider multiTenantProvider : multiTenantLoader) {
						JPALitePersistenceUnit legacyPersistenceUnit = multiTenantProvider.getPersistenceUnit(persistenceUnit);
						if (legacyPersistenceUnit != null) {
							return legacyPersistenceUnit;
						}//if
					}//for
				}//if

				return persistenceUnit;
			}//if
		}//for

		throw new PersistenceUnitNotFoundException(String.format("No PersistenceUnit was found for '%s'. %d SPI services found implementing PersistenceUnitProvider.class.",
																 persistenceUnitName, loader.stream().count()));
	}//getPersistenceUnit

	private PersistenceContext getPersistenceContext(SynchronizationType synchronizationType, Map<String, Object> properties) throws SQLException
	{
		JPALitePersistenceUnit persistenceUnit = getPersistenceUnit();

		DatabasePool databasePool = DatabasePoolFactory.getDatabasePool(persistenceUnit.getDataSourceName());

		Properties localProperties = persistenceUnit.getProperties();
		localProperties.putAll(properties);
		localProperties.put(PERSISTENCE_JTA_MANAGED, synchronizationType == SynchronizationType.SYNCHRONIZED);
		localProperties.putIfAbsent(PERSISTENCE_QUERY_LOG_SLOWTIME, defaultSlowQueryTime);
		localProperties.putIfAbsent(PERSISTENCE_SHOW_SQL, defaultShowQueries);

		return databasePool.getPersistenceContext(persistenceUnit);
	}//getPersistenceContext

	private EntityManager entityManagerBuilder(SynchronizationType synchronizationType, Map<String, Object> entityProperties)
	{
		try {
			PersistenceContext persistenceContext = getPersistenceContext(synchronizationType, entityProperties);
			return new JPALiteEntityManagerImpl(persistenceContext, this);
		}//try
		catch (SQLException ex) {
			throw new PersistenceException("Error connecting to the database", ex);
		}//catch
	}//entityBuilder

	@Override
	public CriteriaBuilder getCriteriaBuilder()
	{
		//Criteria Queries are not supported
		throw new UnsupportedOperationException(NOT_SUPPORTED);
	}

	@Override
	public Metamodel getMetamodel()
	{
		//Criteria Queries are not supported
		throw new UnsupportedOperationException(NOT_SUPPORTED);
	}

	@Override
	public boolean isOpen()
	{
		return openFactory;
	}

	@Override
	public void close()
	{
		openFactory = false;
	}

	@Override
	public Map<String, Object> getProperties()
	{
		return Collections.emptyMap();
	}

	@Override
	public Cache getCache()
	{
		return new EntityCacheImpl(getPersistenceUnit());
	}//getCache

	@Override
	public PersistenceUnitUtil getPersistenceUnitUtil()
	{
		return new PersistenceUnitUtil()
		{
			private JPAEntity checkEntity(Object entity)
			{
				if (entity instanceof JPAEntity jpaEntity) {
					return jpaEntity;
				}//if

				throw new IllegalStateException(entity.getClass().getName() + " is not a JPA Entity");
			}//checkEntity

			@Override
			public boolean isLoaded(Object entity, String field)
			{
				return !checkEntity(entity)._isLazyLoaded(field);
			}

			@Override
			public boolean isLoaded(Object entity)
			{
				return !checkEntity(entity)._isLazyLoaded();
			}

			@Override
			public Object getIdentifier(Object entity)
			{
				JPAEntity jpaEntity = checkEntity(entity);
				return jpaEntity._getEntityState() == EntityState.TRANSIENT ? null : jpaEntity._getPrimaryKey();
			}
		};
	}

	@Override
	public void addNamedQuery(String name, Query query)
	{
		throw new UnsupportedOperationException("Global Named Queries are not supported");
	}

	@Override
	public <T> T unwrap(Class<T> cls)
	{
		if (cls.isAssignableFrom(this.getClass())) {
			return (T) this;
		}

		throw new IllegalArgumentException("Could not unwrap this [" + this + "] as requested Java type [" + cls.getName() + "]");
	}

	@Override
	public <T> void addNamedEntityGraph(String graphName, EntityGraph<T> entityGraph)
	{
		throw new UnsupportedOperationException(NOT_SUPPORTED);
	}
}
