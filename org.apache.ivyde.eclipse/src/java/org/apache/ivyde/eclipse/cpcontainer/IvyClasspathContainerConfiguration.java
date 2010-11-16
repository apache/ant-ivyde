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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.ivyde.eclipse.IvyDEException;
import org.apache.ivyde.eclipse.IvyPlugin;
import org.apache.ivyde.eclipse.retrieve.RetrieveSetup;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IJavaProject;

/**
 * This class is just a simple bean defining the properties which configure an IvyDE classpath
 * container.
 */
public class IvyClasspathContainerConfiguration {

    private final IJavaProject javaProject;

    private IvySettingsSetup ivySettingsSetup = new IvySettingsSetup();

    private ContainerMappingSetup containerMappingSetup = new ContainerMappingSetup();

    private String ivyXmlPath;

    private List/* <String> */confs = Arrays.asList(new String[] {"*"});

    private boolean alphaOrder;

    private boolean resolveInWorkspace;

    private boolean isAdvancedProjectSpecific;

    private boolean isSettingsProjectSpecific;

    private boolean resolveBeforeLaunch;

    /**
     * attributes attached to the container but not IvyDE related (Webtools or AspectJfor instance)
     */
    private IClasspathAttribute[] attributes = new IClasspathAttribute[0];

    private boolean retrievedClasspath;

    private RetrieveSetup retrievedClasspathSetup = new RetrieveSetup();

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
     *            the classpath container's Java project, <code>null</code> is not bind to a project
     * @param path
     *            the path of the classpath container
     * @param editing
     *            if set to true, this bean will be used for edition purpose, so no need to trigger
     *            UI notification about some errors in there
     * @param attributes
     */
    public IvyClasspathContainerConfiguration(IJavaProject javaProject, IPath path,
            boolean editing, IClasspathAttribute[] attributes) {
        this.javaProject = javaProject;
        IvyClasspathContainerConfAdapter.load(this, path, attributes);
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

    public List getConfs() {
        return confs;
    }

    public void setConfs(List confs) {
        this.confs = confs;
    }

    public IvySettingsSetup getIvySettingsSetup() {
        return ivySettingsSetup;
    }

    public void setIvySettingsSetup(IvySettingsSetup ivySettingsSetup) {
        this.ivySettingsSetup = ivySettingsSetup;
    }

    public ContainerMappingSetup getContainerMappingSetup() {
        return containerMappingSetup;
    }

    public void setContainerMappingSetup(ContainerMappingSetup containerMappingSetup) {
        this.containerMappingSetup = containerMappingSetup;
    }

    public boolean isAlphaOrder() {
        return alphaOrder;
    }

    public void setAlphaOrder(boolean alphaOrder) {
        this.alphaOrder = alphaOrder;
    }

    public boolean isResolveInWorkspace() {
        return resolveInWorkspace;
    }

    public void setResolveInWorkspace(boolean resolveInWorkspace) {
        this.resolveInWorkspace = resolveInWorkspace;
    }

    public boolean isAdvancedProjectSpecific() {
        return isAdvancedProjectSpecific;
    }

    public void setAdvancedProjectSpecific(boolean isAdvancedProjectSpecific) {
        this.isAdvancedProjectSpecific = isAdvancedProjectSpecific;
    }

    public boolean isSettingsProjectSpecific() {
        return isSettingsProjectSpecific;
    }

    public void setSettingsProjectSpecific(boolean isSettingsProjectSpecific) {
        this.isSettingsProjectSpecific = isSettingsProjectSpecific;
    }

    public boolean isResolveBeforeLaunch() {
        return resolveBeforeLaunch;
    }

    public void setResolveBeforeLaunch(boolean resolveBeforeLaunch) {
        this.resolveBeforeLaunch = resolveBeforeLaunch;
    }

    public IJavaProject getJavaProject() {
        return javaProject;
    }

    public void setAttributes(IClasspathAttribute[] attributes) {
        this.attributes = attributes;
    }

    public IClasspathAttribute[] getAttributes() {
        return attributes;
    }

    public boolean isRetrievedClasspath() {
        return retrievedClasspath;
    }

    public void setRetrievedClasspath(boolean retrievedClasspath) {
        this.retrievedClasspath = retrievedClasspath;
    }

    public RetrieveSetup getRetrievedClasspathSetup() {
        return retrievedClasspathSetup;
    }

    public void setRetrievedClasspathSetup(RetrieveSetup retrievedClasspathSetup) {
        this.retrievedClasspathSetup = retrievedClasspathSetup;
    }

    // ///////////////////////////
    // Getters that take into account the global preferences
    // ///////////////////////////

    private IProject getProject() {
        if (javaProject == null) {
            return null;
        }
        return javaProject.getProject();
    }

    public String getInheritedIvySettingsPath() throws IvyDEException {
        if (!isSettingsProjectSpecific) {
            return IvyPlugin.getPreferenceStoreHelper().getIvySettingsSetup()
                    .getResolvedIvySettingsPath(getProject());
        }
        return ivySettingsSetup.getResolvedIvySettingsPath(getProject());
    }

    public boolean getInheritedLoadSettingsOnDemandPath() {
        if (!isSettingsProjectSpecific) {
            return IvyPlugin.getPreferenceStoreHelper().getIvySettingsSetup()
                    .isLoadSettingsOnDemand();
        }
        return ivySettingsSetup.isLoadSettingsOnDemand();
    }

    public Collection getInheritedPropertyFiles() throws IvyDEException {
        if (!isSettingsProjectSpecific) {
            return IvyPlugin.getPreferenceStoreHelper().getIvySettingsSetup()
                    .getResolvedPropertyFiles();
        } else {
            return ivySettingsSetup.getResolvedPropertyFiles();
        }
    }

    public Collection getInheritedAcceptedTypes() {
        if (!isAdvancedProjectSpecific) {
            return IvyPlugin.getPreferenceStoreHelper().getContainerMappingSetup()
                    .getAcceptedTypes();
        }
        return containerMappingSetup.getAcceptedTypes();
    }

    public Collection getInheritedSourceTypes() {
        if (!isAdvancedProjectSpecific) {
            return IvyPlugin.getPreferenceStoreHelper().getContainerMappingSetup().getSourceTypes();
        }
        return containerMappingSetup.getSourceTypes();
    }

    public Collection getInheritedSourceSuffixes() {
        if (!isAdvancedProjectSpecific) {
            return IvyPlugin.getPreferenceStoreHelper().getContainerMappingSetup()
                    .getSourceSuffixes();
        }
        return containerMappingSetup.getSourceSuffixes();
    }

    public Collection getInheritedJavadocTypes() {
        if (!isAdvancedProjectSpecific) {
            return IvyPlugin.getPreferenceStoreHelper().getContainerMappingSetup()
                    .getJavadocTypes();
        }
        return containerMappingSetup.getJavadocTypes();
    }

    public Collection getInheritedJavadocSuffixes() {
        if (!isAdvancedProjectSpecific) {
            return IvyPlugin.getPreferenceStoreHelper().getContainerMappingSetup()
                    .getJavadocSuffixes();
        }
        return containerMappingSetup.getJavadocSuffixes();
    }

    public boolean isInheritedAlphaOrder() {
        if (!isAdvancedProjectSpecific) {
            return IvyPlugin.getPreferenceStoreHelper().isAlphOrder();
        }
        return alphaOrder;
    }

    public boolean isInheritedResolveInWorkspace() {
        if (!isAdvancedProjectSpecific) {
            return IvyPlugin.getPreferenceStoreHelper().isResolveInWorkspace();
        }
        return resolveInWorkspace;
    }

    public boolean isInheritedResolveBeforeLaunch() {
        if (!isAdvancedProjectSpecific) {
            return IvyPlugin.getPreferenceStoreHelper().isResolveBeforeLaunch();
        }
        return resolveBeforeLaunch;
    }

    public boolean isInheritedRetrievedClasspath() {
        if (!isAdvancedProjectSpecific) {
            return IvyPlugin.getPreferenceStoreHelper().isRetrievedClasspath();
        }
        return retrievedClasspath;
    }

    public RetrieveSetup getInheritedRetrievedClasspathSetup() {
        if (!isAdvancedProjectSpecific) {
            return IvyPlugin.getPreferenceStoreHelper().getRetrievedClasspathSetup();
        }
        return retrievedClasspathSetup;
    }

    public boolean isInheritedMapIfOnlyOneSource() {
        if (!isAdvancedProjectSpecific) {
            return IvyPlugin.getPreferenceStoreHelper().getContainerMappingSetup()
                    .isMapIfOnlyOneSource();
        }
        return containerMappingSetup.isMapIfOnlyOneSource();
    }

    public boolean isInheritedMapIfOnlyOneJavadoc() {
        if (!isAdvancedProjectSpecific) {
            return IvyPlugin.getPreferenceStoreHelper().getContainerMappingSetup()
                    .isMapIfOnlyOneJavadoc();
        }
        return containerMappingSetup.isMapIfOnlyOneJavadoc();
    }

    public String toString() {
        return ivyXmlPath + confs
                + (javaProject == null ? "" : " in '" + javaProject.getProject().getName() + "'");
    }

}
