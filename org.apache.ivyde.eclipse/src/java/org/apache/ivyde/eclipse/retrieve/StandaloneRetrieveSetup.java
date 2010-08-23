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

import org.apache.ivyde.eclipse.cpcontainer.IvySettingsSetup;

public class StandaloneRetrieveSetup {

    private String name = "dependencies";

    private IvySettingsSetup ivySettingsSetup = new IvySettingsSetup();

    private String ivyXmlPath = "";

    private RetrieveSetup retrieveSetup = new RetrieveSetup();

    private boolean isSettingsProjectSpecific;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public IvySettingsSetup getIvySettingsSetup() {
        return ivySettingsSetup;
    }

    public void setIvySettingsSetup(IvySettingsSetup ivySettingsSetup) {
        this.ivySettingsSetup = ivySettingsSetup;
    }

    public String getIvyXmlPath() {
        return ivyXmlPath;
    }

    public void setIvyXmlPath(String ivyXmlPath) {
        this.ivyXmlPath = ivyXmlPath;
    }

    public RetrieveSetup getRetrieveSetup() {
        return retrieveSetup;
    }

    public void setRetrieveSetup(RetrieveSetup retrieveSetup) {
        this.retrieveSetup = retrieveSetup;
    }

    public boolean isSettingProjectSpecific() {
        return isSettingsProjectSpecific;
    }
    
    public void setSettingsProjectSpecific(boolean isSettingsProjectSpecific) {
        this.isSettingsProjectSpecific = isSettingsProjectSpecific;
    }

    public String toString() {
        return name != null ? name : retrieveSetup.getRetrievePattern();
    }
}
