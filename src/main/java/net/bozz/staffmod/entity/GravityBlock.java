package net.bozz.staffmod.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

public class GravityBlock extends FallingBlockEntity {
    private static final EntityDataAccessor<Boolean> RISING =
            SynchedEntityData.defineId(GravityBlock.class, EntityDataSerializers.BOOLEAN);

    public GravityBlock(EntityType<? extends FallingBlockEntity> type, Level level) {
        super(type, level);
    }

    static void start(ServerLevel level, BlockPos pos, BlockState state, boolean rising) {
        GravityBlock entity = new GravityBlock(ModEntities.GRAVITY_BLOCK.get(), level);
        entity.blockState = state.trySetValue(BlockStateProperties.WATERLOGGED, false);
        entity.entityData.set(RISING, rising);
        entity.blocksBuilding = true;
        entity.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        entity.setStartPos(pos);

        // Keep the original block if Forge cancels the entity spawn.
        if (!level.addFreshEntity(entity)) {
            return;
        }
        if (entity.isRemoved() || level.getBlockState(pos) != state
                || !level.setBlock(pos, state.getFluidState().createLegacyBlock(), Block.UPDATE_ALL)) {
            entity.discard();
        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(RISING, false);
    }

    @Override
    public void tick() {
        if (!entityData.get(RISING)) {
            super.tick();
            return;
        }

        ++time;
        // Accelerate and apply drag just like sand, with the acceleration reversed.
        Vec3 velocity = getDeltaMovement().add(0.0, 0.04, 0.0);
        setDeltaMovement(velocity);
        move(MoverType.SELF, velocity);
        setDeltaMovement(getDeltaMovement().scale(0.98));

        if (level() instanceof ServerLevel level) {
            if (verticalCollision && landAgainstCeiling(level)) {
                discard();
            } else if (verticalCollision || getBoundingBox().maxY >= level.getMaxBuildHeight() || time > 600) {
                if (dropItem && level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
                    spawnAtLocation(blockState.getBlock());
                }
                discard();
            }
        }
    }

    private boolean landAgainstCeiling(ServerLevel level) {
        BlockPos pos = blockPosition();
        BlockState destination = level.getBlockState(pos);
        BlockState carried = blockState.trySetValue(BlockStateProperties.WATERLOGGED,
                level.getFluidState(pos).getType() == Fluids.WATER);
        return !level.isOutsideBuildHeight(pos) && level.getWorldBorder().isWithinBounds(pos)
                && destination.canBeReplaced(new DirectionalPlaceContext(level, pos, Direction.UP,
                        ItemStack.EMPTY, Direction.DOWN))
                && carried.canSurvive(level, pos) && level.setBlock(pos, carried, Block.UPDATE_ALL);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Rising", entityData.get(RISING));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(RISING, tag.getBoolean("Rising"));
    }
}
