/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.ivyde.eclipse.resolvevisualizer.model;

import java.util.Collection;
import java.util.HashSet;

public abstract class IvyNodeElementFilterAdapter implements IIvyNodeElementFilter {
    protected boolean enabled = false;

    public IvyNodeElement[] filter(IvyNodeElement[] unfiltered) {
        if (!enabled) {
            return unfiltered;
        }

        Collection<IvyNodeElement> filtered = new HashSet<>();
        for (IvyNodeElement raw : unfiltered) {
            if (accept(raw)) {
                filtered.add(raw);
            }
        }

        return filtered.toArray(new IvyNodeElement[filtered.size()]);
    }

    public abstract boolean accept(IvyNodeElement unfiltered);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
