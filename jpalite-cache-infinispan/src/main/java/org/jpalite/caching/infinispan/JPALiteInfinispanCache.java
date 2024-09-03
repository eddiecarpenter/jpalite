package org.jpalite.caching.infinispan;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.infinispan.client.runtime.InfinispanClientProducer;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.extern.slf4j.Slf4j;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.commons.configuration.StringConfiguration;
import org.jpalite.CachingException;
import org.jpalite.JPACache;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

@RegisterForReflection
@Slf4j
public class JPALiteInfinispanCache implements JPACache
{
    private static final String REGION_TIMESTAMP_NAME = "org.jpalite.region.$timestamps$";

    private final String cacheClientName;
    private final String configuration;
    private final String regionPrefix;

    private RemoteCacheManager remoteCacheManager;

    public JPALiteInfinispanCache(String cacheClientName, String configuration, String regionPrefix)
    {
        this.regionPrefix    = (regionPrefix == null || "<default>".equals(regionPrefix)) ? "" : regionPrefix + " - ";
        this.cacheClientName = cacheClientName;
        this.configuration   = configuration;
        LOG.info("Loading '{}' as JPALite caching provider for Infinispan client ['{}'] with region prefix ['{}']",
                 JPALiteInfinispanCache.class.getCanonicalName(),
                 cacheClientName,
                 regionPrefix);
    }

    private <T> RemoteCache<String, T> getCache(String cacheRegion)
    {
        if (remoteCacheManager == null) {
            InstanceHandle<InfinispanClientProducer> infinispanClientProducer = Arc.container().instance(InfinispanClientProducer.class);
            if (infinispanClientProducer.isAvailable()) {
                remoteCacheManager = infinispanClientProducer.get().getNamedRemoteCacheManager(cacheClientName);
            }//if
            if (remoteCacheManager == null || !remoteCacheManager.isStarted()) {
                remoteCacheManager = null;
                throw new CachingException("Error loading cache provider");
            }//if
        }//if


        RemoteCache<String, T> cache = remoteCacheManager.getCache(regionPrefix + cacheRegion);
        if (cache == null) {
            cache = remoteCacheManager.administration().getOrCreateCache(regionPrefix + cacheRegion, new StringConfiguration(configuration));
        }//if

        return cache;
    }

    @Override
    public <T> T find(String cacheRegion, String key)
    {
        RemoteCache<String, T> cache = getCache(cacheRegion);
        return cache.get(key);
    }

    @Override
    public boolean containsKey(String cacheRegion, String key)
    {
        RemoteCache<String, Object> cache = getCache(cacheRegion);
        return cache.containsKey(key);
    }

    @Override
    public <T> void add(String cacheRegion, String key, T value, long expireTime, TimeUnit expireTimeUnit)
    {
        getCache(cacheRegion).put(key, value, -1, TimeUnit.SECONDS, expireTime, expireTimeUnit);
        getCache(REGION_TIMESTAMP_NAME).put(cacheRegion, DateTimeFormatter.ISO_INSTANT.format(Instant.now().truncatedTo(ChronoUnit.MILLIS)));
    }

    @Override
    public <T> void replace(String cacheRegion, String key, T value, long expireTime, TimeUnit expireTimeUnit)
    {
        getCache(cacheRegion).replace(key, value, -1, TimeUnit.SECONDS, expireTime, expireTimeUnit);
        getCache(REGION_TIMESTAMP_NAME).put(cacheRegion, DateTimeFormatter.ISO_INSTANT.format(Instant.now().truncatedTo(ChronoUnit.MILLIS)));
    }

    @Override
    public void evict(String cacheRegion, String key)
    {
        getCache(cacheRegion).remove(key);
        getCache(REGION_TIMESTAMP_NAME).put(cacheRegion, DateTimeFormatter.ISO_INSTANT.format(Instant.now().truncatedTo(ChronoUnit.MILLIS)));
    }

    @Override
    public void evictAll(String cacheRegion)
    {
        getCache(cacheRegion).clear();
        getCache(REGION_TIMESTAMP_NAME).put(cacheRegion, DateTimeFormatter.ISO_INSTANT.format(Instant.now().truncatedTo(ChronoUnit.MILLIS)));
    }

    @Override
    public void evictAllRegions()
    {
        RemoteCache<String, String> cache = getCache(REGION_TIMESTAMP_NAME);
        cache.keySet().forEach(region -> getCache(region).clear());
        cache.clear();
    }

    @Override
    public Instant getLastModified(String cacheRegion)
    {
        RemoteCache<String, String> cache = getCache(REGION_TIMESTAMP_NAME);
        String lastModified = cache.get(cacheRegion);
        if (lastModified == null) {
            Instant time = Instant.now();
            cache.put(cacheRegion, DateTimeFormatter.ISO_INSTANT.format(time.truncatedTo(ChronoUnit.MILLIS)));
            return time;
        }

        return Instant.parse(lastModified);
    }
}
