package net.bozz.staffmod.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.DragonFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;

public class GravityBolt extends DragonFireball {
    private static final int MAX_AGE = 100;

    public GravityBolt(EntityType<? extends DragonFireball> type, Level level) {
        super(type, level);
    }

    public GravityBolt(ServerLevel level, Player owner) {
        this(ModEntities.GRAVITY_BOLT.get(), level);
        setOwner(owner);
        setPos(owner.getEyePosition());
        Vec3 look = owner.getLookAngle();
        xPower = look.x * 0.1;
        yPower = look.y * 0.1;
        zPower = look.z * 0.1;
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
        // DragonFireball.onHit creates the dragon's damaging breath cloud.
        if (level() instanceof ServerLevel level) {
            if (hit instanceof BlockHitResult blockHit && getOwner() instanceof Player player) {
                moveBlock(level, blockHit.getBlockPos(), player);
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
