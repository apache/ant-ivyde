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
package org.apache.ivyde.eclipse.resolvevisualizer.label;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.apache.ivyde.eclipse.resolvevisualizer.model.IvyNodeElement;
import org.eclipse.zest.core.viewers.EntityConnectionData;
import org.eclipse.swt.graphics.Color;

public class ShortestRootPathAlgorithm extends LabelDecoratorAlgorithmAdapter {
    public void calculateHighlighted(IvyNodeElement root, IvyNodeElement selected,
                                     Map<EntityConnectionData, ConnectionStyle> highlightRelationships,
                                     Map<IvyNodeElement, Color> highlightEntities) {
        // Calculates the smart path.
        if (selected != null) {
            IvyNodeElement[] path = getShortestPathToDescendent(root, selected);
            if (path.length > 1) {
                for (int i = 0; i < path.length - 1; i++) {
                    EntityConnectionData entityConnectionData = new EntityConnectionData(path[i + 1], path[i]);
                    highlightRelationships.put(entityConnectionData, relationshipColor);
                    highlightEntities.put(path[i], entityColor);
                }
                highlightEntities.put(path[path.length - 1], entityColor);
            }
            // highlightEntities.put(root, DEFAULT_ENTITY_HIGHLIGHT);
        }
    }

    public IvyNodeElement[] getShortestPathToDescendent(IvyNodeElement root, IvyNodeElement target) {
        LinkedList<IvyNodeElement> q = new LinkedList<>();
        Set<IvyNodeElement> orderedSet = new HashSet<>();
        LinkedList<IvyNodeElement> orderedList = new LinkedList<>();
        q.add(root);
        while (!q.isEmpty()) {
            IvyNodeElement head = q.remove(0);
            if (!orderedSet.contains(head)) {
                orderedSet.add(head);
                orderedList.add(head);
                q.addAll(Arrays.asList(head.getDependencies()));
            }
        }
        return fixedWeightDijkstraAlgorithm(orderedList, root, target);
    }

    private IvyNodeElement[] fixedWeightDijkstraAlgorithm(LinkedList<IvyNodeElement> q,
                                                          IvyNodeElement s, IvyNodeElement t) {
        HashMap<IvyNodeElement, IvyNodeElement> previous = new HashMap<>();
        HashMap<IvyNodeElement, Integer> dValues = new HashMap<>();
        for (IvyNodeElement e : q) {
            dValues.put(e, Integer.MAX_VALUE / 10);
        }
        dValues.put(s, 0);

        while (!q.isEmpty()) {
            IvyNodeElement head = q.remove(0);
            for (IvyNodeElement v : head.getDependencies()) {
                if (dValues.get(head) + 1 < dValues.get(v)) {
                    previous.put(v, head);
                    dValues.put(v, dValues.get(head) + 1);
                }
            }
        }
        LinkedList<IvyNodeElement> path = new LinkedList<>();
        IvyNodeElement currentNode = t;
        while (previous.containsKey(currentNode)) {
            path.add(currentNode);
            currentNode = previous.get(currentNode);
        }
        path.add(currentNode);
        return path.toArray(new IvyNodeElement[path.size()]);
    }
}
