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

package org.jpalite.impl.db;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.quarkus.runtime.Application;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.persistence.*;
import jakarta.transaction.Status;
import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionManager;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jpalite.PersistenceContext;
import org.jpalite.*;
import org.jpalite.impl.EntityL1LocalCacheImpl;
import org.jpalite.impl.caching.EntityCacheImpl;
import org.jpalite.impl.queries.EntityDeleteQueryImpl;
import org.jpalite.impl.queries.EntityInsertQueryImpl;
import org.jpalite.impl.queries.EntityUpdateQueryImpl;
import org.jpalite.queries.EntityQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.jpalite.JPALiteEntityManager.*;
import static org.jpalite.PersistenceAction.*;

/**
 * The persistence context is responsible for managing the connection, persisting entities to the database and keeps
 * tract of transaction blocks started and needs to do the cleanup on close.
 */
public class PersistenceContextImpl implements PersistenceContext
{
    private static final Logger LOG = LoggerFactory.getLogger(PersistenceContextImpl.class);
    private static final Tracer TRACER = GlobalOpenTelemetry.get().getTracer(PersistenceContextImpl.class.getName());
    /**
     * The database pool we belong to
     */
    private final DatabasePool pool;
    /**
     * Control counter to manage transaction depth. Every call to {@link #begin()} will increment it and calls to
     * {@link #commit()} and {@link #rollback()} will decrement it.
     */
    private final AtomicInteger transactionDepth;
    /**
     * Control variable to record the current {@link #transactionDepth}.
     */
    private final Deque<Integer> openStack;
    /**
     * The connection name used to open a new connection
     */
    private final Deque<String> connectionNames;
    /**
     * Stack for all save points created by beginTrans()
     */
    private final Deque<Savepoint> savepoints;
    /**
     * The level 1 cache
     */
    private final EntityLocalCache entityL1Cache;
    /**
     * The level 2 cache
     */
    private final EntityCache entityL2Cache;
    /**
     * List of all callback listeners
     */
    private final List<EntityTransactionListener> listeners;
    /**
     * List of callback listeners to add. This list is populated if a new listener is removed form with in a callback.
     */
    private final List<EntityTransactionListener> pendingAdd;
    /**
     * List of callback listeners to delete. This list is populated if a listener is removed form with in a callback.
     */
    private final List<EntityTransactionListener> pendingRemoval;
    /**
     * The connection assigned to the manager
     */
    private ConnectionWrapper connection;
    /**
     * The last query executed in by the connection
     */
    private String lastQuery;
    /**
     * The current connection name assigned to the connection
     */
    private String connectionName;
    /**
     * The execution time after which queries are considered run too slowly
     */
    private long slowQueryTime;
    /**
     * If true create a connection that shows the SQL
     */
    private boolean showSql;
    /**
     * The cache store mode in effect
     */
    private CacheStoreMode cacheStoreMode;
    /**
     * Control variable to indicate that we have forced rollback
     */
    private boolean rollbackOnly = false;
    /**
     * Read only indicator
     */
    private boolean readOnly;
    /**
     * Control variable to make sure that a transaction callback does not call begin, commit or rollback
     */
    private boolean inCallbackHandler;
    /**
     * The JTA transaction manager
     */
    private TransactionManager transactionManager;
    /**
     * True if join to a JTA transaction
     */
    private boolean joinedToTransaction;
    /**
     * True if the context should automatically detect and join a JTA managed transaction.
     */
    private boolean autoJoinTransaction;
    /**
     * The persistence context properties
     */
    private final Map<String, Object> properties;
    /**
     * The persistence unit used to create the context
     */
    private final JPALitePersistenceUnit persistenceUnit;
    private final long threadId;
    private final long instanceNr;
    private static final AtomicLong instanceCount = new AtomicLong(0);
    private boolean released;
    private final String hostname;

    private enum CallbackMethod
    {
        PRE_BEGIN,
        POST_BEGIN,
        PRE_COMMIT,
        POST_COMMIT,
        PRE_ROLLBACK,
        POST_ROLLBACK
    }

    public PersistenceContextImpl(DatabasePool pool, JPALitePersistenceUnit persistenceUnit)
    {
        this.pool            = pool;
        readOnly             = false;
        this.persistenceUnit = persistenceUnit;
        properties           = new HashMap<>();
        listeners            = new ArrayList<>();
        pendingAdd           = new ArrayList<>();
        pendingRemoval       = new ArrayList<>();
        transactionDepth     = new AtomicInteger(0);
        instanceNr           = instanceCount.incrementAndGet();
        openStack            = new ArrayDeque<>();
        connectionNames      = new ArrayDeque<>();
        savepoints           = new ArrayDeque<>();
        connectionName       = Thread.currentThread().getName();
        cacheStoreMode       = CacheStoreMode.USE;
        slowQueryTime        = 500L;
        joinedToTransaction  = false;
        autoJoinTransaction  = false;
        transactionManager   = null;
        showSql              = false;
        released             = false;

        hostname = ConfigProvider.getConfig().getOptionalValue("HOSTNAME", String.class).orElse("localhost");
        threadId = Thread.currentThread().threadId();
        persistenceUnit.getProperties().forEach((k, v) -> setProperty(k.toString(), v));

        entityL1Cache = new EntityL1LocalCacheImpl(this);

        entityL2Cache = new EntityCacheImpl(this.persistenceUnit);

        LOG.debug("Created {}", this);
    }//PersistenceContextImpl

    @Override
    public JPALitePersistenceUnit getPersistenceUnit()
    {
        return persistenceUnit;
    }//getPersistenceUnit

    @Override
    public void setProperty(String name, Object value)
    {
        switch (name) {
            case PERSISTENCE_CACHE_STOREMODE -> {
                if (value instanceof String strValue) {
                    value = CacheStoreMode.valueOf(strValue);
                }//if
                if (value instanceof CacheStoreMode cacheMode) {
                    cacheStoreMode = cacheMode;
                }//if
            }
            case PERSISTENCE_JTA_MANAGED -> {
                if (value instanceof String strValue) {
                    value = Boolean.parseBoolean(strValue);
                }//if
                if (value instanceof Boolean jtaManaged) {
                    autoJoinTransaction = jtaManaged;
                }//if
            }
            case PERSISTENCE_QUERY_LOG_SLOWTIME -> {
                if (value instanceof String strValue) {
                    value = Long.parseLong(strValue);
                }//if
                if (value instanceof Long slowQuery) {
                    slowQueryTime = slowQuery;
                }//if
            }
            case PERSISTENCE_SHOW_SQL -> {
                if (value instanceof String strValue) {
                    value = Boolean.parseBoolean(strValue);
                }//if
                if (value instanceof Boolean showQuerySql) {
                    this.showSql = showQuerySql;
                    if (connection != null) {
                        connection.setEnableLogging(this.showSql);
                    }//if
                }//if
            }
            default -> {
                //ignore the rest
            }
        }//switch

        properties.put(name, value);
    }

    @Override
    public Map<String, Object> getProperties()
    {
        return properties;
    }

    @Override
    public EntityLocalCache l1Cache()
    {
        return entityL1Cache;
    }//l1Cache

    @Override
    public EntityCache l2Cache()
    {
        return entityL2Cache;
    }//l2Cache

    private void checkEntityAttached(JPAEntity entity)
    {
        if (entity._getEntityState() != EntityState.MANAGED) {
            throw new IllegalArgumentException("Entity is not current attached to a Persistence Context");
        }//if

        if (entity._getPersistenceContext() != this) {
            throw new IllegalArgumentException("Entity is not being managed by this Persistence Context");
        }//if
    }//checkEntityAttached

    private void checkRecursiveCallback()
    {
        if (inCallbackHandler) {
            throw new PersistenceException("The EntityTransaction methods begin, commit and rollback cannot be called from within a EntityListener callback");
        }//if
    }//checkRecursiveCallback

    private void checkThread()
    {
        if (threadId != Thread.currentThread().threadId()) {
            throw new IllegalStateException("Persistence Context is assigned different thread. Expected " + threadId + ", calling thread is " + Thread.currentThread().threadId());
        }//if
    }//checkThread

    private void checkReleaseState()
    {
        if (released) {
            throw new PersistenceException("Persistence Context has detached from the database pool cannot be used");
        }//if
    }//checkReleaseState

    private void checkOpen()
    {
        checkReleaseState();

        if (connection == null) {
            throw new IllegalStateException("Persistence Context is closed.");
        }//if
    }//checkOpen

    @Override
    public String toString()
    {
        return "Persistence Context " + instanceNr + " [Stack " + openStack.size() + ", " + pool + "]";
    }//toString

    @Override
    public void addTransactionListener(EntityTransactionListener listener)
    {
        if (inCallbackHandler) {
            pendingAdd.add(listener);
        }//if
        else {
            listeners.add(listener);
        }//else
    }//addTransactionListener

    @Override
    public void removeTransactionListener(EntityTransactionListener listener)
    {
        if (inCallbackHandler) {
            pendingRemoval.add(listener);
        }//if
        else {
            listeners.remove(listener);
        }//else
    }//removeTransactionListener

    @Override
    public void setLastQuery(String lastQuery)
    {
        this.lastQuery = lastQuery;
    }

    @Override
    public String getLastQuery()
    {
        return lastQuery;
    }

    @Override
    public int getTransactionDepth()
    {
        return transactionDepth.get();
    }

    @Override
    public int getOpenLevel()
    {
        return openStack.size();
    }

    @Override
    public String getConnectionName()
    {
        return connectionName;
    }

    @Override
    public void setConnectionName(String connectionName)
    {
        this.connectionName = connectionName;
    }

    @SuppressWarnings({"java:S1141", "java:S2077"})
    //Having try-resource in a bigger try block is allowed. Dynamically formatted SQL is verified to be safe
    @Override
    @Nonnull
    public Connection getConnection(String connectionName)
    {
        checkReleaseState();
        checkThread();

        openStack.push(transactionDepth.get());
        connectionNames.push(this.connectionName);

        if (connectionName == null) {
            if (this.connectionName == null) {
                this.connectionName = Thread.currentThread().getName();
            }//if
        }//if
        else {
            this.connectionName = connectionName;
        }//else
        LOG.trace("Opening persistence context. Level: {} with cursor {}", openStack.size(), this.connectionName);

        if (connection == null) {
            try {
                connection = new ConnectionWrapper(this, pool.getConnection(), slowQueryTime);

                try (Statement writeStmt = connection.createStatement()) {
                    String applicationName = Application.currentApplication().getName() + "@" + hostname;
                    if (applicationName.length() > 61) {
                        applicationName = applicationName.substring(0, 61);
                    }//if
                    String applicationNameQry = "set application_name to '" + applicationName + "'";
                    writeStmt.execute(applicationNameQry);
                }//try
                catch (SQLException ex) {
                    LOG.error("Error setting the JDBC application name", ex);
                }//catch

                connection.setEnableLogging(showSql);
            }//try
            catch (SQLException ex) {
                throw new PersistenceException("Error configuring database connection", ex);
            }//catch
        }//if

        connection.setName(this.connectionName);

        if (isAutoJoinTransaction()) {
            joinTransaction();
        }//if

        return connection;
    }//getConnection

    @Override
    public boolean isReleased()
    {
        return released;
    }//if

    @Override
    public void release()
    {
        checkThread();

        if (connection != null) {
            LOG.warn("Closing unexpected open transaction on {}", connection, new PersistenceException("Possible unhandled exception"));
            openStack.clear();
            connectionNames.clear();

            close();
        }//if

        released = true;
    }//release

    @Override
    public void close()
    {
        checkOpen();
        checkThread();

        LOG.trace("Closing connection level: {}", openStack.size());
        if (!connectionNames.isEmpty()) {
            connectionName = connectionNames.pop();
        }//if

        if (!openStack.isEmpty()) {
            int transDepth = openStack.pop();
            if (transDepth < this.transactionDepth.get()) {
                LOG.warn("Closing unexpected open transaction", new PersistenceException("Possible unhandled exception"));
                rollbackToDepth(transDepth);

                //Check if the rollback closed the connection, if so we are done
                if (connection == null) {
                    return;
                }//if
            }//if
        }//if

        if (openStack.isEmpty()) {
            LOG.trace("At level 0, releasing connection {}", connection);

            l1Cache().clear();
            openStack.clear();
            connectionNames.clear();
            savepoints.clear();
            transactionDepth.set(0);
            rollbackOnly = false;
            readOnly     = false;
            try {
                if (!connection.isClosed() && !connection.getAutoCommit()) {
                    connection.rollback();
                    connection.setAutoCommit(true);
                }//if
                connection.realClose();
                connection = null;
            }//try
            catch (SQLException ex) {
                LOG.error("Error closing connection", ex);
            }//catch
        }//if

    }//close

    @Override
    public <X> X mapResultSet(@Nonnull X entity, ResultSet resultSet)
    {
        return mapResultSet(entity, null, resultSet);
    }

    @Override
    public <X> X mapResultSet(@Nonnull X entity, String colPrefix, ResultSet resultSet)
    {
        ((JPAEntity) entity)._setPersistenceContext(this);
        ((JPAEntity) entity)._mapResultSet(colPrefix, resultSet);
        l1Cache().manage((JPAEntity) entity);
        return entity;
    }

    private boolean doesNeedFlushing(JPAEntity entity)
    {
        if (entity._getPersistenceContext() != this) {
            throw new PersistenceException("Entity belongs to another persistence context and cannot be updated. I am  [" + this + "], Entity [" + entity + "]");
        }//if

        return entity._getEntityState() == EntityState.MANAGED && (entity._getPendingAction() != PersistenceAction.NONE || entity._getLockMode() == LockModeType.OPTIMISTIC_FORCE_INCREMENT);
    }//doesNeedFlushing

    @Override
    public void flush()
    {
        checkOpen();
        checkThread();

        l1Cache().foreach(e ->
                          {
                              if (doesNeedFlushing((JPAEntity) e)) {
                                  flushEntityInternal((JPAEntity) e);
                              }//if
                          });
    }//flush

    @Override
    public void flushOnType(Class<?> entityClass)
    {
        checkOpen();
        checkThread();

        l1Cache().foreachType(entityClass, e ->
        {
            if (doesNeedFlushing((JPAEntity) e)) {
                flushEntityInternal((JPAEntity) e);
            }//if
        });
    }//flushOnType

    @Override
    public void flushEntity(@Nonnull JPAEntity entity)
    {
        checkOpen();
        checkThread();
        checkEntityAttached(entity);

        flushEntityInternal(entity);
    }

    @SuppressWarnings("java:S6205") //Not a redundant block
    private void invokeCallbackHandlers(PersistenceAction action, boolean preAction, Object entity)
    {
        /*
         * Callback are not invoked if the transaction is marked for rollback
         */
        if (!getRollbackOnly()) {
            EntityMetaData<?> metaData = EntityMetaDataManager.getMetaData(entity.getClass());
            try {
                switch (action) {
                    case INSERT -> {
                        if (preAction) {
                            metaData.getLifecycleListeners().prePersist(entity);
                        }
                        else {
                            metaData.getLifecycleListeners().postPersist(entity);
                        }
                    }
                    case UPDATE -> {
                        if (preAction) {
                            metaData.getLifecycleListeners().preUpdate(entity);
                        }
                        else {
                            metaData.getLifecycleListeners().postUpdate(entity);
                        }
                    }
                    case DELETE -> {
                        if (preAction) {
                            metaData.getLifecycleListeners().preRemove(entity);
                        }
                        else {
                            metaData.getLifecycleListeners().postRemove(entity);
                        }
                    }
                    default -> {//do nothing
                    }
                }//switch
            }//try
            catch (PersistenceException ex) {
                setRollbackOnly();
                throw ex;
            }//catch
        }//if
    }//invokeCallbackHandlers

    private void bindParameters(PreparedStatement statement, Object... params)
    {
        if (params != null) {
            int startAt = 0;

            for (Object param : params) {
                try {
                    startAt++;

                    if (param instanceof Boolean) {
                        param = param == Boolean.TRUE ? 1 : 0;
                    }//if
                    if (param instanceof byte[] vBytes) {
                        statement.setBytes(startAt, vBytes);
                    }//if
                    else {
                        statement.setObject(startAt, param, Types.OTHER);
                    }//else
                }//try
                catch (SQLException ex) {
                    throw new PersistenceException("Error setting parameter (" + startAt + "=" + param, ex);
                }//catch
            }//for
        }//if
    }//bindParameters

    private boolean isOptimisticLocked(JPAEntity entity)
    {
        return (entity._getLockMode() == LockModeType.OPTIMISTIC || entity._getLockMode() == LockModeType.OPTIMISTIC_FORCE_INCREMENT);
    }//isOptimisticLocked

    @SuppressWarnings("unchecked")
    private void cascadePersist(Set<MappingType> mappings, @Nonnull JPAEntity entity)
    {
        try {
            for (EntityField field : entity._getMetaData().getEntityFields()) {
                if ((field.getCascade().contains(CascadeType.ALL) || field.getCascade().contains(CascadeType.PERSIST))) {

                    if (field.getMappingType() == MappingType.ONE_TO_MANY && mappings.contains(MappingType.ONE_TO_MANY)) {
                        List<JPAEntity> entityList = (List<JPAEntity>) field.invokeGetter(entity);
                        if (entityList != null) {
                            entityList.stream()
                                      //Check if the entity is new and unattached or was persisted but not flushed
                                      .filter(e -> (e._getEntityState() == EntityState.TRANSIENT || e._getPendingAction() == PersistenceAction.INSERT))
                                      .forEach(e ->
                                               {
                                                   try {
                                                       EntityField entityField = e._getMetaData().getEntityField(field.getMappedBy());
                                                       entityField.invokeSetter(e, entity);
                                                       e._setPendingAction(PersistenceAction.INSERT);
                                                       l1Cache().manage(e);
                                                       flushEntity(e);
                                                   }//try
                                                   catch (RuntimeException ex) {
                                                       setRollbackOnly();
                                                       throw new PersistenceException("Error cascading persist entity", ex);
                                                   }//catch
                                               });
                        }//if
                        entity._clearField(field.getName());
                    }//if
                    else if ((field.getMappingType() == MappingType.MANY_TO_ONE && mappings.contains(MappingType.MANY_TO_ONE) || (field.getMappingType() == MappingType.ONE_TO_ONE && mappings.contains(MappingType.ONE_TO_ONE)))) {
                        JPAEntity jpaEntity = (JPAEntity) field.invokeGetter(entity);
                        flushEntity(jpaEntity);
                    }//else if

                }//if
            }//for
        }//try
        catch (RuntimeException ex) {
            setRollbackOnly();
            throw new PersistenceException("Error cascading persist entity", ex);
        }//catch
    }//cascadePersist

    @SuppressWarnings("unchecked")
    private void cascadeRemove(Set<MappingType> mappings, @Nonnull JPAEntity entity)
    {
        try {
            for (EntityField field : entity._getMetaData().getEntityFields()) {

                if (mappings.contains(field.getMappingType()) && (field.getCascade().contains(CascadeType.ALL) || field.getCascade().contains(CascadeType.REMOVE))) {
                    if (mappings.contains(MappingType.MANY_TO_ONE) || mappings.contains(MappingType.ONE_TO_ONE)) {
                        JPAEntity entityValue = (JPAEntity) field.invokeGetter(entity);
                        if (entityValue != null && !entityValue._isLazyLoaded()) {
                            entityValue._setPendingAction(DELETE);
                            flushEntity(entityValue);
                        }//if
                    }//if
                    else if (mappings.contains(MappingType.ONE_TO_MANY)) {
                        List<JPAEntity> entityList = (List<JPAEntity>) field.invokeGetter(entity);
                        if (entityList != null) {
                            entityList.stream()
                                      .filter(e -> (!e._isLazyLoaded()))
                                      .forEach(e ->
                                               {
                                                   e._setPendingAction(DELETE);
                                                   flushEntity(e);
                                               });
                        }//if
                    }//else if
                }//if
            }//for
        }//try
        catch (RuntimeException ex) {
            setRollbackOnly();
            throw new PersistenceException("Error cascading remove entity", ex);
        }//catch
    }//cascadeRemove

    private EntityQuery getFlushQuery(PersistenceAction action, @Nonnull JPAEntity entity)
    {
        EntityMetaData<?> metaData = EntityMetaDataManager.getMetaData(entity.getClass());
        return switch (action) {
            case INSERT -> {
                cascadePersist(Set.of(MappingType.MANY_TO_ONE), entity);
                yield new EntityInsertQueryImpl(entity, metaData);
            }
            case UPDATE -> new EntityUpdateQueryImpl(entity, metaData);
            case DELETE -> {
                cascadeRemove(Set.of(MappingType.ONE_TO_MANY, MappingType.ONE_TO_ONE), entity);
                yield new EntityDeleteQueryImpl(entity, metaData);
            }
            default -> throw new IllegalStateException("Unexpected value: " + action);
        };
    }//getFlushQuery

    private void flushEntityInternal(@Nonnull JPAEntity entity)
    {
        PersistenceAction action = entity._getPendingAction();
        if (action == NONE) {
			/*
			 If the entity is not new and is not dirty but is locked optimistically, we need to update the version
			 */
            if (entity._getLockMode() == LockModeType.OPTIMISTIC_FORCE_INCREMENT) {
                action = UPDATE;
            }//if
            else {
                return;
            }//else
        }//if

        Span span = TRACER.spanBuilder("PersistenceContextImpl::flushEntity").setSpanKind(SpanKind.SERVER).startSpan();
        try (Scope ignored = span.makeCurrent()) {
            span.setAttribute("action", action.name());
            invokeCallbackHandlers(action, true, entity);
            if (!getRollbackOnly()) {
                entity._setPendingAction(NONE);
                EntityQuery flushQuery = getFlushQuery(action, entity);

                if (flushQuery.getQuery() != null && !flushQuery.getQuery().isBlank()) {
                    String sqlQuery = flushQuery.getQuery();
                    span.setAttribute("query", sqlQuery);

                    //noinspection SqlSourceToSinkFlow
                    try (PreparedStatement statement = connection.prepareStatement(sqlQuery, Statement.RETURN_GENERATED_KEYS)) {
                        bindParameters(statement, flushQuery.getParameters());

                        int rows = statement.executeUpdate();
                        if (rows > 0) {
                            if (action == PersistenceAction.DELETE) {
                                entity._setEntityState(EntityState.REMOVED);
                                if (entity._getMetaData().isCacheable()) {
                                    l2Cache().evict(entity.get$$EntityClass(), entity._getPrimaryKey());
                                }//if

                                cascadeRemove(Set.of(MappingType.MANY_TO_ONE), entity);
                            }//if
                            else {
                                if (action == PersistenceAction.INSERT) {
                                    try (ResultSet vResultSet = statement.getGeneratedKeys()) {
                                        if (vResultSet.next()) {
                                            entity._setPersistenceContext(this);
                                            entity._mapResultSet(null, vResultSet);
                                        }//if
                                    }//try

                                    if (cacheStoreMode == CacheStoreMode.USE) {
                                        l2Cache().add(entity);
                                    }//else if
                                }//if
                                else if (entity._getMetaData().isCacheable() && cacheStoreMode != CacheStoreMode.BYPASS) {
                                    l2Cache().replace(entity);
                                }//else if

                                cascadePersist(Set.of(MappingType.ONE_TO_MANY, MappingType.ONE_TO_ONE), entity);
                            }//else
                        }//if
                        /*
                         * If zero rows were updated or deleted and the entity was optimistic locked, then throw an exception
                         */
                        else if (action != INSERT && isOptimisticLocked(entity)) {
                            setRollbackOnly();

                            /*
                             Delete the cached record for the rare case where a cached record might be out of date.
                             NOTE: This is highly unlikely and an error in itself.
                             */
                            if (entity._getMetaData().isCacheable()) {
                                l2Cache().evict(entity.get$$EntityClass(), entity._getPrimaryKey());
                            }//if

                            throw new OptimisticLockException(entity);
                        }//else if
                    }//try
                    catch (SQLException ex) {
                        setRollbackOnly();

                        LOG.error("Failed to flush entity {}, Query: {}", entity._getMetaData().getName(), flushQuery.getQuery(), ex);
                        throw new PersistenceException("Error persisting entity in database");
                    }//catch
                }//if
            }//if

            entity._clearModified();
            invokeCallbackHandlers(action, false, entity);
        }//try
        finally {
            span.end();
        }//finally
    }//flushEntity


    //<editor-fold desc="Transaction Manager Functions">
    @Override
    public void setAutoJoinTransaction()
    {
        autoJoinTransaction = true;
    }//setAutoJoinTransaction

    @Override
    public boolean isAutoJoinTransaction()
    {
        return autoJoinTransaction;
    }//isAutoJoinTransactions

    @Override
    public void joinTransaction()
    {
        if (!joinedToTransaction) {
            try {
                if (transactionManager == null) {
                    transactionManager = (TransactionManager) CDI.current().select(getClass().getClassLoader().loadClass(TransactionManager.class.getName())).get();
                    if (transactionManager == null) {
                        throw new ClassNotFoundException("Transaction Manager not set");
                    }//if
                }//if

                //If we not in a JTA transaction, escape here
                if (!isInJTATransaction()) {
                    return;
                }//if

                joinedToTransaction = true;

                switch (transactionManager.getStatus()) {
                    case Status.STATUS_ACTIVE, Status.STATUS_PREPARED, Status.STATUS_PREPARING -> begin();
                    case Status.STATUS_MARKED_ROLLBACK -> {
                        begin();
                        setRollbackOnly();
                    }
                    default ->
                            throw new TransactionRequiredException("Explicitly joining a JTA transaction requires a JTA transaction be currently active");
                }//switch
            }//try
            catch (ClassNotFoundException ex) {
                throw new PersistenceException("No JTA TransactionManager found, mostly likely this is not an EE application", ex);
            }//catch
            catch (SystemException ex) {
                throw new PersistenceException(ex.getMessage(), ex);
            }//catch
        }//if
    }//joinTransaction

    @Override
    public boolean isJoinedToTransaction()
    {
        return joinedToTransaction;
    }

    private boolean isInJTATransaction()
    {
        if (transactionManager != null) {
            try {
                int status = transactionManager.getStatus();
                return (status == Status.STATUS_ACTIVE || status == Status.STATUS_COMMITTING || status == Status.STATUS_MARKED_ROLLBACK || status == Status.STATUS_PREPARED || status == Status.STATUS_PREPARING);
            }
            catch (Exception ex) {
                throw new PersistenceException(ex);
            }
        }//if
        return false;
    }//joinTransaction


    @Override
    public void afterCompletion(int status)
    {
        if (isActive() && status == Status.STATUS_ROLLEDBACK) {
            setRollbackOnly();
            rollback();
        }//if
    }//afterCompletion

    private void rollbackToDepth(int depth)
    {
        while (transactionDepth.get() > depth) {
            rollback();
        }//while
    }//rollbackToDepth

    @Override
    public EntityTransaction getTransaction()
    {
        if (isAutoJoinTransaction() || isJoinedToTransaction()) {
            throw new IllegalStateException("Transaction is not accessible when using JTA with JPA-compliant transaction access enabled");
        }//if

        return this;
    }//getTransaction

    private void transactionCallback(CallbackMethod callback)
    {
        checkRecursiveCallback();

        inCallbackHandler = true;
        for (EntityTransactionListener listener : listeners) {
            switch (callback) {
                case PRE_BEGIN -> listener.preTransactionBeginEvent();
                case POST_BEGIN -> listener.postTransactionBeginEvent();
                case PRE_COMMIT -> listener.preTransactionCommitEvent();
                case POST_COMMIT -> listener.postTransactionCommitEvent();
                case PRE_ROLLBACK -> listener.preTransactionRollbackEvent();
                case POST_ROLLBACK -> listener.postTransactionRollbackEvent();
            }//switch
        }//for
        inCallbackHandler = false;

        if (!pendingRemoval.isEmpty()) {
            pendingRemoval.forEach(listeners::remove);
        }//if

        if (!pendingAdd.isEmpty()) {
            listeners.addAll(pendingAdd);
        }//if
    }//transactionCallback

    @Override
    public void begin()
    {
        checkReleaseState();
        checkThread();
        Span span = TRACER.spanBuilder("PersistenceContextImpl::begin").setSpanKind(SpanKind.SERVER).startSpan();
        try (Scope ignore = span.makeCurrent()) {
            checkRecursiveCallback();

            if (isActive()) {
                if (getRollbackOnly()) {
                    throw new IllegalStateException("Transaction is current in a rollback only state");
                }//if
                LOG.trace("Set a savepoint at depth {}", transactionDepth.get());
                savepoints.add(connection.setSavepoint());
                transactionDepth.incrementAndGet();
                LOG.debug("Legacy support - Transaction is already active, using depth counter");
            }//if
            else {
                LOG.trace("Beginning a new transaction on {}", this);
                transactionCallback(CallbackMethod.PRE_BEGIN);
                rollbackOnly = false;
                getConnection(connectionName).setAutoCommit(false);
                transactionDepth.set(1);
                l2Cache().begin();
                transactionCallback(CallbackMethod.POST_BEGIN);
            }//else
        }//try
        catch (SQLException ex) {
            throw new PersistenceException("Error beginning a transaction", ex);
        }//catch
        catch (SystemException ex) {
            throw new PersistenceException("Error beginning a transaction in TransactionManager", ex);
        }//catch
        finally {
            span.end();
        }
    }//begin

    @Override
    public void commit()
    {
        checkThread();

        if (isActive()) {
            Span span = TRACER.spanBuilder("PersistenceContextImpl::commit").setSpanKind(SpanKind.SERVER).startSpan();
            try (Scope ignored = span.makeCurrent()) {
                if (getRollbackOnly()) {
                    span.setStatus(StatusCode.ERROR, "Transaction marked for rollback and cannot be committed");
                    throw new RollbackException("Transaction marked for rollback and cannot be committed");
                }//if

                if (transactionDepth.decrementAndGet() > 0) {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("Commit savepoint at depth {}", transactionDepth.get());
                    }//if
                    savepoints.removeLast();
                    return;
                }//if

                transactionCallback(CallbackMethod.PRE_COMMIT);

                flush();
                connection.commit();
                connection.setAutoCommit(true);
                l1Cache().clear();
                l2Cache().commit();

                transactionCallback(CallbackMethod.POST_COMMIT);
                close();
                LOG.trace("Transaction Committed on {}", this);
            }//try
            catch (SQLException ex) {
                setRollbackOnly();
                throw new PersistenceException("Error committing transaction", ex);
            }//catch
            catch (SystemException ex) {
                setRollbackOnly();
                throw new PersistenceException("Error committing transaction in TransactionManager", ex);
            }//catch
            finally {
                span.end();
            }//finally
        }//if
        transactionDepth.set(0);
    }//commit

    @Override
    public void rollback()
    {
        checkThread();
        Span span = TRACER.spanBuilder("PersistenceContextImpl::rollback").setSpanKind(SpanKind.SERVER).startSpan();
        try (Scope ignored = span.makeCurrent()) {
            if (isActive()) {
                if (transactionDepth.decrementAndGet() > 0) {
                    connection.rollback(savepoints.pop());
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("Rolling back to savepoint at depth {}", transactionDepth.get());
                    }//if
                    rollbackOnly = false;
                    return;
                }//if

                transactionCallback(CallbackMethod.PRE_ROLLBACK);

                rollbackOnly = false;
                connection.rollback();
                l2Cache().rollback();
                connection.setAutoCommit(true);

                l1Cache().clear();
                transactionCallback(CallbackMethod.POST_ROLLBACK);

                close();
                LOG.trace("Transaction rolled back on {}", this);
            }//if
        }//try
        catch (SQLException ex) {
            throw new PersistenceException("Error rolling transaction back", ex);
        }//catch
        catch (SystemException ex) {
            throw new PersistenceException("Error rolling transaction back in TransactionManager", ex);
        }//catch
        finally {
            span.end();
        }//finally
    }//rollback

    @Override
    public void setRollbackOnly()
    {
        rollbackOnly = true;
    }

    @Override
    public boolean getRollbackOnly()
    {
        return rollbackOnly;
    }

    @Override
    public boolean isActive()
    {
        return transactionDepth.get() > 0;
    }
    //</editor-fold>

    public boolean isReadonly()
    {
        return readOnly;
    }

    //<editor-fold desc="Legacy support - Methods used by JPADatabase" defaultstate="collapsed">
    public long getSlowQueryTime()
    {
        return slowQueryTime;
    }

    public void setSlowQueryTime(long pSlowQueryTime)
    {
        slowQueryTime = pSlowQueryTime;
    }

    public boolean isEnableLogging()
    {
        return connection.isEnableLogging();
    }

    public void setEnableLogging(boolean pEnableLogging)
    {
        checkOpen();
        connection.setEnableLogging(pEnableLogging && showSql);
    }//setEnableLogging

    public void setReadonly(boolean pReadonly)
    {
        readOnly = pReadonly;
    }

    public void setAuditWriter(PrintWriter pAuditWriter)
    {
        connection.setAuditWriter(pAuditWriter);
    }

    public PrintWriter getAuditWriter()
    {
        return connection.getAuditWriter();
    }
    //</editor-fold>

    @SuppressWarnings("unchecked")
    @Override
    public <T> T unwrap(Class<T> cls)
    {
        if (cls.isAssignableFrom(this.getClass())) {
            return (T) this;
        }

        if (cls.isAssignableFrom(pool.getClass())) {
            return (T) pool;
        }//if

        throw new IllegalArgumentException("Could not unwrap this [" + this + "] as requested Java type [" + cls.getName() + "]");
    }//unwrap
}//PersistenceContextImpl
