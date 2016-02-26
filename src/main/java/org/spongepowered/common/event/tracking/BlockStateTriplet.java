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
package org.spongepowered.common.event.tracking;

import com.flowpowered.math.vector.Vector3i;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.data.Transaction;

import java.util.LinkedHashMap;

import javax.annotation.Nullable;

public final class BlockStateTriplet {

    @Nullable private final LinkedHashMap<Vector3i, Transaction<BlockSnapshot>> populatorList;
    @Nullable private final BlockSnapshot blockSnapshot;
    @Nullable private final Transaction<BlockSnapshot> transaction;

    public BlockStateTriplet(@Nullable LinkedHashMap<Vector3i, Transaction<BlockSnapshot>> populatorList,
            @Nullable BlockSnapshot blockSnapshot, @Nullable Transaction<BlockSnapshot> transaction) {
        this.populatorList = populatorList;
        this.blockSnapshot = blockSnapshot;
        this.transaction = transaction;
    }

    @Nullable
    public LinkedHashMap<Vector3i, Transaction<BlockSnapshot>> getPopulatorList() {
        return this.populatorList;
    }

    @Nullable
    public BlockSnapshot getBlockSnapshot() {
        return this.blockSnapshot;
    }

    @Nullable
    public Transaction<BlockSnapshot> getTransaction() {
        return this.transaction;
    }
}