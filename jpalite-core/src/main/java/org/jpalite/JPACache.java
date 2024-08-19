package org.jpalite;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

public interface JPACache
{
	<T> T find(String cacheRegion, String key);

	boolean containsKey(String cacheRegion, String key);

	<T> void add(String cacheRegion, String key, T value, long expireTime, TimeUnit expireTimeUnit);

	<T> void replace(String cacheRegion, String key, T value, long expireTime, TimeUnit expireTimeUnit);

	void evict(String cacheRegion, String key);

	void evictAll(String cacheRegion);

	void evictAllRegions();

	Instant getLastModified(String cacheRegion);
}
