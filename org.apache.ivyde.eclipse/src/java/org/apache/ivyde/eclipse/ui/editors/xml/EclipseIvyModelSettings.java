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
package org.apache.ivyde.eclipse.ui.editors.xml;

import org.apache.ivy.Ivy;
import org.apache.ivyde.common.model.IvyModelSettings;
import org.apache.ivyde.eclipse.IvyDEException;
import org.apache.ivyde.eclipse.IvyPlugin;
import org.apache.ivyde.eclipse.cpcontainer.IvyClasspathContainer;
import org.apache.ivyde.eclipse.cpcontainer.IvyClasspathUtil;
import org.apache.ivyde.eclipse.ui.preferences.PreferenceConstants;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;

public class EclipseIvyModelSettings implements IvyModelSettings {
    
    private IJavaProject javaProject;
    private IvyClasspathContainer cp;
    
    public EclipseIvyModelSettings(IJavaProject javaProject) {
        this.javaProject = javaProject;
        cp = IvyClasspathUtil.getIvyClasspathContainer(javaProject);
    }

    public String getDefaultOrganization() {
        return IvyPlugin.getDefault().getPreferenceStore()
                        .getString(PreferenceConstants.ORGANISATION);
    }

    public String getDefaultOrganizationURL() {
        return IvyPlugin.getDefault().getPreferenceStore()
            .getString(PreferenceConstants.ORGANISATION_URL);
    }

    public Ivy getIvyInstance() {
        if (cp == null) {
            return null;
        }
        try {
            return cp.getConf().getIvy();
        } catch (IvyDEException e) {
            e.log(IStatus.WARNING, null);
            return null;
        }
    }

    public void logError(String message, Exception e) {
        IvyPlugin.log(Status.ERROR, message, e);
    }

}
