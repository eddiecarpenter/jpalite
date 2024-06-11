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

import io.jpalite.PersistenceUnit;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionSynchronizationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Unremovable
@ApplicationScoped
@Slf4j
public class PersistenceProducer
{
	private final Map<String, EntityManagerFactory> entityManagerFactoryList = new ConcurrentHashMap<>();

	@Inject
	TransactionManager transactionManager;

	@Inject
	TransactionSynchronizationRegistry transactionSynchronizationRegistry;

	public EntityManagerFactory getEntityManagerFactory(String persistenceUnit)
	{

		String persistenceUnitName = "<default>";
		if (persistenceUnit != null && !persistenceUnit.isBlank()) {
			if (persistenceUnit.startsWith("${")) {
				String key = persistenceUnit.substring(2, persistenceUnit.lastIndexOf('}'));
				Config configProvider = ConfigProvider.getConfig();
				Optional<String> value = configProvider.getOptionalValue(key, String.class);
				if (value.isPresent()) {
					LOG.debug("Persistence unit name is defined using a variable {} = {} ", persistenceUnitName, value.get());
					persistenceUnitName = value.get();
				}//if
				else {
					LOG.debug("Persistence unit name defined using a variable {} but variable is not defined", persistenceUnitName);
				}//else
			}//if
			else {
				persistenceUnitName = persistenceUnit;
			}
		}//if

		LOG.debug("Producing new Entity Manager Factory using @PersistenceUnit(\"{}\")", persistenceUnitName);
		return entityManagerFactoryList.computeIfAbsent(persistenceUnitName, Persistence::createEntityManagerFactory);
	}

	public EntityManager getEntityManager(String persistenceUnit)
	{
		return new TransactionScopedEntityManagerImpl(getEntityManagerFactory(persistenceUnit),
													  transactionManager,
													  transactionSynchronizationRegistry);
	}//getEntityManager

	@Unremovable
	@Produces
	@PersistenceUnit
	public EntityManagerFactory injectEntityManagerFactory(InjectionPoint injectionPoint)
	{
		String persistenceUnitName = "<default>";

		Annotation qualifier = null;
		if (injectionPoint.getAnnotated() != null) {
			qualifier = injectionPoint.getAnnotated().getAnnotations()
					.stream()
					.filter(PersistenceUnit.class::isInstance)
					.findFirst()
					.orElse(null);
		}//if

		if (qualifier == null) {
			/**
			 * If PersistenceUnit annotation is not found, check if it was not provided
			 * as a qualifier
			 */
			qualifier = injectionPoint.getQualifiers()
					.stream()
					.filter(PersistenceUnit.class::isInstance)
					.findFirst()
					.orElse(null);
		}//if

		if (qualifier instanceof PersistenceUnit persistenceUnit) {
			persistenceUnitName = persistenceUnit.value();
		}//if

		return getEntityManagerFactory(persistenceUnitName);
	}//getEntityManagerFactory

	@Unremovable
	@Produces
	@PersistenceUnit
	public EntityManager injectEntityManager(InjectionPoint injectionPoint)
	{
		return new TransactionScopedEntityManagerImpl(injectEntityManagerFactory(injectionPoint),
													  transactionManager,
													  transactionSynchronizationRegistry);
	}//getEntityManager

	@Unremovable
	@Produces
	public EntityManagerFactory injectDefaultEntityManagerFactory(InjectionPoint injectionPoint)
	{
		return injectEntityManagerFactory(injectionPoint);
	}

	@Unremovable
	@Produces
	public EntityManager injectDefaultEntityManager(InjectionPoint injectionPoint)
	{
		return new TransactionScopedEntityManagerImpl(injectEntityManagerFactory(injectionPoint),
													  transactionManager,
													  transactionSynchronizationRegistry);
	}//getEntityManager
}
