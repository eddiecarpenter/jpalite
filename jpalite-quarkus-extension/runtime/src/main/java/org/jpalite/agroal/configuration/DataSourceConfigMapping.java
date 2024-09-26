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

package org.jpalite.agroal.configuration;

import io.agroal.api.configuration.AgroalConnectionFactoryConfiguration;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.*;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

@ConfigMapping(prefix = "jpalite.datasource")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface DataSourceConfigMapping
{
    /**
     * The default data source
     */
    @WithParentName
    DataSourceConfig defaultDataSource();

    /**
     * The named data sources
     */
    @ConfigDocSection
    @ConfigDocMapKey("datasource-name")
    @WithParentName
    Map<String, DataSourceConfig> namedDataSource();

    interface DataSourceConfig
    {
        /**
         * SThe kind of database we will connect to (e.g. h2, postgresql)
         */
        @WithDefault("postgresql")
        @WithName("db-kind")
        String dbKind();

        /**
         * The datasource username
         */
        Optional<String> username();

        /**
         * The datasource password
         */
        Optional<String> password();

        /**
         * The JDBC Settings for the data source
         *
         * @return
         */
        JDBCConfig jdbc();
    }//DataSourceConfig

    interface JDBCConfig
    {
        /**
         * The datasource URL
         */
        Optional<String> url();

        /**
         * The datasource driver class name
         */
        @WithDefault("org.postgresql.Driver")
        String driver();

        /**
         * The transaction isolation level to use
         */
        @WithName("transaction-isolation-level")
        @WithDefault("READ_COMMITTED")
        @WithConverter(TransactionIsolationConverter.class)
        AgroalConnectionFactoryConfiguration.TransactionIsolation transactionIsolationLevel();

        /**
         * The database schema to use (if supported by the database kind)
         */
        @WithName("currentSchema")
        Optional<String> currentSchema();

        /**
         * Enable datasource metrics collection. If unspecified, collecting metrics will be enabled by default if a metrics extension is active.
         */
        @WithName("enable-metrics")
        @WithDefault("true")
        boolean enableMetrics();

        /**
         * The datasource pool minimum size
         */
        @WithDefault("0")
        @WithName("min-size")
        int minSize();

        /**
         * The datasource pool maximum size
         */
        @WithDefault("10")
        @WithName("max-size")
        int maxSize();

        /**
         * The initial pool size
         */
        @WithDefault("0")
        @WithName("initial-size")
        int initialSize();

        /**
         * The max lifetime of a connection.
         */
        @WithDefault("5M")
        @WithName("max-lifetime")
        @WithConverter(DurationConverter.class)
        Duration maxLifetime();

        /**
         * The interval at which we try to remove idle connections.
         */
        @WithDefault("2M")
        @WithName("idle-removal-interval")
        @WithConverter(DurationConverter.class)
        Duration idleRemovalInterval();

        /**
         * The interval at which we check for connection leaks.
         */
        @WithDefault("1M")
        @WithName("leak-detection-interval")
        @WithConverter(DurationConverter.class)
        Duration leakDetectionInterval();

        /**
         * The timeout before cancelling the acquisition of a new connection
         */
        @WithDefault("1M")
        @WithName("acquisition-timeout")
        @WithConverter(DurationConverter.class)
        Duration acquisitionTimeout();

        /**
         * Request agroal to generate an extended leak report for leaks that are detected
         */
        @WithDefault("true")
        @WithName("extended-leak-report")
        boolean leakReport();

        /**
         * The interval at which we validate idle connections in the background. Set to 0 to disable background validation.
         */
        @WithDefault("2M")
        @WithName("validation-interval")
        @WithConverter(DurationConverter.class)
        Duration validationInterval();

        /**
         * Query executed to validate a connection.
         */
        @WithDefault("select 1")
        @WithName("validation-query")
        String validationQuery();

        /**
         * Other unspecified properties to be passed to the JDBC driver when creating new connections.
         */
        @WithName("additional-jdbc-properties")
        Map<String, String> jdbcProperties();
    }//JDBCConfig

    default DataSourceConfig getDataSourceConfig(String dataSourceName)
    {
        if ("<default>".equals(dataSourceName)) {
            return defaultDataSource();
        }

        return namedDataSource().get(dataSourceName);
    }//getDataSource
}//DataSourceConfigMapping
