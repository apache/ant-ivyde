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
package org.apache.ivyde.eclipse.cp;

import java.util.ArrayList;
import java.util.List;

import org.apache.ivyde.eclipse.IvyDEException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;

/**
 * This class is just a simple bean defining the properties which configure an IvyDE classpath
 * container.
 */
public class SettingsSetup {

    private String ivySettingsPath;

    private List<String> propertyFiles = new ArrayList<>();

    private boolean loadSettingsOnDemand = false;

    private String ivyUserDir = "";

    /**
     * Default constructor
     */
    public SettingsSetup() {
        // default constructor
    }

    public void set(SettingsSetup setup) {
        this.ivySettingsPath = setup.ivySettingsPath;
        this.propertyFiles = setup.propertyFiles;
        this.loadSettingsOnDemand = setup.loadSettingsOnDemand;
        this.ivyUserDir = setup.ivyUserDir;
    }

    public ResolvedPath getResolvedIvySettingsPath(IProject project) {
        return new ResolvedPath(ivySettingsPath, project);
    }

    public String getRawIvySettingsPath() {
        return ivySettingsPath;
    }

    public void setIvySettingsPath(String ivySettingsPath) {
        this.ivySettingsPath = ivySettingsPath;
    }

    public List<String> getRawPropertyFiles() {
        return propertyFiles;
    }

    public List<String> getResolvedPropertyFiles() throws IvyDEException {
        List<String> resolvedProps = new ArrayList<>();
        IStringVariableManager manager = VariablesPlugin.getDefault().getStringVariableManager();
        try {
            for (String propFile : propertyFiles) {
                String resolvedProp = manager.performStringSubstitution(propFile, false);
                resolvedProps.add(resolvedProp);
            }
        } catch (CoreException e) {
            throw new IvyDEException("Unrecognized variables",
                    "Unrecognized variables in the Ivy settings file " + ivySettingsPath, e);
        }
        return resolvedProps;
    }

    public void setPropertyFiles(List<String> propertyFiles) {
        this.propertyFiles = propertyFiles;
    }

    public boolean isLoadSettingsOnDemand() {
        return loadSettingsOnDemand;
    }

    public void setLoadSettingsOnDemand(boolean loadSettingsOnDemand) {
        this.loadSettingsOnDemand = loadSettingsOnDemand;
    }

    public void setIvyUserDir(String ivyUserDir) {
        this.ivyUserDir = ivyUserDir;
    }

    public ResolvedPath getResolvedIvyUserDir(IProject project) {
        return new ResolvedPath(ivyUserDir, project);
    }

    public String getRawIvyUserDir() {
        return ivyUserDir;
    }

}
