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
package org.spongepowered.common.registry.type.event;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import org.spongepowered.api.event.cause.entity.teleport.TeleportType;
import org.spongepowered.api.event.cause.entity.teleport.TeleportTypes;
import org.spongepowered.api.registry.CatalogRegistryModule;
import org.spongepowered.api.registry.util.RegisterCatalog;
import org.spongepowered.common.event.entity.teleport.SpongeTeleportType;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class TeleportTypeRegistryModule implements CatalogRegistryModule<TeleportType> {

    public static TeleportTypeRegistryModule getInstance() {
        return Holder.INSTANCE;
    }

    @SuppressWarnings("serial")
    @RegisterCatalog(TeleportTypes.class)
    private final Map<String, TeleportType> teleportTypeMappings = new HashMap<String, TeleportType>() {{
        put("minecraft:command", TeleportTypes.COMMAND);
        put("minecraft:entity_teleport", TeleportTypes.ENTITY_TELEPORT);
        put("minecraft:portal", TeleportTypes.PORTAL);
        put("sponge:plugin", TeleportTypes.PLUGIN);
        put("sponge:unknown", TeleportTypes.UNKNOWN);
    }};

    @Override
    public Optional<TeleportType> getById(String id) {
        return Optional.ofNullable(this.teleportTypeMappings.get(checkNotNull(id).toLowerCase(Locale.ENGLISH)));
    }

    @Override
    public Collection<TeleportType> getAll() {
        return ImmutableList.copyOf(this.teleportTypeMappings.values());
    }

    private static final class Holder {
        static final TeleportTypeRegistryModule INSTANCE = new TeleportTypeRegistryModule();
    }
}
