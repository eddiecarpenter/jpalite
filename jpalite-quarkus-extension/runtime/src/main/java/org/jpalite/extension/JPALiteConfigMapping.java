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

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefaults;
import io.smallrye.config.WithParentName;
import io.smallrye.config.WithUnnamedKey;
import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import jakarta.persistence.spi.PersistenceUnitTransactionType;
import org.eclipse.microprofile.config.spi.Converter;
import org.jpalite.impl.CacheFormat;

import java.util.Map;

@ConfigMapping(prefix = "jpalite.persistenceUnit")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface JPALiteConfigMapping
{
    String DEFAULT_PERSISTENCE_UNIT_NAME = "<default>";

    /**
     * The defined units
     *
     * @return Map of units
     */
    @ConfigDocSection
    @ConfigDocMapKey("name")
    @WithUnnamedKey(DEFAULT_PERSISTENCE_UNIT_NAME)
    @WithDefaults
    @WithParentName
    Map<String, PersistenceUnitConfig> namedPersistenceUnits();

    class PersistenceUnitTransactionTypeConverter implements Converter<PersistenceUnitTransactionType>
    {
        @Override
        public PersistenceUnitTransactionType convert(String value) throws IllegalArgumentException, NullPointerException
        {
            return PersistenceUnitTransactionType.valueOf(value.toUpperCase());
        }
    }//PersistenceUnitTransactionTypeConverter

    class SharedCacheModeConverter implements Converter<SharedCacheMode>
    {
        @Override
        public SharedCacheMode convert(String value) throws IllegalArgumentException, NullPointerException
        {
            return SharedCacheMode.valueOf(value.toUpperCase());
        }
    }

    class CacheFormatConverter implements Converter<CacheFormat>
    {
        @Override
        public CacheFormat convert(String value) throws IllegalArgumentException, NullPointerException
        {
            return CacheFormat.valueOf(value.toUpperCase());
        }
    }

    class ValidationModeConverter implements Converter<ValidationMode>
    {
        @Override
        public ValidationMode convert(String value) throws IllegalArgumentException, NullPointerException
        {
            return ValidationMode.valueOf(value.toUpperCase());
        }
    }

    default PersistenceUnitConfig getPersistenceUnit(String persistenceUnitName)
    {
        return namedPersistenceUnits().get(persistenceUnitName);
    }//getPersistenceUnit
}//JPAConfigMapping
