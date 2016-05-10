/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.common.event;

import static com.google.common.base.Preconditions.checkArgument;

import com.flowpowered.math.vector.Vector3d;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldServerMulti;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntitySnapshot;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.projectile.source.ProjectileSource;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.block.CollideBlockEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.block.NotifyNeighborBlockEvent;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.event.cause.entity.spawn.SpawnCause;
import org.spongepowered.api.event.entity.CollideEntityEvent;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.util.GuavaCollectors;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.data.util.NbtDataUtil;
import org.spongepowered.common.entity.PlayerTracker;
import org.spongepowered.common.event.tracking.CauseTracker;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.event.tracking.PhaseData;
import org.spongepowered.common.interfaces.IMixinChunk;
import org.spongepowered.common.interfaces.entity.IMixinEntity;
import org.spongepowered.common.interfaces.world.IMixinWorldServer;
import org.spongepowered.common.registry.provider.DirectionFacingProvider;
import org.spongepowered.common.util.StaticMixinHelper;
import org.spongepowered.common.util.VecHelper;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

public class SpongeCommonEventFactory {

    @SuppressWarnings("unchecked")
    @Nullable
    public static CollideEntityEvent callCollideEntityEvent(net.minecraft.world.World world, @Nullable net.minecraft.entity.Entity sourceEntity,
                                                            List<net.minecraft.entity.Entity> entities) {
        final Cause cause;
        if (sourceEntity != null) {
            cause = Cause.of(NamedCause.source(sourceEntity));
        } else {
            IMixinWorldServer spongeWorld = (IMixinWorldServer) world;
            CauseTracker causeTracker = spongeWorld.getCauseTracker();
            PhaseContext context = causeTracker.getStack().peekContext();

            final Optional<BlockSnapshot> currentTickingBlock = context.firstNamed(NamedCause.SOURCE, BlockSnapshot.class);
            final Optional<TileEntity> currentTickingTileEntity = context.firstNamed(NamedCause.SOURCE, TileEntity.class);
            final Optional<Entity> currentTickingEntity = context.firstNamed(NamedCause.SOURCE, Entity.class);
            if (currentTickingBlock.isPresent()) {
                cause = Cause.of(NamedCause.source(currentTickingBlock.get()));
            } else if (currentTickingTileEntity.isPresent()) {
                cause = Cause.of(NamedCause.source(currentTickingTileEntity.get()));
            } else if (currentTickingEntity.isPresent()) {
                cause = Cause.of(NamedCause.source(currentTickingEntity.get()));
            } else {
                cause = null;
            }

            if (cause == null) {
                return null;
            }
        }

        List<Entity> spEntities = (List<Entity>) (List<?>) entities;
        CollideEntityEvent event = SpongeEventFactory.createCollideEntityEvent(cause, spEntities, (World) world);
        SpongeImpl.postEvent(event);
        return event;
    }

    @SuppressWarnings("rawtypes")
    public static NotifyNeighborBlockEvent callNotifyNeighborEvent(World world, BlockPos pos, EnumSet notifiedSides) {
        final CauseTracker causeTracker = ((IMixinWorldServer) world).getCauseTracker();
        final PhaseData peek = causeTracker.getStack().peek();
        final PhaseContext context = peek.getContext();
        final BlockSnapshot snapshot = world.createSnapshot(pos.getX(), pos.getY(), pos.getZ());
        final Map<Direction, BlockState> neighbors = new HashMap<>();

        for (Object obj : notifiedSides) {
            EnumFacing notifiedSide = (EnumFacing) obj;
            BlockPos offset = pos.offset(notifiedSide);
            Direction direction = DirectionFacingProvider.getInstance().getKey(notifiedSide).get();
            Location<World> location = new Location<>(world, VecHelper.toVector3i(offset));
            if (location.getBlockY() >= 0 && location.getBlockY() <= 255) {
                neighbors.put(direction, location.getBlock());
            }
        }

        ImmutableMap<Direction, BlockState> originalNeighbors = ImmutableMap.copyOf(neighbors);
        // Determine cause
        final Cause.Builder builder = Cause.source(snapshot);
        final IMixinChunk mixinChunk = (IMixinChunk) causeTracker.getMinecraftWorld().getChunkFromBlockCoords(pos);

        peek.getState().getPhase().populateCauseForNotifyNeighborEvent(peek.getState(), context, builder, causeTracker, mixinChunk, pos);

        NotifyNeighborBlockEvent event = SpongeEventFactory.createNotifyNeighborBlockEvent(builder.build(), originalNeighbors, neighbors);
        StaticMixinHelper.processingInternalForgeEvent = true;
        SpongeImpl.postEvent(event);
        StaticMixinHelper.processingInternalForgeEvent = false;
        return event;
    }

    public static InteractBlockEvent.Secondary callInteractBlockEventSecondary(Cause cause, Optional<Vector3d> interactionPoint, BlockSnapshot targetBlock, Direction targetSide, EnumHand hand) {
        return callInteractBlockEventSecondary(cause, Tristate.UNDEFINED, Tristate.UNDEFINED, Tristate.UNDEFINED, Tristate.UNDEFINED, interactionPoint, targetBlock, targetSide, hand);
    }

    public static InteractBlockEvent.Secondary callInteractBlockEventSecondary(Cause cause, Tristate originalUseBlockResult, Tristate useBlockResult, Tristate originalUseItemResult, Tristate useItemResult, Optional<Vector3d> interactionPoint, BlockSnapshot targetBlock, Direction targetSide, EnumHand hand) {
        InteractBlockEvent.Secondary event;
        if (hand == EnumHand.MAIN_HAND) {
            event = SpongeEventFactory.createInteractBlockEventSecondaryMainHand(cause, originalUseBlockResult, useBlockResult, originalUseItemResult, useItemResult, interactionPoint, targetBlock, targetSide);
        } else {
            event = SpongeEventFactory.createInteractBlockEventSecondaryOffHand(cause, originalUseBlockResult, useBlockResult, originalUseItemResult, useItemResult, interactionPoint, targetBlock, targetSide);
        }
        SpongeImpl.postEvent(event);
        return event;
    }

    public static boolean handleCollideBlockEvent(Block block, net.minecraft.world.World world, BlockPos pos, IBlockState state, net.minecraft.entity.Entity entity, Direction direction) {
        final WorldServer worldServer = (WorldServer) world;
        final IMixinWorldServer mixinWorldServer = (IMixinWorldServer) worldServer;
        final CauseTracker causeTracker = mixinWorldServer.getCauseTracker();
        final Cause.Builder builder = Cause.source(entity);
        builder.named(NamedCause.of(NamedCause.PHYSICAL, entity));

        if (!(entity instanceof EntityPlayer)) {
            IMixinEntity spongeEntity = (IMixinEntity) entity;
            Optional<User> user = spongeEntity.getTrackedPlayer(NbtDataUtil.SPONGE_ENTITY_CREATOR);
            if (user.isPresent()) {
                builder.named(NamedCause.owner(user.get()));
            }
        }

        // TODO: Add target side support
        CollideBlockEvent event = SpongeEventFactory.createCollideBlockEvent(builder.build(), (BlockState) state,
                new Location<>((World) world, VecHelper.toVector3d(pos)), direction);
        boolean cancelled = SpongeImpl.postEvent(event);
        if (!cancelled) {
            IMixinEntity spongeEntity = (IMixinEntity) entity;
            if (!pos.equals(spongeEntity.getLastCollidedBlockPos())) {
                final PhaseData peek = causeTracker.getStack().peek();
                final Optional<User> notifier = peek.getContext().firstNamed(NamedCause.NOTIFIER, User.class);
                if (notifier.isPresent()) {
                    IMixinChunk spongeChunk = (IMixinChunk) world.getChunkFromBlockCoords(pos);
                    spongeChunk.addTrackedBlockPosition(block, pos, notifier.get(), PlayerTracker.Type.NOTIFIER);
                }
            }
        }

        return cancelled;
    }

    public static boolean handleCollideImpactEvent(net.minecraft.entity.Entity projectile, @Nullable ProjectileSource projectileSource,
            RayTraceResult movingObjectPosition) {
        final WorldServer worldServer = (WorldServer) projectile.worldObj;
        final IMixinWorldServer mixinWorldServer = (IMixinWorldServer) worldServer;
        final CauseTracker causeTracker = mixinWorldServer.getCauseTracker();
        RayTraceResult.Type movingObjectType = movingObjectPosition.typeOfHit;
        final Cause.Builder builder = Cause.source(projectile).named("ProjectileSource", projectileSource == null
                                                                                         ? ProjectileSource.UNKNOWN
                                                                                         : projectileSource);
        final Optional<User> notifier = causeTracker.getStack().peek()
                .getContext()
                .firstNamed(NamedCause.NOTIFIER, User.class);
        notifier.ifPresent(user -> builder.named(NamedCause.OWNER, user));

        Location<World> impactPoint = new Location<>((World) projectile.worldObj, VecHelper.toVector3d(movingObjectPosition.hitVec));
        boolean cancelled = false;

        if (movingObjectType == RayTraceResult.Type.BLOCK) {
            BlockSnapshot targetBlock = ((World) projectile.worldObj).createSnapshot(VecHelper.toVector3i(movingObjectPosition.getBlockPos()));
            Direction side = Direction.NONE;
            if (movingObjectPosition.sideHit != null) {
                side = DirectionFacingProvider.getInstance().getKey(movingObjectPosition.sideHit).get();
            }

            CollideBlockEvent.Impact event = SpongeEventFactory.createCollideBlockEventImpact(builder.build(), impactPoint, targetBlock.getState(),
                    targetBlock.getLocation().get(), side);
            cancelled = SpongeImpl.postEvent(event);
            // Track impact block if event is not cancelled
            if (!cancelled && notifier.isPresent()) {
                BlockPos targetPos = VecHelper.toBlockPos(impactPoint.getBlockPosition());
                IMixinChunk spongeChunk = (IMixinChunk) projectile.worldObj.getChunkFromBlockCoords(targetPos);
                spongeChunk.addTrackedBlockPosition((Block) targetBlock.getState().getType(), targetPos, notifier.get(), PlayerTracker.Type.NOTIFIER);
            }
        } else if (movingObjectPosition.entityHit != null) { // entity
            ArrayList<Entity> entityList = new ArrayList<>();
            entityList.add((Entity) movingObjectPosition.entityHit);
            CollideEntityEvent.Impact event = SpongeEventFactory.createCollideEntityEventImpact(builder.build(), entityList, impactPoint,
                    (World) projectile.worldObj);
            return SpongeImpl.postEvent(event);
        }

        return cancelled;
    }


    public static void checkSpawnEvent(Entity entity, Cause cause) {
        checkArgument(cause.root() instanceof SpawnCause, "The cause does not have a SpawnCause! It has instead: {}", cause.root().toString());
        checkArgument(cause.containsNamed(NamedCause.SOURCE), "The cause does not have a \"Source\" named object!");
        checkArgument(cause.get(NamedCause.SOURCE, SpawnCause.class).isPresent(), "The SpawnCause is not the \"Source\" of the cause!");

    }

}
