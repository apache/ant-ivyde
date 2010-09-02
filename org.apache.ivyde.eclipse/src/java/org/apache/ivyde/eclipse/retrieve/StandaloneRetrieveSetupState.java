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
package org.apache.ivyde.eclipse.retrieve;

import java.util.Collection;

import org.apache.ivyde.eclipse.CachedIvy;
import org.apache.ivyde.eclipse.IvyDEException;
import org.eclipse.core.resources.IProject;

public class StandaloneRetrieveSetupState extends CachedIvy {

    private final StandaloneRetrieveSetup setup;

    public StandaloneRetrieveSetupState(StandaloneRetrieveSetup setup) {
        this.setup = setup;
    }

    protected String getIvySettingsPath() throws IvyDEException {
        return setup.getInheritedIvySettingsPath();
    }

    protected String getIvyXmlPath() {
        return setup.getIvyXmlPath();
    }

    protected IProject getProject() {
        return setup.getProject();
    }

    protected Collection getPropertyFiles() throws IvyDEException {
        return setup.getInheritedPropertyFiles();
    }

    protected boolean isLoadSettingsOnDemandPath() {
        return setup.isInheritedLoadSettingsOnDemand();
    }

    protected boolean isResolveInWorkspace() {
        return false;
    }

}
