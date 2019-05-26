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
package org.apache.ivyde.eclipse.resolvevisualizer;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import org.apache.ivyde.eclipse.resolvevisualizer.model.IIvyNodeElementFilter;
import org.apache.ivyde.eclipse.resolvevisualizer.model.IvyNodeElement;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.zest.core.viewers.IGraphEntityContentProvider;

public class ResolveVisualizerContentProvider implements IGraphEntityContentProvider {
    final Collection<IIvyNodeElementFilter> filters = new HashSet<>();

    // Returns all entities that should be linked with the given entity
    public Object[] getConnectedTo(Object entity) {
        return filter(((IvyNodeElement) entity).getDependencies());
    }

    public Object[] getElements(Object inputElement) {
        if (inputElement == null) {
            return new Object[] {};
        } else {
            IvyNodeElement inputNode = (IvyNodeElement) inputElement;
            List<IvyNodeElement> elements = Arrays.asList(filter(inputNode.getDeepDependencies()));
            Collections.sort(elements, new IvyNodeElementComparator());
            return elements.toArray();
        }
    }

    public IvyNodeElement[] filter(IvyNodeElement[] deepDependencies) {
        IvyNodeElement[] filtered = deepDependencies;
        for (IIvyNodeElementFilter filter : filters) {
            filtered = filter.filter(filtered); // I love this line
        }

        return filtered;
    }

    public void addFilter(IIvyNodeElementFilter filter) {
        filters.add(filter);
    }

    public void dispose() {
        // nothing to dispose
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }

    private class IvyNodeElementComparator implements Comparator<IvyNodeElement> {
        public int compare(IvyNodeElement element1, IvyNodeElement element2) {
            if (element1.getDepth() > element2.getDepth()) {
                return -1;
            } else if (element1.getDepth() < element2.getDepth()) {
                return 1;
            }

            return element1.getModuleRevisionId().toString().compareTo(element2.getModuleRevisionId().toString());
        }
    }
}
