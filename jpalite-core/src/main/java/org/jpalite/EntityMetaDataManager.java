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

package org.jpalite;

import org.jpalite.impl.ConverterClassImpl;
import org.jpalite.impl.EntityMetaDataImpl;
import jakarta.annotation.Nonnull;
import jakarta.persistence.PersistenceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@SuppressWarnings("java:S3740") // Suppress SonarQube warning about using Generics
public class EntityMetaDataManager
{
	private static final Logger LOG = LoggerFactory.getLogger(EntityMetaDataManager.class);
	private static final Map<String, EntityMetaData<?>> REGISTRY_ENTITY_CLASSES = new ConcurrentHashMap<>();
	private static final Map<String, String> REGISTRY_ENTITY_NAMES = new ConcurrentHashMap<>();
	private static final Map<Class<?>, ConverterClass> REGISTRY_CONVERTERS = new ConcurrentHashMap<>();
	private static boolean registryLoaded = false;
	private static final ReentrantLock lock = new ReentrantLock();

	static {
		loadEntities();
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static void loadEntities()
	{
		lock.lock();
		try {
			if (!registryLoaded) {
				try {
					REGISTRY_CONVERTERS.clear();

					ClassLoader loader = Thread.currentThread().getContextClassLoader();

					long start = System.currentTimeMillis();
					ServiceLoader<FieldConvertType> converterLoader = ServiceLoader.load(FieldConvertType.class);
					for (FieldConvertType<?, ?> converter : converterLoader) {
						registerConverter(converter);
					}

					LOG.info("Loaded {} converters in {}ms", REGISTRY_CONVERTERS.size(), System.currentTimeMillis() - start);

					start = System.currentTimeMillis();
					Enumeration<URL> urls = loader.getResources("META-INF/persistenceUnits.properties");
					while (urls.hasMoreElements()) {
						URL location = urls.nextElement();
						try (InputStream inputStream = location.openStream()) {
							Properties properties = new Properties();
							properties.load(inputStream);
							properties.forEach((k, v) ->
											   {
												   try {
													   Class<?> entityClass = loader.loadClass(v.toString());
													   String regEntity = REGISTRY_ENTITY_NAMES.get(entityClass.getSimpleName());
													   if (regEntity == null || !regEntity.equals(v.toString())) {
														   register(new EntityMetaDataImpl(entityClass));
													   }//if
												   }
												   catch (ClassNotFoundException ex) {
													   LOG.warn("Error loading Entity {}", v, ex);
												   }//catch
											   });
						}//try
					}//while
					LOG.info("Loaded {} entities in {}ms", REGISTRY_ENTITY_CLASSES.size(), System.currentTimeMillis() - start);
				}//try
				catch (IOException ex) {
					throw new PersistenceException("Error reading persistenceUnits.properties", ex);
				}//catch

				registryLoaded = true;
			}//if
		}//try
		finally {
			lock.unlock();
		}
	}//loadEntities

	public static int getEntityCount()
	{
		return REGISTRY_ENTITY_NAMES.size();
	}//getEntityCount

	@Nonnull
	public static <T> EntityMetaData<T> getMetaData(Class<?> entityName)
	{
		EntityMetaData metaData = REGISTRY_ENTITY_CLASSES.get(entityName.getCanonicalName());
		if (metaData == null) {
			throw new IllegalArgumentException(entityName.getCanonicalName() + " is not a known entity or not yet registered");
		}//if

		return metaData;
	}//getMetaData

	public static void registerConverter(@Nonnull FieldConvertType<?, ?> converter)
	{
		ConverterClass convertClass = new ConverterClassImpl(converter);
		if (convertClass.isAutoApply()) {
			REGISTRY_CONVERTERS.put(convertClass.getAttributeType(), convertClass);
		}
	}

	public static void register(@Nonnull EntityMetaData<?> metaData)
	{
		if (REGISTRY_ENTITY_NAMES.containsKey(metaData.getName())) {
			throw new IllegalArgumentException("EntityMetaData already registered for " + metaData.getName());
		}//if

		REGISTRY_ENTITY_NAMES.put(metaData.getName(), metaData.getEntityClass().getCanonicalName());
		REGISTRY_ENTITY_CLASSES.put(metaData.getEntityClass().getCanonicalName(), metaData);
	}//register

	public static boolean isRegistered(Class<?> entityName)
	{
		return REGISTRY_ENTITY_CLASSES.containsKey(entityName.getCanonicalName());
	}//isRegistered

	public static ConverterClass getConvertClass(Class<?> attributeType)
	{
		return REGISTRY_CONVERTERS.get(attributeType);
	}//getConvertClass

	public static EntityMetaData getMetaData(String entityName)
	{
		EntityMetaData<?> metaData = null;
		String entityClass = REGISTRY_ENTITY_NAMES.get(entityName);
		if (entityClass != null) {
			metaData = REGISTRY_ENTITY_CLASSES.get(entityClass);
		}//if
		if (metaData == null) {
			throw new IllegalArgumentException(entityName + " is not a known entity or not yet registered");
		}//if
		return metaData;
	}//getMetaData

	private EntityMetaDataManager()
	{
		//Made private to prevent instantiation
	}//EntityMetaDataManager
}//EntityMetaDataManager
