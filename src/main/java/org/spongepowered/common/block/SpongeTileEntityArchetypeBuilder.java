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
package org.spongepowered.common.block;

import static com.google.common.base.Preconditions.checkState;

import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.block.tileentity.TileEntityArchetype;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.tileentity.TileEntityType;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.manipulator.DataManipulator;
import org.spongepowered.api.data.persistence.AbstractDataBuilder;
import org.spongepowered.api.data.persistence.DataBuilder;
import org.spongepowered.api.data.persistence.InvalidDataException;
import org.spongepowered.api.data.value.BaseValue;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.common.data.util.DataVersions;

import java.util.Optional;

public class SpongeTileEntityArchetypeBuilder extends AbstractDataBuilder<TileEntityArchetype> implements TileEntityArchetype.Builder {

    BlockState blockState;
    TileEntityType tileEntityType;
    DataContainer tileData;

    public SpongeTileEntityArchetypeBuilder() {
        super(TileEntityArchetype.class, DataVersions.TileEntitArchetype.BASE_VERSION);
    }

    @Override
    public DataBuilder<TileEntityArchetype> reset() {
        this.blockState = null;
        this.tileEntityType = null;
        this.tileData = null;
        return this;
    }

    @Override
    public DataBuilder<TileEntityArchetype> from(TileEntityArchetype value) {
        this.tileEntityType = value.getTileEntityType();
        this.blockState = value.getState();
        this.tileData = value.getTileData();
        return this;
    }

    @Override
    public TileEntityArchetype.Builder state(BlockState state) {
        return this;
    }

    @Override
    public TileEntityArchetype.Builder tile(TileEntityType tileEntityType) {
        return this;
    }

    @Override
    public TileEntityArchetype.Builder from(Location<World> location) {
        return this;
    }

    @Override
    public TileEntityArchetype.Builder tile(TileEntity tileEntity) {
        return this;
    }

    @Override
    public TileEntityArchetype.Builder tileData(DataView dataView) {
        return this;
    }

    @Override
    public TileEntityArchetype.Builder setData(DataManipulator<?, ?> manipulator) {
        return this;
    }

    @Override
    public <E, V extends BaseValue<E>> TileEntityArchetype.Builder set(V value) {
        return this;
    }

    @Override
    public <E, V extends BaseValue<E>> TileEntityArchetype.Builder set(Key<V> key, E value) {
        return this;
    }

    @Override
    public TileEntityArchetype.Builder raw(DataView dataView) {
        return this;
    }

    @Override
    public TileEntityArchetype build() {
        checkState(this.blockState != null, "BlockState cannot be null!");
        checkState(this.tileEntityType != null, "TileEntityType cannot be null!");
        checkState(this.tileData != null, "TileEntity data cannot be null!");
        checkState(!this.tileData.isEmpty(), "TileEntity data cannot be empty!");
        return new SpongeTileEntityArchetype(this);
    }



    @Override
    protected Optional<TileEntityArchetype> buildContent(DataView container) throws InvalidDataException {
        return Optional.empty();
    }

}
