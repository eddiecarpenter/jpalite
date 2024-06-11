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

package io.jpalite.agroal;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.AgroalConnectionPoolConfiguration;
import io.agroal.api.configuration.AgroalDataSourceConfiguration;
import io.agroal.api.configuration.supplier.AgroalConnectionFactoryConfigurationSupplier;
import io.agroal.api.configuration.supplier.AgroalConnectionPoolConfigurationSupplier;
import io.agroal.api.configuration.supplier.AgroalDataSourceConfigurationSupplier;
import io.agroal.api.security.NamePrincipal;
import io.agroal.api.security.SimplePassword;
import io.agroal.pool.DataSource;
import io.jpalite.agroal.configuration.DataSourceConfigMapping;
import io.quarkus.arc.Unremovable;
import io.smallrye.config.SmallRyeConfig;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.persistence.PersistenceException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.Config;

import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
@Unremovable
@Slf4j
public class AgroalDataSources
{
	private final Map<String, AgroalDataSource> dataSources = new ConcurrentHashMap<>();

	@Inject
	Config configProvider;

	public AgroalDataSource getDataSource(String dataSourceName)
	{
		return dataSources.computeIfAbsent(dataSourceName, this::loadDataSource);
	}//getDataSource

	private AgroalDataSource loadDataSource(String dataSourceName)
	{
		SmallRyeConfig config = configProvider.unwrap(SmallRyeConfig.class);

		DataSourceConfigMapping dataSourceConfigMapping = config.getConfigMapping(DataSourceConfigMapping.class);

		DataSourceConfigMapping.DataSourceConfig dataSourceConfig = dataSourceConfigMapping.getDataSourceConfig(dataSourceName);
		if (dataSourceConfig == null) {
			throw new PersistenceException("DataSource '" + dataSourceName + "' not found");
		}//if

		if (dataSourceConfig.jdbc().url().isEmpty()) {
			throw new PersistenceException("The jdbc.url property is missing for DataSource '" + dataSourceName + "'");
		}//if

		LOG.info("Creating Agroal data source ({}) for url {}", dataSourceName, dataSourceConfig.jdbc().url().get());
		AgroalConnectionFactoryConfigurationSupplier connectionConfiguration = new AgroalConnectionFactoryConfigurationSupplier()
				.jdbcUrl(dataSourceConfig.jdbc().url().get())
				.connectionProviderClassName(dataSourceConfig.jdbc().driver())
				.autoCommit(true)
				.jdbcTransactionIsolation(dataSourceConfig.jdbc().transactionIsolationLevel());


		dataSourceConfig.jdbc().jdbcProperties().forEach((k, v) -> connectionConfiguration.jdbcProperty(k, v));
		if (dataSourceConfig.jdbc().currentSchema().isPresent()) {
			connectionConfiguration.jdbcProperty("currentSchema", dataSourceConfig.jdbc().currentSchema().get());
		}//if

		if (dataSourceConfig.username().isPresent()) {
			NamePrincipal username = new NamePrincipal(dataSourceConfig.username().get());
			connectionConfiguration.principal(username).recoveryPrincipal(username);
		}//if
		if (dataSourceConfig.password().isPresent()) {
			SimplePassword password = new SimplePassword(dataSourceConfig.password().get());
			connectionConfiguration.credential(password).credential(password);
		}//if

		AgroalConnectionPoolConfigurationSupplier poolConfiguration = new AgroalConnectionPoolConfigurationSupplier()
				.minSize(dataSourceConfig.jdbc().minSize())
				.maxSize(dataSourceConfig.jdbc().maxSize())
				.initialSize(dataSourceConfig.jdbc().initialSize())
				.connectionValidator(selectValidator(dataSourceConfig.jdbc().validationQuery()))
				.acquisitionTimeout(dataSourceConfig.jdbc().acquisitionTimeout())
				.leakTimeout(dataSourceConfig.jdbc().leakDetectionInterval())
				.validationTimeout(dataSourceConfig.jdbc().validationInterval())
				.reapTimeout(dataSourceConfig.jdbc().idleRemovalInterval())
				.maxLifetime(dataSourceConfig.jdbc().maxLifetime())
				.enhancedLeakReport(dataSourceConfig.jdbc().leakReport())
				.connectionFactoryConfiguration(connectionConfiguration);


		AgroalDataSourceConfigurationSupplier agroalConfig = new AgroalDataSourceConfigurationSupplier()
				.dataSourceImplementation(AgroalDataSourceConfiguration.DataSourceImplementation.AGROAL)
				.metricsEnabled(dataSourceConfig.jdbc().enableMetrics())
				.connectionPoolConfiguration(poolConfiguration);

		LOG.debug("Creating agroal pool {} with URI {}", dataSourceName, dataSourceConfig.jdbc().url());
		return new DataSource(agroalConfig.get());
	}//loadDataSource

	private AgroalConnectionPoolConfiguration.ConnectionValidator selectValidator(final String validationQuery)
	{
		return connection ->
		{
			try (Statement check = connection.createStatement()) {
				check.execute(validationQuery);
				return connection.isValid(0);
			}
			catch (Exception t) {
				return false;
			}
		};
	}//SelectValidator
}//AgroalDataSources
