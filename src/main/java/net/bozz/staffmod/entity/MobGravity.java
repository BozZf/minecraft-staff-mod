package net.bozz.staffmod.entity;

import net.bozz.staffmod.StaffMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = StaffMod.MOD_ID)
public class MobGravity {
    private static final String DATA_KEY = StaffMod.MOD_ID + ":gravity";
    private static final int DURATION_TICKS = 40;
    private static final double SPEED = 0.6;
    private static final TagKey<EntityType<?>> IMMUNE = TagKey.create(Registries.ENTITY_TYPE,
            new ResourceLocation(StaffMod.MOD_ID, "gravity_immune"));

    public static void apply(Entity target, Player player) {
        if (!(target instanceof Mob mob) || !mob.isAlive() || mob.getType().is(IMMUNE)) {
            return;
        }
        int heightDifference = mob.blockPosition().getY() - player.blockPosition().getY();
        if (heightDifference == 0) {
            return;
        }

        CompoundTag effect = new CompoundTag();
        effect.putInt("Ticks", DURATION_TICKS);
        effect.putInt("Direction", heightDifference > 0 ? -1 : 1);
        mob.getPersistentData().put(DATA_KEY, effect);
        move(mob, effect.getInt("Direction"));
    }

    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || mob.level().isClientSide
                || !mob.getPersistentData().contains(DATA_KEY)) {
            return;
        }

        CompoundTag effect = mob.getPersistentData().getCompound(DATA_KEY);
        int ticks = effect.getInt("Ticks");
        int direction = effect.getInt("Direction");
        boolean stopped = direction < 0 ? mob.onGround() : mob.verticalCollision && !mob.onGround();
        if (ticks <= 0 || stopped || !mob.isAlive() || mob.getType().is(IMMUNE)) {
            mob.getPersistentData().remove(DATA_KEY);
            return;
        }

        effect.putInt("Ticks", ticks - 1);
        move(mob, direction);
    }

    private static void move(Mob mob, int direction) {
        mob.setDeltaMovement(mob.getDeltaMovement().x, direction * SPEED, mob.getDeltaMovement().z);
        mob.hasImpulse = true;
    }
}
