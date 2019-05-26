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
package org.apache.ivyde.internal.eclipse.retrieve;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.ivyde.internal.eclipse.IvyPlugin;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ISaveContext;
import org.eclipse.core.resources.ISaveParticipant;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.osgi.service.prefs.BackingStoreException;

public class RetrieveSetupManager implements ISaveParticipant {

    private static final String PREF_ID = "org.apache.ivyde.eclipse.standaloneretrieve";

    private final Map<IProject, IEclipsePreferences> projectPrefs = new HashMap<>();

    public List<StandaloneRetrieveSetup> getSetup(IProject project) throws IOException {

        IEclipsePreferences pref;
        synchronized (projectPrefs) {
            pref = projectPrefs.get(project);
        }
        if (pref == null) {
            IScopeContext projectScope = new ProjectScope(project);
            pref = projectScope.getNode(IvyPlugin.ID);
        }
        String retrieveSetup = pref.get(PREF_ID, null);
        if (retrieveSetup == null) {
            return new ArrayList<>();
        }
        List<StandaloneRetrieveSetup> retrieveSetups;

        StandaloneRetrieveSerializer serializer = new StandaloneRetrieveSerializer();
        try (ByteArrayInputStream in = new ByteArrayInputStream(retrieveSetup.getBytes())) {
            retrieveSetups = serializer.read(in, project);
        }
        // we don't care
        return retrieveSetups;
    }

    public void save(final IProject project, List<StandaloneRetrieveSetup> retrieveSetups)
            throws IOException {
        StandaloneRetrieveSerializer serializer = new StandaloneRetrieveSerializer();
        final String retrieveSetup;
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            serializer.write(out, retrieveSetups);
            retrieveSetup = new String(out.toByteArray());
        }

        synchronized (projectPrefs) {
            IEclipsePreferences pref = projectPrefs.get(project);
            if (pref == null) {
                IScopeContext projectScope = new ProjectScope(project);
                pref = projectScope.getNode(IvyPlugin.ID);
                projectPrefs.put(project, pref);
            }
            pref.put(PREF_ID, retrieveSetup);
        }
    }

    public void prepareToSave(ISaveContext context) throws CoreException {
        // nothing to do
    }

    public void saving(ISaveContext context) throws CoreException {
        Map<IProject, IEclipsePreferences> toFlush;
        synchronized (projectPrefs) {
            toFlush = new HashMap<>(projectPrefs);
            projectPrefs.clear();
        }
        for (Entry<IProject, IEclipsePreferences> entry : toFlush.entrySet()) {
            try {
                entry.getValue().flush();
            } catch (BackingStoreException e) {
                IvyPlugin.logError("Failed to save the state of the Ivy preferences of "
                        + entry.getKey().getName(), e);
            }
        }
    }

    public void rollback(ISaveContext context) {
        // nothing to do
    }

    public void doneSaving(ISaveContext context) {
        // nothing to do
    }

}
