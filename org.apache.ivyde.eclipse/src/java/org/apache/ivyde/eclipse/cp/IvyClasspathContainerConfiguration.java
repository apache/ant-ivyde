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

import java.util.Collections;
import java.util.List;

import org.apache.ivyde.internal.eclipse.IvyPlugin;
import org.apache.ivyde.internal.eclipse.cpcontainer.IvyClasspathContainerConfAdapter;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IJavaProject;

/**
 * This class is just a simple bean defining the properties which configure an IvyDE classpath
 * container.
 */
public class IvyClasspathContainerConfiguration {

    private IJavaProject javaProject;

    private String ivyXmlPath;

    private List<String> confs = Collections.singletonList("*");

    private SettingsSetup settingsSetup = new SettingsSetup();

    private ClasspathSetup classpathSetup = new ClasspathSetup();

    private MappingSetup mappingSetup = new MappingSetup();

    private AdvancedSetup advancedSetup = new AdvancedSetup();

    private boolean settingsProjectSpecific;

    private boolean classthProjectSpecific;

    private boolean mappingProjectSpecific;

    private boolean advancedProjectSpecific;

    /**
     * attributes attached to the container but not IvyDE related (Webtools or AspectJfor instance)
     */
    private IClasspathAttribute[] attributes = new IClasspathAttribute[0];

    /**
     * Constructor
     *
     * @param javaProject
     *            the classpath container's Java project, <code>null</code> is not bind to a project
     * @param ivyXmlPath
     *            the path to the ivy.xml
     * @param editing
     *            if set to true, this bean will be used for edition purpose, so no need to trigger
     *            UI notification about some errors in there
     */
    public IvyClasspathContainerConfiguration(IJavaProject javaProject, String ivyXmlPath,
            boolean editing) {
        this.javaProject = javaProject;
        this.ivyXmlPath = ivyXmlPath;
    }

    /**
     * Constructor
     *
     * @param javaProject
     *            the classpath container's Java project, <code>null</code> if not bind to a project
     * @param path
     *            the path of the classpath container
     * @param editing
     *            if set to true, this bean will be used for edition purpose, so no need to trigger
     *            UI notification about some errors in there
     * @param attributes an array of IClasspathAttribute
     */
    public IvyClasspathContainerConfiguration(IJavaProject javaProject, IPath path,
            boolean editing, IClasspathAttribute[] attributes) {
        this.javaProject = javaProject;
        IvyClasspathContainerConfAdapter.load(this, path, attributes);
    }

    public IPath getPath() {
        return IvyClasspathContainerConfAdapter.getPath(this);
    }

    // ///////////////////////////
    // Simple setters and getters
    // ///////////////////////////

    public String getIvyXmlPath() {
        return ivyXmlPath;
    }

    public void setIvyXmlPath(String ivyXmlPath) {
        this.ivyXmlPath = ivyXmlPath;
    }

    public List<String> getConfs() {
        return confs;
    }

    public void setConfs(List<String> confs) {
        this.confs = confs;
    }

    public SettingsSetup getIvySettingsSetup() {
        return settingsSetup;
    }

    public void setIvySettingsSetup(SettingsSetup ivySettingsSetup) {
        this.settingsSetup = ivySettingsSetup;
    }

    public ClasspathSetup getClasspathSetup() {
        return classpathSetup;
    }

    public void setClasspathSetup(ClasspathSetup classpathSetup) {
        this.classpathSetup = classpathSetup;
    }

    public MappingSetup getMappingSetup() {
        return mappingSetup;
    }

    public void setMappingSetup(MappingSetup mappingSetup) {
        this.mappingSetup = mappingSetup;
    }

    public AdvancedSetup getAdvancedSetup() {
        return advancedSetup;
    }

    public void setAdvancedSetup(AdvancedSetup advancedSetup) {
        this.advancedSetup = advancedSetup;
    }

    public boolean isClassthProjectSpecific() {
        return classthProjectSpecific;
    }

    public void setClassthProjectSpecific(boolean classthProjectSpecific) {
        this.classthProjectSpecific = classthProjectSpecific;
    }

    public boolean isMappingProjectSpecific() {
        return mappingProjectSpecific;
    }

    public void setMappingProjectSpecific(boolean mappingProjectSpecific) {
        this.mappingProjectSpecific = mappingProjectSpecific;
    }

    public boolean isSettingsProjectSpecific() {
        return settingsProjectSpecific;
    }

    public void setSettingsProjectSpecific(boolean isSettingsProjectSpecific) {
        this.settingsProjectSpecific = isSettingsProjectSpecific;
    }

    public boolean isAdvancedProjectSpecific() {
        return advancedProjectSpecific;
    }

    public void setAdvancedProjectSpecific(boolean advancedProjectSpecific) {
        this.advancedProjectSpecific = advancedProjectSpecific;
    }

    public IJavaProject getJavaProject() {
        return javaProject;
    }

    public void setProject(IJavaProject javaProject) {
        this.javaProject = javaProject;
    }

    public void setAttributes(IClasspathAttribute[] attributes) {
        this.attributes = attributes;
    }

    public IClasspathAttribute[] getAttributes() {
        return attributes;
    }

    // ///////////////////////////
    // Getters that take into account the global preferences
    // ///////////////////////////

    public IProject getProject() {
        if (javaProject == null) {
            return null;
        }
        return javaProject.getProject();
    }

    public SettingsSetup getInheritedSettingsSetup() {
        if (!settingsProjectSpecific) {
            return IvyPlugin.getPreferenceStoreHelper().getSettingsSetup();
        }
        return settingsSetup;
    }

    public ClasspathSetup getInheritedClasspathSetup() {
        if (!classthProjectSpecific) {
            return IvyPlugin.getPreferenceStoreHelper().getClasspathSetup();
        }
        return classpathSetup;
    }

    public MappingSetup getInheritedMappingSetup() {
        if (!mappingProjectSpecific) {
            return IvyPlugin.getPreferenceStoreHelper().getMappingSetup();
        }
        return mappingSetup;
    }

    public AdvancedSetup getInheritedAdvancedSetup() {
        if (!advancedProjectSpecific) {
            return IvyPlugin.getPreferenceStoreHelper().getAdvancedSetup();
        }
        return advancedSetup;
    }

    public String toString() {
        return ivyXmlPath + confs
                + (javaProject == null ? "" : " in '" + javaProject.getProject().getName() + "'");
    }

}
