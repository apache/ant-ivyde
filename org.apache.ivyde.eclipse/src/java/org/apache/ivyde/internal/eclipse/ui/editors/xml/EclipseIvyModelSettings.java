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
package org.apache.ivyde.internal.eclipse.ui.editors.xml;

import java.util.List;

import org.apache.ivy.Ivy;
import org.apache.ivyde.common.model.IvyModelSettings;
import org.apache.ivyde.eclipse.cp.IvyClasspathContainer;
import org.apache.ivyde.eclipse.cp.IvyClasspathContainerHelper;
import org.apache.ivyde.internal.eclipse.IvyPlugin;
import org.apache.ivyde.internal.eclipse.cpcontainer.IvyClasspathContainerImpl;
import org.apache.ivyde.internal.eclipse.ui.preferences.PreferenceConstants;
import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.IJavaProject;

public class EclipseIvyModelSettings implements IvyModelSettings {

    private final IvyClasspathContainerImpl ivycp;

    public EclipseIvyModelSettings(IJavaProject javaProject) {
        this(IvyClasspathContainerHelper.getContainers(javaProject));
    }

    public EclipseIvyModelSettings(IFile ivyfile) {
        this(IvyClasspathContainerHelper.getContainersFromIvyFile(ivyfile));
    }

    private EclipseIvyModelSettings(List<IvyClasspathContainer> containers) {
        this(containers.isEmpty() ? null : (IvyClasspathContainerImpl) containers.get(0));
    }

    private EclipseIvyModelSettings(IvyClasspathContainerImpl ivycp) {
        this.ivycp = ivycp;
    }

    public String getDefaultOrganization() {
        return IvyPlugin.getDefault().getPreferenceStore().getString(
            PreferenceConstants.ORGANISATION);
    }

    public String getDefaultOrganizationURL() {
        return IvyPlugin.getDefault().getPreferenceStore().getString(
            PreferenceConstants.ORGANISATION_URL);
    }

    public Ivy getIvyInstance() {
        if (ivycp == null) {
            return null;
        }
        return ivycp.getState().getCachedIvy();
    }

    public void logError(String message, Exception e) {
        IvyPlugin.logError(message, e);
    }

}
