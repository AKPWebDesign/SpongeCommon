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
package org.spongepowered.common.data;

import static com.google.common.base.Preconditions.checkNotNull;

import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.api.CatalogType;
import org.spongepowered.api.data.Archetype;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.LocateableSnapshot;
import org.spongepowered.api.data.Property;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.manipulator.DataManipulator;
import org.spongepowered.api.data.merge.MergeFunction;
import org.spongepowered.api.data.persistence.InvalidDataException;
import org.spongepowered.api.data.property.PropertyStore;
import org.spongepowered.api.data.value.BaseValue;
import org.spongepowered.api.data.value.immutable.ImmutableValue;
import org.spongepowered.common.data.nbt.NbtDataProcessor;
import org.spongepowered.common.data.nbt.NbtDataType;
import org.spongepowered.common.data.persistence.NbtTranslator;
import org.spongepowered.common.data.nbt.validation.ValidationType;
import org.spongepowered.common.data.property.SpongePropertyRegistry;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public abstract class AbstractArchetype<C extends CatalogType, S extends LocateableSnapshot<S>> implements Archetype<S> {

    protected final C type;
    protected NBTTagCompound data;

    protected AbstractArchetype(C type, NBTTagCompound data) {
        this.type = type;
        this.data = data;
    }

    protected abstract NbtDataType getDataType();

    protected abstract ValidationType getValidationType();

    @Override
    public boolean validateRawData(DataView container) {
        return false;
    }

    @Override
    public void setRawData(DataView container) throws InvalidDataException {
        checkNotNull(container, "Raw data cannot be null!");
        final NBTTagCompound copy = NbtTranslator.getInstance().translateData(container);
        SpongeDataManager.getInstance().getValidators(this.getValidationType()).validate(copy);
        this.data = copy;
    }

    @Override
    public <T extends Property<?, ?>> Optional<T> getProperty(Class<T> propertyClass) {
        final Optional<PropertyStore<T>> optional = SpongePropertyRegistry.getInstance().getStore(propertyClass);
        if (optional.isPresent()) {
            return optional.get().getFor(this);
        }
        return Optional.empty();
    }

    @Override
    public Collection<Property<?, ?>> getApplicableProperties() {
        return SpongePropertyRegistry.getInstance().getPropertiesFor(this);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends DataManipulator<?, ?>> Optional<T> get(Class<T> containerClass) {
        return SpongeDataManager.getInstance().getRawNbtProcessor(this.getDataType(), containerClass)
                .flatMap(processor -> processor.readFromCompound(this.data));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends DataManipulator<?, ?>> Optional<T> getOrCreate(Class<T> containerClass) {
        return SpongeDataManager.getInstance().getRawNbtProcessor(this.getDataType(), containerClass)
                .flatMap(processor -> processor.readFromCompound(this.data));
    }

    @Override
    public boolean supports(Class<? extends DataManipulator<?, ?>> holderClass) {
        // By default, if there is a processor, we can check compatibilty with that
        // Otherwise, it's true because of custom data.
        return SpongeDataManager.getInstance().getRawNbtProcessor(this.getDataType(), holderClass)
                .map(processor -> processor.isCompatible(this.getDataType()))
                .orElse(true);
    }

    @Override
    public <E> DataTransactionResult offer(Key<? extends BaseValue<E>> key, E value) {
        return SpongeDataManager.getInstance().getNbtProcessor(this.getDataType(), key)
                .map(processor -> processor.offer(this, value))
                .orElse(DataTransactionResult.failNoData());
    }

    @Override
    public DataTransactionResult offer(DataManipulator<?, ?> valueContainer, MergeFunction function) {
        return SpongeDataManager.getInstance().getRawNbtProcessor(this.getDataType(), valueContainer.getClass())
                .map(processor -> {
                    Optional<DataManipulator<?, ?>> manipulator = processor.readFromCompound(this.data);
                    DataManipulator<?, ?> newManipulator;
                    if (manipulator.isPresent()) {
                        newManipulator = function.merge(manipulator.get(), valueContainer);
                    } else {
                        newManipulator = valueContainer;
                    }
                    final Optional<NBTTagCompound> optional = processor.storeToCompound(this.data, newManipulator);
                    if (optional.isPresent()) {
                        this.data = optional.get();
                    }
                    return DataTransactionResult.failNoData();

                })
                .orElse(DataTransactionResult.failNoData());
    }

    @Override
    public DataTransactionResult remove(Class<? extends DataManipulator<?, ?>> containerClass) {
        return null;
    }

    @Override
    public DataTransactionResult remove(Key<?> key) {
        return null;
    }

    @Override
    public DataTransactionResult undo(DataTransactionResult result) {
        return null;
    }

    @Override
    public DataTransactionResult copyFrom(DataHolder that, MergeFunction function) {
        return null;
    }

    @Override
    public Collection<DataManipulator<?, ?>> getContainers() {
        return null;
    }

    @Override
    public <E> Optional<E> get(Key<? extends BaseValue<E>> key) {
        return null;
    }

    @Override
    public <E, V extends BaseValue<E>> Optional<V> getValue(Key<V> key) {
        return null;
    }

    @Override
    public boolean supports(Key<?> key) {
        return false;
    }

    @Override
    public Set<Key<?>> getKeys() {
        return null;
    }

    @Override
    public Set<ImmutableValue<?>> getValues() {
        return null;
    }
}
