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
package org.apache.ivyde.eclipse.cpcontainer;

import java.util.List;

/**
 * This class is just a simple bean defining the properties which configure an IvyDE classpath
 * container.
 */
public class IvySettingsSetup {

    private String ivySettingsPath;

    private List/* <String> */propertyFiles;

    private boolean loadSettingsOnDemand = false;

    /**
     * Default constructor
     */
    public IvySettingsSetup() {
        // default constructor
    }

    public void set(IvySettingsSetup setup) {
        this.ivySettingsPath = setup.ivySettingsPath;
        this.propertyFiles = setup.propertyFiles;
        this.loadSettingsOnDemand = setup.loadSettingsOnDemand;
    }

    public String getIvySettingsPath() {
        return ivySettingsPath;
    }

    public void setIvySettingsPath(String ivySettingsPath) {
        this.ivySettingsPath = ivySettingsPath;
    }

    public List getPropertyFiles() {
        return propertyFiles;
    }

    public void setPropertyFiles(List propertyFiles) {
        this.propertyFiles = propertyFiles;
    }

    public boolean isLoadSettingsOnDemand() {
        return loadSettingsOnDemand;
    }

    public void setLoadSettingsOnDemand(boolean loadSettingsOnDemand) {
        this.loadSettingsOnDemand = loadSettingsOnDemand;
    }

}
