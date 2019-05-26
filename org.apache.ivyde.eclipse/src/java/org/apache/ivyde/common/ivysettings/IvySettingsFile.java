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
package org.apache.ivyde.common.ivysettings;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.core.settings.XmlSettingsParser;
import org.apache.ivyde.common.model.IvyFile;
import org.apache.ivyde.common.model.IvyModelSettings;
import org.apache.ivyde.internal.eclipse.IvyPlugin;

public class IvySettingsFile extends IvyFile {
    private static final Pattern CLASSPATH_URL_PATTERN = Pattern
            .compile("<[\\s]*classpath[^>]+url=\"([^\"]+)");

    private static final Pattern CLASSPATH_FILE_PATTERN = Pattern
            .compile("<[\\s]*classpath[^>]+file=\"([^\"]+)");

    private static final Pattern TYPEDEF_PATTERN = Pattern
            .compile("<[\\s]*typedef[^>]+name=\"([^\"]+)\"[^>]+classname=\"([^\"]+)");

    private final File file;

    public IvySettingsFile(IvyModelSettings settings, File file, String projectName, String doc,
            int currentOffset) {
        super(settings, projectName, doc, currentOffset);
        this.file = file;
    }

    public URL[] getClasspathUrls() {
        List<URL> urls = new ArrayList<>();
        Matcher m = CLASSPATH_URL_PATTERN.matcher(getDoc());
        while (m.find()) {
            try {
                urls.add(new URL(substitute(m.group(1))));
            } catch (MalformedURLException e) {
                // ignored
            }
        }
        m = CLASSPATH_FILE_PATTERN.matcher(getDoc());
        while (m.find()) {
            try {
                urls.add(new URL(substitute(m.group(1))));
            } catch (MalformedURLException e) {
                try {
                    urls.add(new File(substitute(m.group(1))).toURI().toURL());
                } catch (MalformedURLException e1) {
                    // ignored
                }
            }
        }
        return urls.toArray(new URL[urls.size()]);
    }

    private String substitute(String str) {
        Map<String, String> variables = new HashMap<>();
        if (file.getParentFile() != null) {
            URI settingsDirUri = file.getParentFile().toURI();
            variables.put("ivy.settings.dir", settingsDirUri.toString());
        }
        return IvyPatternHelper.substituteVariables(str, variables);
    }

    public Map<Object, Object> getTypedefs() {
        Map<Object, Object> p = getDefaultTypedefs();
        Matcher m = TYPEDEF_PATTERN.matcher(getDoc());
        while (m.find()) {
            p.put(substitute(m.group(1)), substitute(m.group(2)));
        }
        return p;
    }

    public static Map<Object, Object> getDefaultTypedefs() {
        Properties p = new Properties();
        try {
            p.load(XmlSettingsParser.class.getResourceAsStream("typedef.properties"));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            IvyPlugin.logError(e.getMessage(), e);
        }
        return p;
    }

}
