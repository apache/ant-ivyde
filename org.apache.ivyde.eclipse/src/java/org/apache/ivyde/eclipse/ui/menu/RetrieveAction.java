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

import org.apache.ivy.Ivy;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivyde.eclipse.IvyDEException;
import org.apache.ivyde.eclipse.IvyMarkerManager;
import org.apache.ivyde.eclipse.IvyPlugin;
import org.apache.ivyde.eclipse.retrieve.IvyRetriever;
import org.apache.ivyde.eclipse.retrieve.StandaloneRetrieveSetup;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.Action;

public class RetrieveAction extends Action {

    private final StandaloneRetrieveSetup retrieveSetup;

    public RetrieveAction(StandaloneRetrieveSetup retrieveSetup) {
        this.retrieveSetup = retrieveSetup;
    }

    public void run() {
        Ivy ivy;
        ModuleDescriptor md;
        try {
            ivy = retrieveSetup.getState().getCachedIvy();
            md = retrieveSetup.getState().getCachedModuleDescriptor();
        } catch (IvyDEException e) {
            e.log(IStatus.ERROR, null);
            return;
        }
        IvyRetriever retriever = new IvyRetriever(ivy, md, false, new NullProgressMonitor(),
                retrieveSetup);
        IStatus status = retriever.resolve();
        IvyMarkerManager ivyMarkerManager = IvyPlugin.getDefault().getIvyMarkerManager();
        ivyMarkerManager.setResolveStatus(status, retrieveSetup.getProject(),
            retrieveSetup.getIvyXmlPath());
        if (status.isOK() || status.getCode() == IStatus.CANCEL) {
            IvyPlugin.log(IStatus.INFO, "Successful retrieve of '" + retrieveSetup.getName()
                    + "' in " + retrieveSetup.getProject().getName(), null);
            return;
        }
        IvyPlugin.log(status);
    }
}
