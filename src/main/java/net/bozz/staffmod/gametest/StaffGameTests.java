package net.bozz.staffmod.gametest;

import net.bozz.staffmod.StaffMod;
import net.bozz.staffmod.entity.GravityBolt;
import net.bozz.staffmod.entity.ModEntities;
import net.bozz.staffmod.entity.MobGravity;
import net.bozz.staffmod.item.ModItems;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;

import java.util.List;

@GameTestHolder(value = StaffMod.MOD_ID, namespace = StaffMod.MOD_ID)
public class StaffGameTests {
    private static final String ARENA = "staffmod:gravity_arena";

    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void dropsBlockAbovePlayer(GameTestHelper helper) {
        BlockPos target = new BlockPos(5, 4, 3);
        BlockPos floor = new BlockPos(5, 0, 3);
        helper.setBlock(target, Blocks.STONE);
        helper.setBlock(floor, Blocks.OBSIDIAN);

        Player player = aimStaff(helper, 2, target);
        fire(helper, player);
        helper.assertFalse(ModItems.STAFF.get().use(helper.getLevel(), player, InteractionHand.MAIN_HAND)
                .getResult().consumesAction(), "The staff fired during its cooldown");

        helper.succeedWhen(() -> {
            helper.assertBlockPresent(Blocks.AIR, target);
            helper.assertBlockPresent(Blocks.STONE, floor.above());
            helper.assertEntityNotPresent(ModEntities.GRAVITY_BLOCK.get());
            helper.assertEntityNotPresent(EntityType.AREA_EFFECT_CLOUD);
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void raisesBlockBelowPlayerAndPreservesState(GameTestHelper helper) {
        BlockPos target = new BlockPos(5, 1, 3);
        BlockPos ceiling = new BlockPos(5, 5, 3);
        helper.setBlock(target, Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
        helper.setBlock(ceiling, Blocks.OBSIDIAN);
        Player player = aimStaff(helper, 0, target);
        fire(helper, player);
        // The target is above the player at launch and below them at impact.
        player.setPos(player.getX(), player.getY() + 3, player.getZ());

        helper.succeedWhen(() -> {
            helper.assertBlockPresent(Blocks.AIR, target);
            helper.assertBlockPresent(Blocks.OAK_LOG, ceiling.below());
            helper.assertBlockProperty(ceiling.below(), RotatedPillarBlock.AXIS, Direction.Axis.X);
            helper.assertEntityNotPresent(ModEntities.GRAVITY_BLOCK.get());
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void leavesBlockAtPlayerHeight(GameTestHelper helper) {
        BlockPos target = new BlockPos(5, 2, 3);
        helper.setBlock(target, Blocks.STONE);
        GravityBolt bolt = fire(helper, aimStaff(helper, 2, target));

        helper.succeedWhen(() -> {
            helper.assertTrue(bolt.isRemoved(), "Waiting for the bolt to hit");
            helper.assertBlockPresent(Blocks.STONE, target);
            helper.assertEntityNotPresent(ModEntities.GRAVITY_BLOCK.get());
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void leavesBlockEntityInPlace(GameTestHelper helper) {
        BlockPos target = new BlockPos(5, 4, 3);
        helper.setBlock(target, Blocks.CHEST);
        GravityBolt bolt = fire(helper, aimStaff(helper, 2, target));

        helper.succeedWhen(() -> {
            helper.assertTrue(bolt.isRemoved(), "Waiting for the bolt to hit");
            helper.assertBlockPresent(Blocks.CHEST, target);
            helper.assertEntityNotPresent(ModEntities.GRAVITY_BLOCK.get());
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void leavesBlockWhenItsPathIsObstructed(GameTestHelper helper) {
        BlockPos target = new BlockPos(5, 4, 3);
        helper.setBlock(target, Blocks.STONE);
        helper.setBlock(target.below(), Blocks.OBSIDIAN);
        GravityBolt bolt = fire(helper, aimStaff(helper, 2, target));

        helper.succeedWhen(() -> {
            helper.assertTrue(bolt.isRemoved(), "Waiting for the bolt to hit");
            helper.assertBlockPresent(Blocks.STONE, target);
            helper.assertBlockPresent(Blocks.OBSIDIAN, target.below());
            helper.assertEntityNotPresent(ModEntities.GRAVITY_BLOCK.get());
        });
    }

    private static Player aimStaff(GameTestHelper helper, int playerY, BlockPos target) {
        Player player = helper.makeMockSurvivalPlayer();
        BlockPos start = helper.absolutePos(new BlockPos(1, playerY, 3));
        player.moveTo(start.getX() + 0.5, start.getY(), start.getZ() + 0.5);
        player.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(helper.absolutePos(target)));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.STAFF.get()));
        return player;
    }

    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void raisesPassiveMobBelowPlayer(GameTestHelper helper) {
        assertMobMoves(helper, EntityType.COW, 1, 3, true);
    }

    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void dropsHostileMobAbovePlayer(GameTestHelper helper) {
        assertMobMoves(helper, EntityType.ZOMBIE, 4, 2, false);
    }

    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void raisesHostileMobBelowPlayer(GameTestHelper helper) {
        assertMobMoves(helper, EntityType.ZOMBIE, 1, 3, true);
    }

    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void dropsPassiveMobAbovePlayer(GameTestHelper helper) {
        assertMobMoves(helper, EntityType.COW, 4, 2, false);
    }

    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void leavesMobAtPlayerHeight(GameTestHelper helper) {
        BlockPos target = new BlockPos(5, 2, 3);
        Mob mob = stationaryMob(helper, EntityType.COW, target);
        double startY = mob.getY();
        GravityBolt bolt = fire(helper, aimStaff(helper, 2, target));
        helper.succeedWhen(() -> {
            helper.assertTrue(bolt.isRemoved(), "Waiting for the bolt to hit");
            helper.assertTrue(Math.abs(mob.getY() - startY) < 0.01, "A mob at equal height moved");
            helper.assertFalse(mob.hasEffect(MobEffects.LEVITATION), "Vanilla levitation was applied");
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void leavesBossesImmune(GameTestHelper helper) {
        BlockPos target = new BlockPos(5, 3, 3);
        Mob wither = stationaryMob(helper, EntityType.WITHER, target);
        wither.setNoAi(true);
        Player player = aimStaff(helper, 1, target);
        Mob dragon = EntityType.ENDER_DRAGON.create(helper.getLevel());
        helper.assertTrue(dragon != null, "Could not create the dragon");
        dragon.setPos(wither.position());
        MobGravity.apply(dragon, player);
        helper.assertTrue(dragon.getDeltaMovement().equals(Vec3.ZERO), "The dragon was affected");
        double startY = wither.getY();
        float health = wither.getHealth();
        GravityBolt bolt = fire(helper, player);
        helper.succeedWhen(() -> {
            helper.assertTrue(bolt.isRemoved(), "Waiting for the bolt to hit");
            helper.assertTrue(Math.abs(wither.getY() - startY) < 0.01, "The Wither moved");
            helper.assertTrue(wither.getHealth() == health, "The Wither took projectile damage");
            helper.assertFalse(wither.hasEffect(MobEffects.LEVITATION), "The Wither received levitation");
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void mobGravityExpires(GameTestHelper helper) {
        BlockPos target = new BlockPos(5, 1, 3);
        Mob mob = stationaryMob(helper, EntityType.COW, target);
        MobGravity.apply(mob, aimStaff(helper, 3, target));
        double startY = mob.getY();
        helper.runAfterDelay(3, () ->
                helper.assertTrue(mob.getY() > startY, "Gravity never moved the mob"));
        helper.runAfterDelay(42, () -> {
            mob.setDeltaMovement(Vec3.ZERO);
            double height = mob.getY();
            helper.runAfterDelay(2, () -> {
                helper.assertTrue(Math.abs(mob.getY() - height) < 0.01, "Gravity kept applying after two seconds");
                helper.assertTrue(mob.isNoGravity(), "The mob's original gravity setting changed");
                helper.succeed();
            });
        });
    }

    @GameTest(template = ARENA)
    public static void projectileStartsAtIncreasedSpeed(GameTestHelper helper) {
        GravityBolt bolt = fire(helper, aimStaff(helper, 2, new BlockPos(5, 2, 3)));
        helper.assertTrue(Math.abs(bolt.getDeltaMovement().length() - 2.4) < 0.001, "Unexpected launch speed");
        helper.assertTrue(bolt.isNoGravity(), "The projectile should fly straight");
        bolt.discard();
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void risingMobStopsAtCeiling(GameTestHelper helper) {
        BlockPos target = new BlockPos(5, 1, 3);
        helper.setBlock(new BlockPos(5, 3, 3), Blocks.OBSIDIAN);
        Mob mob = stationaryMob(helper, EntityType.COW, target);
        double startY = mob.getY();
        MobGravity.apply(mob, aimStaff(helper, 4, target));
        helper.runAfterDelay(5, () -> {
            double ceilingY = mob.getY();
            helper.assertTrue(ceilingY > startY && ceilingY < startY + 1, "The mob did not stop below the ceiling");
            helper.runAfterDelay(2, () -> {
                helper.assertTrue(Math.abs(mob.getY() - ceilingY) < 0.01, "The mob kept rising into the ceiling");
                helper.succeed();
            });
        });
    }

    @GameTest(template = ARENA)
    public static void leavesPlayersImmune(GameTestHelper helper) {
        Player target = helper.makeMockSurvivalPlayer();
        target.setPos(helper.absoluteVec(new Vec3(5, 4, 3)));
        Player caster = aimStaff(helper, 1, new BlockPos(5, 4, 3));
        MobGravity.apply(target, caster);
        helper.assertTrue(target.getDeltaMovement().equals(Vec3.ZERO), "A player was affected");
        helper.succeed();
    }

    private static void assertMobMoves(GameTestHelper helper, EntityType<? extends Mob> type,
                                       int mobY, int playerY, boolean rising) {
        BlockPos target = new BlockPos(5, mobY, 3);
        Mob mob = stationaryMob(helper, type, target);
        double startY = mob.getY();
        float health = mob.getHealth();
        GravityBolt bolt = fire(helper, aimStaff(helper, playerY, target));
        helper.succeedWhen(() -> {
            helper.assertTrue(bolt.isRemoved(), "Waiting for the bolt to hit");
            helper.assertTrue(rising ? mob.getY() > startY : mob.getY() < startY, "The mob moved in the wrong direction");
            helper.assertTrue(mob.getHealth() == health, "The projectile damaged the mob");
            helper.assertFalse(mob.hasEffect(MobEffects.LEVITATION), "Vanilla levitation was applied");
        });
    }

    private static Mob stationaryMob(GameTestHelper helper, EntityType<? extends Mob> type, BlockPos target) {
        Mob mob = helper.spawnWithNoFreeWill(type, target);
        mob.setNoGravity(true);
        return mob;
    }

    private static GravityBolt fire(GameTestHelper helper, Player player) {
        ItemStack staff = player.getItemInHand(InteractionHand.MAIN_HAND);
        helper.assertTrue(staff.use(helper.getLevel(), player, InteractionHand.MAIN_HAND).getResult().consumesAction(),
                "The staff did not fire");
        List<GravityBolt> bolts = helper.getLevel().getEntities(ModEntities.GRAVITY_BOLT.get(),
                new AABB(player.getEyePosition(), player.getEyePosition()).inflate(1), bolt -> true);
        helper.assertTrue(bolts.size() == 1, "Expected exactly one bolt near the player's eyes");
        return bolts.get(0);
    }
}
