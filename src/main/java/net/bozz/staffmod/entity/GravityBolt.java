package net.bozz.staffmod.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;

public class GravityBolt extends ShulkerBullet {
    private static final int MAX_AGE = 100;
    private static final double SPEED = 2.4;

    public GravityBolt(EntityType<? extends ShulkerBullet> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public GravityBolt(ServerLevel level, Player owner) {
        this(ModEntities.GRAVITY_BOLT.get(), level);
        setOwner(owner);
        setPos(owner.getEyePosition());
        Vec3 look = owner.getLookAngle();
        setDeltaMovement(look.scale(SPEED));
    }

    @Override
    public void checkDespawn() {
        // A player's staff also works in peaceful difficulty.
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount >= MAX_AGE) {
            discard();
        }
    }

    @Override
    protected void onHit(HitResult hit) {
        // Apply the staff's gravity effect instead of vanilla damage and levitation.
        if (level() instanceof ServerLevel level) {
            if (getOwner() instanceof Player player) {
                if (hit instanceof BlockHitResult blockHit) {
                    moveBlock(level, blockHit.getBlockPos(), player);
                } else if (hit instanceof EntityHitResult entityHit) {
                    MobGravity.apply(entityHit.getEntity(), player);
                }
            }
            discard();
        }
    }

    private static void moveBlock(ServerLevel level, BlockPos pos, Player player) {
        int playerY = player.blockPosition().getY();
        if (pos.getY() == playerY || !player.getAbilities().mayBuild || !player.mayInteract(level, pos)
                || !level.getWorldBorder().isWithinBounds(pos)) {
            return;
        }

        Direction direction = pos.getY() > playerY ? Direction.DOWN : Direction.UP;
        BlockState state = level.getBlockState(pos);
        BlockPos next = pos.relative(direction);
        if (state.hasBlockEntity() || state.getDestroySpeed(level, pos) < 0
                || state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                || state.hasProperty(BlockStateProperties.BED_PART)
                || state.getCollisionShape(level, pos).isEmpty()
                || level.isOutsideBuildHeight(next) || !FallingBlock.isFree(level.getBlockState(next))) {
            return;
        }

        if (MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, state, player))
                || level.getBlockState(pos) != state) {
            return;
        }

        GravityBlock.start(level, pos, state, direction == Direction.UP);
    }
}
