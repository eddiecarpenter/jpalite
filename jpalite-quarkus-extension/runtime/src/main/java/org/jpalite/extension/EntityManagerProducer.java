package org.jpalite.extension;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionSynchronizationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
@Slf4j
public class EntityManagerProducer
{
    @Inject
    TransactionManager transactionManager;

    @Inject
    TransactionSynchronizationRegistry transactionSynchronizationRegistry;


    private final Map<String, EntityManagerFactory> entityManagerFactoryList = new ConcurrentHashMap<>();

    public EntityManagerFactory getEntityManagerFactory(String persistenceUnitName)
    {
        if (persistenceUnitName.startsWith("${")) {
            String vKey = persistenceUnitName.substring(2, persistenceUnitName.lastIndexOf('}'));
            Config vConfigProvider = ConfigProvider.getConfig();
            Optional<String> vValue = vConfigProvider.getOptionalValue(vKey, String.class);
            if (vValue.isPresent()) {
                LOG.debug("Persistence unit name is defined using a variable {} = {} ", persistenceUnitName, vValue.get());
                persistenceUnitName = vValue.get();
            }//if
            else {
                LOG.debug("Persistence unit name defined using a variable {} but variable is not defined", persistenceUnitName);
            }//else
        }//if

        return entityManagerFactoryList.computeIfAbsent(persistenceUnitName, Persistence::createEntityManagerFactory);
    }

    public EntityManager getEntityManager(String persistenceUnit)
    {
        return new TransactionScopedEntityManagerImpl(getEntityManagerFactory(persistenceUnit),
                                                      transactionManager,
                                                      transactionSynchronizationRegistry);
    }//getEntityManager

}
