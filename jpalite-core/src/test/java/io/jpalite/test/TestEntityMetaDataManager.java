package io.jpalite.test;

import io.jpalite.EntityMetaDataManager;
import io.jpalite.impl.EntityMetaDataImpl;

public class TestEntityMetaDataManager
{
	static {
		EntityMetaDataManager.register(new EntityMetaDataImpl<>(RatePlan.class));
		EntityMetaDataManager.register(new EntityMetaDataImpl<>(FullName.class));
		EntityMetaDataManager.register(new EntityMetaDataImpl<>(Employee.class));
		EntityMetaDataManager.register(new EntityMetaDataImpl<>(Department.class));
		EntityMetaDataManager.register(new EntityMetaDataImpl<>(Company.class));
		EntityMetaDataManager.register(new EntityMetaDataImpl<>(Phone.class));

		EntityMetaDataManager.register(new EntityMetaDataImpl<>(Employee1.class));
		EntityMetaDataManager.register(new EntityMetaDataImpl<>(Department1.class));
	}

	public static void init()
	{
		//do nothing
	}
}
