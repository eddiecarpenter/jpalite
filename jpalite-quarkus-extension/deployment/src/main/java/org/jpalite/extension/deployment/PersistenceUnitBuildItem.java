package org.jpalite.extension.deployment;

import io.quarkus.builder.item.MultiBuildItem;

public final class PersistenceUnitBuildItem extends MultiBuildItem
{

    private final String name;

    public PersistenceUnitBuildItem(String name)
    {
        this.name = name;
    }

    public String getPersistenceUnitName()
    {
        return name;
    }
}
