package org.jpalite.repository;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import java.util.Optional;

@Slf4j
public class JpaRepositoryUtil
{
	private JpaRepositoryUtil()
	{
		//Prevent the class from being instantiated
	}

	public static String getPersistenceUnitName(String persistenceUnit)
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
					LOG.warn("Persistence unit name defined using a variable {} but variable is not defined", persistenceUnitName);
				}//else
			}//if
			else {
				persistenceUnitName = persistenceUnit;
			}
		}//if
		return persistenceUnitName;
	}
}
