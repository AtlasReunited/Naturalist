package com.starfish_studios.naturalist.core.registry;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.world.entity.MobCategory;

public class NaturalistMobCategories {
    @ExpectPlatform
    public static MobCategory getFireflyCategory() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static MobCategory getDragonflyCategory() {
        throw new AssertionError();
    }
}
