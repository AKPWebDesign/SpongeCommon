/*
 * Copyright (c) 2016 VoxelBox // Imagiverse <http://imagiver.se>.
 * All Rights Reserved.
 */
package org.spongepowered.common.data.persistence;

import com.google.common.reflect.TypeToken;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.persistence.DataTranslator;
import org.spongepowered.api.data.persistence.InvalidDataException;
import org.spongepowered.api.world.schematic.Schematic;

import java.util.Optional;

public class LegacySchematicTranslator implements DataTranslator<Schematic> {

    private static final LegacySchematicTranslator INSTANCE = new LegacySchematicTranslator();
    private static final TypeToken<Schematic> TYPE_TOKEN = TypeToken.of(Schematic.class);
    
    public static LegacySchematicTranslator get() {
        return INSTANCE;
    }
    
    private LegacySchematicTranslator() {
        
    }
    
    @Override
    public String getId() {
        return "sponge:legacy_schematic";
    }

    @Override
    public String getName() {
        return "Legacy Schematic translator";
    }

    @Override
    public TypeToken<Schematic> getToken() {
        return TYPE_TOKEN;
    }

    @Override
    public Optional<Schematic> translate(DataView view) throws InvalidDataException {
        return null;
    }

    @Override
    public DataContainer translate(Schematic obj) throws InvalidDataException {
        return null;
    }

}
