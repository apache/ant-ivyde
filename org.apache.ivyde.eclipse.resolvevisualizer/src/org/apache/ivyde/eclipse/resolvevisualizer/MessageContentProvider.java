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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivyde.eclipse.resolvevisualizer.model.IvyNodeElement;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.ui.forms.IMessageManager;

public class MessageContentProvider {
    private IMessageManager manager;

    /**
     * Called when the view selection changes and the message list needs to be rebuilt.
     *
     * @param root IvyNodeElement
     */
    public void selectionChanged(IvyNodeElement root) {
        if (root == null) {
            return;
        }
        manager.removeAllMessages();

        Map<ModuleId, Collection<IvyNodeElement>> conflicts = new HashMap<>();

        for (IvyNodeElement deepDependency : root.getDeepDependencies()) {
            if (deepDependency.getConflicts().length > 0) {
                Collection<IvyNodeElement> conflictParticipants = conflicts.get(deepDependency
                        .getModuleRevisionId().getModuleId());
                if (conflictParticipants == null) {
                    conflictParticipants = new HashSet<>();
                }
                conflictParticipants.add(deepDependency);
                conflicts.put(deepDependency.getModuleRevisionId().getModuleId(), conflictParticipants);
            }
        }

        for (Map.Entry<ModuleId, Collection<IvyNodeElement>> conflict : conflicts.entrySet()) {
            final ModuleId conflictKey = conflict.getKey();
            manager.addMessage(conflictKey,
                    "Conflict on module " + conflictKey.getOrganisation() + "#" + conflictKey.getName(),
                    conflict.getValue(), IMessageProvider.ERROR);
        }
    }

    public void setMessageManager(IMessageManager manager) {
        this.manager = manager;
    }
}
