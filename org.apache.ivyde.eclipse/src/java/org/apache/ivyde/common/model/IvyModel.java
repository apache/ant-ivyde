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
package org.apache.ivyde.common.model;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.ivy.Ivy;

public abstract class IvyModel {
    private final Map MODEL = new HashMap();

    private Properties _defaults;
    private IvyModelSettings settings;

    public IvyModel(IvyModelSettings settings) {
        loadDefaults();
        this.settings = settings;
    }

    public IvyTag getIvyTag(String tagName, String parentName) {
        Object tag = MODEL.get(tagName);
        if (tag instanceof List) {
            List all = (List) tag;
            for (Iterator iter = all.iterator(); iter.hasNext();) {
                IvyTag t = (IvyTag) iter.next();
                if (t.getParent() != null && t.getParent().getName().equals(parentName)) {
                    return t;
                }
            }
            return null;
        }
        return (IvyTag) tag;
    }

    private void loadDefaults() {
        _defaults = new Properties();
        try {
            _defaults.load(IvyModel.class.getResourceAsStream("defaults.properties"));
        } catch (IOException e) {
            // should never never happen
            settings.logError("The default properties could not be loaded", e);
        }
    }

    public IvyTag getRootIvyTag() {
        return (IvyTag) MODEL.get(getRootIvyTagName());
    }

    protected abstract String getRootIvyTagName();

    protected Ivy getIvy() {
        return settings.getIvyInstance();
    }
    
    public IvyModelSettings getSettings() {
        return settings;
    }
    

    protected void addTag(String name, List list) {
        MODEL.put(name, list);
    }

    protected void addTag(IvyTag ivyTag) {
        if (!MODEL.containsKey(ivyTag.getName())) {
            MODEL.put(ivyTag.getName(), ivyTag);
            for (Iterator it = ivyTag.getChilds().iterator(); it.hasNext();) {
                IvyTag child = (IvyTag) it.next();
                addTag(child);
            }
        } else {
            // the model already contains a tag for this name... maybe we should add it to a list, 
            // but we still have problem of tags with infinite children hierarchy (like chain of chain)
            // where we need to stop adding children somewhere.
        }
    }
    
    protected String getDefault(String name) {
        return _defaults.getProperty(name);
    }

    public abstract IvyFile newIvyFile(String name, String content, int documentOffset);

    public void refreshIfNeeded(IvyFile file) {
    }
    
    protected void clearModel() {
        MODEL.clear();
    }
}
