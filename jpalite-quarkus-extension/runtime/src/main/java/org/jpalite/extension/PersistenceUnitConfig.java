package org.jpalite.extension;

import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;
import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import jakarta.persistence.spi.PersistenceUnitTransactionType;
import org.jpalite.JPALiteEntityManager;
import org.jpalite.impl.CacheFormat;

import java.util.Map;

public interface PersistenceUnitConfig
{
    /**
     * Specifies a valid datasource that will be associated with the persistence unit.
     * The default value points to the default datasource.
     */
    @WithDefault("<default>")
    @WithName("datasource-name")
    String datasourceName();

    /**
     * The region prefix to use (second level caching)
     */
    @WithDefault("<default>")
    @WithName("cache-region-prefix")
    String cacheRegionPrefix();

    /**
     * Retrieves the cache client (infinispan-client, redis-client, etc) to be used by the cache provider.
     * Care should be taken to reference a client that is compatible with the cache provider specified.
     *
     * @return The cache client name as specified in the persistence unit configuration, or the default value if not set.
     * @see PersistenceUnitConfig#cacheClient()
     */
    @WithDefault("<default>")
    @WithName("cache-client")
    String cacheClient();

    /**
     * The name of the 2nd level cache provider that is to be used.
     */
    @WithDefault("org.jpalite.impl.caching.JPALiteInfinispanCache")
    @WithName("cache-provider")
    String cacheProvider();

    /**
     * The cache configuration to use when create a new cache
     */
    @WithDefault("<distributed-cache mode=\"ASYNC\" statistics=\"true\">" +
                 "  <encoding media-type=\"application/x-protostream\"/>" +
                 "  <memory max-count=\"1000000\" when-full=\"REMOVE\"/>" +
                 "</distributed-cache>")
    @WithName("cache-config")
    String cacheConfig();

    /**
     * The cache format to use when storing cache entities
     */
    @WithDefault("JSON")
    @WithName("cache-format")
    @WithConverter(JPALiteConfigMapping.CacheFormatConverter.class)
    CacheFormat cacheFormat();

    /**
     * Set to TRUE if the persistence unit reference a multi-tenant data source
     */
    @WithDefault("FALSE")
    @WithName("multi-tenant")
    Boolean multiTenant();

    /**
     * The cache share mode
     */
    @WithDefault("ENABLE_SELECTIVE")
    @WithConverter(JPALiteConfigMapping.SharedCacheModeConverter.class)
    @WithName("shared-cache-mode")
    SharedCacheMode sharedCacheMode();

    /**
     * Additional properties associated with the persistence unit.
     * See {@link JPALiteEntityManager} for a list of valid properties.
     */
    Map<String, String> properties();

    /**
     * Set the transaction type value for the persistence unit.
     */
    @WithName("transaction-type")
    @WithDefault("RESOURCE_LOCAL")
    @WithConverter(JPALiteConfigMapping.PersistenceUnitTransactionTypeConverter.class)
    PersistenceUnitTransactionType transactionType();

    /**
     * The validation mode
     */
    @WithName("validation-mode")
    @WithDefault("NONE")
    @WithConverter(JPALiteConfigMapping.ValidationModeConverter.class)
    ValidationMode validationMode();
}//PersistenceUnitConfig
