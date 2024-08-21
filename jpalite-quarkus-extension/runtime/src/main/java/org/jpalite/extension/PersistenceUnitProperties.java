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

package org.jpalite.extension;

import io.smallrye.config.SmallRyeConfig;
import jakarta.persistence.PersistenceException;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jpalite.impl.CustomPersistenceUnit;

public class PersistenceUnitProperties extends CustomPersistenceUnit
{

    public PersistenceUnitProperties(String unitName)
    {
        super(unitName);

        Config configProvider = ConfigProvider.getConfig();
        SmallRyeConfig config = configProvider.unwrap(SmallRyeConfig.class);
        JPALiteConfigMapping jpaConfigMapping = config.getConfigMapping(JPALiteConfigMapping.class);
        PersistenceUnitConfig unitConfig = jpaConfigMapping.getPersistenceUnit(unitName);
        if (unitConfig == null) {
            throw new PersistenceException("Persistent Unit '" + unitName + "' not found");
        }//if

        getProperties().putAll(unitConfig.properties());
        setMultiTenantMode(unitConfig.multiTenant());
        setTransactionType(unitConfig.transactionType());
        setDataSourceName(unitConfig.datasourceName());
        setCacheRegionPrefix(unitConfig.cacheRegionPrefix());
        setCacheFormat(unitConfig.cacheFormat());
        setSharedCacheMode(unitConfig.sharedCacheMode());
        setValidationMode(unitConfig.validationMode());
        setCacheClient(unitConfig.cacheClient());
        setCacheProvider(unitConfig.cacheProvider());
        setCacheConfig(unitConfig.cacheConfig());
    }//PersistenceUnitProperties

}//PersistenceUnitProperties
