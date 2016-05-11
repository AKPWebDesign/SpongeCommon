/*
 * Copyright (c) 2016 VoxelBox // Imagiverse <http://imagiver.se>.
 * All Rights Reserved.
 */
package org.spongepowered.common.registry.type.world;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import org.spongepowered.api.registry.util.RegisterCatalog;
import org.spongepowered.api.world.schematic.PaletteType;
import org.spongepowered.api.world.schematic.PaletteTypes;
import org.spongepowered.common.registry.SpongeAdditionalCatalogRegistryModule;
import org.spongepowered.common.world.schematic.BimapPalette;
import org.spongepowered.common.world.schematic.GlobalPalette;
import org.spongepowered.common.world.schematic.SpongePaletteType;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class PaletteTypeRegistryModule implements SpongeAdditionalCatalogRegistryModule<PaletteType> {

    @RegisterCatalog(PaletteTypes.class) private final Map<String, PaletteType> paletteMappings = Maps.newHashMap();

    @Override
    public void registerAdditionalCatalog(PaletteType extraCatalog) {
        checkNotNull(extraCatalog);
        String id = extraCatalog.getId();
        checkArgument(id.indexOf(' ') == -1, "Palette Type ID " + id + " may not contain a space");
        this.paletteMappings.put(id.toLowerCase(Locale.ENGLISH), extraCatalog);
    }

    @Override
    public Optional<PaletteType> getById(String id) {
        return Optional.ofNullable(this.paletteMappings.get(id));
    }

    @Override
    public Collection<PaletteType> getAll() {
        return ImmutableList.copyOf(this.paletteMappings.values());
    }

    @Override
    public void registerDefaults() {
        registerAdditionalCatalog(new SpongePaletteType("global", () -> GlobalPalette.instance));
        registerAdditionalCatalog(new SpongePaletteType("local", BimapPalette::new));
    }

    @Override
    public boolean allowsApiRegistration() {
        return true;
    }

}
