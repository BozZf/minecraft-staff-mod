package net.bozz.staffmod.entity;

import net.bozz.staffmod.StaffMod;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    private static final DeferredRegister<EntityType<?>> TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, StaffMod.MOD_ID);

    public static final RegistryObject<EntityType<GravityBolt>> GRAVITY_BOLT = TYPES.register("gravity_bolt",
            () -> EntityType.Builder.<GravityBolt>of(GravityBolt::new, MobCategory.MISC)
                    .sized(0.6F, 0.6F).clientTrackingRange(8).updateInterval(2).noSave()
                    .build(StaffMod.MOD_ID + ":gravity_bolt"));

    public static final RegistryObject<EntityType<GravityBlock>> GRAVITY_BLOCK = TYPES.register("gravity_block",
            () -> EntityType.Builder.<GravityBlock>of(GravityBlock::new, MobCategory.MISC)
                    .sized(0.98F, 0.98F).clientTrackingRange(10).updateInterval(2)
                    .build(StaffMod.MOD_ID + ":gravity_block"));

    public static void register(IEventBus eventBus) {
        TYPES.register(eventBus);
    }
}
