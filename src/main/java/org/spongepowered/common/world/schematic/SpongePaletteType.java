/*
 * Copyright (c) 2016 VoxelBox // Imagiverse <http://imagiver.se>.
 * All Rights Reserved.
 */
package org.spongepowered.common.world.schematic;

import org.spongepowered.api.world.schematic.Palette;
import org.spongepowered.api.world.schematic.PaletteType;
import org.spongepowered.common.SpongeCatalogType;

import java.util.function.Supplier;

public class SpongePaletteType extends SpongeCatalogType implements PaletteType {

    private final Supplier<Palette> builder;

    public SpongePaletteType(String id, Supplier<Palette> builder) {
        super(id);
        this.builder = builder;
    }

    @Override
    public Palette create() {
        return this.builder.get();
    }

}
