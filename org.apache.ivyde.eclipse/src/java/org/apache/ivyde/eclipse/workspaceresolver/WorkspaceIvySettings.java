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
package org.apache.ivyde.eclipse.workspaceresolver;

import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.resolver.ChainResolver;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.eclipse.jdt.core.IJavaProject;

public class WorkspaceIvySettings extends IvySettings {

    private final IJavaProject javaProject;

    public WorkspaceIvySettings(IJavaProject javaProject) {
        this.javaProject = javaProject;
        setDefaultLatestStrategy(new IvyDEStrategy());
    }

    public DependencyResolver getResolver(ModuleRevisionId mrid) {
        return decorate(super.getResolver(mrid));
    }

    public DependencyResolver getDefaultResolver() {
        return decorate(super.getDefaultResolver());
    }

    private DependencyResolver decorate(DependencyResolver resolver) {
        if (resolver == null) {
            return resolver;
        }
        ChainResolver chain = new ChainResolver();
        chain.setName(javaProject.getElementName() + "-ivyde-workspace-chain-resolver");
        chain.setSettings(this);
        chain.setReturnFirst(true);
        chain.add(new WorkspaceResolver(javaProject, this));
        chain.add(resolver);
        return chain;
        
    }
}
