package com.starfish_studios.naturalist.core.registry.fabric;

import net.minecraft.world.entity.MobCategory;

public class NaturalistMobCategoriesImpl {
    static {
        MobCategory.values();
    }
    public static MobCategory FIREFLIES;
    public static MobCategory DRAGONFLIES;

    public static MobCategory getFireflyCategory() {
        return FIREFLIES;
    }
    public static MobCategory getDragonflyCategory() {
        return DRAGONFLIES;
    }
}
