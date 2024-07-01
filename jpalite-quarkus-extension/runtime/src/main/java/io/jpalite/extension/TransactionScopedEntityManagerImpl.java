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
import io.jpalite.PersistenceContext;
import io.quarkus.arc.Arc;
import io.quarkus.runtime.BlockingOperationControl;
import io.quarkus.runtime.BlockingOperationNotAllowedException;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.persistence.*;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionSynchronizationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("SqlSourceToSinkFlow")
public class TransactionScopedEntityManagerImpl implements JPALiteEntityManager
{
	private static final Logger LOG = LoggerFactory.getLogger(TransactionScopedEntityManagerImpl.class);
	private final EntityManagerFactory entityManagerFactory;
	private final TransactionManager transactionManager;
	private final TransactionSynchronizationRegistry transactionSynchronizationRegistry;
	private final String syncKey = getClass().getName() + "-entityManager";
	private final Map<String, Object> properties;

	private static class EntityManagerSession implements Closeable
	{
		private final boolean canClose;
		private final EntityManager entityManager;

		public EntityManagerSession(EntityManager entityManager, boolean canClose)
		{
			this.canClose = canClose;
			this.entityManager = entityManager;
		}

		@Override
		public void close()
		{
			if (canClose) {
				entityManager.close();
			}//if
		}

		public EntityManager getEntityManager()
		{
			return entityManager;
		}
	}

	public TransactionScopedEntityManagerImpl(EntityManagerFactory entityManagerFactory,
											  TransactionManager transactionManager,
											  TransactionSynchronizationRegistry transactionSynchronizationRegistry)
	{
		properties = new HashMap<>();
		this.entityManagerFactory = entityManagerFactory;
		this.transactionManager = transactionManager;
		this.transactionSynchronizationRegistry = transactionSynchronizationRegistry;
	}

	private EntityManagerSession getEntityManager()
	{
		if (!BlockingOperationControl.isBlockingAllowed()) {
			throw new BlockingOperationNotAllowedException("You have attempted to perform a blocking operation on a IO thread. This is not allowed, " +
																   "as blocking the IO thread will cause major performance issues with your application. " +
																   "If you want to perform blocking EntityManager operations make sure you are doing it from a worker thread.");
		}

		if (!Arc.container().requestContext().isActive()) {
			throw new ContextNotActiveException("Cannot use the EntityManager because neither a transaction nor a CDI request context is active. " +
														"Consider adding @Transactional to your method to automatically activate a transaction, " +
														"or @ActivateRequestContext if you have valid reasons not to use transactions.");
		}//if

		LOG.trace("Fetching EntityManager");
		if (isInTransaction()) {
			LOG.trace("In JTA Transaction, getting EntityManager from JTA");
			EntityManager entityManager = (EntityManager) transactionSynchronizationRegistry.getResource(syncKey);
			if (entityManager != null) {
				properties.forEach(entityManager::setProperty);
				return new EntityManagerSession(entityManager, false);
			}//if

			final EntityManager newEntityManager = entityManagerFactory.createEntityManager(SynchronizationType.SYNCHRONIZED, properties);
			transactionSynchronizationRegistry.putResource(syncKey, newEntityManager);

			newEntityManager.joinTransaction();
			transactionSynchronizationRegistry.registerInterposedSynchronization(new Synchronization()
			{
				@Override
				public void beforeCompletion()
				{
					PersistenceContext persistenceContexter = newEntityManager.unwrap(PersistenceContext.class);
					persistenceContexter.commit();
				}

				@Override
				public void afterCompletion(int pStatus)
				{
					PersistenceContext persistenceContexter = newEntityManager.unwrap(PersistenceContext.class);
					persistenceContexter.afterCompletion(pStatus);
					persistenceContexter.release();
					newEntityManager.close();
				}
			});
			return new EntityManagerSession(newEntityManager, false);
		}//if

		return new EntityManagerSession(entityManagerFactory.createEntityManager(SynchronizationType.UNSYNCHRONIZED, properties),
										true);
	}//getEntityManager

	private boolean isInTransaction()
	{
		try {
			int status = transactionManager.getStatus();
			return (status == Status.STATUS_ACTIVE || status == Status.STATUS_COMMITTING || status == Status.STATUS_MARKED_ROLLBACK || status == Status.STATUS_PREPARED || status == Status.STATUS_PREPARING);
		}
		catch (Exception ex) {
			throw new PersistenceException(ex);
		}
	}

	@Override
	public <T> void flushEntity(@Nonnull T entity)
	{
		try (EntityManagerSession session = getEntityManager()) {
			if (session.getEntityManager() instanceof JPALiteEntityManager vTSEntityManager) {
				vTSEntityManager.flushEntity(entity);
			}//if
		}
	}

	@Override
	public void flushOnType(Class<?> entityClass)
	{
		try (EntityManagerSession session = getEntityManager()) {
			if (session.getEntityManager() instanceof JPALiteEntityManager vTSEntityManager) {
				vTSEntityManager.flushOnType(entityClass);
			}//if
		}
	}

	@Override
	public <X> X mapResultSet(@Nonnull X entity, ResultSet resultSet)
	{
		try (EntityManagerSession session = getEntityManager()) {
			if (session.getEntityManager() instanceof JPALiteEntityManager jpaEntityManager) {
				return jpaEntityManager.mapResultSet(entity, resultSet);
			}//if
		}
		return entity;
	}

	@Override
	public void persist(Object entity)
	{
		try (EntityManagerSession session = getEntityManager()) {
			session.getEntityManager().persist(entity);
		}
	}

	@Override
	public <T> T merge(T entity)
	{
		try (EntityManagerSession session = getEntityManager()) {
			return session.getEntityManager().merge(entity);
		}
	}

	@Override
	public <T> T clone(@Nonnull T entity)
	{
		try (EntityManagerSession session = getEntityManager()) {
			if (session.getEntityManager() instanceof JPALiteEntityManager jpaEntityManager) {
				return jpaEntityManager.clone(entity);
			}//if
		}
		return entity;
	}

	@Override
	public void remove(Object entity)
	{
		try (EntityManagerSession session = getEntityManager()) {
			session.getEntityManager().remove(entity);
		}
	}

	@Override
	public <T> T find(Class<T> entityClass, Object primaryKey)
	{
		try (EntityManagerSession session = getEntityManager()) {
			return session.getEntityManager().find(entityClass, primaryKey);
		}
	}

	@Override
	public <T> T find(Class<T> entityClass, Object primaryKey, Map<String, Object> properties)
	{
		try (EntityManagerSession session = getEntityManager()) {
			return session.getEntityManager().find(entityClass, primaryKey, properties);
		}
	}

	@Override
	public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode)
	{
		try (EntityManagerSession session = getEntityManager()) {
			return session.getEntityManager().find(entityClass, primaryKey, lockMode);
		}
	}

	@Override
	public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode, Map<String, Object> properties)
	{
		try (EntityManagerSession session = getEntityManager()) {
			return session.getEntityManager().find(entityClass, primaryKey, lockMode, properties);
		}
	}

	@Override
	public <T> T getReference(Class<T> entityClass, Object primaryKey)
	{
		try (EntityManagerSession session = getEntityManager()) {
			return session.getEntityManager().getReference(entityClass, primaryKey);
		}
	}

	@Override
	public void flush()
	{
		try (EntityManagerSession session = getEntityManager()) {
			session.getEntityManager().flush();
		}
	}

	@Override
	public void setFlushMode(FlushModeType flushMode)
	{
		try (EntityManagerSession session = getEntityManager()) {
			session.getEntityManager().setFlushMode(flushMode);
		}
	}

	@Override
	public FlushModeType getFlushMode()
	{
		try (EntityManagerSession session = getEntityManager()) {
			return session.getEntityManager().getFlushMode();
		}
	}

	@Override
	public void lock(Object entity, LockModeType lockMode)
	{
		try (EntityManagerSession session = getEntityManager()) {
			session.getEntityManager().lock(entity, lockMode);
		}
	}

	@Override
	public void lock(Object entity, LockModeType lockMode, Map<String, Object> properties)
	{
		try (EntityManagerSession session = getEntityManager()) {
			session.getEntityManager().lock(entity, lockMode, properties);
		}
	}

	@Override
	public void refresh(Object entity)
	{
		try (EntityManagerSession session = getEntityManager()) {
			session.getEntityManager().refresh(entity);
		}
	}

	@Override
	public void refresh(Object entity, Map<String, Object> properties)
	{
		try (EntityManagerSession session = getEntityManager()) {
			session.getEntityManager().refresh(entity, properties);
		}
	}

	@Override
	public void refresh(Object entity, LockModeType lockMode)
	{
		try (EntityManagerSession session = getEntityManager()) {
			session.getEntityManager().refresh(entity, lockMode);
		}
	}

	@Override
	public void refresh(Object entity, LockModeType lockMode, Map<String, Object> properties)
	{
		try (EntityManagerSession session = getEntityManager()) {
			session.getEntityManager().refresh(entity, lockMode, properties);
		}
	}

	@Override
	public void clear()
	{
		try (EntityManagerSession session = getEntityManager()) {
			session.getEntityManager().clear();
		}
	}

	@Override
	public void detach(Object entity)
	{
		try (EntityManagerSession session = getEntityManager()) {
			session.getEntityManager().detach(entity);
		}
	}

	@Override
	public boolean contains(Object entity)
	{
		try (EntityManagerSession session = getEntityManager()) {
			return session.getEntityManager().contains(entity);
		}
	}

	@Override
	public LockModeType getLockMode(Object entity)
	{
		try (EntityManagerSession session = getEntityManager()) {
			return session.getEntityManager().getLockMode(entity);
		}
	}

	@Override
	public void setProperty(String propertyName, Object value)
	{
		properties.put(propertyName, value);
		try (EntityManagerSession session = getEntityManager()) {
			session.getEntityManager().setProperty(propertyName, value);
		}
	}

	@Override
	public Map<String, Object> getProperties()
	{
		try (EntityManagerSession session = getEntityManager()) {
			return session.getEntityManager().getProperties();
		}
	}

	@Override
	public Query createQuery(String sqlString)
	{
		try (EntityManagerSession session = getEntityManager()) {
			return session.getEntityManager().createQuery(sqlString);
		}
	}

	@Override
	public <T> TypedQuery<T> createQuery(CriteriaQuery<T> criteriaQuery)
	{
		try (EntityManagerSession session = getEntityManager()) {
			return session.getEntityManager().createQuery(criteriaQuery);
		}
	}

	@Override
	public Query createQuery(CriteriaUpdate updateQuery)
	{
		try (EntityManagerSession session = getEntityManager()) {
			return session.getEntityManager().createQuery(updateQuery);
		}
	}

	@Override
	public Query createQuery(CriteriaDelete deleteQuery)
	{
		try (EntityManagerSession session = getEntityManager()) {
			return session.getEntityManager().createQuery(deleteQuery);
		}
	}

	@Override
	public <T> TypedQuery<T> createQuery(String sqlString, Class<T> resultClass)
	{
		try (EntityManagerSession session = getEntityManager()) {
			return session.getEntityManager().createQuery(sqlString, resultClass);
		}
	}

	@Override
	public Query createNamedQuery(String name)
	{
		try (EntityManagerSession session = getEntityManager()) {
			return session.getEntityManager().createNamedQuery(name);
		}
	}

	@Override
	public <T> TypedQuery<T> createNamedQuery(String name, Class<T> resultClass)
	{
		try (EntityManagerSession session = getEntityManager()) {
			return session.getEntityManager().createNamedQuery(name, resultClass);
		}
	}

	@Override
	public Query createNativeQuery(String sqlString)
	{
		try (EntityManagerSession session = getEntityManager()) {
			return session.getEntityManager().createNativeQuery(sqlString);
		}
	}

	@Override
	public Query createNativeQuery(String sqlString, Class resultClass)
	{
		try (EntityManagerSession session = getEntityManager()) {
			return session.getEntityManager().createNativeQuery(sqlString, resultClass);
		}
	}

	@Override
	public Query createNativeQuery(String sqlString, String resultSetMapping)
	{
		try (EntityManagerSession session = getEntityManager()) {
			return session.getEntityManager().createNativeQuery(sqlString, resultSetMapping);
		}
	}

	@Override
	public StoredProcedureQuery createNamedStoredProcedureQuery(String name)
	{
		try (EntityManagerSession session = getEntityManager()) {
			return session.getEntityManager().createNamedStoredProcedureQuery(name);
		}
	}

	@Override
	public StoredProcedureQuery createStoredProcedureQuery(String procedureName)
	{
		try (EntityManagerSession session = getEntityManager()) {
			return session.getEntityManager().createStoredProcedureQuery(procedureName);
		}
	}

	@Override
	public StoredProcedureQuery createStoredProcedureQuery(String procedureName, Class... resultClasses)
	{
		try (EntityManagerSession session = getEntityManager()) {
			return session.getEntityManager().createStoredProcedureQuery(procedureName, resultClasses);
		}
	}

	@Override
	public StoredProcedureQuery createStoredProcedureQuery(String procedureName, String... resultSetMappings)
	{
		try (EntityManagerSession session = getEntityManager()) {
			return session.getEntityManager().createStoredProcedureQuery(procedureName, resultSetMappings);
		}
	}

	@Override
	public void joinTransaction()
	{
		try (EntityManagerSession session = getEntityManager()) {
			session.getEntityManager().joinTransaction();
		}
	}

	@Override
	public boolean isJoinedToTransaction()
	{
		try (EntityManagerSession session = getEntityManager()) {
			return session.getEntityManager().isJoinedToTransaction();
		}
	}

	@Override
	public <T> T unwrap(Class<T> cls)
	{
		try (EntityManagerSession session = getEntityManager()) {
			return session.getEntityManager().unwrap(cls);
		}
	}

	@Override
	public Object getDelegate()
	{
		try (EntityManagerSession session = getEntityManager()) {
			return session.getEntityManager().getDelegate();
		}
	}

	@Override
	public void close()
	{
		throw new IllegalStateException("Not supported for transaction scoped entity managers");
	}

	@Override
	public boolean isOpen()
	{
		return true;
	}

	@Override
	public EntityTransaction getTransaction()
	{
		throw new IllegalStateException("Not supported for JTA entity managers");
	}

	@Override
	public EntityManagerFactory getEntityManagerFactory()
	{
		return entityManagerFactory;
	}

	@Override
	public CriteriaBuilder getCriteriaBuilder()
	{
		try (EntityManagerSession session = getEntityManager()) {
			return session.getEntityManager().getCriteriaBuilder();
		}
	}

	@Override
	public Metamodel getMetamodel()
	{
		try (EntityManagerSession session = getEntityManager()) {
			return session.getEntityManager().getMetamodel();
		}
	}

	@Override
	public <T> EntityGraph<T> createEntityGraph(Class<T> rootType)
	{
		try (EntityManagerSession session = getEntityManager()) {
			return session.getEntityManager().createEntityGraph(rootType);
		}
	}

	@Override
	public EntityGraph<?> createEntityGraph(String graphName)
	{
		try (EntityManagerSession session = getEntityManager()) {
			return session.getEntityManager().createEntityGraph(graphName);
		}
	}

	@Override
	public EntityGraph<?> getEntityGraph(String graphName)
	{
		try (EntityManagerSession session = getEntityManager()) {
			return session.getEntityManager().getEntityGraph(graphName);
		}
	}

	@Override
	public <T> List<EntityGraph<? super T>> getEntityGraphs(Class<T> entityClass)
	{
		try (EntityManagerSession session = getEntityManager()) {
			return session.getEntityManager().getEntityGraphs(entityClass);
		}
	}
}
