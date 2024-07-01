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
package io.jpalite.impl;

import io.jpalite.PersistenceContext;
import io.jpalite.*;
import io.jpalite.impl.queries.*;
import io.jpalite.queries.EntityQuery;
import io.jpalite.queries.QueryLanguage;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.quarkus.runtime.BlockingOperationControl;
import io.quarkus.runtime.BlockingOperationNotAllowedException;
import jakarta.annotation.Nonnull;
import jakarta.persistence.*;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.metamodel.Metamodel;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.sql.ResultSet;
import java.util.*;

import static jakarta.persistence.LockModeType.*;

/**
 * The entity manager implementation
 */
@Slf4j
@ToString(of = {"persistenceContext", "entityManagerFactory", "threadId"})
public class JPALiteEntityManagerImpl implements JPALiteEntityManager
{
	private static final String CRITERIA_QUERY_NOT_SUPPORTED = "CriteriaQuery is not supported";
	private static final String ENTITY_GRAPH_NOT_SUPPORTED = "EntityGraph is not supported";
	private static final String STORED_PROCEDURE_QUERY_NOT_SUPPORTED = "StoredProcedureQuery is not supported";
	private static final Tracer TRACER = GlobalOpenTelemetry.get().getTracer(JPALiteEntityManagerImpl.class.getName());
	private final EntityManagerFactory entityManagerFactory;
	private final PersistenceContext persistenceContext;
	private final long threadId;
	private final Throwable opened;
	private final Map<String, Object> properties;

	private boolean entityManagerOpen;
	private FlushModeType flushMode;

	public JPALiteEntityManagerImpl(PersistenceContext persistenceContext, EntityManagerFactory factory)
	{
		this.persistenceContext = persistenceContext;
		this.entityManagerFactory = factory;

		entityManagerOpen = true;
		flushMode = FlushModeType.AUTO;
		properties = new HashMap<>(persistenceContext.getProperties());
		threadId = Thread.currentThread().threadId();

		if (LOG.isTraceEnabled()) {
			opened = new Throwable();
		}//if
		else {
			opened = null;
		}//else
	}//JPALiteEntityManagerImpl

	//<editor-fold desc="Method Entry Checkers">
	@Override
	@SuppressWarnings("java:S6205") // false error
	public void setProperty(String name, Object value)
	{
		checkOpen();

		persistenceContext.setProperty(name, value);
		properties.put(name, value);
	}//setProperty

	@Override
	public Map<String, Object> getProperties()
	{
		checkOpen();
		return properties;
	}//getProperties

	private void checkOpen()
	{
		if (!isOpen()) {
			throw new IllegalStateException("EntityManager is closed");
		}//if

		if (threadId != Thread.currentThread().threadId()) {
			throw new IllegalStateException("Entity Managers are NOT threadsafe. Opened at ", opened);
		}//if

		if (!BlockingOperationControl.isBlockingAllowed()) {
			throw new BlockingOperationNotAllowedException("You have attempted to perform a blocking operation on a IO thread. This is not allowed, as blocking the IO thread will cause major performance issues with your application. If you want to perform blocking EntityManager operations make sure you are doing it from a worker thread.");
		}//if
	}//checkOpen

	private void checkEntity(Object entity)
	{
		if (entity == null) {
			throw new IllegalArgumentException("Entity cannot be null");
		}

		if (!(entity instanceof JPAEntity)) {
			throw new IllegalArgumentException("Entity is not an instance of JPAEntity");
		}
	}

	private void checkEntityClass(Class<?> entityClass)
	{
		if (!(JPAEntity.class.isAssignableFrom(entityClass))) {
			throw new IllegalArgumentException("Entity " + entityClass.getName() + " is not created using EntityManager");
		}//if
	}//checkEntityClass

	private void checkEntityAttached(JPAEntity entity)
	{
		if (entity._getEntityState() != EntityState.MANAGED) {
			throw new IllegalArgumentException("Entity is not current attached to a persistence context");
		}//if

		if (entity._getPersistenceContext() != persistenceContext) {
			throw new IllegalArgumentException("Entity is not being managed by this Persistence Context");
		}//if
	}//checkEntityObject

	private void checkTransactionRequired()
	{
		if (!persistenceContext.isActive()) {
			throw new TransactionRequiredException();
		}//if
	}//checkTransactionRequired
	//</editor-fold>

	@Override
	public EntityTransaction getTransaction()
	{
		checkOpen();
		return persistenceContext.getTransaction();
	}

	@Override
	public EntityManagerFactory getEntityManagerFactory()
	{
		checkOpen();

		return entityManagerFactory;
	}

	@Override
	public void close()
	{
		checkOpen();
		entityManagerOpen = false;
	}

	@Override
	public boolean isOpen()
	{
		return entityManagerOpen;
	}

	@Override
	public <X> X mapResultSet(@Nonnull X entity, ResultSet resultSet)
	{
		checkEntity(entity);
		return persistenceContext.mapResultSet(entity, resultSet);
	}

	@Override
	public void setFlushMode(FlushModeType flushMode)
	{
		checkOpen();
		this.flushMode = flushMode;
	}//setFlushMode

	@Override
	public FlushModeType getFlushMode()
	{
		checkOpen();
		return flushMode;
	}//getFlushMode

	@Override
	public void clear()
	{
		checkOpen();
		persistenceContext.l1Cache().clear();
	}//clear

	@Override
	public void detach(Object entity)
	{
		checkOpen();
		checkEntity(entity);
		checkEntityClass(entity.getClass());
		checkEntityAttached((JPAEntity) entity);
		persistenceContext.l1Cache().detach((JPAEntity) entity);
	}//detach

	@Override
	public boolean contains(Object entity)
	{
		checkOpen();
		checkEntity(entity);
		checkEntityClass(entity.getClass());
		return persistenceContext.l1Cache().contains((JPAEntity) entity);
	}//contains

	//<editor-fold desc="Unsupported Entity Graph Functions">
	@Override
	public <T> EntityGraph<T> createEntityGraph(Class<T> rootType)
	{
		checkOpen();

		throw new UnsupportedOperationException(ENTITY_GRAPH_NOT_SUPPORTED);
	}

	@Override
	public EntityGraph<?> createEntityGraph(String graphName)
	{
		checkOpen();

		throw new UnsupportedOperationException(ENTITY_GRAPH_NOT_SUPPORTED);
	}

	@Override
	@SuppressWarnings("java:S4144")//Not an error
	public EntityGraph<?> getEntityGraph(String graphName)
	{
		checkOpen();

		throw new UnsupportedOperationException(ENTITY_GRAPH_NOT_SUPPORTED);
	}

	@Override
	@SuppressWarnings("java:S4144")//Not an error
	public <T> List<EntityGraph<? super T>> getEntityGraphs(Class<T> entityClass)
	{
		checkOpen();

		throw new UnsupportedOperationException(ENTITY_GRAPH_NOT_SUPPORTED);
	}
	//</editor-fold>

	//<editor-fold desc="Persistence Functions">
	@Override
	public void flush()
	{
		checkOpen();
		checkTransactionRequired();

		persistenceContext.flush();
	}//flush

	@Override
	public void flushOnType(Class<?> entityClass)
	{
		persistenceContext.flushOnType(entityClass);
	}//flushEntities

	@Override
	public <T> void flushEntity(@Nonnull T entity)
	{
		checkOpen();
		checkEntity(entity);
		checkTransactionRequired();
		checkEntityAttached((JPAEntity) entity);

		persistenceContext.flushEntity((JPAEntity) entity);
	}//flushEntity

	@Override
	public void persist(@Nonnull Object entity)
	{
		checkOpen();
		checkEntity(entity);
		checkTransactionRequired();
		checkEntityClass(entity.getClass());

		if (((JPAEntity) entity)._getEntityState() == EntityState.MANAGED) {
			//An existing managed entity is ignored
			return;
		}//if

		if (((JPAEntity) entity)._getEntityState() == EntityState.REMOVED) {
			throw new PersistenceException("Attempting to persist an entity that was removed from the database");
		}//if

		((JPAEntity) entity)._setPendingAction(PersistenceAction.INSERT);
		persistenceContext.l1Cache().manage((JPAEntity) entity);

		if (flushMode == FlushModeType.AUTO) {
			flushEntity((JPAEntity) entity);
		}//if
	}//persist

	/**
	 * Many-to-one fields (entities) might be indirectly attached but contain One-to-Many fields
	 */
	private void cascadeMerge(JPAEntity entity)
	{
		entity._getMetaData()
				.getEntityFields()
				.stream()
				.filter(f -> f.isEntityField() && !entity._isLazyLoaded(f.getName()))
				.filter(f -> f.getCascade().contains(CascadeType.ALL) || f.getCascade().contains(CascadeType.MERGE))
				.forEach(f ->
						 {
							 try {
								 if (f.getMappingType() == MappingType.ONE_TO_MANY || f.getMappingType() == MappingType.MANY_TO_MANY) {
									 @SuppressWarnings("unchecked")
									 List<JPAEntity> entityList = (List<JPAEntity>) f.invokeGetter(entity);

									 List<JPAEntity> newEntityList = new ArrayList<>();
									 for (JPAEntity ref : entityList) {
										 newEntityList.add(merge(ref));
									 }//for
									 f.invokeSetter(entity, newEntityList);
								 }//if
								 else {
									 if (f.getMappingType() == MappingType.MANY_TO_ONE || f.getMappingType() == MappingType.ONE_TO_ONE) {
										 JPAEntity ref = (JPAEntity) f.invokeGetter(entity);
										 f.invokeSetter(entity, merge(ref));
									 }
								 }
							 }//try
							 catch (PersistenceException ex) {
								 LOG.error("Error processing cascading fields");
								 throw ex;
							 }//catch
							 catch (RuntimeException ex) {
								 LOG.error("Error merging ManyToOne field", ex);
								 throw new PersistenceException("Error merging ManyToOne field");
							 }//catch
						 });
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> X merge(X entity)
	{
		Span span = TRACER.spanBuilder("EntityManager::merge").setSpanKind(SpanKind.SERVER).startSpan();
		try (Scope ignored = span.makeCurrent()) {
			checkOpen();
			checkEntity(entity);
			checkTransactionRequired();
			checkEntityClass(entity.getClass());

			JPAEntity jpaEntity = (JPAEntity) entity;
			return switch (jpaEntity._getEntityState()) {
				case MANAGED -> {
					checkEntityAttached(jpaEntity);
					yield entity;
				}
				case DETACHED -> {
					X latestEntity = (X) find(jpaEntity.getClass(), jpaEntity._getPrimaryKey(), jpaEntity._getLockMode());
					if (latestEntity == null) {
						throw new IllegalArgumentException("Original entity not found");
					}//if
					((JPAEntity) latestEntity)._merge(jpaEntity);
					cascadeMerge(jpaEntity);
					yield latestEntity;
				}

				case REMOVED ->
						throw new PersistenceException("Attempting to merge an entity that was removed from the database");

				case TRANSIENT -> {
					Object primaryKey = jpaEntity._getPrimaryKey();
					if (primaryKey != null) {
						X persistedEntity = find((Class<X>) entity.getClass(), primaryKey);
						if (persistedEntity != null) {
							((JPAEntity) persistedEntity)._merge(jpaEntity);
							yield persistedEntity;
						}//if
					}//if

					persist(entity);
					yield entity;
				}
			};
		}//try
		finally {
			span.end();
		}
	}//merge

	@Override
	@SuppressWarnings("unchecked")
	public <T> T clone(@Nonnull T entity)
	{
		checkOpen();
		checkEntity(entity);
		checkEntityClass(entity.getClass());

		return (T) ((JPAEntity) entity)._clone();
	}//clone

	@Override
	public void remove(Object entity)
	{
		checkOpen();
		checkEntity(entity);
		checkTransactionRequired();
		checkEntityClass(entity.getClass());

		((JPAEntity) entity)._setPendingAction(PersistenceAction.DELETE);

		if (flushMode == FlushModeType.AUTO) {
			flushEntity((JPAEntity) entity);
		}//if
	}//remove
	//</editor-fold>

	//<editor-fold desc="Refresh Functions">
	@Override
	public void refresh(Object entity)
	{
		checkEntity(entity);
		checkEntityClass(entity.getClass());
		refresh(entity, ((JPAEntity) entity)._getLockMode(), Collections.emptyMap());
	}//refresh

	@Override
	public void refresh(Object entity, Map<String, Object> properties)
	{
		checkEntity(entity);
		checkEntityClass(entity.getClass());
		refresh(entity, ((JPAEntity) entity)._getLockMode(), properties);
	}//refresh

	@Override
	public void refresh(Object entity, LockModeType lockMode)
	{
		checkEntity(entity);
		checkEntityClass(entity.getClass());
		refresh(entity, lockMode, Collections.emptyMap());
	}//refresh

	@Override
	public void refresh(Object entity, LockModeType lockMode, Map<String, Object> properties)
	{
		checkOpen();
		checkEntity(entity);
		checkTransactionRequired();
		checkEntityAttached((JPAEntity) entity);

		((JPAEntity) entity)._setLockMode(lockMode);
		((JPAEntity) entity)._refreshEntity(properties);
	}//refresh
	//</editor-fold>

	//<editor-fold desc="Search Functions">
	@Override
	public <T> T find(Class<T> entityClass, Object primaryKey)
	{
		return find(entityClass, primaryKey, LockModeType.NONE, null);
	}

	@Override
	public <T> T find(Class<T> entityClass, Object primaryKey, Map<String, Object> properties)
	{
		return find(entityClass, primaryKey, LockModeType.NONE, properties);
	}

	@Override
	public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode)
	{
		return find(entityClass, primaryKey, lockMode, null);
	}

	@Override
	public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode, Map<String, Object> properties)
	{
		Span span = TRACER.spanBuilder("EntityManager::find").setSpanKind(SpanKind.SERVER).startSpan();
		try (Scope ignored = span.makeCurrent()) {
			checkOpen();
			checkEntityClass(entityClass);

			EntityMetaData<?> metaData = EntityMetaDataManager.getMetaData(entityClass);
			span.setAttribute("entity", metaData.getName());

			Map<String, Object> hints = new HashMap<>(this.properties);
			if (properties != null) {
				hints.putAll(properties);
			}//if

			EntityQuery entityQuery = new EntitySelectQueryImpl(primaryKey, metaData);
			JPALiteQueryImpl<T> query = new JPALiteQueryImpl<>(entityQuery.getQuery(),
															   entityQuery.getLanguage(),
															   persistenceContext,
															   entityClass,
															   hints,
															   lockMode);
			query.setParameter(1, primaryKey);
			try {
				return query.getSingleResult();
			}//try
			catch (NoResultException ex) {
				return null;
			}//catch
		}//try
		finally {
			span.end();
		}
	}//find

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getReference(Class<T> entityClass, Object primaryKey)
	{
		checkEntityClass(entityClass);

		EntityMetaData<T> metaData = EntityMetaDataManager.getMetaData(entityClass);
		JPAEntity newEntity = (JPAEntity) metaData.getNewEntity();
		newEntity._makeReference(primaryKey);
		persistenceContext.l1Cache().manage(newEntity);

		return (T) newEntity;
	}
	//</editor-fold>

	//<editor-fold desc="Locking Functions">
	@Override
	public LockModeType getLockMode(Object entity)
	{
		checkOpen();
		checkEntity(entity);
		checkEntityClass(entity.getClass());
		checkEntityAttached((JPAEntity) entity);

		return ((JPAEntity) entity)._getLockMode();
	}

	@Override
	public void lock(Object entity, LockModeType lockMode)
	{
		lock(entity, lockMode, null);
	}//lock

	@Override
	public void lock(Object entity, LockModeType lockMode, Map<String, Object> properties)
	{
		checkOpen();
		checkEntity(entity);
		checkEntityClass(entity.getClass());
		checkTransactionRequired();

		if (entity instanceof JPAEntity jpaEntity) {
			jpaEntity._setLockMode(lockMode);

			//For pessimistic locking a select for update query is to be executed
			if (lockMode == PESSIMISTIC_READ || lockMode == PESSIMISTIC_FORCE_INCREMENT || lockMode == PESSIMISTIC_WRITE) {
				Map<String, Object> hints = new HashMap<>(this.properties);
				if (properties != null) {
					hints.putAll(properties);
				}//if

				String sqlQuery = "select " +
						jpaEntity._getMetaData().getIdField().getColumn() +
						" from " +
						jpaEntity._getMetaData().getTable() +
						" where " +
						jpaEntity._getMetaData().getIdField().getColumn() +
						"=?";

				JPALiteQueryImpl<?> query = new JPALiteQueryImpl<>(sqlQuery,
																   QueryLanguage.NATIVE,
																   persistenceContext,
																   jpaEntity._getMetaData().getEntityClass(),
																   hints,
																   lockMode);
				query.setParameter(1, jpaEntity._getPrimaryKey());

				try {
					//Lock to row and continue
					query.getSingleResult();
				}//try
				catch (NoResultException ex) {
					getTransaction().setRollbackOnly();
					throw new EntityNotFoundException(jpaEntity._getMetaData().getName() + " with key " + jpaEntity._getPrimaryKey() + " not found");
				}//catch
				catch (PersistenceException ex) {
					getTransaction().setRollbackOnly();
				}//if
			}//if
			else {
				//For optimistic locking we need to flush the entity
				flush();
			}//else
		}//if
	}//lock
	//</editor-fold>

	//<editor-fold desc="Create Query Functions">
	@Override
	public Query createQuery(String query)
	{
		checkOpen();
		return new QueryImpl(query, persistenceContext, Object[].class, properties);
	}//createQuery

	@Override
	public <T> TypedQuery<T> createQuery(String query, Class<T> resultClass)
	{
		checkOpen();
		return new TypedQueryImpl<>(query, QueryLanguage.JPQL, persistenceContext, resultClass, properties);
	}//createQuery

	@Override
	public <T> TypedQuery<T> createNamedQuery(String name, Class<T> resultClass)
	{
		checkOpen();

		NamedQueries namedQueries = resultClass.getAnnotation(NamedQueries.class);
		if (namedQueries != null) {
			for (NamedQuery namedQuery : namedQueries.value()) {
				if (namedQuery.name().equals(name)) {
					return new NamedQueryImpl<>(namedQuery, persistenceContext, resultClass, properties);
				}//if
			}//for
		}//if

		NamedQuery namedQuery = resultClass.getAnnotation(NamedQuery.class);
		if (namedQuery != null && namedQuery.name().equals(name)) {
			return new NamedQueryImpl<>(namedQuery, persistenceContext, resultClass, properties);
		}//if

		NamedNativeQueries namedNativeQueries = resultClass.getAnnotation(NamedNativeQueries.class);
		if (namedNativeQueries != null) {
			for (NamedNativeQuery nativeQuery : namedNativeQueries.value()) {
				if (nativeQuery.name().equals(name)) {
					return new NamedNativeQueryImpl<>(nativeQuery, persistenceContext, resultClass, properties);
				}//if
			}//for
		}//if

		NamedNativeQuery namedNativeQuery = resultClass.getAnnotation(NamedNativeQuery.class);
		if (namedNativeQuery != null && namedNativeQuery.name().equals(name)) {
			return new NamedNativeQueryImpl<>(namedNativeQuery, persistenceContext, resultClass, properties);
		}//if

		throw new IllegalArgumentException("Named query '" + name + "' not found");
	}//createNamedQuery

	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	public Query createNativeQuery(String sqlString, Class resultClass)
	{
		checkOpen();
		return new NativeQueryImpl<>(sqlString, persistenceContext, resultClass, properties);
	}//createNativeQuery

	@Override
	public Query createNativeQuery(String sqlString)
	{
		checkOpen();
		return new NativeQueryImpl<>(sqlString, persistenceContext, Object.class, properties);
	}

	@Override
	public <T> TypedQuery<T> createQuery(CriteriaQuery<T> criteriaQuery)
	{
		checkOpen();
		throw new UnsupportedOperationException(CRITERIA_QUERY_NOT_SUPPORTED);
	}

	@Override
	public Query createQuery(CriteriaUpdate updateQuery)
	{
		checkOpen();
		throw new UnsupportedOperationException(CRITERIA_QUERY_NOT_SUPPORTED);
	}

	@Override
	public Query createQuery(CriteriaDelete deleteQuery)
	{
		checkOpen();
		throw new UnsupportedOperationException(CRITERIA_QUERY_NOT_SUPPORTED);
	}

	@Override
	public Query createNamedQuery(String name)
	{
		checkOpen();

		throw new UnsupportedOperationException("Global Named Queries are not supported");
	}

	@Override
	public Query createNativeQuery(String sqlString, String resultSetMapping)
	{
		checkOpen();

		throw new UnsupportedOperationException("ResultSetMapping is not supported");
	}//createNativeQuery

	@Override
	public StoredProcedureQuery createNamedStoredProcedureQuery(String name)
	{
		checkOpen();

		throw new UnsupportedOperationException(STORED_PROCEDURE_QUERY_NOT_SUPPORTED);
	}

	@Override
	public StoredProcedureQuery createStoredProcedureQuery(String procedureName)
	{
		checkOpen();

		throw new UnsupportedOperationException(STORED_PROCEDURE_QUERY_NOT_SUPPORTED);
	}

	@Override
	public StoredProcedureQuery createStoredProcedureQuery(String procedureName, Class... resultClasses)
	{
		checkOpen();

		throw new UnsupportedOperationException(STORED_PROCEDURE_QUERY_NOT_SUPPORTED);
	}

	@Override
	public StoredProcedureQuery createStoredProcedureQuery(String procedureName, String... resultSetMappings)
	{
		checkOpen();

		throw new UnsupportedOperationException(STORED_PROCEDURE_QUERY_NOT_SUPPORTED);
	}

	@Override
	@SuppressWarnings("java:S4144")//Not an error
	public CriteriaBuilder getCriteriaBuilder()
	{
		checkOpen();

		throw new UnsupportedOperationException(CRITERIA_QUERY_NOT_SUPPORTED);
	}
	//</editor-fold>

	@Override
	public void joinTransaction()
	{
		checkOpen();

		persistenceContext.joinTransaction();
	}

	@Override
	public boolean isJoinedToTransaction()
	{
		checkOpen();

		return persistenceContext.isJoinedToTransaction();
	}

	@Override
	public Metamodel getMetamodel()
	{
		checkOpen();
		return entityManagerFactory.getMetamodel();
	}

	@Override
	public Object getDelegate()
	{
		checkOpen();
		return this;
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public <T> T unwrap(Class<T> cls)
	{
		checkOpen();

		if (cls.isAssignableFrom(this.getClass())) {
			return (T) this;
		}

		if (cls.isAssignableFrom(PersistenceContext.class)) {
			return (T) persistenceContext;
		}

		throw new IllegalArgumentException("Could not unwrap this [" + this + "] as requested Java type [" + cls.getName() + "]");
	}
}//JPALiteEntityManagerImpl

//--------------------------------------------------------------------[ End ]---
