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
package org.apache.ivyde.common.ivyfile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ivyde.common.model.IvyFile;
import org.apache.ivyde.common.model.IvyModelSettings;

public class IvyModuleDescriptorFile extends IvyFile {
    private static final Pattern CONF_PATTERN = Pattern.compile("<[\\s]*conf[^>]+name=\"([^\"]+)");

    private static final Pattern CONFIGURATIONS_END_PATTERN = Pattern
            .compile("</[\\s]*configurations[\\s]*>");

    private static final Pattern CONFIGURATIONS_START_PATTERN = Pattern
            .compile("<[\\s]*configurations[\\s]*>");

    public IvyModuleDescriptorFile(IvyModelSettings settings, String projectName, String doc) {
        this(settings, projectName, doc, 0);
    }

    public IvyModuleDescriptorFile(IvyModelSettings settings, String projectName, String doc,
            int currentOffset) {
        super(settings, projectName, doc, currentOffset);
    }

    public String[] getConfigurationNames() {
        Pattern p = CONFIGURATIONS_START_PATTERN;
        Matcher m = p.matcher(getDoc());
        if (m.find()) {
            int start = m.end();
            p = CONFIGURATIONS_END_PATTERN;
            m = p.matcher(getDoc());
            int end = getDoc().length();
            if (m.find(start)) {
                end = m.start();
            }
            p = CONF_PATTERN;
            m = p.matcher(getDoc());
            List ret = new ArrayList();
            for (boolean found = m.find(start); found && m.end() < end; found = m.find()) {
                ret.add(m.group(1));
            }
            return (String[]) ret.toArray(new String[ret.size()]);
        } else {
            return new String[] {"default"};
        }
    }

    public String getOrganisation() {
        Pattern p = Pattern.compile("<[\\s]*info[^>]*organisation[\\s]*=[\\s]*\"([^\"]+)");
        Matcher m = p.matcher(getDoc());
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    public String getDependencyOrganisation() {
        Map otherAttValues = getAllAttsValues();
        return getDependencyOrganisation(otherAttValues);
    }

    public String getDependencyOrganisation(Map otherAttValues) {
        return otherAttValues != null && otherAttValues.get("org") != null ? (String) otherAttValues
                .get("org")
                : getOrganisation();
    }
}
