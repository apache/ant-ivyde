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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.ivy.core.module.id.ModuleRevisionId;

/**
 * Assists in the further separation of concerns between the view and the Ivy resolve report. The view looks at the
 * IvyNode in a unique way that can lead to expensive operations if we do not achieve this separation.
 */
public class IvyNodeElement {
    private ModuleRevisionId moduleRevisionId;
    private boolean evicted = false;
    private int depth = Integer.MAX_VALUE / 10;
    private final Collection<IvyNodeElement> dependencies = new HashSet<>();
    private final Collection<IvyNodeElement> callers = new HashSet<>();
    private Collection<IvyNodeElement> conflicts = new HashSet<>();
    private int hash;

    /**
     * The caller configurations that caused this node to be reached in the resolution, grouped by caller.
     */
    private final Map<IvyNodeElement, String[]> callerConfigurationMap = new HashMap<>();

    /**
     * We try to avoid building the list of this nodes deep dependencies by storing them in this cache by depth level.
     */
    private IvyNodeElement[] deepDependencyCache;

    public boolean equals(Object obj) {
        if (obj instanceof IvyNodeElement) {
            IvyNodeElement elem = (IvyNodeElement) obj;
            return elem.getOrganization().equals(getOrganization()) && elem.getName().equals(getName())
                    && elem.getRevision().equals(getRevision());
        }
        return false;
    }

    public int hashCode() {
        if (hash == 0) {
            // CheckStyle:MagicNumber| OFF
            hash = 31;
            hash = hash * 13 + getModuleRevisionId().hashCode();
            hash = hash * 13 + Arrays.hashCode(getDeepDependencies());
            hash = hash * 13 + Arrays.hashCode(getConflicts());
            hash = hash * 13 + Arrays.hashCode(getCallers());
            // CheckStyle:MagicNumber| ON
        }
        return hash;
    }

    public IvyNodeElement[] getDependencies() {
        return dependencies.toArray(new IvyNodeElement[dependencies.size()]);
    }

    /**
     * Recursive dependency retrieval.
     *
     * @return The array of nodes that represents a node's immediate and transitive dependencies down to an arbitrary
     *         depth.
     */
    public IvyNodeElement[] getDeepDependencies() {
        if (deepDependencyCache == null) {
            deepDependencyCache = getDeepDependencies(this).toArray(new IvyNodeElement[] {});
        }
        return deepDependencyCache;
    }

    /**
     * Recursive dependency retrieval
     *
     * @param node IvyNodeElement
     * @return Collection&lt;IvyNodeElement&gt;
     */
    private Collection<IvyNodeElement> getDeepDependencies(IvyNodeElement node) {
        Collection<IvyNodeElement> deepDependencies = new HashSet<>();
        deepDependencies.add(node);

        for (IvyNodeElement directDependency : node.getDependencies()) {
            deepDependencies.addAll(getDeepDependencies(directDependency));
        }

        return deepDependencies;
    }

    /**
     * @param caller IvyNodeElement
     * @return An array of configurations by which this module was resolved
     */
    public String[] getCallerConfigurations(IvyNodeElement caller) {
        return callerConfigurationMap.get(caller);
    }

    public void setCallerConfigurations(IvyNodeElement caller, String[] configurations) {
        callerConfigurationMap.put(caller, configurations);
    }

    public String getOrganization() {
        return moduleRevisionId.getOrganisation();
    }

    public String getName() {
        return moduleRevisionId.getName();
    }

    public String getRevision() {
        return moduleRevisionId.getRevision();
    }

    public boolean isEvicted() {
        return evicted;
    }

    public void setEvicted(boolean evicted) {
        this.evicted = evicted;
    }

    public int getDepth() {
        return depth;
    }

    /**
     * Set this node's depth and recursively update the node's children to relative to the new value.
     *
     * @param depth int
     */
    public void setDepth(int depth) {
        this.depth = depth;
        for (IvyNodeElement dependency : dependencies) {
            dependency.setDepth(depth + 1);
        }
    }

    public IvyNodeElement[] getConflicts() {
        return conflicts.toArray(new IvyNodeElement[conflicts.size()]);
    }

    public void setConflicts(Collection<IvyNodeElement> conflicts) {
        this.conflicts = conflicts;
    }

    public ModuleRevisionId getModuleRevisionId() {
        return moduleRevisionId;
    }

    public void setModuleRevisionId(ModuleRevisionId moduleRevisionId) {
        this.moduleRevisionId = moduleRevisionId;
    }

    public void addCaller(IvyNodeElement caller) {
        callers.add(caller);
        caller.dependencies.add(this);
    }

    public IvyNodeElement[] getCallers() {
        return callers.toArray(new IvyNodeElement[callers.size()]);
    }
}
