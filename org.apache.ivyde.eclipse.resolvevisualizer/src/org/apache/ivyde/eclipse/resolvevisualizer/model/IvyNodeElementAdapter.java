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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.IvyNode;
import org.apache.ivy.core.resolve.IvyNodeCallers.Caller;

public class IvyNodeElementAdapter {
    /**
     * Adapt all dependencies and evictions from the ResolveReport.
     *
     * @param report ResolveReport
     * @return the root node adapted from the ResolveReport
     */
    public static IvyNodeElement adapt(ResolveReport report) {
        Map<ModuleRevisionId, IvyNodeElement> resolvedNodes = new HashMap<>();

        IvyNodeElement root = new IvyNodeElement();
        root.setModuleRevisionId(report.getModuleDescriptor().getModuleRevisionId());
        resolvedNodes.put(report.getModuleDescriptor().getModuleRevisionId(), root);

        List<IvyNode> dependencies = report.getDependencies();

        // First pass - build the map of resolved nodes by revision id
        for (IvyNode node : dependencies) {
            if (node.getAllEvictingNodes() != null) {
                // Nodes that are evicted as a result of conf inheritance still appear
                // as dependencies, but with eviction data. They also appear as evictions.
                // We map them as evictions rather than dependencies.
                continue;
            }
            IvyNodeElement nodeElement = new IvyNodeElement();
            nodeElement.setModuleRevisionId(node.getResolvedId());
            resolvedNodes.put(node.getResolvedId(), nodeElement);
        }

        // Second pass - establish relationships between the resolved nodes
        for (IvyNode node : dependencies) {
            if (node.getAllEvictingNodes() != null) {
                continue; // see note above
            }

            IvyNodeElement nodeElement = resolvedNodes.get(node.getResolvedId());
            for (Caller call : node.getAllRealCallers()) {
                IvyNodeElement caller = resolvedNodes.get(call.getModuleRevisionId());
                if (caller != null) {
                    nodeElement.addCaller(caller);
                    nodeElement.setCallerConfigurations(caller, call.getCallerConfigurations());
                }
            }
        }

        for (IvyNode eviction : report.getEvictedNodes()) {
            IvyNodeElement evictionElement = new IvyNodeElement();
            evictionElement.setModuleRevisionId(eviction.getResolvedId());
            evictionElement.setEvicted(true);

            for (Caller call : eviction.getAllCallers()) {
                IvyNodeElement caller = resolvedNodes.get(call.getModuleRevisionId());
                if (caller != null) {
                    evictionElement.addCaller(caller);
                    evictionElement.setCallerConfigurations(caller, call.getCallerConfigurations());
                }
            }
        }

        // Recursively set depth starting at root
        root.setDepth(0);
        findConflictsBeneathNode(root);

        return root;
    }

    /**
     * Derives configuration conflicts that exist between node and all of its descendant dependencies.
     *
     * @param node IvyNodeElement
     */
    private static void findConflictsBeneathNode(IvyNodeElement node) {
        // Derive conflicts
        Map<ModuleId, Collection<IvyNodeElement>> moduleRevisionMap = new HashMap<>();
        for (IvyNodeElement deepDependency : node.getDeepDependencies()) {
            if (deepDependency.isEvicted()) {
                continue;
            }

            ModuleId moduleId = deepDependency.getModuleRevisionId().getModuleId();
            if (moduleRevisionMap.containsKey(moduleId)) {
                Collection<IvyNodeElement> conflicts = moduleRevisionMap.get(moduleId);
                conflicts.add(deepDependency);
                for (IvyNodeElement conflict : conflicts) {
                    conflict.setConflicts(conflicts);
                }
            } else {
                List<IvyNodeElement> immutableMatchingSet = Collections.singletonList(deepDependency);
                moduleRevisionMap.put(moduleId, new HashSet<>(immutableMatchingSet));
            }
        }
    }
}
