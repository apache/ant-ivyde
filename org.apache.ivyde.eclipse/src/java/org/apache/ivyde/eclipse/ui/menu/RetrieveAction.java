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
package org.apache.ivyde.eclipse.ui.menu;

import java.util.List;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivyde.eclipse.IvyMarkerManager;
import org.apache.ivyde.eclipse.IvyPlugin;
import org.apache.ivyde.eclipse.cpcontainer.IvyClasspathUtil;
import org.apache.ivyde.eclipse.resolve.IvyResolver;
import org.apache.ivyde.eclipse.retrieve.RetrieveSetup;
import org.apache.ivyde.eclipse.retrieve.StandaloneRetrieveSetup;
import org.apache.ivyde.eclipse.retrieve.StandaloneRetrieveSetupState;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.Action;

public class RetrieveAction extends Action {

    private final StandaloneRetrieveSetup setup;

    public RetrieveAction(StandaloneRetrieveSetup retrieveSetup) {
        this.setup = retrieveSetup;
    }

    public void run() {
        StandaloneRetrieveSetupState state = setup.getState();
        Ivy ivy = state.getSafelyIvy();
        if (ivy == null) {
            return;
        }
        ModuleDescriptor md = state.getSafelyModuleDescriptor(ivy);
        if (md == null) {
            return;
        }
        RetrieveSetup retrieveSetup = setup.getRetrieveSetup();
        List confs = IvyClasspathUtil.split(retrieveSetup.getRetrieveConfs());
        IvyResolver resolver = new IvyResolver(ivy, setup.getIvyXmlPath(), md,
                new NullProgressMonitor(), confs, setup.getProject());
        IStatus status = resolver.resolve();
        IvyMarkerManager ivyMarkerManager = IvyPlugin.getDefault().getIvyMarkerManager();
        ivyMarkerManager.setResolveStatus(status, setup.getProject(), setup.getIvyXmlPath());
        if (status.isOK() || status.getCode() == IStatus.CANCEL) {
            IvyPlugin.log(IStatus.INFO, "Successful retrieve of '" + setup.getName() + "' in "
                    + setup.getProject().getName(), null);
            return;
        }
        IvyPlugin.log(status);
    }
}
