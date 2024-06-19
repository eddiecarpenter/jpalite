package io.jpalite.test;

import io.jpalite.EntityMetaDataManager;
import io.jpalite.impl.EntityMetaDataImpl;
import io.jpalite.impl.fieldtypes.*;

public class TestEntityMetaDataManager
{
	static {
		// This is a hack to get the JPA to work
		EntityMetaDataManager.registerConverter(new BooleanFieldType());
		EntityMetaDataManager.registerConverter(new BoolFieldType());
		EntityMetaDataManager.registerConverter(new IntegerFieldType());
		EntityMetaDataManager.registerConverter(new IntFieldType());
		EntityMetaDataManager.registerConverter(new LongLongFieldType());
		EntityMetaDataManager.registerConverter(new LongFieldType());
		EntityMetaDataManager.registerConverter(new DoubleDoubleFieldType());
		EntityMetaDataManager.registerConverter(new DoubleFieldType());
		EntityMetaDataManager.registerConverter(new LocalDateTimeFieldType());
		EntityMetaDataManager.registerConverter(new TimestampFieldType());
		EntityMetaDataManager.registerConverter(new BytesFieldType());
		EntityMetaDataManager.registerConverter(new StringFieldType());
		EntityMetaDataManager.registerConverter(new BigDecimalFieldType());

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
