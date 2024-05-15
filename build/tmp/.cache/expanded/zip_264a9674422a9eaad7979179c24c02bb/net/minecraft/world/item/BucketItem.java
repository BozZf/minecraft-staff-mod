package net.minecraft.world.item;

import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class BucketItem extends Item implements DispensibleContainerItem {
   private final Fluid content;

   // Forge: Use the other constructor that takes a Supplier
   @Deprecated
   public BucketItem(Fluid pContent, Item.Properties pProperties) {
      super(pProperties);
      this.content = pContent;
      this.fluidSupplier = net.minecraftforge.registries.ForgeRegistries.FLUIDS.getDelegateOrThrow(pContent);
   }

   /**
    * @param supplier A fluid supplier such as {@link net.minecraftforge.registries.RegistryObject<Fluid>}
    */
   public BucketItem(java.util.function.Supplier<? extends Fluid> supplier, Item.Properties builder) {
      super(builder);
      this.content = null;
      this.fluidSupplier = supplier;
   }

   /**
    * Called to trigger the item's "innate" right click behavior. To handle when this item is used on a Block, see
    * {@link #onItemUse}.
    */
   public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pHand) {
      ItemStack itemstack = pPlayer.getItemInHand(pHand);
      BlockHitResult blockhitresult = getPlayerPOVHitResult(pLevel, pPlayer, this.content == Fluids.EMPTY ? ClipContext.Fluid.SOURCE_ONLY : ClipContext.Fluid.NONE);
      InteractionResultHolder<ItemStack> ret = net.minecraftforge.event.ForgeEventFactory.onBucketUse(pPlayer, pLevel, itemstack, blockhitresult);
      if (ret != null) return ret;
      if (blockhitresult.getType() == HitResult.Type.MISS) {
         return InteractionResultHolder.pass(itemstack);
      } else if (blockhitresult.getType() != HitResult.Type.BLOCK) {
         return InteractionResultHolder.pass(itemstack);
      } else {
         BlockPos blockpos = blockhitresult.getBlockPos();
         Direction direction = blockhitresult.getDirection();
         BlockPos blockpos1 = blockpos.relative(direction);
         if (pLevel.mayInteract(pPlayer, blockpos) && pPlayer.mayUseItemAt(blockpos1, direction, itemstack)) {
            if (this.content == Fluids.EMPTY) {
               BlockState blockstate1 = pLevel.getBlockState(blockpos);
               Block $$10 = blockstate1.getBlock();
               if ($$10 instanceof BucketPickup) {
                  BucketPickup bucketpickup = (BucketPickup)$$10;
                  ItemStack itemstack2 = bucketpickup.pickupBlock(pPlayer, pLevel, blockpos, blockstate1);
                  if (!itemstack2.isEmpty()) {
                     pPlayer.awardStat(Stats.ITEM_USED.get(this));
                     bucketpickup.getPickupSound(blockstate1).ifPresent((p_150709_) -> {
                        pPlayer.playSound(p_150709_, 1.0F, 1.0F);
                     });
                     pLevel.gameEvent(pPlayer, GameEvent.FLUID_PICKUP, blockpos);
                     ItemStack itemstack1 = ItemUtils.createFilledResult(itemstack, pPlayer, itemstack2);
                     if (!pLevel.isClientSide) {
                        CriteriaTriggers.FILLED_BUCKET.trigger((ServerPlayer)pPlayer, itemstack2);
                     }

                     return InteractionResultHolder.sidedSuccess(itemstack1, pLevel.isClientSide());
                  }
               }

               return InteractionResultHolder.fail(itemstack);
            } else {
               BlockState blockstate = pLevel.getBlockState(blockpos);
               BlockPos blockpos2 = canBlockContainFluid(pLevel, blockpos, blockstate) ? blockpos : blockpos1;
               if (this.emptyContents(pPlayer, pLevel, blockpos2, blockhitresult, itemstack)) {
                  this.checkExtraContent(pPlayer, pLevel, itemstack, blockpos2);
                  if (pPlayer instanceof ServerPlayer) {
                     CriteriaTriggers.PLACED_BLOCK.trigger((ServerPlayer)pPlayer, blockpos2, itemstack);
                  }

                  pPlayer.awardStat(Stats.ITEM_USED.get(this));
                  return InteractionResultHolder.sidedSuccess(getEmptySuccessItem(itemstack, pPlayer), pLevel.isClientSide());
               } else {
                  return InteractionResultHolder.fail(itemstack);
               }
            }
         } else {
            return InteractionResultHolder.fail(itemstack);
         }
      }
   }

   public static ItemStack getEmptySuccessItem(ItemStack pBucketStack, Player pPlayer) {
      return !pPlayer.getAbilities().instabuild ? new ItemStack(Items.BUCKET) : pBucketStack;
   }

   public void checkExtraContent(@Nullable Player pPlayer, Level pLevel, ItemStack pContainerStack, BlockPos pPos) {
   }

   @Deprecated //Forge: use the ItemStack sensitive version
   public boolean emptyContents(@Nullable Player pPlayer, Level pLevel, BlockPos pPos, @Nullable BlockHitResult pResult) {
      return this.emptyContents(pPlayer, pLevel, pPos, pResult, null);
   }

   public boolean emptyContents(@Nullable Player pPlayer, Level pLevel, BlockPos pPos, @Nullable BlockHitResult pResult, @Nullable ItemStack container) {
      Fluid $$6 = this.content;
      if ($$6 instanceof FlowingFluid flowingfluid) {
         BlockState blockstate;
         Block $$7;
         boolean $$8;
         boolean flag2;
         label82: {
            blockstate = pLevel.getBlockState(pPos);
            $$7 = blockstate.getBlock();
            $$8 = blockstate.canBeReplaced(this.content);
            if (!blockstate.isAir() && !$$8) {
               label80: {
                  if ($$7 instanceof LiquidBlockContainer) {
                     LiquidBlockContainer liquidblockcontainer = (LiquidBlockContainer)$$7;
                     if (liquidblockcontainer.canPlaceLiquid(pPlayer, pLevel, pPos, blockstate, this.content)) {
                        break label80;
                     }
                  }

                  flag2 = false;
                  break label82;
               }
            }

            flag2 = true;
         }

         boolean flag1 = flag2;
         java.util.Optional<net.minecraftforge.fluids.FluidStack> containedFluidStack = java.util.Optional.ofNullable(container).flatMap(net.minecraftforge.fluids.FluidUtil::getFluidContained);
         if (!flag1) {
            return pResult != null && this.emptyContents(pPlayer, pLevel, pResult.getBlockPos().relative(pResult.getDirection()), (BlockHitResult)null, container);
         } else if (containedFluidStack.isPresent() && this.content.getFluidType().isVaporizedOnPlacement(pLevel, pPos, containedFluidStack.get())) {
            this.content.getFluidType().onVaporize(pPlayer, pLevel, pPos, containedFluidStack.get());
            return true;
         } else if (pLevel.dimensionType().ultraWarm() && this.content.is(FluidTags.WATER)) {
            int l = pPos.getX();
            int i = pPos.getY();
            int j = pPos.getZ();
            pLevel.playSound(pPlayer, pPos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F + (pLevel.random.nextFloat() - pLevel.random.nextFloat()) * 0.8F);

            for(int k = 0; k < 8; ++k) {
               pLevel.addParticle(ParticleTypes.LARGE_SMOKE, (double)l + Math.random(), (double)i + Math.random(), (double)j + Math.random(), 0.0D, 0.0D, 0.0D);
            }

            return true;
         } else {
            if ($$7 instanceof LiquidBlockContainer liquid && liquid.canPlaceLiquid(pPlayer, pLevel, pPos, blockstate, content)) {
               LiquidBlockContainer liquidblockcontainer1 = (LiquidBlockContainer)$$7;
               if (this.content == Fluids.WATER) {
                  liquidblockcontainer1.placeLiquid(pLevel, pPos, blockstate, flowingfluid.getSource(false));
                  this.playEmptySound(pPlayer, pLevel, pPos);
                  return true;
               }
            }

            if (!pLevel.isClientSide && $$8 && !blockstate.liquid()) {
               pLevel.destroyBlock(pPos, true);
            }

            if (!pLevel.setBlock(pPos, this.content.defaultFluidState().createLegacyBlock(), 11) && !blockstate.getFluidState().isSource()) {
               return false;
            } else {
               this.playEmptySound(pPlayer, pLevel, pPos);
               return true;
            }
         }
      }
      return false;
   }

   protected void playEmptySound(@Nullable Player pPlayer, LevelAccessor pLevel, BlockPos pPos) {
      SoundEvent soundevent = this.content.getFluidType().getSound(pPlayer, pLevel, pPos, net.minecraftforge.common.SoundActions.BUCKET_EMPTY);
      if(soundevent == null) soundevent = this.content.is(FluidTags.LAVA) ? SoundEvents.BUCKET_EMPTY_LAVA : SoundEvents.BUCKET_EMPTY;
      pLevel.playSound(pPlayer, pPos, soundevent, SoundSource.BLOCKS, 1.0F, 1.0F);
      pLevel.gameEvent(pPlayer, GameEvent.FLUID_PLACE, pPos);
   }

   @Override
   public net.minecraftforge.common.capabilities.ICapabilityProvider initCapabilities(ItemStack stack, @Nullable net.minecraft.nbt.CompoundTag nbt) {
      if (this.getClass() == BucketItem.class)
         return new net.minecraftforge.fluids.capability.wrappers.FluidBucketWrapper(stack);
      else
         return super.initCapabilities(stack, nbt);
   }

   private final java.util.function.Supplier<? extends Fluid> fluidSupplier;
   public Fluid getFluid() { return fluidSupplier.get(); }

   protected boolean canBlockContainFluid(Level worldIn, BlockPos posIn, BlockState blockstate) {
      return blockstate.getBlock() instanceof LiquidBlockContainer liquid && liquid.canPlaceLiquid(null, worldIn, posIn, blockstate, this.content);
   }
}
