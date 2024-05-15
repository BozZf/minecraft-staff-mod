package net.minecraft.client.resources;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Iterables;
import com.google.common.hash.Hashing;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.InsecurePublicKeyException;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;
import com.mojang.authlib.properties.Property;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.Optionull;
import net.minecraft.Util;
import net.minecraft.client.renderer.texture.HttpTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SkinManager {
   private static final String PROPERTY_TEXTURES = "textures";
   private final LoadingCache<SkinManager.CacheKey, CompletableFuture<PlayerSkin>> skinCache;
   private final SkinManager.TextureCache skinTextures;
   private final SkinManager.TextureCache capeTextures;
   private final SkinManager.TextureCache elytraTextures;

   public SkinManager(TextureManager pTextureManager, Path pRoot, final MinecraftSessionService pSessionService, final Executor pExecutor) {
      this.skinTextures = new SkinManager.TextureCache(pTextureManager, pRoot, Type.SKIN);
      this.capeTextures = new SkinManager.TextureCache(pTextureManager, pRoot, Type.CAPE);
      this.elytraTextures = new SkinManager.TextureCache(pTextureManager, pRoot, Type.ELYTRA);
      this.skinCache = CacheBuilder.newBuilder().expireAfterAccess(Duration.ofSeconds(15L)).build(new CacheLoader<SkinManager.CacheKey, CompletableFuture<PlayerSkin>>() {
         public CompletableFuture<PlayerSkin> load(SkinManager.CacheKey p_298169_) {
            GameProfile gameprofile = p_298169_.profile();
            return CompletableFuture.supplyAsync(() -> {
               try {
                  try {
                     return SkinManager.TextureInfo.unpack(pSessionService.getTextures(gameprofile, true), true);
                  } catch (InsecurePublicKeyException insecurepublickeyexception) {
                     return SkinManager.TextureInfo.unpack(pSessionService.getTextures(gameprofile, false), false);
                  }
               } catch (Throwable throwable) {
                  return SkinManager.TextureInfo.EMPTY;
               }
            }, Util.backgroundExecutor()).thenComposeAsync((p_301039_) -> {
               return SkinManager.this.registerTextures(gameprofile, p_301039_);
            }, pExecutor);
         }
      });
   }

   public Supplier<PlayerSkin> lookupInsecure(GameProfile pProfile) {
      CompletableFuture<PlayerSkin> completablefuture = this.getOrLoad(pProfile);
      PlayerSkin playerskin = DefaultPlayerSkin.get(pProfile);
      return () -> {
         return completablefuture.getNow(playerskin);
      };
   }

   public PlayerSkin getInsecureSkin(GameProfile pProfile) {
      PlayerSkin playerskin = this.getOrLoad(pProfile).getNow((PlayerSkin)null);
      return playerskin != null ? playerskin : DefaultPlayerSkin.get(pProfile);
   }

   public CompletableFuture<PlayerSkin> getOrLoad(GameProfile pProfile) {
      return this.skinCache.getUnchecked(new SkinManager.CacheKey(pProfile));
   }

   CompletableFuture<PlayerSkin> registerTextures(GameProfile pProfile, SkinManager.TextureInfo pTextureInfo) {
      MinecraftProfileTexture minecraftprofiletexture = pTextureInfo.skin();
      CompletableFuture<ResourceLocation> completablefuture;
      PlayerSkin.Model playerskin$model;
      if (minecraftprofiletexture != null) {
         completablefuture = this.skinTextures.getOrLoad(minecraftprofiletexture);
         playerskin$model = PlayerSkin.Model.byName(minecraftprofiletexture.getMetadata("model"));
      } else {
         PlayerSkin playerskin = DefaultPlayerSkin.get(pProfile);
         completablefuture = CompletableFuture.completedFuture(playerskin.texture());
         playerskin$model = playerskin.model();
      }

      String s = Optionull.map(minecraftprofiletexture, MinecraftProfileTexture::getUrl);
      MinecraftProfileTexture minecraftprofiletexture1 = pTextureInfo.cape();
      CompletableFuture<ResourceLocation> completablefuture1 = minecraftprofiletexture1 != null ? this.capeTextures.getOrLoad(minecraftprofiletexture1) : CompletableFuture.completedFuture((ResourceLocation)null);
      MinecraftProfileTexture minecraftprofiletexture2 = pTextureInfo.elytra();
      CompletableFuture<ResourceLocation> completablefuture2 = minecraftprofiletexture2 != null ? this.elytraTextures.getOrLoad(minecraftprofiletexture2) : CompletableFuture.completedFuture((ResourceLocation)null);
      return CompletableFuture.allOf(completablefuture, completablefuture1, completablefuture2).thenApply((p_296316_) -> {
         return new PlayerSkin(completablefuture.join(), s, completablefuture1.join(), completablefuture2.join(), playerskin$model, pTextureInfo.secure());
      });
   }

   @Nullable
   static Property getTextureProperty(GameProfile pProfile) {
      return Iterables.getFirst(pProfile.getProperties().get("textures"), (Property)null);
   }

   @OnlyIn(Dist.CLIENT)
   static record CacheKey(GameProfile profile) {
      public boolean equals(Object pOther) {
         if (!(pOther instanceof SkinManager.CacheKey skinmanager$cachekey)) {
            return false;
         } else {
            return this.profile.getId().equals(skinmanager$cachekey.profile.getId()) && Objects.equals(this.texturesData(), skinmanager$cachekey.texturesData());
         }
      }

      public int hashCode() {
         return this.profile.getId().hashCode() + Objects.hashCode(this.texturesData()) * 31;
      }

      @Nullable
      private String texturesData() {
         Property property = SkinManager.getTextureProperty(this.profile);
         return property != null ? property.value() : null;
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class TextureCache {
      private final TextureManager textureManager;
      private final Path root;
      private final MinecraftProfileTexture.Type type;
      private final Map<String, CompletableFuture<ResourceLocation>> textures = new Object2ObjectOpenHashMap<>();

      TextureCache(TextureManager pTextureManager, Path pRoot, MinecraftProfileTexture.Type pType) {
         this.textureManager = pTextureManager;
         this.root = pRoot;
         this.type = pType;
      }

      public CompletableFuture<ResourceLocation> getOrLoad(MinecraftProfileTexture pTexture) {
         String s = pTexture.getHash();
         CompletableFuture<ResourceLocation> completablefuture = this.textures.get(s);
         if (completablefuture == null) {
            completablefuture = this.registerTexture(pTexture);
            this.textures.put(s, completablefuture);
         }

         return completablefuture;
      }

      private CompletableFuture<ResourceLocation> registerTexture(MinecraftProfileTexture pTexture) {
         String s = Hashing.sha1().hashUnencodedChars(pTexture.getHash()).toString();
         ResourceLocation resourcelocation = this.getTextureLocation(s);
         Path path = this.root.resolve(s.length() > 2 ? s.substring(0, 2) : "xx").resolve(s);
         CompletableFuture<ResourceLocation> completablefuture = new CompletableFuture<>();
         HttpTexture httptexture = new HttpTexture(path.toFile(), pTexture.getUrl(), DefaultPlayerSkin.getDefaultTexture(), this.type == Type.SKIN, () -> {
            completablefuture.complete(resourcelocation);
         });
         this.textureManager.register(resourcelocation, httptexture);
         return completablefuture;
      }

      private ResourceLocation getTextureLocation(String pName) {
         String s1;
         switch (this.type) {
            case SKIN:
               s1 = "skins";
               break;
            case CAPE:
               s1 = "capes";
               break;
            case ELYTRA:
               s1 = "elytra";
               break;
            default:
               throw new IncompatibleClassChangeError();
         }

         String s = s1;
         return new ResourceLocation(s + "/" + pName);
      }
   }

   @OnlyIn(Dist.CLIENT)
   static record TextureInfo(@Nullable MinecraftProfileTexture skin, @Nullable MinecraftProfileTexture cape, @Nullable MinecraftProfileTexture elytra, boolean secure) {
      public static final SkinManager.TextureInfo EMPTY = new SkinManager.TextureInfo((MinecraftProfileTexture)null, (MinecraftProfileTexture)null, (MinecraftProfileTexture)null, true);

      public static SkinManager.TextureInfo unpack(Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> pTextureGetter, boolean pSecure) {
         return pTextureGetter.isEmpty() ? EMPTY : new SkinManager.TextureInfo(pTextureGetter.get(Type.SKIN), pTextureGetter.get(Type.CAPE), pTextureGetter.get(Type.ELYTRA), pSecure);
      }
   }
}