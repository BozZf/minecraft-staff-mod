package net.minecraft.client.renderer.chunk;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.google.common.primitives.Doubles;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.ReferenceArraySet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.thread.ProcessorMailbox;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class SectionRenderDispatcher {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int MAX_WORKERS_32_BIT = 4;
   private static final int MAX_HIGH_PRIORITY_QUOTA = 2;
   private final PriorityBlockingQueue<SectionRenderDispatcher.RenderSection.CompileTask> toBatchHighPriority = Queues.newPriorityBlockingQueue();
   private final Queue<SectionRenderDispatcher.RenderSection.CompileTask> toBatchLowPriority = Queues.newLinkedBlockingDeque();
   private int highPriorityQuota = 2;
   private final Queue<SectionBufferBuilderPack> freeBuffers;
   private final Queue<Runnable> toUpload = Queues.newConcurrentLinkedQueue();
   private volatile int toBatchCount;
   private volatile int freeBufferCount;
   final SectionBufferBuilderPack fixedBuffers;
   private final ProcessorMailbox<Runnable> mailbox;
   private final Executor executor;
   ClientLevel level;
   final LevelRenderer renderer;
   private Vec3 camera = Vec3.ZERO;

   public SectionRenderDispatcher(ClientLevel pLevel, LevelRenderer pRenderer, Executor pExecutor, boolean pIs64Bit, SectionBufferBuilderPack pFixedBuffers) {
      this(pLevel, pRenderer, pExecutor, pIs64Bit, pFixedBuffers, -1);
   }

   public SectionRenderDispatcher(ClientLevel pLevel, LevelRenderer pRenderer, Executor pExecutor, boolean pIs64Bit, SectionBufferBuilderPack pFixedBuffers, int countRenderBuilders) {
      this.level = pLevel;
      this.renderer = pRenderer;
      int i = Math.max(1, (int)((double)Runtime.getRuntime().maxMemory() * 0.3D) / (RenderType.chunkBufferLayers().stream().mapToInt(RenderType::bufferSize).sum() * 4) - 1);
      int j = Runtime.getRuntime().availableProcessors();
      int k = pIs64Bit ? j : Math.min(j, 4);
      int l = countRenderBuilders < 0 ? Math.max(1, Math.min(k, i)) : countRenderBuilders;
      this.fixedBuffers = pFixedBuffers;
      List<SectionBufferBuilderPack> list = Lists.newArrayListWithExpectedSize(l);

      try {
         for(int i1 = 0; i1 < l; ++i1) {
            list.add(new SectionBufferBuilderPack());
         }
      } catch (OutOfMemoryError outofmemoryerror) {
         LOGGER.warn("Allocated only {}/{} buffers", list.size(), l);
         int j1 = Math.min(list.size() * 2 / 3, list.size() - 1);

         for(int k1 = 0; k1 < j1; ++k1) {
            list.remove(list.size() - 1);
         }

         System.gc();
      }

      this.freeBuffers = Queues.newArrayDeque(list);
      this.freeBufferCount = this.freeBuffers.size();
      this.executor = pExecutor;
      this.mailbox = ProcessorMailbox.create(pExecutor, "Section Renderer");
      this.mailbox.tell(this::runTask);
   }

   public void setLevel(ClientLevel pLevel) {
      this.level = pLevel;
   }

   private void runTask() {
      if (!this.freeBuffers.isEmpty()) {
         SectionRenderDispatcher.RenderSection.CompileTask sectionrenderdispatcher$rendersection$compiletask = this.pollTask();
         if (sectionrenderdispatcher$rendersection$compiletask != null) {
            SectionBufferBuilderPack sectionbufferbuilderpack = this.freeBuffers.poll();
            this.toBatchCount = this.toBatchHighPriority.size() + this.toBatchLowPriority.size();
            this.freeBufferCount = this.freeBuffers.size();
            CompletableFuture.supplyAsync(Util.wrapThreadWithTaskName(sectionrenderdispatcher$rendersection$compiletask.name(), () -> {
               return sectionrenderdispatcher$rendersection$compiletask.doTask(sectionbufferbuilderpack);
            }), this.executor).thenCompose((p_298155_) -> {
               return p_298155_;
            }).whenComplete((p_299295_, p_297995_) -> {
               if (p_297995_ != null) {
                  Minecraft.getInstance().delayCrash(CrashReport.forThrowable(p_297995_, "Batching sections"));
               } else {
                  this.mailbox.tell(() -> {
                     if (p_299295_ == SectionRenderDispatcher.SectionTaskResult.SUCCESSFUL) {
                        sectionbufferbuilderpack.clearAll();
                     } else {
                        sectionbufferbuilderpack.discardAll();
                     }

                     this.freeBuffers.add(sectionbufferbuilderpack);
                     this.freeBufferCount = this.freeBuffers.size();
                     this.runTask();
                  });
               }
            });
         }
      }
   }

   @Nullable
   private SectionRenderDispatcher.RenderSection.CompileTask pollTask() {
      if (this.highPriorityQuota <= 0) {
         SectionRenderDispatcher.RenderSection.CompileTask sectionrenderdispatcher$rendersection$compiletask = this.toBatchLowPriority.poll();
         if (sectionrenderdispatcher$rendersection$compiletask != null) {
            this.highPriorityQuota = 2;
            return sectionrenderdispatcher$rendersection$compiletask;
         }
      }

      SectionRenderDispatcher.RenderSection.CompileTask sectionrenderdispatcher$rendersection$compiletask1 = this.toBatchHighPriority.poll();
      if (sectionrenderdispatcher$rendersection$compiletask1 != null) {
         --this.highPriorityQuota;
         return sectionrenderdispatcher$rendersection$compiletask1;
      } else {
         this.highPriorityQuota = 2;
         return this.toBatchLowPriority.poll();
      }
   }

   public String getStats() {
      return String.format(Locale.ROOT, "pC: %03d, pU: %02d, aB: %02d", this.toBatchCount, this.toUpload.size(), this.freeBufferCount);
   }

   public int getToBatchCount() {
      return this.toBatchCount;
   }

   public int getToUpload() {
      return this.toUpload.size();
   }

   public int getFreeBufferCount() {
      return this.freeBufferCount;
   }

   public void setCamera(Vec3 pCamera) {
      this.camera = pCamera;
   }

   public Vec3 getCameraPosition() {
      return this.camera;
   }

   public void uploadAllPendingUploads() {
      Runnable runnable;
      while((runnable = this.toUpload.poll()) != null) {
         runnable.run();
      }

   }

   public void rebuildSectionSync(SectionRenderDispatcher.RenderSection pSection, RenderRegionCache pRegionCache) {
      pSection.compileSync(pRegionCache);
   }

   public void blockUntilClear() {
      this.clearBatchQueue();
   }

   public void schedule(SectionRenderDispatcher.RenderSection.CompileTask pTask) {
      this.mailbox.tell(() -> {
         if (pTask.isHighPriority) {
            this.toBatchHighPriority.offer(pTask);
         } else {
            this.toBatchLowPriority.offer(pTask);
         }

         this.toBatchCount = this.toBatchHighPriority.size() + this.toBatchLowPriority.size();
         this.runTask();
      });
   }

   public CompletableFuture<Void> uploadSectionLayer(BufferBuilder.RenderedBuffer pRenderedBuffer, VertexBuffer pVertexBuffer) {
      return CompletableFuture.runAsync(() -> {
         if (!pVertexBuffer.isInvalid()) {
            pVertexBuffer.bind();
            pVertexBuffer.upload(pRenderedBuffer);
            VertexBuffer.unbind();
         }
      }, this.toUpload::add);
   }

   private void clearBatchQueue() {
      while(!this.toBatchHighPriority.isEmpty()) {
         SectionRenderDispatcher.RenderSection.CompileTask sectionrenderdispatcher$rendersection$compiletask = this.toBatchHighPriority.poll();
         if (sectionrenderdispatcher$rendersection$compiletask != null) {
            sectionrenderdispatcher$rendersection$compiletask.cancel();
         }
      }

      while(!this.toBatchLowPriority.isEmpty()) {
         SectionRenderDispatcher.RenderSection.CompileTask sectionrenderdispatcher$rendersection$compiletask1 = this.toBatchLowPriority.poll();
         if (sectionrenderdispatcher$rendersection$compiletask1 != null) {
            sectionrenderdispatcher$rendersection$compiletask1.cancel();
         }
      }

      this.toBatchCount = 0;
   }

   public boolean isQueueEmpty() {
      return this.toBatchCount == 0 && this.toUpload.isEmpty();
   }

   public void dispose() {
      this.clearBatchQueue();
      this.mailbox.close();
      this.freeBuffers.clear();
   }

   @OnlyIn(Dist.CLIENT)
   public static class CompiledSection {
      public static final SectionRenderDispatcher.CompiledSection UNCOMPILED = new SectionRenderDispatcher.CompiledSection() {
         public boolean facesCanSeeEachother(Direction p_301280_, Direction p_299155_) {
            return false;
         }
      };
      final Set<RenderType> hasBlocks = new ObjectArraySet<>(RenderType.chunkBufferLayers().size());
      final List<BlockEntity> renderableBlockEntities = Lists.newArrayList();
      VisibilitySet visibilitySet = new VisibilitySet();
      @Nullable
      BufferBuilder.SortState transparencyState;

      public boolean hasNoRenderableLayers() {
         return this.hasBlocks.isEmpty();
      }

      public boolean isEmpty(RenderType pRenderType) {
         return !this.hasBlocks.contains(pRenderType);
      }

      public List<BlockEntity> getRenderableBlockEntities() {
         return this.renderableBlockEntities;
      }

      public boolean facesCanSeeEachother(Direction pFace1, Direction pFace2) {
         return this.visibilitySet.visibilityBetween(pFace1, pFace2);
      }
   }

   @OnlyIn(Dist.CLIENT)
   public class RenderSection {
      public static final int SIZE = 16;
      public final int index;
      public final AtomicReference<SectionRenderDispatcher.CompiledSection> compiled = new AtomicReference<>(SectionRenderDispatcher.CompiledSection.UNCOMPILED);
      final AtomicInteger initialCompilationCancelCount = new AtomicInteger(0);
      @Nullable
      private SectionRenderDispatcher.RenderSection.RebuildTask lastRebuildTask;
      @Nullable
      private SectionRenderDispatcher.RenderSection.ResortTransparencyTask lastResortTransparencyTask;
      private final Set<BlockEntity> globalBlockEntities = Sets.newHashSet();
      private final Map<RenderType, VertexBuffer> buffers = RenderType.chunkBufferLayers().stream().collect(Collectors.toMap((p_298649_) -> {
         return p_298649_;
      }, (p_299941_) -> {
         return new VertexBuffer(VertexBuffer.Usage.STATIC);
      }));
      private AABB bb;
      private boolean dirty = true;
      final BlockPos.MutableBlockPos origin = new BlockPos.MutableBlockPos(-1, -1, -1);
      private final BlockPos.MutableBlockPos[] relativeOrigins = Util.make(new BlockPos.MutableBlockPos[6], (p_300613_) -> {
         for(int i = 0; i < p_300613_.length; ++i) {
            p_300613_[i] = new BlockPos.MutableBlockPos();
         }

      });
      private boolean playerChanged;

      public RenderSection(int pIndex, int pOriginX, int pOriginY, int pOriginZ) {
         this.index = pIndex;
         this.setOrigin(pOriginX, pOriginY, pOriginZ);
      }

      private boolean doesChunkExistAt(BlockPos pPos) {
         return SectionRenderDispatcher.this.level.getChunk(SectionPos.blockToSectionCoord(pPos.getX()), SectionPos.blockToSectionCoord(pPos.getZ()), ChunkStatus.FULL, false) != null;
      }

      public boolean hasAllNeighbors() {
         int i = 24;
         if (!(this.getDistToPlayerSqr() > 576.0D)) {
            return true;
         } else {
            return this.doesChunkExistAt(this.relativeOrigins[Direction.WEST.ordinal()]) && this.doesChunkExistAt(this.relativeOrigins[Direction.NORTH.ordinal()]) && this.doesChunkExistAt(this.relativeOrigins[Direction.EAST.ordinal()]) && this.doesChunkExistAt(this.relativeOrigins[Direction.SOUTH.ordinal()]);
         }
      }

      public AABB getBoundingBox() {
         return this.bb;
      }

      public VertexBuffer getBuffer(RenderType pRenderType) {
         return this.buffers.get(pRenderType);
      }

      public void setOrigin(int pX, int pY, int pZ) {
         this.reset();
         this.origin.set(pX, pY, pZ);
         this.bb = new AABB((double)pX, (double)pY, (double)pZ, (double)(pX + 16), (double)(pY + 16), (double)(pZ + 16));

         for(Direction direction : Direction.values()) {
            this.relativeOrigins[direction.ordinal()].set(this.origin).move(direction, 16);
         }

      }

      protected double getDistToPlayerSqr() {
         Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
         double d0 = this.bb.minX + 8.0D - camera.getPosition().x;
         double d1 = this.bb.minY + 8.0D - camera.getPosition().y;
         double d2 = this.bb.minZ + 8.0D - camera.getPosition().z;
         return d0 * d0 + d1 * d1 + d2 * d2;
      }

      void beginLayer(BufferBuilder pBufferBuilder) {
         pBufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
      }

      public SectionRenderDispatcher.CompiledSection getCompiled() {
         return this.compiled.get();
      }

      private void reset() {
         this.cancelTasks();
         this.compiled.set(SectionRenderDispatcher.CompiledSection.UNCOMPILED);
         this.dirty = true;
      }

      public void releaseBuffers() {
         this.reset();
         this.buffers.values().forEach(VertexBuffer::close);
      }

      public BlockPos getOrigin() {
         return this.origin;
      }

      public void setDirty(boolean pPlayerChanged) {
         boolean flag = this.dirty;
         this.dirty = true;
         this.playerChanged = pPlayerChanged | (flag && this.playerChanged);
      }

      public void setNotDirty() {
         this.dirty = false;
         this.playerChanged = false;
      }

      public boolean isDirty() {
         return this.dirty;
      }

      public boolean isDirtyFromPlayer() {
         return this.dirty && this.playerChanged;
      }

      public BlockPos getRelativeOrigin(Direction pDirection) {
         return this.relativeOrigins[pDirection.ordinal()];
      }

      public boolean resortTransparency(RenderType pRenderType, SectionRenderDispatcher pSectionRenderDispatcher) {
         SectionRenderDispatcher.CompiledSection sectionrenderdispatcher$compiledsection = this.getCompiled();
         if (this.lastResortTransparencyTask != null) {
            this.lastResortTransparencyTask.cancel();
         }

         if (!sectionrenderdispatcher$compiledsection.hasBlocks.contains(pRenderType)) {
            return false;
         } else {
            this.lastResortTransparencyTask = new SectionRenderDispatcher.RenderSection.ResortTransparencyTask(new net.minecraft.world.level.ChunkPos(getOrigin()), this.getDistToPlayerSqr(), sectionrenderdispatcher$compiledsection);
            pSectionRenderDispatcher.schedule(this.lastResortTransparencyTask);
            return true;
         }
      }

      protected boolean cancelTasks() {
         boolean flag = false;
         if (this.lastRebuildTask != null) {
            this.lastRebuildTask.cancel();
            this.lastRebuildTask = null;
            flag = true;
         }

         if (this.lastResortTransparencyTask != null) {
            this.lastResortTransparencyTask.cancel();
            this.lastResortTransparencyTask = null;
         }

         return flag;
      }

      public SectionRenderDispatcher.RenderSection.CompileTask createCompileTask(RenderRegionCache pRegionCache) {
         boolean flag = this.cancelTasks();
         BlockPos blockpos = this.origin.immutable();
         int i = 1;
         RenderChunkRegion renderchunkregion = pRegionCache.createRegion(SectionRenderDispatcher.this.level, blockpos.offset(-1, -1, -1), blockpos.offset(16, 16, 16), 1);
         boolean flag1 = this.compiled.get() == SectionRenderDispatcher.CompiledSection.UNCOMPILED;
         if (flag1 && flag) {
            this.initialCompilationCancelCount.incrementAndGet();
         }

         this.lastRebuildTask = new SectionRenderDispatcher.RenderSection.RebuildTask(new net.minecraft.world.level.ChunkPos(getOrigin()), this.getDistToPlayerSqr(), renderchunkregion, !flag1 || this.initialCompilationCancelCount.get() > 2);
         return this.lastRebuildTask;
      }

      public void rebuildSectionAsync(SectionRenderDispatcher pSectionRenderDispatcher, RenderRegionCache pRegionCache) {
         SectionRenderDispatcher.RenderSection.CompileTask sectionrenderdispatcher$rendersection$compiletask = this.createCompileTask(pRegionCache);
         pSectionRenderDispatcher.schedule(sectionrenderdispatcher$rendersection$compiletask);
      }

      void updateGlobalBlockEntities(Collection<BlockEntity> pBlockEntities) {
         Set<BlockEntity> set = Sets.newHashSet(pBlockEntities);
         Set<BlockEntity> set1;
         synchronized(this.globalBlockEntities) {
            set1 = Sets.newHashSet(this.globalBlockEntities);
            set.removeAll(this.globalBlockEntities);
            set1.removeAll(pBlockEntities);
            this.globalBlockEntities.clear();
            this.globalBlockEntities.addAll(pBlockEntities);
         }

         SectionRenderDispatcher.this.renderer.updateGlobalBlockEntities(set1, set);
      }

      public void compileSync(RenderRegionCache pRegionCache) {
         SectionRenderDispatcher.RenderSection.CompileTask sectionrenderdispatcher$rendersection$compiletask = this.createCompileTask(pRegionCache);
         sectionrenderdispatcher$rendersection$compiletask.doTask(SectionRenderDispatcher.this.fixedBuffers);
      }

      public boolean isAxisAlignedWith(int pX, int pY, int pZ) {
         BlockPos blockpos = this.getOrigin();
         return pX == SectionPos.blockToSectionCoord(blockpos.getX()) || pZ == SectionPos.blockToSectionCoord(blockpos.getZ()) || pY == SectionPos.blockToSectionCoord(blockpos.getY());
      }

      @OnlyIn(Dist.CLIENT)
      abstract class CompileTask implements Comparable<SectionRenderDispatcher.RenderSection.CompileTask> {
         protected final double distAtCreation;
         protected final AtomicBoolean isCancelled = new AtomicBoolean(false);
         protected final boolean isHighPriority;
         protected java.util.Map<net.minecraft.core.BlockPos, net.minecraftforge.client.model.data.ModelData> modelData;

         public CompileTask(double pDistAtCreation, boolean pIsHighPriority) {
            this(null, pDistAtCreation, pIsHighPriority);
         }

         public CompileTask(@Nullable net.minecraft.world.level.ChunkPos pos, double pDistAtCreation, boolean pIsHighPriority) {
            this.distAtCreation = pDistAtCreation;
            this.isHighPriority = pIsHighPriority;
            if (pos == null) {
               this.modelData = java.util.Collections.emptyMap();
            } else {
               this.modelData = net.minecraft.client.Minecraft.getInstance().level.getModelDataManager().getAt(pos);
            }
         }

         public abstract CompletableFuture<SectionRenderDispatcher.SectionTaskResult> doTask(SectionBufferBuilderPack pSectionBufferBuilderPack);

         public abstract void cancel();

         protected abstract String name();

         public int compareTo(SectionRenderDispatcher.RenderSection.CompileTask pOther) {
            return Doubles.compare(this.distAtCreation, pOther.distAtCreation);
         }

         public net.minecraftforge.client.model.data.ModelData getModelData(net.minecraft.core.BlockPos pos) {
            return modelData.getOrDefault(pos, net.minecraftforge.client.model.data.ModelData.EMPTY);
         }
      }

      @OnlyIn(Dist.CLIENT)
      class RebuildTask extends SectionRenderDispatcher.RenderSection.CompileTask {
         @Nullable
         protected RenderChunkRegion region;

         @Deprecated
         public RebuildTask(double pDistAtCreation, @Nullable RenderChunkRegion pRegion, boolean pIsHighPriority) {
            this(null, pDistAtCreation, pRegion, pIsHighPriority);
         }

         public RebuildTask(@Nullable net.minecraft.world.level.ChunkPos pos, double pDistAtCreation, @Nullable RenderChunkRegion pRegion, boolean pIsHighPriority) {
            super(pos, pDistAtCreation, pIsHighPriority);
            this.region = pRegion;
         }

         protected String name() {
            return "rend_chk_rebuild";
         }

         public CompletableFuture<SectionRenderDispatcher.SectionTaskResult> doTask(SectionBufferBuilderPack pSectionBufferBuilderPack) {
            if (this.isCancelled.get()) {
               return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
            } else if (!RenderSection.this.hasAllNeighbors()) {
               this.region = null;
               RenderSection.this.setDirty(false);
               this.isCancelled.set(true);
               return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
            } else if (this.isCancelled.get()) {
               return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
            } else {
               Vec3 vec3 = SectionRenderDispatcher.this.getCameraPosition();
               float f = (float)vec3.x;
               float f1 = (float)vec3.y;
               float f2 = (float)vec3.z;
               SectionRenderDispatcher.RenderSection.RebuildTask.CompileResults sectionrenderdispatcher$rendersection$rebuildtask$compileresults = this.compile(f, f1, f2, pSectionBufferBuilderPack);
               RenderSection.this.updateGlobalBlockEntities(sectionrenderdispatcher$rendersection$rebuildtask$compileresults.globalBlockEntities);
               if (this.isCancelled.get()) {
                  sectionrenderdispatcher$rendersection$rebuildtask$compileresults.renderedLayers.values().forEach(BufferBuilder.RenderedBuffer::release);
                  return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
               } else {
                  SectionRenderDispatcher.CompiledSection sectionrenderdispatcher$compiledsection = new SectionRenderDispatcher.CompiledSection();
                  sectionrenderdispatcher$compiledsection.visibilitySet = sectionrenderdispatcher$rendersection$rebuildtask$compileresults.visibilitySet;
                  sectionrenderdispatcher$compiledsection.renderableBlockEntities.addAll(sectionrenderdispatcher$rendersection$rebuildtask$compileresults.blockEntities);
                  sectionrenderdispatcher$compiledsection.transparencyState = sectionrenderdispatcher$rendersection$rebuildtask$compileresults.transparencyState;
                  List<CompletableFuture<Void>> list = Lists.newArrayList();
                  sectionrenderdispatcher$rendersection$rebuildtask$compileresults.renderedLayers.forEach((p_301240_, p_299440_) -> {
                     list.add(SectionRenderDispatcher.this.uploadSectionLayer(p_299440_, RenderSection.this.getBuffer(p_301240_)));
                     sectionrenderdispatcher$compiledsection.hasBlocks.add(p_301240_);
                  });
                  return Util.sequenceFailFast(list).handle((p_297447_, p_298622_) -> {
                     if (p_298622_ != null && !(p_298622_ instanceof CancellationException) && !(p_298622_ instanceof InterruptedException)) {
                        Minecraft.getInstance().delayCrash(CrashReport.forThrowable(p_298622_, "Rendering section"));
                     }

                     if (this.isCancelled.get()) {
                        return SectionRenderDispatcher.SectionTaskResult.CANCELLED;
                     } else {
                        RenderSection.this.compiled.set(sectionrenderdispatcher$compiledsection);
                        RenderSection.this.initialCompilationCancelCount.set(0);
                        SectionRenderDispatcher.this.renderer.addRecentlyCompiledSection(RenderSection.this);
                        return SectionRenderDispatcher.SectionTaskResult.SUCCESSFUL;
                     }
                  });
               }
            }
         }

         private SectionRenderDispatcher.RenderSection.RebuildTask.CompileResults compile(float pX, float pY, float pZ, SectionBufferBuilderPack pSectionBufferBuilderPack) {
            SectionRenderDispatcher.RenderSection.RebuildTask.CompileResults sectionrenderdispatcher$rendersection$rebuildtask$compileresults = new SectionRenderDispatcher.RenderSection.RebuildTask.CompileResults();
            int i = 1;
            BlockPos blockpos = RenderSection.this.origin.immutable();
            BlockPos blockpos1 = blockpos.offset(15, 15, 15);
            VisGraph visgraph = new VisGraph();
            RenderChunkRegion renderchunkregion = this.region;
            this.region = null;
            PoseStack posestack = new PoseStack();
            if (renderchunkregion != null) {
               ModelBlockRenderer.enableCaching();
               Set<RenderType> set = new ReferenceArraySet<>(RenderType.chunkBufferLayers().size());
               RandomSource randomsource = RandomSource.create();
               BlockRenderDispatcher blockrenderdispatcher = Minecraft.getInstance().getBlockRenderer();

               for(BlockPos blockpos2 : BlockPos.betweenClosed(blockpos, blockpos1)) {
                  BlockState blockstate = renderchunkregion.getBlockState(blockpos2);
                  if (blockstate.isSolidRender(renderchunkregion, blockpos2)) {
                     visgraph.setOpaque(blockpos2);
                  }

                  if (blockstate.hasBlockEntity()) {
                     BlockEntity blockentity = renderchunkregion.getBlockEntity(blockpos2);
                     if (blockentity != null) {
                        this.handleBlockEntity(sectionrenderdispatcher$rendersection$rebuildtask$compileresults, blockentity);
                     }
                  }

                  FluidState fluidstate = blockstate.getFluidState();
                  if (!fluidstate.isEmpty()) {
                     RenderType rendertype = ItemBlockRenderTypes.getRenderLayer(fluidstate);
                     BufferBuilder bufferbuilder = pSectionBufferBuilderPack.builder(rendertype);
                     if (set.add(rendertype)) {
                        RenderSection.this.beginLayer(bufferbuilder);
                     }

                     blockrenderdispatcher.renderLiquid(blockpos2, renderchunkregion, bufferbuilder, blockstate, fluidstate);
                  }

                  if (blockstate.getRenderShape() != RenderShape.INVISIBLE) {
                     var model = blockrenderdispatcher.getBlockModel(blockstate);
                     var modelData = model.getModelData(renderchunkregion, blockpos2, blockstate, getModelData(blockpos2));
                     randomsource.setSeed(blockstate.getSeed(blockpos2));
                     for (RenderType rendertype2 : model.getRenderTypes(blockstate, randomsource, modelData)) {
                     BufferBuilder bufferbuilder2 = pSectionBufferBuilderPack.builder(rendertype2);
                     if (set.add(rendertype2)) {
                        RenderSection.this.beginLayer(bufferbuilder2);
                     }

                     posestack.pushPose();
                     posestack.translate((float)(blockpos2.getX() & 15), (float)(blockpos2.getY() & 15), (float)(blockpos2.getZ() & 15));
                     blockrenderdispatcher.renderBatched(blockstate, blockpos2, renderchunkregion, posestack, bufferbuilder2, true, randomsource, modelData, rendertype2);
                     posestack.popPose();
                     }
                  }
               }

               if (set.contains(RenderType.translucent())) {
                  BufferBuilder bufferbuilder1 = pSectionBufferBuilderPack.builder(RenderType.translucent());
                  if (!bufferbuilder1.isCurrentBatchEmpty()) {
                     bufferbuilder1.setQuadSorting(VertexSorting.byDistance(pX - (float)blockpos.getX(), pY - (float)blockpos.getY(), pZ - (float)blockpos.getZ()));
                     sectionrenderdispatcher$rendersection$rebuildtask$compileresults.transparencyState = bufferbuilder1.getSortState();
                  }
               }

               for(RenderType rendertype1 : set) {
                  BufferBuilder.RenderedBuffer bufferbuilder$renderedbuffer = pSectionBufferBuilderPack.builder(rendertype1).endOrDiscardIfEmpty();
                  if (bufferbuilder$renderedbuffer != null) {
                     sectionrenderdispatcher$rendersection$rebuildtask$compileresults.renderedLayers.put(rendertype1, bufferbuilder$renderedbuffer);
                  }
               }

               ModelBlockRenderer.clearCache();
            }

            sectionrenderdispatcher$rendersection$rebuildtask$compileresults.visibilitySet = visgraph.resolve();
            return sectionrenderdispatcher$rendersection$rebuildtask$compileresults;
         }

         private <E extends BlockEntity> void handleBlockEntity(SectionRenderDispatcher.RenderSection.RebuildTask.CompileResults pCompileResults, E pBlockEntity) {
            BlockEntityRenderer<E> blockentityrenderer = Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(pBlockEntity);
            if (blockentityrenderer != null) {
               if (blockentityrenderer.shouldRenderOffScreen(pBlockEntity)) {
                  pCompileResults.globalBlockEntities.add(pBlockEntity);
               }
               else pCompileResults.blockEntities.add(pBlockEntity); //FORGE: Fix MC-112730
            }

         }

         public void cancel() {
            this.region = null;
            if (this.isCancelled.compareAndSet(false, true)) {
               RenderSection.this.setDirty(false);
            }

         }

         @OnlyIn(Dist.CLIENT)
         static final class CompileResults {
            public final List<BlockEntity> globalBlockEntities = new ArrayList<>();
            public final List<BlockEntity> blockEntities = new ArrayList<>();
            public final Map<RenderType, BufferBuilder.RenderedBuffer> renderedLayers = new Reference2ObjectArrayMap<>();
            public VisibilitySet visibilitySet = new VisibilitySet();
            @Nullable
            public BufferBuilder.SortState transparencyState;
         }
      }

      @OnlyIn(Dist.CLIENT)
      class ResortTransparencyTask extends SectionRenderDispatcher.RenderSection.CompileTask {
         private final SectionRenderDispatcher.CompiledSection compiledSection;

         @Deprecated
         public ResortTransparencyTask(double pDistAtCreation, SectionRenderDispatcher.CompiledSection pCompiledSection) {
            this(null, pDistAtCreation, pCompiledSection);
         }

         public ResortTransparencyTask(@Nullable net.minecraft.world.level.ChunkPos pos, double pDistAtCreation, SectionRenderDispatcher.CompiledSection pCompiledSection) {
            super(pos, pDistAtCreation, true);
            this.compiledSection = pCompiledSection;
         }

         protected String name() {
            return "rend_chk_sort";
         }

         public CompletableFuture<SectionRenderDispatcher.SectionTaskResult> doTask(SectionBufferBuilderPack pSectionBufferBuilderPack) {
            if (this.isCancelled.get()) {
               return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
            } else if (!RenderSection.this.hasAllNeighbors()) {
               this.isCancelled.set(true);
               return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
            } else if (this.isCancelled.get()) {
               return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
            } else {
               Vec3 vec3 = SectionRenderDispatcher.this.getCameraPosition();
               float f = (float)vec3.x;
               float f1 = (float)vec3.y;
               float f2 = (float)vec3.z;
               BufferBuilder.SortState bufferbuilder$sortstate = this.compiledSection.transparencyState;
               if (bufferbuilder$sortstate != null && !this.compiledSection.isEmpty(RenderType.translucent())) {
                  BufferBuilder bufferbuilder = pSectionBufferBuilderPack.builder(RenderType.translucent());
                  RenderSection.this.beginLayer(bufferbuilder);
                  bufferbuilder.restoreSortState(bufferbuilder$sortstate);
                  bufferbuilder.setQuadSorting(VertexSorting.byDistance(f - (float)RenderSection.this.origin.getX(), f1 - (float)RenderSection.this.origin.getY(), f2 - (float)RenderSection.this.origin.getZ()));
                  this.compiledSection.transparencyState = bufferbuilder.getSortState();
                  BufferBuilder.RenderedBuffer bufferbuilder$renderedbuffer = bufferbuilder.end();
                  if (this.isCancelled.get()) {
                     bufferbuilder$renderedbuffer.release();
                     return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
                  } else {
                     CompletableFuture<SectionRenderDispatcher.SectionTaskResult> completablefuture = SectionRenderDispatcher.this.uploadSectionLayer(bufferbuilder$renderedbuffer, RenderSection.this.getBuffer(RenderType.translucent())).thenApply((p_297230_) -> {
                        return SectionRenderDispatcher.SectionTaskResult.CANCELLED;
                     });
                     return completablefuture.handle((p_301037_, p_300486_) -> {
                        if (p_300486_ != null && !(p_300486_ instanceof CancellationException) && !(p_300486_ instanceof InterruptedException)) {
                           Minecraft.getInstance().delayCrash(CrashReport.forThrowable(p_300486_, "Rendering section"));
                        }

                        return this.isCancelled.get() ? SectionRenderDispatcher.SectionTaskResult.CANCELLED : SectionRenderDispatcher.SectionTaskResult.SUCCESSFUL;
                     });
                  }
               } else {
                  return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
               }
            }
         }

         public void cancel() {
            this.isCancelled.set(true);
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   static enum SectionTaskResult {
      SUCCESSFUL,
      CANCELLED;
   }
}
