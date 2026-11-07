package net.bozz.staffmod.gametest;

import net.bozz.staffmod.StaffMod;
import net.bozz.staffmod.entity.GravityBolt;
import net.bozz.staffmod.entity.ModEntities;
import net.bozz.staffmod.item.ModItems;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
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
