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
package org.apache.ivyde.internal.eclipse.cpcontainer;

import java.util.Collection;

import org.apache.ivyde.eclipse.IvyDEException;
import org.apache.ivyde.eclipse.cp.IvyClasspathContainerConfiguration;
import org.apache.ivyde.eclipse.cp.ResolvedPath;
import org.apache.ivyde.internal.eclipse.CachedIvy;
import org.eclipse.core.resources.IProject;

/**
 * This class is a front end to the container configuration. It computes the configuration status,
 * build the Ivy bean on demand and can cache it.
 */
public class IvyClasspathContainerState extends CachedIvy {

    private IvyClasspathContainerConfiguration conf;

    public IvyClasspathContainerState(IvyClasspathContainerConfiguration conf) {
        this.conf = conf;
    }

    public void setConf(IvyClasspathContainerConfiguration conf) {
        this.conf = conf;
        reset();
    }

    protected ResolvedPath getIvySettingsPath() {
        return conf.getInheritedSettingsSetup().getResolvedIvySettingsPath(getProject());
    }

    protected String getIvyXmlPath() {
        return conf.getIvyXmlPath();
    }

    protected IProject getProject() {
        return conf.getProject();
    }

    protected Collection<String> getPropertyFiles() throws IvyDEException {
        return conf.getInheritedSettingsSetup().getResolvedPropertyFiles();
    }

    protected boolean isLoadSettingsOnDemandPath() {
        return conf.getInheritedSettingsSetup().isLoadSettingsOnDemand();
    }

    protected ResolvedPath getIvyUserDir() {
        return conf.getInheritedSettingsSetup().getResolvedIvyUserDir(getProject());
    }

    protected boolean isResolveInWorkspace() {
        return conf.getInheritedClasspathSetup().isResolveInWorkspace();
    }

    protected boolean isTransitiveResolve() {
        return conf.getInheritedClasspathSetup().isTransitiveResolve();
    }

    public String toString() {
        return conf.toString();
    }

}
