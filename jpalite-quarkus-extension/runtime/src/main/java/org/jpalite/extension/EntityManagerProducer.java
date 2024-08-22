package org.jpalite.extension;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionSynchronizationRegistry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class EntityManagerProducer
{
    @Inject
    TransactionManager transactionManager;

    @Inject
    TransactionSynchronizationRegistry transactionSynchronizationRegistry;


    private final Map<String, EntityManagerFactory> entityManagerFactoryList = new ConcurrentHashMap<>();

    public EntityManagerFactory getEntityManagerFactory(String persistenceUnitName)
    {
        return entityManagerFactoryList.computeIfAbsent(persistenceUnitName, Persistence::createEntityManagerFactory);
    }

    public EntityManager getEntityManager(String persistenceUnit)
    {
        return new TransactionScopedEntityManagerImpl(getEntityManagerFactory(persistenceUnit),
                                                      transactionManager,
                                                      transactionSynchronizationRegistry);
    }//getEntityManager

}
