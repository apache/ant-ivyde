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

import java.util.Map;

import org.apache.ivyde.eclipse.resolvevisualizer.model.IvyNodeElement;
import org.eclipse.swt.graphics.Color;
import org.eclipse.zest.core.viewers.EntityConnectionData;

public class DirectDependenciesAlgorithm extends LabelDecoratorAlgorithmAdapter {
    public void calculateHighlighted(IvyNodeElement root, IvyNodeElement selected,
                                     Map<EntityConnectionData, ConnectionStyle> highlightRelationships,
                                     Map<IvyNodeElement, Color> highlightEntities) {
        if (selected != null) {
            highlightEntities.put(root, entityColor);
            for (IvyNodeElement dependency : root.getDependencies()) {
                highlightEntities.put(dependency, entityColor);
                highlightRelationships.put(new EntityConnectionData(root, dependency), relationshipColor);
            }
        }
    }
}
