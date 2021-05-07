package com.coxspecialloothider;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("example")
public interface CoxSpecialLootHiderConfig extends Config
{
    // only hide in solos ?
    @ConfigItem(position = 0, keyName = "soloOnly", name = "Cencor solo only", description = "Only cencors the loot in solo raids.")
    default boolean soloOnly() { return false; }
}
