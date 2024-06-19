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

package io.jpalite.impl.caching;

import io.jpalite.*;
import io.jpalite.impl.CacheFormat;
import io.jpalite.impl.JPAConfig;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import jakarta.annotation.Nonnull;
import jakarta.persistence.SharedCacheMode;
import jakarta.transaction.SystemException;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("java:S3740")//Have to work without generics
@Slf4j
public class EntityCacheImpl implements EntityCache
{
	private static final int ACTION_REPLACE = 1;
	private static final int ACTION_REMOVE = 2;

	private static final Tracer TRACER = GlobalOpenTelemetry.get().getTracer(EntityCacheImpl.class.getName());
	public static final String NO_TRANSACTION_ACTIVE = "No Transaction active";
	public static final String ENTITY_ATTR = "entity";
	public static final String ENTITY_KEY = "key";
	private static final boolean CACHING_ENABLED = JPAConfig.getValue("jpalite.persistence.l2cache", true);

	private final CacheFormat cacheFormat;
	private final List<CacheEntry> batchQueue = new ArrayList<>();
	private boolean inTransaction;
	private JPACache jpaCache = null;

	private record CacheEntry(int action, JPAEntity entity)
	{
	}

	@SuppressWarnings("unchecked")
	public EntityCacheImpl(JPALitePersistenceUnit persistenceUnit)
	{
		cacheFormat = persistenceUnit.getCacheFormat();
		inTransaction = false;
		if (CACHING_ENABLED && !persistenceUnit.getSharedCacheMode().equals(SharedCacheMode.NONE)) {
			try {
				Class<JPACache> jpaCacheClass = (Class<JPACache>) Thread.currentThread().getContextClassLoader().loadClass(persistenceUnit.getCacheProvider());
				jpaCache = jpaCacheClass.getConstructor(String.class, String.class, String.class).newInstance(persistenceUnit.getCacheClient(), persistenceUnit.getCacheConfig(), persistenceUnit.getCacheRegionPrefix());
			}
			catch (ClassNotFoundException | InvocationTargetException | InstantiationException |
				   IllegalAccessException | NoSuchMethodException ex) {
				throw new CachingException("Error loading cache provider class [" + persistenceUnit.getCacheProvider() + "]", ex);
			}
		}//if
	}//EntityCacheImpl

	public <T> T find(Class<T> entityType, Object primaryKey)
	{
		Span span = TRACER.spanBuilder("EntityCache::find").setSpanKind(SpanKind.SERVER).startSpan();
		try (Scope ignored = span.makeCurrent()) {
			long start = System.currentTimeMillis();
			if (jpaCache != null) {
				EntityMetaData<T> metaData = EntityMetaDataManager.getMetaData(entityType);
				if (metaData.isCacheable()) {
					String key = primaryKey.toString();
					span.setAttribute(ENTITY_KEY, key);
					span.setAttribute(ENTITY_ATTR, entityType.getName());
					if (cacheFormat == CacheFormat.BINARY) {
						byte[] bytes = jpaCache.find(metaData.getName(), key);
						if (bytes != null) {
							LOG.debug("Searching L2 cache (Binary) for key [{}] - Hit in {}ms", key, System.currentTimeMillis() - start);
							T entity = metaData.getNewEntity();
							((JPAEntity) entity)._deserialize(bytes);
							return entity;
						}//if
					}//if
					else {
						String jsonStr = jpaCache.find(metaData.getName(), key);
						if (jsonStr != null) {
							LOG.debug("Searching L2 cache (JSON) for key [{}] - Hit in {}ms", key, System.currentTimeMillis() - start);
							T entity = metaData.getNewEntity();
							((JPAEntity) entity)._fromJson(jsonStr);
							return entity;
						}//if
					}
					LOG.debug("Searching L2 cache for key [{}] - Missed in {}ms", key, System.currentTimeMillis() - start);
				}//if
				else {
					LOG.debug("Entity {} is not cacheable", metaData.getName());
				}//else
			}//if
		}//try
		finally {
			span.end();
		}//finally

		return null;
	}//find

	@Override
	public void replace(JPAEntity entity)
	{
		if (jpaCache != null && entity._getMetaData().isCacheable() && inTransaction) {
			batchQueue.add(new CacheEntry(ACTION_REPLACE, entity));
		}//if
	}//replace

	@Override
	public void add(JPAEntity entity)
	{
		Span span = TRACER.spanBuilder("EntityCache::add").setSpanKind(SpanKind.SERVER).startSpan();
		try (Scope ignored = span.makeCurrent()) {
			if (jpaCache != null && entity._getMetaData().isCacheable()) {
				long start = System.currentTimeMillis();
				String key = entity._getPrimaryKey().toString();
				span.setAttribute(ENTITY_KEY, key);
				span.setAttribute(ENTITY_ATTR, entity._getMetaData().getName());

				jpaCache.add(entity._getMetaData().getName(), key, (cacheFormat.equals(CacheFormat.BINARY) ? entity._serialize() : entity._toJson()), entity._getMetaData().getIdleTime(), entity._getMetaData().getCacheTimeUnit());
				LOG.debug("Adding/Replacing Entity with key [{}] in L2 cache in {}ms", key, System.currentTimeMillis() - start);
			}//if
		}//try
		finally {
			span.end();
		}
	}//add

	@Override
	@Nonnull
	public <T> Instant getLastModified(Class<T> entityType)
	{
		Span span = TRACER.spanBuilder("EntityCache::getLastModified").setSpanKind(SpanKind.SERVER).startSpan();
		try (Scope ignored = span.makeCurrent()) {
			EntityMetaData<T> metaData = EntityMetaDataManager.getMetaData(entityType);
			if (jpaCache != null && metaData.isCacheable()) {
				return jpaCache.getLastModified(metaData.getName());
			}//if

			return Instant.now();
		}//try
		finally {
			span.end();
		}//finally
	}//getLastModified

	@Override
	public boolean contains(Class entityType, Object primaryKey)
	{
		Span span = TRACER.spanBuilder("EntityCache::contains").setSpanKind(SpanKind.SERVER).startSpan();
		try (Scope ignored = span.makeCurrent()) {
			EntityMetaData<?> metaData = EntityMetaDataManager.getMetaData(entityType);
			if (jpaCache != null && metaData.isCacheable()) {
				return jpaCache.containsKey(metaData.getName(), primaryKey.toString());
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
		Span span = TRACER.spanBuilder("EntityCache::evict using Primary key").setSpanKind(SpanKind.SERVER).startSpan();
		try (Scope ignored = span.makeCurrent()) {
			EntityMetaData<?> metaData = EntityMetaDataManager.getMetaData(entityType);
			if (jpaCache != null && metaData.isCacheable()) {
				jpaCache.evict(metaData.getName(), primaryKey.toString());
			}//if
		}//try
		finally {
			span.end();
		}//finally
	}//evict

	@Override
	public void evict(Class entityType)
	{
		Span span = TRACER.spanBuilder("EntityCache::evict by type").setSpanKind(SpanKind.SERVER).startSpan();
		try (Scope ignored = span.makeCurrent()) {
			EntityMetaData<?> metaData = EntityMetaDataManager.getMetaData(entityType);
			if (jpaCache != null && metaData.isCacheable()) {
				jpaCache.evictAll(metaData.getName());
			}//if
		}//try
		finally {
			span.end();
		}
	}//evict

	@Override
	public void evictAll()
	{
		Span span = TRACER.spanBuilder("EntityCache::evictAll").setSpanKind(SpanKind.SERVER).startSpan();
		try (Scope ignored = span.makeCurrent()) {
			if (jpaCache != null) {
				jpaCache.evictAllRegions();
			}//if
		}//try
		finally {
			span.end();
		}//finally
	}//evictAll

	@Override
	public void begin() throws SystemException
	{
		Span span = TRACER.spanBuilder("EntityCache::begin").setSpanKind(SpanKind.SERVER).startSpan();
		try (Scope ignored = span.makeCurrent()) {
			if (CACHING_ENABLED) {
				if (inTransaction) {
					throw new SystemException("Transaction already in progress");
				}//if
				inTransaction = true;
			}//if
		}//try
		finally {
			span.end();
		}//finally
	}//begin

	@Override
	public void commit() throws SystemException
	{
		Span span = TRACER.spanBuilder("EntityCache::commit").setSpanKind(SpanKind.SERVER).startSpan();
		try (Scope ignored = span.makeCurrent()) {
			if (jpaCache != null) {
				if (!inTransaction) {
					throw new SystemException(NO_TRANSACTION_ACTIVE);
				}//if

				inTransaction = false;
				batchQueue.forEach(e -> {
					if (e.action == ACTION_REMOVE) {
						jpaCache.evict(e.entity()._getMetaData().getName(),
									   e.entity._getPrimaryKey().toString());
					}//if
					else {
						jpaCache.replace(e.entity()._getMetaData().getName(),
										 e.entity._getPrimaryKey().toString(),
										 (cacheFormat.equals(CacheFormat.BINARY) ? e.entity()._serialize() : e.entity()._toJson()),
										 e.entity()._getMetaData().getIdleTime(),
										 e.entity()._getMetaData().getCacheTimeUnit());
					}//if
				});
				batchQueue.clear();
			}//if
		}//try
		finally {
			span.end();
		}//finally
	}//commit


	@Override
	public void rollback() throws SystemException
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
	@SuppressWarnings("unchecked")
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
