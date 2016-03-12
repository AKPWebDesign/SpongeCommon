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
package org.spongepowered.common.event.tracking.phase;

import static com.google.common.base.Preconditions.*;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C01PacketChatMessage;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C0EPacketClickWindow;
import net.minecraft.network.play.client.C10PacketCreativeInventoryAction;
import net.minecraft.network.play.client.C16PacketClientStatus;
import net.minecraft.world.World;
import org.apache.http.annotation.Immutable;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntitySnapshot;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.event.entity.SpawnEntityEvent;
import org.spongepowered.api.event.item.inventory.ClickInventoryEvent;
import org.spongepowered.api.item.inventory.Container;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.transaction.SlotTransaction;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.data.util.NbtDataUtil;
import org.spongepowered.common.event.tracking.CauseTracker;
import org.spongepowered.common.event.tracking.IPhaseState;
import org.spongepowered.common.event.tracking.ISpawnableState;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.event.tracking.TrackingHelper;
import org.spongepowered.common.event.tracking.phase.util.PacketFunction;
import org.spongepowered.common.interfaces.entity.IMixinEntity;
import org.spongepowered.common.item.inventory.util.ItemStackUtil;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.Nullable;

public class PacketPhase extends TrackingPhase {

    // Inventory static fields
    final static int MAGIC_CLICK_OUTSIDE_SURVIVAL = -999;
    final static int MAGIC_CLICK_OUTSIDE_CREATIVE = -1;

    // Flag masks
    final static int MASK_NONE              = 0x00000;
    final static int MASK_OUTSIDE           = 0x30000;
    final static int MASK_MODE              = 0x0FE00;
    final static int MASK_DRAGDATA          = 0x001F8;
    final static int MASK_BUTTON            = 0x00007;

    // Mask presets
    final static int MASK_ALL               = MASK_OUTSIDE | MASK_MODE | MASK_BUTTON | MASK_DRAGDATA;
    final static int MASK_NORMAL            = MASK_MODE | MASK_BUTTON | MASK_DRAGDATA;
    final static int MASK_DRAG              = MASK_OUTSIDE | MASK_NORMAL;

    // Click location semaphore flags
    final static int CLICK_INSIDE_WINDOW    = 0x01 << 16 << 0;
    final static int CLICK_OUTSIDE_WINDOW   = 0x01 << 16 << 1;
    final static int CLICK_ANYWHERE         = CLICK_INSIDE_WINDOW | CLICK_OUTSIDE_WINDOW;
    
    // Modes flags
    final static int MODE_CLICK             = 0x01 << 9 << 0;
    final static int MODE_SHIFT_CLICK       = 0x01 << 9 << 1;
    final static int MODE_HOTBAR            = 0x01 << 9 << 2;
    final static int MODE_PICKBLOCK         = 0x01 << 9 << 3;
    final static int MODE_DROP              = 0x01 << 9 << 4;
    final static int MODE_DRAG              = 0x01 << 9 << 5;
    final static int MODE_DOUBLE_CLICK      = 0x01 << 9 << 6;
    
    // Drag mode flags, bitmasked from button and only set if MODE_DRAG
    final static int DRAG_MODE_SPLIT_ITEMS  = 0x01 << 6 << 0;
    final static int DRAG_MODE_ONE_ITEM     = 0x01 << 6 << 1;
    final static int DRAG_MODE_ANY          = DRAG_MODE_SPLIT_ITEMS | DRAG_MODE_ONE_ITEM;

    // Drag status flags, bitmasked from button and only set if MODE_DRAG
    final static int DRAG_STATUS_STARTED    = 0x01 << 3 << 0;
    final static int DRAG_STATUS_ADD_SLOT   = 0x01 << 3 << 1;
    final static int DRAG_STATUS_STOPPED    = 0x01 << 3 << 2;

    // Buttons flags, only set if *not* MODE_DRAG
    final static int BUTTON_PRIMARY         = 0x01 << 0 << 0;
    final static int BUTTON_SECONDARY       = 0x01 << 0 << 1;
    final static int BUTTON_MIDDLE          = 0x01 << 0 << 2;

    public interface IPacketState extends IPhaseState {

        boolean matches(int packetState);

        default void populateContext(EntityPlayerMP playerMP, Packet<?> packet, PhaseContext context) {

        }

    }

    public enum Inventory implements IPacketState, ISpawnableState {

        INVENTORY,
        DROP_ITEM(MODE_CLICK | MODE_PICKBLOCK | BUTTON_PRIMARY | CLICK_OUTSIDE_WINDOW) {
            @Override
            public void populateContext(EntityPlayerMP playerMP, Packet<?> packet, PhaseContext context) {
                context.add(NamedCause.of(TrackingHelper.DESTRUCT_ITEM_DROPS, false));
            }

            @Override
            public ClickInventoryEvent createInventoryEvent(EntityPlayerMP playerMP, Container openContainer, Transaction<ItemStackSnapshot> transaction,
                    List<SlotTransaction> slotTransactions, List<Entity> capturedEntities, Cause cause, int usedButton) {
                Iterator<Entity> iterator = capturedEntities.iterator();
                ImmutableList.Builder<EntitySnapshot> entitySnapshotBuilder = new ImmutableList.Builder<>();
                while (iterator.hasNext()) {
                    Entity currentEntity = iterator.next();
                    ((IMixinEntity) currentEntity).trackEntityUniqueId(NbtDataUtil.SPONGE_ENTITY_CREATOR, playerMP.getUniqueID());
                    entitySnapshotBuilder.add(currentEntity.createSnapshot());
                }
                final org.spongepowered.api.world.World spongeWorld = (org.spongepowered.api.world.World) playerMP.worldObj;
                return SpongeEventFactory.createClickInventoryEventDropFull(cause, transaction, capturedEntities,
                        entitySnapshotBuilder.build(), openContainer, spongeWorld, slotTransactions);
            }
        },
        DROP_ITEMS,
        DROP_INVENTORY,
        DROP_SINGLE_ITEM_FROM_INVENTORY(MODE_CLICK | MODE_PICKBLOCK | BUTTON_SECONDARY | CLICK_OUTSIDE_WINDOW) {
            @Override
            public ClickInventoryEvent createInventoryEvent(EntityPlayerMP playerMP, Container openContainer, Transaction<ItemStackSnapshot> transaction,
                    List<SlotTransaction> slotTransactions, List<Entity> capturedEntities, Cause cause, int usedButton) {
                final Iterator<Entity> iterator = capturedEntities.iterator();
                final ImmutableList.Builder<EntitySnapshot> entitySnapshotBuilder = new ImmutableList.Builder<>();
                while (iterator.hasNext()) {
                    final Entity currentEntity = iterator.next();
                    ((IMixinEntity) currentEntity).trackEntityUniqueId(NbtDataUtil.SPONGE_ENTITY_CREATOR, playerMP.getUniqueID());
                    entitySnapshotBuilder.add(currentEntity.createSnapshot());
                }
                final org.spongepowered.api.world.World spongeWorld = (org.spongepowered.api.world.World) playerMP.worldObj;
                return SpongeEventFactory.createClickInventoryEventDropSingle(cause, transaction, capturedEntities,
                        entitySnapshotBuilder.build(), openContainer, spongeWorld, slotTransactions);

            }
        },
        SWITCH_HOTBAR_NUMBER_PRESS(MODE_HOTBAR, MASK_MODE) {
            @Override
            public ClickInventoryEvent createInventoryEvent(EntityPlayerMP playerMP, Container openContainer, Transaction<ItemStackSnapshot> transaction,
                    List<SlotTransaction> slotTransactions, List<Entity> capturedEntities, Cause cause, int usedButton) {
                return SpongeEventFactory.createClickInventoryEventNumberPress(cause, transaction, openContainer,
                        slotTransactions, usedButton);
            }
        },
        PRIMARY_INVENTORY_CLICK(MODE_CLICK | MODE_DROP | MODE_PICKBLOCK | BUTTON_PRIMARY | CLICK_INSIDE_WINDOW) {
            @Override
            public ClickInventoryEvent createInventoryEvent(EntityPlayerMP playerMP, Container openContainer, Transaction<ItemStackSnapshot> transaction,
                    List<SlotTransaction> slotTransactions, List<Entity> capturedEntities, Cause cause, int usedButton) {
                return SpongeEventFactory.createClickInventoryEventPrimary(cause, transaction, openContainer, slotTransactions);
            }
        },
        PRIMARY_INVENTORY_SHIFT_CLICK(MODE_SHIFT_CLICK | BUTTON_PRIMARY, MASK_NORMAL) {
            @Override
            public ClickInventoryEvent createInventoryEvent(EntityPlayerMP playerMP, Container openContainer, Transaction<ItemStackSnapshot> transaction,
                    List<SlotTransaction> slotTransactions, List<Entity> capturedEntities, Cause cause, int usedButton) {
                return SpongeEventFactory.createClickInventoryEventShiftPrimary(cause, transaction, openContainer, slotTransactions);
            }
        },
        MIDDLE_INVENTORY_CLICK(MODE_CLICK | MODE_PICKBLOCK | BUTTON_MIDDLE, MASK_NORMAL) {
            @Override
            public ClickInventoryEvent createInventoryEvent(EntityPlayerMP playerMP, Container openContainer, Transaction<ItemStackSnapshot> transaction,
                    List<SlotTransaction> slotTransactions, List<Entity> capturedEntities, Cause cause, int usedButton) {
                return SpongeEventFactory.createClickInventoryEventMiddle(cause, transaction, openContainer, slotTransactions);
            }
        },
        SECONDARY_INVENTORY_CLICK(MODE_CLICK | MODE_PICKBLOCK | BUTTON_SECONDARY | CLICK_INSIDE_WINDOW) {
            @Override
            public ClickInventoryEvent createInventoryEvent(EntityPlayerMP playerMP, Container openContainer, Transaction<ItemStackSnapshot> transaction,
                    List<SlotTransaction> slotTransactions, List<Entity> capturedEntities, Cause cause, int usedButton) {
                return SpongeEventFactory.createClickInventoryEventSecondary(cause, transaction, openContainer, slotTransactions);
            }
        },
        SECONDARY_INVENTORY_CLICK_DROP(MODE_DROP | BUTTON_SECONDARY, MASK_NORMAL) {
            @Override
            public ClickInventoryEvent createInventoryEvent(EntityPlayerMP playerMP, Container openContainer, Transaction<ItemStackSnapshot> transaction,
                    List<SlotTransaction> slotTransactions, List<Entity> capturedEntities, Cause cause, int usedButton) {
                return SpongeEventFactory.createClickInventoryEventSecondary(cause, transaction, openContainer, slotTransactions);
            }
        },
        SECONDARY_INVENTORY_SHIFT_CLICK(MODE_SHIFT_CLICK | BUTTON_SECONDARY, MASK_NORMAL) {
            @Override
            public ClickInventoryEvent createInventoryEvent(EntityPlayerMP playerMP, Container openContainer, Transaction<ItemStackSnapshot> transaction,
                    List<SlotTransaction> slotTransactions, List<Entity> capturedEntities, Cause cause, int usedButton) {
                return SpongeEventFactory.createClickInventoryEventShiftSecondary(cause, transaction, openContainer, slotTransactions);

            }
        },
        DOUBLE_CLICK_INVENTORY(MODE_DOUBLE_CLICK | BUTTON_PRIMARY | BUTTON_SECONDARY, MASK_MODE | MASK_BUTTON) {
            @Override
            public ClickInventoryEvent createInventoryEvent(EntityPlayerMP playerMP, Container openContainer, Transaction<ItemStackSnapshot> transaction,
                    List<SlotTransaction> slotTransactions, List<Entity> capturedEntities, Cause cause, int usedButton) {
                return SpongeEventFactory.createClickInventoryEventDouble(cause, transaction, openContainer, slotTransactions);
            }
        },
        PRIMARY_DRAG_INVENTORY_START(MODE_DRAG | DRAG_MODE_SPLIT_ITEMS | DRAG_STATUS_STARTED | CLICK_OUTSIDE_WINDOW, MASK_DRAG),
        SECONDARY_DRAG_INVENTORY_START(MODE_DRAG | DRAG_MODE_ONE_ITEM | DRAG_STATUS_STARTED | CLICK_OUTSIDE_WINDOW, MASK_DRAG),
        PRIMARY_DRAG_INVENTORY_ADDSLOT(MODE_DRAG | DRAG_MODE_SPLIT_ITEMS | DRAG_STATUS_ADD_SLOT | CLICK_INSIDE_WINDOW, MASK_DRAG),
        SECONDARY_DRAG_INVENTORY_ADDSLOT(MODE_DRAG | DRAG_MODE_ONE_ITEM | DRAG_STATUS_ADD_SLOT | CLICK_INSIDE_WINDOW, MASK_DRAG),
        PRIMARY_DRAG_INVENTORY_STOP(MODE_DRAG | DRAG_MODE_SPLIT_ITEMS | DRAG_STATUS_STOPPED | CLICK_OUTSIDE_WINDOW, MASK_DRAG) {
            @Override
            public ClickInventoryEvent createInventoryEvent(EntityPlayerMP playerMP, Container openContainer, Transaction<ItemStackSnapshot> transaction,
                    List<SlotTransaction> slotTransactions, List<Entity> capturedEntities, Cause cause, int usedButton) {
                return SpongeEventFactory.createClickInventoryEventDragPrimary(cause, transaction, openContainer, slotTransactions);
            }
        },
        SECONDARY_DRAG_INVENTORY_STOP(MODE_DRAG | DRAG_MODE_ONE_ITEM | DRAG_STATUS_STOPPED | CLICK_OUTSIDE_WINDOW, MASK_DRAG) {
            @Override
            public ClickInventoryEvent createInventoryEvent(EntityPlayerMP playerMP, Container openContainer, Transaction<ItemStackSnapshot> transaction,
                    List<SlotTransaction> slotTransactions, List<Entity> capturedEntities, Cause cause, int usedButton) {
                return SpongeEventFactory.createClickInventoryEventDragSecondary(cause, transaction, openContainer, slotTransactions);
            }
        },
        ;

        /**
         * Flags we care about
         */
        final int stateId;

        /**
         * Mask for flags we care about, the caremask if you will
         */
        final int stateMask;

        /**
         * Don't care about anything
         */
        Inventory() {
            this(0, MASK_NONE);
        }

        /**
         * We care a lot
         * 
         * @param stateId state
         */
        Inventory(int stateId) {
            this(stateId, MASK_ALL);
        }

        /**
         * We care about some things
         * 
         * @param stateId flags we care about
         * @param stateMask caring mask
         */
        Inventory(int stateId, int stateMask) {
            this.stateId = stateId & stateMask;
            this.stateMask = stateMask;
        }

        @Override
        public PacketPhase getPhase() {
            return TrackingPhases.PACKET;
        }

        @Override
        public boolean isBusy() {
            return true;
        }

        @Nullable
        @Override
        public SpawnEntityEvent createSpawnEventPostProcess(Cause cause, CauseTracker causeTracker, PhaseContext phaseContext,
                List<EntitySnapshot> entitySnapshots) {
            return null;
        }

        @Override
        public boolean matches(int packetState) {
            return this.stateMask != MASK_NONE && ((packetState & this.stateMask & this.stateId) == (packetState & this.stateMask));
        }

        @Nullable
        public ClickInventoryEvent createInventoryEvent(EntityPlayerMP playerMP, Container openContainer, Transaction<ItemStackSnapshot> transaction,
                List<SlotTransaction> slotTransactions, List<Entity> capturedEntities, Cause cause, int usedButton) {
            return null;
        }

        public static Inventory fromWindowPacket(C0EPacketClickWindow windowPacket) {
            final int mode = 0x01 << 9 << windowPacket.getMode();
            final int packed = windowPacket.getUsedButton();
            final int unpacked = mode == MODE_DRAG ? (0x01 << 6 << (packed >> 2 & 3)) | (0x01 << 3 << (packed & 3)) : (0x01 << (packed & 3));
            return Inventory.fromState(Inventory.clickType(windowPacket.getSlotId()) | mode | unpacked);
        }

        public static Inventory fromState(final int state) {
            for (Inventory inventory : Inventory.values()) {
                if (inventory.matches(state)) {
                    return inventory;
                }
            }
            return Inventory.INVENTORY;
        }

        private static int clickType(int slotId) {
            return (slotId == MAGIC_CLICK_OUTSIDE_SURVIVAL || slotId == MAGIC_CLICK_OUTSIDE_CREATIVE) ? CLICK_OUTSIDE_WINDOW : CLICK_INSIDE_WINDOW;
        }

    }

    public enum General implements ISpawnableState, IPacketState {
        UNKNOWN,
        MOVEMENT,
        INTERACTION() {
            @Override
            public boolean canSwitchTo(IPhaseState state) {
                return state == BlockPhase.State.BLOCK_DECAY;
            }
        },
        IGNORED,
        INTERACT_ENTITY,
        ATTACK_ENTITY() {
            @Override
            public void populateContext(EntityPlayerMP playerMP, Packet<?> packet, PhaseContext context) {
                final C02PacketUseEntity useEntityPacket = (C02PacketUseEntity) packet;
                net.minecraft.entity.Entity entity = useEntityPacket.getEntityFromWorld(playerMP.worldObj);
                context.add(NamedCause.of(TrackingHelper.TARGETED_ENTITY, entity));
                context.add(NamedCause.of(TrackingHelper.TRACKED_ENTITY_ID, entity.getEntityId()));
            }
        },
        INTERACT_AT_ENTITY,
        CHAT() {
            @Override
            public void populateContext(EntityPlayerMP playerMP, Packet<?> packet, PhaseContext context) {
                C01PacketChatMessage chatMessage = (C01PacketChatMessage) packet;
                if (chatMessage.getMessage().contains("kill")) {
                    context.add(NamedCause.of(TrackingHelper.DESTRUCT_ITEM_DROPS, true));
                }
            }
        },
        CREATIVE_INVENTORY,
        PLACE_BLOCK() {
            @Override
            public void populateContext(EntityPlayerMP playerMP, Packet<?> packet, PhaseContext context) {
                final C08PacketPlayerBlockPlacement placeBlock = (C08PacketPlayerBlockPlacement) packet;
                context.add(NamedCause.of(TrackingHelper.ITEM_USED, ItemStackUtil.cloneDefensive(placeBlock.getStack())));
                context.add(NamedCause.of(TrackingHelper.PLACED_BLOCK_POSITION, placeBlock.getPosition()));
            }
        },
        OPEN_INVENTORY, REQUEST_RESPAWN;

        @Override
        public PacketPhase getPhase() {
            return TrackingPhases.PACKET;
        }

        @Override
        public boolean isBusy() {
            return true;
        }

        @Nullable
        @Override
        public SpawnEntityEvent createSpawnEventPostProcess(Cause cause, CauseTracker causeTracker, PhaseContext phaseContext,
                List<EntitySnapshot> entitySnapshots) {
            return null;
        }

        @Override
        public boolean matches(int packetState) {
            return false;
        }

    }

    private final Map<Class<? extends Packet<?>>, Function<Packet<?>, IPacketState>> packetTranslationMap = new IdentityHashMap<>();
    private final Map<Class<? extends Packet<?>>, PacketFunction> packetUnwindMap = new IdentityHashMap<>();

    @SuppressWarnings("unchecked")
    public IPacketState getStateForPacket(Packet<?> packet) {
        final Class<? extends Packet<?>> packetClass = (Class<? extends Packet<?>>) packet.getClass();
        final Function<Packet<?>, IPacketState> packetStateFunction = this.packetTranslationMap.get(packetClass);
        if (packetStateFunction != null) {
            return packetStateFunction.apply(packet);
        }
        return General.UNKNOWN;
    }

    public PhaseContext populateContext(Packet<?> packet, EntityPlayerMP entityPlayerMP, IPhaseState state, PhaseContext context) {
        checkNotNull(packet, "Packet cannot be null!");
        checkArgument(!context.isComplete(), "PhaseContext cannot be marked as completed!");
        ((IPacketState) state).populateContext(entityPlayerMP, packet, context);
        return context;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void unwind(CauseTracker causeTracker, IPhaseState phaseState, PhaseContext phaseContext) {
        final Packet<?> packetIn = phaseContext.firstNamed(TrackingHelper.CAPTURED_PACKET, Packet.class).get();
        final EntityPlayerMP player = phaseContext.firstNamed(TrackingHelper.PACKET_PLAYER, EntityPlayerMP.class).get();
        final Class<? extends Packet<?>> packetInClass = (Class<? extends Packet<?>>) packetIn.getClass();
        final PacketFunction unwindFunction = this.packetUnwindMap.get(packetInClass);
        checkArgument(phaseState instanceof IPacketState, "PhaseState passed in is not an instance of IPacketState! Got %s", phaseState);
        if (unwindFunction != null) {
            unwindFunction.unwind(packetIn, (IPacketState) phaseState, player, phaseContext);
        } else {
            System.err.printf("Encountered an unmapped packet! Please fix! Packet: %s%n. PhaseContext: %s%n", packetIn, phaseContext);
            SpongeImpl.getLogger().error("Shit happened yo.");
        }
    }

    public PacketPhase(TrackingPhase parent) {
        super(parent);
        this.packetTranslationMap.put(C01PacketChatMessage.class, packet -> General.CHAT);
        this.packetTranslationMap.put(C02PacketUseEntity.class, packet -> {
            final C02PacketUseEntity useEntityPacket = (C02PacketUseEntity) packet;
            final C02PacketUseEntity.Action action = useEntityPacket.getAction();
            if ( action == C02PacketUseEntity.Action.INTERACT) {
                return General.INTERACT_ENTITY;
            } else if (action == C02PacketUseEntity.Action.ATTACK) {
                return General.ATTACK_ENTITY;
            } else if (action == C02PacketUseEntity.Action.INTERACT_AT) {
                return General.INTERACT_AT_ENTITY;
            } else {
                return General.UNKNOWN;
            }
        });
        this.packetTranslationMap.put(C07PacketPlayerDigging.class, packet -> {
            final C07PacketPlayerDigging playerDigging = (C07PacketPlayerDigging) packet;
            final C07PacketPlayerDigging.Action action = playerDigging.getStatus();
            final IPacketState state = INTERACTION_ACTION_MAPPINGS.get(action);
            return state == null ? General.IGNORED : state;
        });
        this.packetTranslationMap.put(C08PacketPlayerBlockPlacement.class, packet -> General.PLACE_BLOCK);
        this.packetTranslationMap.put(C10PacketCreativeInventoryAction.class, packet -> General.CREATIVE_INVENTORY);
        this.packetTranslationMap.put(C0EPacketClickWindow.class, packet -> Inventory.fromWindowPacket((C0EPacketClickWindow) packet));
        this.packetTranslationMap.put(C16PacketClientStatus.class, packet -> {
            final C16PacketClientStatus clientStatus = (C16PacketClientStatus) packet;
            final C16PacketClientStatus.EnumState status = clientStatus.getStatus();
            if ( status == C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT) {
                return General.OPEN_INVENTORY;
            } else if ( status == C16PacketClientStatus.EnumState.PERFORM_RESPAWN) {
                return General.REQUEST_RESPAWN;
            } else {
                return General.IGNORED;
            }
        });

        this.packetUnwindMap.put(C02PacketUseEntity.class, PacketFunction.USE_ENTITY);
        this.packetUnwindMap.put(C07PacketPlayerDigging.class, PacketFunction.ACTION);
        this.packetUnwindMap.put(C10PacketCreativeInventoryAction.class, PacketFunction.CREATIVE);
        this.packetUnwindMap.put(C0EPacketClickWindow.class, PacketFunction.INVENTORY);

    }


    public static final ImmutableMap<C07PacketPlayerDigging.Action, IPacketState> INTERACTION_ACTION_MAPPINGS = ImmutableMap.<C07PacketPlayerDigging.Action, IPacketState>builder()
            .put(C07PacketPlayerDigging.Action.DROP_ITEM, Inventory.DROP_ITEM)
            .put(C07PacketPlayerDigging.Action.DROP_ALL_ITEMS, Inventory.DROP_INVENTORY)
            .put(C07PacketPlayerDigging.Action.START_DESTROY_BLOCK, General.INTERACTION)
            .put(C07PacketPlayerDigging.Action.ABORT_DESTROY_BLOCK, General.INTERACTION)
            .put(C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK, General.INTERACTION)
            .put(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, General.INTERACTION)
            .build();
    @Override
    public boolean requiresBlockCapturing(IPhaseState currentState) {
        return false;
    }

}