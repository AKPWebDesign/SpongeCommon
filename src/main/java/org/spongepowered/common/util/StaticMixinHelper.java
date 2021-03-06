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
package org.spongepowered.common.util;

import com.google.common.collect.ImmutableList;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import org.spongepowered.api.world.gen.PopulatorType;

import java.util.Arrays;
import java.util.UUID;

public class StaticMixinHelper {

    public static final ImmutableList<EnumFacing> VALID_HANGING_FACES = ImmutableList.copyOf(Arrays.asList(EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.EAST, EnumFacing.WEST));
    public static final BlockPos HANGING_OFFSET_EAST = new BlockPos(1, 1, 0);
    public static final BlockPos HANGING_OFFSET_WEST = new BlockPos(-1, 1, 0);
    public static final BlockPos HANGING_OFFSET_NORTH = new BlockPos(0, 1, -1);
    public static final BlockPos HANGING_OFFSET_SOUTH = new BlockPos(0, 1, 1);
    public static EntityPlayerMP packetPlayer = null;
    public static boolean processingInternalForgeEvent = false;
    // Set before firing an internal Forge BlockBreak event to handle extended blockstate
    public static IBlockState breakEventExtendedState = null;
    @SuppressWarnings("rawtypes")
    public static Class lastPopulatorClass = null;
    public static EntityLivingBase currentTargetEntity = null;
    public static PopulatorType runningGenerator = null;
    public static long lastInventoryOpenPacketTimeStamp = 0;
    public static boolean convertingMapFormat = false;
    public static UUID INVALID_WORLD_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    public static boolean setCustomNameTagSkip = false;

    // For spawning
    public static boolean gettingSpawnList;
    public static boolean structureSpawning;
    public static boolean dispenserDispensing;

    // For animation packet
    public static int lastAnimationPacketTick = 0;
    public static int lastSecondaryPacketTick = 0;
    public static int lastPrimaryPacketTick = 0;
    public static EntityPlayerMP lastAnimationPlayer = null;
}
