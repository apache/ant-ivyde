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
package org.apache.ivyde.internal.eclipse.ui.menu;

import java.util.List;

import org.apache.ivyde.eclipse.cp.RetrieveSetup;
import org.apache.ivyde.internal.eclipse.IvyPlugin;
import org.apache.ivyde.internal.eclipse.cpcontainer.IvyClasspathUtil;
import org.apache.ivyde.internal.eclipse.resolve.IvyResolveJob;
import org.apache.ivyde.internal.eclipse.resolve.IvyResolver;
import org.apache.ivyde.internal.eclipse.resolve.ResolveRequest;
import org.apache.ivyde.internal.eclipse.retrieve.StandaloneRetrieveSetup;
import org.eclipse.jface.action.Action;

public class RetrieveAction extends Action {

    private final StandaloneRetrieveSetup setup;

    private final IvyResolveJob ivyResolveJob;

    public RetrieveAction(StandaloneRetrieveSetup retrieveSetup) {
        this.setup = retrieveSetup;
        ivyResolveJob = IvyPlugin.getDefault().getIvyResolveJob();
    }

    public void run() {
        RetrieveSetup retrieveSetup = setup.getRetrieveSetup();
        List<String> confs = IvyClasspathUtil.split(retrieveSetup.getRetrieveConfs());
        IvyResolver resolver = new IvyResolver(setup.getIvyXmlPath(), confs, setup.getProject());
        resolver.setRetrievePattern(retrieveSetup.getRetrievePattern());
        resolver.setRetrieveSync(retrieveSetup.isRetrieveSync());
        resolver.setRetrieveTypes(retrieveSetup.getRetrieveTypes());
        ResolveRequest request = new ResolveRequest(resolver, setup.getState());
        ivyResolveJob.addRequest(request);
    }
}
