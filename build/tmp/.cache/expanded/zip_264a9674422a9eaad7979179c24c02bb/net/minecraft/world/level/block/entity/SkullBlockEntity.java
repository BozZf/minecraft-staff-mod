package net.minecraft.world.level.block.entity;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.ProfileResult;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Services;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.util.StringUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.state.BlockState;

public class SkullBlockEntity extends BlockEntity {
   public static final String TAG_SKULL_OWNER = "SkullOwner";
   public static final String TAG_NOTE_BLOCK_SOUND = "note_block_sound";
   @Nullable
   private static GameProfileCache profileCache;
   @Nullable
   private static MinecraftSessionService sessionService;
   @Nullable
   private static Executor mainThreadExecutor;
   private static final Executor CHECKED_MAIN_THREAD_EXECUTOR = (p_296964_) -> {
      Executor executor = mainThreadExecutor;
      if (executor != null) {
         executor.execute(p_296964_);
      }

   };
   @Nullable
   private GameProfile owner;
   @Nullable
   private ResourceLocation noteBlockSound;
   private int animationTickCount;
   private boolean isAnimating;

   public SkullBlockEntity(BlockPos pPos, BlockState pBlockState) {
      super(BlockEntityType.SKULL, pPos, pBlockState);
   }

   public static void setup(Services pServices, Executor pMainThreadExecutor) {
      profileCache = pServices.profileCache();
      sessionService = pServices.sessionService();
      mainThreadExecutor = pMainThreadExecutor;
   }

   public static void clear() {
      profileCache = null;
      sessionService = null;
      mainThreadExecutor = null;
   }

   protected void saveAdditional(CompoundTag pTag) {
      super.saveAdditional(pTag);
      if (this.owner != null) {
         CompoundTag compoundtag = new CompoundTag();
         NbtUtils.writeGameProfile(compoundtag, this.owner);
         pTag.put("SkullOwner", compoundtag);
      }

      if (this.noteBlockSound != null) {
         pTag.putString("note_block_sound", this.noteBlockSound.toString());
      }

   }

   public void load(CompoundTag pTag) {
      super.load(pTag);
      if (pTag.contains("SkullOwner", 10)) {
         this.setOwner(NbtUtils.readGameProfile(pTag.getCompound("SkullOwner")));
      } else if (pTag.contains("ExtraType", 8)) {
         String s = pTag.getString("ExtraType");
         if (!StringUtil.isNullOrEmpty(s)) {
            this.setOwner(new GameProfile(Util.NIL_UUID, s));
         }
      }

      if (pTag.contains("note_block_sound", 8)) {
         this.noteBlockSound = ResourceLocation.tryParse(pTag.getString("note_block_sound"));
      }

   }

   public static void animation(Level pLevel, BlockPos pPos, BlockState pState, SkullBlockEntity pBlockEntity) {
      if (pState.hasProperty(SkullBlock.POWERED) && pState.getValue(SkullBlock.POWERED)) {
         pBlockEntity.isAnimating = true;
         ++pBlockEntity.animationTickCount;
      } else {
         pBlockEntity.isAnimating = false;
      }

   }

   public float getAnimation(float pPartialTick) {
      return this.isAnimating ? (float)this.animationTickCount + pPartialTick : (float)this.animationTickCount;
   }

   @Nullable
   public GameProfile getOwnerProfile() {
      return this.owner;
   }

   @Nullable
   public ResourceLocation getNoteBlockSound() {
      return this.noteBlockSound;
   }

   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   /**
    * Get an NBT compound to sync to the client with SPacketChunkData, used for initial loading of the chunk or when
    * many blocks change at once. This compound comes back to you clientside in {@link handleUpdateTag}
    */
   public CompoundTag getUpdateTag() {
      return this.saveWithoutMetadata();
   }

   public void setOwner(@Nullable GameProfile pOwner) {
      synchronized(this) {
         this.owner = pOwner;
      }

      this.updateOwnerProfile();
   }

   private void updateOwnerProfile() {
      if (this.owner != null && !Util.isBlank(this.owner.getName()) && !hasTextures(this.owner)) {
         fetchGameProfile(this.owner.getName()).thenAcceptAsync((p_296965_) -> {
            this.owner = p_296965_.orElse(this.owner);
            this.setChanged();
         }, CHECKED_MAIN_THREAD_EXECUTOR);
      } else {
         this.setChanged();
      }
   }

   @Nullable
   public static GameProfile getOrResolveGameProfile(CompoundTag pTag) {
      if (pTag.contains("SkullOwner", 10)) {
         return NbtUtils.readGameProfile(pTag.getCompound("SkullOwner"));
      } else {
         if (pTag.contains("SkullOwner", 8)) {
            String s = pTag.getString("SkullOwner");
            if (!Util.isBlank(s)) {
               pTag.remove("SkullOwner");
               resolveGameProfile(pTag, s);
            }
         }

         return null;
      }
   }

   public static void resolveGameProfile(CompoundTag pTag) {
      String s = pTag.getString("SkullOwner");
      if (!Util.isBlank(s)) {
         resolveGameProfile(pTag, s);
      }

   }

   private static void resolveGameProfile(CompoundTag pTag, String pProfileName) {
      fetchGameProfile(pProfileName).thenAccept((p_296969_) -> {
         pTag.put("SkullOwner", NbtUtils.writeGameProfile(new CompoundTag(), p_296969_.orElse(new GameProfile(Util.NIL_UUID, pProfileName))));
      });
   }

   private static CompletableFuture<Optional<GameProfile>> fetchGameProfile(String pProfileName) {
      GameProfileCache gameprofilecache = profileCache;
      return gameprofilecache == null ? CompletableFuture.completedFuture(Optional.empty()) : gameprofilecache.getAsync(pProfileName).thenCompose((p_296970_) -> {
         return p_296970_.isPresent() ? fillProfileTextures(p_296970_.get()) : CompletableFuture.completedFuture(Optional.empty());
      }).thenApplyAsync((p_296966_) -> {
         GameProfileCache gameprofilecache1 = profileCache;
         if (gameprofilecache1 != null) {
            p_296966_.ifPresent(gameprofilecache1::add);
            return p_296966_;
         } else {
            return Optional.empty();
         }
      }, CHECKED_MAIN_THREAD_EXECUTOR);
   }

   private static CompletableFuture<Optional<GameProfile>> fillProfileTextures(GameProfile pProfile) {
      return hasTextures(pProfile) ? CompletableFuture.completedFuture(Optional.of(pProfile)) : CompletableFuture.supplyAsync(() -> {
         MinecraftSessionService minecraftsessionservice = sessionService;
         if (minecraftsessionservice != null) {
            ProfileResult profileresult = minecraftsessionservice.fetchProfile(pProfile.getId(), true);
            return profileresult == null ? Optional.of(pProfile) : Optional.of(profileresult.profile());
         } else {
            return Optional.empty();
         }
      }, Util.backgroundExecutor());
   }

   private static boolean hasTextures(GameProfile pProfile) {
      return pProfile.getProperties().containsKey("textures");
   }
}