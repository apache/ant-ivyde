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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Comparator;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivyde.eclipse.IvyPlugin;
import org.apache.ivyde.eclipse.resolve.IvyResolveJob;
import org.apache.ivyde.eclipse.resolve.ResolveRequest;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.swt.widgets.Display;

/**
 * Eclipse classpath container that will contain the ivy resolved entries.
 */
public class IvyClasspathContainer implements IClasspathContainer {

    public static final String CONTAINER_ID
        = "org.apache.ivyde.eclipse.cpcontainer.IVYDE_CONTAINER";

    private IClasspathEntry[] classpathEntries;

    private IPath path;

    private IvyClasspathContainerConfiguration conf;

    private final IvyClasspathContainerState state;

    /**
     * Create an Ivy class path container from some predefined classpath entries. The provided class
     * path entries should come from the default "persisted" classpath container. Note that no
     * resolve nor resolve are executed here, so some inconsistencies might exist between the
     * ivy.xml and the provided classpath entries.
     * 
     * @param javaProject
     *            the project of containing this container
     * @param path
     *            the path the container
     * @param ivyFile
     *            the path to the ivy file
     * @param confs
     *            the configuration that will be resolved
     * @param classpathEntries
     *            the entries to start with
     */
    public IvyClasspathContainer(IJavaProject javaProject, IPath path,
            IClasspathEntry[] classpathEntries, IClasspathAttribute[] attributes) {
        this.path = path;
        conf = new IvyClasspathContainerConfiguration(javaProject, path, false, attributes);
        state = new IvyClasspathContainerState(conf);
        this.classpathEntries = classpathEntries;
    }

    public IvyClasspathContainer(IvyClasspathContainer cp) {
        path = cp.path;
        conf = cp.conf;
        classpathEntries = cp.classpathEntries;
        state = cp.state;
    }

    public IvyClasspathContainerConfiguration getConf() {
        return conf;
    }

    public void setConf(IvyClasspathContainerConfiguration conf) {
        this.conf = conf;
        state.setConf(conf);
    }

    public IvyClasspathContainerState getState() {
        return state;
    }

    public String getDescription() {
        return conf.getIvyXmlPath() + " " + conf.getConfs();
    }

    public int getKind() {
        return K_APPLICATION;
    }

    public IPath getPath() {
        return path;
    }

    public IClasspathEntry[] getClasspathEntries() {
        return classpathEntries;
    }

    public IStatus launchResolve(boolean usePreviousResolveIfExist, IProgressMonitor monitor) {
        ResolveRequest request = new ResolveRequest(new IvyClasspathResolver(this,
                usePreviousResolveIfExist), getState());
        request.setInWorkspace(getConf().isInheritedResolveInWorkspace());
        IvyResolveJob resolveJob = IvyPlugin.getDefault().getIvyResolveJob();
        if (monitor != null) {
            return resolveJob.launchRequest(request, monitor);
        }
        resolveJob.addRequest(request);
        return Status.OK_STATUS;
    }

    void updateClasspathEntries(final IClasspathEntry[] newEntries) {
        IClasspathEntry[] entries;
        if (newEntries != null) {
            entries = newEntries;
        } else {
            entries = new IClasspathEntry[0];
        }
        setClasspathEntries(entries);
    }

    private void setClasspathEntries(final IClasspathEntry[] entries) {
        Display.getDefault().syncExec(new Runnable() {
            public void run() {
                if (conf.isInheritedAlphaOrder()) {
                    Arrays.sort(entries, new Comparator() {
                        public int compare(Object o1, Object o2) {
                            return ((IClasspathEntry) o1).getPath().lastSegment()
                                    .compareTo(((IClasspathEntry) o2).getPath().lastSegment());
                        }
                    });
                }
                classpathEntries = entries;
                notifyUpdateClasspathEntries();
            }
        });
    }

    void notifyUpdateClasspathEntries() {
        try {
            JavaCore.setClasspathContainer(path, new IJavaProject[] {conf.getJavaProject()},
                new IClasspathContainer[] {new IvyClasspathContainer(IvyClasspathContainer.this)},
                null);
        } catch (JavaModelException e) {
            // unless there are some issues with the JDT, this should never happen
            IvyPlugin.log(e);
        }
    }

    public URL getReportUrl() {
        Ivy ivy = state.getCachedIvy();
        if (ivy == null) {
            return null;
        }
        ModuleDescriptor md = state.getCachedModuleDescriptor(ivy);
        if (md == null) {
            return null;
        }
        String resolveId = IvyClasspathUtil.buildResolveId(conf.isInheritedUseExtendedResolveId(), md);
        try {
            return ivy
                    .getResolutionCacheManager()
                    .getConfigurationResolveReportInCache(resolveId, md.getConfigurationsNames()[0])
                    .toURL();
        } catch (MalformedURLException e) {
            // should never happen
            throw new RuntimeException(e);
        }
    }

    public void reloadSettings() {
        state.setIvySettingsLastModified(-1);
        launchResolve(false, null);
    }

    public String toString() {
        return conf.toString();
    }
}
