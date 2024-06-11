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

import io.jpalite.*;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.infinispan.client.runtime.InfinispanClientProducer;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.SharedCacheMode;
import jakarta.transaction.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.exceptions.HotRodClientException;
import org.infinispan.commons.api.query.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("java:S3740")//Have to work without generics
@Slf4j
public class EntityL2CacheImpl implements EntityCache
{
	private static final Tracer TRACER = GlobalOpenTelemetry.get().getTracer(EntityL2CacheImpl.class.getName());
	public static final String NO_TRANSACTION_ACTIVE = "No Transaction active";
	public static final String ENTITY_ATTR = "entity";
	private RemoteCacheManager remoteCacheManager;
	private final JPALitePersistenceUnit persistenceUnit;
	private static final boolean CACHING_ENABLED = JPAConfig.getValue("tradeswitch.persistence.l2cache", true);
	private boolean inTransaction;
	private final List<CacheEntry> batchQueue = new ArrayList<>();

	private static final int ACTION_ADD = 0;
	private static final int ACTION_UPDATE = 1;
	private static final int ACTION_REMOVE = 2;

	@Getter
	public static class CacheEntry
	{
		private final int action;

		private final String key;
		private final Object value;
		private final long lifespan;
		private final TimeUnit lifespanUnit;
		private final long maxIdleTime;
		private final TimeUnit maxIdleTimeUnit;

		public CacheEntry(int action, String key, Object value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit)
		{
			this.action = action;
			this.key = key;
			this.value = value;
			this.lifespan = lifespan;
			this.lifespanUnit = lifespanUnit;
			this.maxIdleTime = maxIdleTime;
			this.maxIdleTimeUnit = maxIdleTimeUnit;
		}
	}

	public EntityL2CacheImpl(JPALitePersistenceUnit persistenceUnit)
	{
		this.persistenceUnit = persistenceUnit;
		remoteCacheManager = null;
		inTransaction = false;
	}//EntityCacheImpl

	@Nullable
	@SuppressWarnings("java:S1168") // Null is expected and indicates that caching is not enabled
	private <T> RemoteCache<String, T> getCache()
	{
		if (CACHING_ENABLED && !persistenceUnit.getSharedCacheMode().equals(SharedCacheMode.NONE)) {
			if (remoteCacheManager == null) {
				InstanceHandle<InfinispanClientProducer> infinispanClientProducer = Arc.container().instance(InfinispanClientProducer.class);
				if (infinispanClientProducer.isAvailable()) {
					remoteCacheManager = infinispanClientProducer.get().getNamedRemoteCacheManager(persistenceUnit.getCacheProvider());
				}//if
				if (remoteCacheManager == null || !remoteCacheManager.isStarted()) {
					remoteCacheManager = null;
					return null;
				}//if
			}//if

			RemoteCache<String, T> cache = remoteCacheManager.getCache(persistenceUnit.getCacheName());
			if (cache == null) {
				cache = remoteCacheManager.administration().getOrCreateCache(persistenceUnit.getCacheName(), persistenceUnit.getCacheConfig());
			}//if
			return cache;
		}

		return null;
	}

	private String makeCacheKey(Class<?> entityClass, Object key)
	{
		return entityClass.getSimpleName() +
				":P:" +
				(key == null ? "<NULL>" : key.toString());
	}//makeCacheKey

	private <T> void checkEntityType(Class<T> entityType)
	{
		EntityMetaData metaData = EntityMetaDataManager.getMetaData(entityType);
		if (!metaData.isCacheable()) {
			throw new IllegalArgumentException("Entity [" + entityType.getName() + "] is not cacheable.");
		}//if
	}//checkEntityType

	private void checkEntityInstance(JPAEntity entity)
	{
		if (!entity._getMetaData().isCacheable()) {
			throw new IllegalArgumentException("Entity [" + entity.getClass().getName() + "] is not cacheable.");
		}//if
	}//entity

	public <T> T find(Class<T> entityType, Object primaryKey)
	{
		Span span = TRACER.spanBuilder("EntityL2CacheImpl::find").setSpanKind(SpanKind.SERVER).startSpan();
		try (Scope ignored = span.makeCurrent()) {
			checkEntityType(entityType);

			long start = System.currentTimeMillis();
			RemoteCache<String, T> cache = getCache();
			if (cache != null) {
				String key = makeCacheKey(entityType, primaryKey);
				span.setAttribute("key", key);
				span.setAttribute(ENTITY_ATTR, entityType.getName());
				T entityObject = cache.get(key);
				if (entityObject != null) {
					LOG.debug("Searching L2 cache for key [{}] - Hit in {}ms", key, System.currentTimeMillis() - start);
					return entityObject;
				}//if
				LOG.debug("Searching L2 cache for key [{}] - Missed in {}ms", key, System.currentTimeMillis() - start);
			}//if
		}//try
		finally {
			span.end();
		}//finally

		return null;
	}//find

	@Override
	@Nonnull
	public <T> List<T> search(Class<T> entityType, String query)
	{
		Span span = TRACER.spanBuilder("EntityL2CacheImpl::search").setSpanKind(SpanKind.SERVER).startSpan();
		try (Scope ignored = span.makeCurrent()) {
			checkEntityType(entityType);
			RemoteCache<String, String> cache = getCache();
			if (cache != null) {
				String queryText = "from org.tradeswitch." + entityType.getSimpleName() + " " + query;
				try {
					span.setAttribute("query", queryText);
					span.setAttribute(ENTITY_ATTR, entityType.getName());

					LOG.debug("Querying L2 cache : {}", queryText);
					Query<T> q = cache.query(queryText);
					List<T> result = q.execute().list();
					LOG.debug("Querying L2 cache - Found {} records", result.size());
					return result;
				}//try
				catch (HotRodClientException ex) {
					LOG.debug("Search error:{}", ex.getMessage(), ex);
				}//catch
			}//if
			return Collections.emptyList();
		}//try
		finally {
			span.end();
		}//finally
	}//search

	@Override
	public void update(JPAEntity entity)
	{
		checkEntityInstance(entity);

		if (CACHING_ENABLED && inTransaction) {
			RemoteCache<String, Object> cache = getCache();
			if (cache != null) {
				String key = makeCacheKey(entity.getClass(), entity._getPrimaryKey());
				batchQueue.add(new CacheEntry(ACTION_UPDATE, key, entity, -1, TimeUnit.SECONDS, entity._getMetaData().getIdleTime(), entity._getMetaData().getCacheTimeUnit()));
				batchQueue.add(new CacheEntry(ACTION_ADD, entity.getClass().getName(), System.currentTimeMillis(), -1, TimeUnit.SECONDS, -1, TimeUnit.SECONDS));
			}//if
		}//if
	}//update

	@Override
	public void add(JPAEntity entity)
	{
		checkEntityInstance(entity);

		Span span = TRACER.spanBuilder("EntityL2CacheImpl::add").setSpanKind(SpanKind.SERVER).startSpan();
		try (Scope ignored = span.makeCurrent()) {
			if (CACHING_ENABLED) {
				long start = System.currentTimeMillis();
				RemoteCache<String, Object> cache = getCache();
				if (cache != null) {
					String key = makeCacheKey(entity.getClass(), entity._getPrimaryKey());
					span.setAttribute("key", key);
					span.setAttribute(ENTITY_ATTR, entity._getMetaData().getName());

					cache.put(key, entity, -1, TimeUnit.SECONDS, entity._getMetaData().getIdleTime(), entity._getMetaData().getCacheTimeUnit());
					LOG.debug("Adding/Replacing Entity with key [{}] in L2 cache in {}ms", key, System.currentTimeMillis() - start);
				}//if
			}//else
		}//try
		finally {
			span.end();
		}
	}//add

	@Override
	public void remove(JPAEntity entity)
	{
		Span span = TRACER.spanBuilder("EntityL2CacheImpl::remove").setSpanKind(SpanKind.SERVER).startSpan();
		try (Scope ignored = span.makeCurrent()) {
			checkEntityInstance(entity);

			if (CACHING_ENABLED) {
				long start = System.currentTimeMillis();
				RemoteCache<String, Object> cache = getCache();
				if (cache != null) {
					String key = makeCacheKey(entity.getClass(), entity._getPrimaryKey());
					span.setAttribute("key", key);
					span.setAttribute(ENTITY_ATTR, entity._getMetaData().getName());
					if (inTransaction) {
						batchQueue.add(new CacheEntry(ACTION_REMOVE, key, entity, -1, TimeUnit.SECONDS, -1, TimeUnit.SECONDS));
						batchQueue.add(new CacheEntry(ACTION_ADD, entity.getClass().getName(), System.currentTimeMillis(), -1, TimeUnit.SECONDS, -1, TimeUnit.SECONDS));
					}//if
					else {
						cache.remove(key);
						//Write a timestamp for the update
						cache.put(entity.getClass().getName(), System.currentTimeMillis(), -1, TimeUnit.SECONDS, -1, TimeUnit.SECONDS);
						LOG.debug("Removed Entity with key [{}] from L2 cache in {}m", key, System.currentTimeMillis() - start);
					}//else
				}//if
			}//if
		}//try
		finally {
			span.end();
		}//finally
	}//remove

	public <T> long lastModified(Class<T> entityType)
	{
		Long time = -1L;
		Span span = TRACER.spanBuilder("EntityL2CacheImpl::lastModified").setSpanKind(SpanKind.SERVER).startSpan();
		try (Scope ignored = span.makeCurrent()) {
			checkEntityType(entityType);
			RemoteCache<String, Long> cache = getCache();
			if (cache != null) {
				span.setAttribute(ENTITY_ATTR, entityType.getName());
				time = cache.get(entityType.getName());
				if (time != null) {
					return time;
				}//if

				time = System.currentTimeMillis();
				cache.put(entityType.getName(), time, -1, TimeUnit.SECONDS, -1, TimeUnit.SECONDS);
			}//if
		}//try
		finally {
			span.end();
		}//finally

		return time;
	}//lastModified

	@Override
	public boolean contains(Class entityType, Object primaryKey)
	{
		Span span = TRACER.spanBuilder("EntityL2CacheImpl::contains").setSpanKind(SpanKind.SERVER).startSpan();
		try (Scope ignored = span.makeCurrent()) {
			checkEntityType(entityType);

			RemoteCache<String, Object> cache = getCache();
			if (cache != null) {
				String key = makeCacheKey(entityType, primaryKey);
				return cache.containsKey(key);
			}//if
			return false;
		}//try
		finally {
			span.end();
		}//finally
	}//contains

	@Override
	public void evict(Class entityType, Object primaryKey)
	{
		Span span = TRACER.spanBuilder("EntityL2CacheImpl::evict using Primarykey").setSpanKind(SpanKind.SERVER).startSpan();
		try (Scope ignored = span.makeCurrent()) {
			checkEntityType(entityType);
			RemoteCache<String, Object> cache = getCache();
			if (cache != null) {
				String key = makeCacheKey(entityType, primaryKey);
				cache.remove(key);
			}//if
		}//try
		finally {
			span.end();
		}//finally
	}//evict

	@Override
	public void evict(Class entityType)
	{
		Span span = TRACER.spanBuilder("EntityL2CacheImpl::evict by type").setSpanKind(SpanKind.SERVER).startSpan();
		try (Scope ignored = span.makeCurrent()) {
			checkEntityType(entityType);
			RemoteCache<String, Object> cache = getCache();
			if (cache != null) {
				Query<Object> q = cache.query("delete from " + entityType.getName());
				q.executeStatement();
			}//if
		}//try
		finally {
			span.end();
		}
	}//evict

	@Override
	public void evictAll()
	{
		Span span = TRACER.spanBuilder("EntityL2CacheImpl::evictAll").setSpanKind(SpanKind.SERVER).startSpan();
		try (Scope ignored = span.makeCurrent()) {
			RemoteCache<String, Object> cache = getCache();
			if (cache != null) {
				cache.clear();
			}//if
		}//try
		finally {
			span.end();
		}//finally
	}//evictAll

	@Override
	public void begin() throws NotSupportedException, SystemException
	{
		Span span = TRACER.spanBuilder("EntityL2CacheImpl::begin").setSpanKind(SpanKind.SERVER).startSpan();
		try (Scope ignored = span.makeCurrent()) {
			if (CACHING_ENABLED) {
				if (inTransaction) {
					throw new NotSupportedException("Transaction already in progress");
				}//if
				inTransaction = true;
			}//if
		}//try
		finally {
			span.end();
		}//finally
	}//begin

	@Override
	public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, IllegalStateException, SystemException
	{
		Span span = TRACER.spanBuilder("EntityL2CacheImpl::commit").setSpanKind(SpanKind.SERVER).startSpan();
		try (Scope ignored = span.makeCurrent()) {
			if (CACHING_ENABLED) {
				if (!inTransaction) {
					throw new SystemException(NO_TRANSACTION_ACTIVE);
				}//if

				inTransaction = false;
				RemoteCache<String, Object> cache = getCache();
				if (cache != null) {
					batchQueue.forEach(e -> {
						if (e.action == ACTION_REMOVE) {
							cache.remove(e.getKey());
						}//if
						else {
							if (e.action == ACTION_ADD) {
								cache.put(e.getKey(), e.getValue(), e.getLifespan(), e.getLifespanUnit(), e.getMaxIdleTime(), e.getMaxIdleTimeUnit());
							}//if
							else {
								cache.replace(e.getKey(), e.getValue(), e.getLifespan(), e.getLifespanUnit(), e.getMaxIdleTime(), e.getMaxIdleTimeUnit());
							}//else
						}//if
					});
					batchQueue.clear();
				}//if
			}//if
		}//try
		finally {
			span.end();
		}//finally
	}//commit


	@Override
	public void rollback() throws IllegalStateException, SecurityException, SystemException
	{
		if (CACHING_ENABLED) {
			if (!inTransaction) {
				throw new SystemException(NO_TRANSACTION_ACTIVE);
			}//if

			inTransaction = false;
			batchQueue.clear();
		}//if
	}//rollback

	@Override
	public <T> T unwrap(Class<T> cls)
	{
		if (cls.isAssignableFrom(this.getClass())) {
			return (T) this;
		}

		if (cls.isAssignableFrom(EntityCache.class)) {
			return (T) this;
		}

		throw new IllegalArgumentException("Could not unwrap this [" + this + "] as requested Java type [" + cls.getName() + "]");
	}
}
