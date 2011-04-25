/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.Iterator;
import java.util.Map;

import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivyde.eclipse.resolvevisualizer.model.IvyNodeElement;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.ui.forms.IMessageManager;

public class MessageContentProvider {
    private IMessageManager manager;

    /**
     * Called when the view selection changes and the message list needs to be rebuilt.
     */
    public void selectionChanged(IvyNodeElement root) {
        if (root == null) {
            return;
        }
        manager.removeAllMessages();

        Map/* <ModuleId, Collection<IvyNodeElement>> */conflicts = new HashMap/* <ModuleId, Collection<IvyNodeElement>> */();

        IvyNodeElement[] deepDependencies = root.getDeepDependencies();
        for (int i = 0; i < deepDependencies.length; i++) {
            if (deepDependencies[i].getConflicts().length > 0) {
                Collection/* <IvyNodeElement> */conflictParticipants = (Collection) conflicts.get(deepDependencies[i]
                        .getModuleRevisionId().getModuleId());
                if (conflictParticipants == null)
                    conflictParticipants = new HashSet/* <IvyNodeElement> */();
                conflictParticipants.add(deepDependencies[i]);
                conflicts.put(deepDependencies[i].getModuleRevisionId().getModuleId(), conflictParticipants);
            }
        }

        for (Iterator conflictIter = conflicts.keySet().iterator(); conflictIter.hasNext();) {
            ModuleId conflictKey = (ModuleId) conflictIter.next();
            manager.addMessage(conflictKey,
                    "Conflict on module " + conflictKey.getOrganisation() + "#" + conflictKey.getName(),
                    conflicts.get(conflictKey), IMessageProvider.ERROR);
        }
    }

    public void setMessageManager(IMessageManager manager) {
        this.manager = manager;
    }
}
