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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Comparator;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.util.Message;
import org.apache.ivyde.eclipse.IvyDEException;
import org.apache.ivyde.eclipse.IvyPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.DeltaProcessingState;
import org.eclipse.jdt.internal.core.JavaElementDelta;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.internal.ui.packageview.PackageExplorerContentProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

/**
 * Eclipse classpath container that will contain the ivy resolved entries.
 */
public class IvyClasspathContainer implements IClasspathContainer {

    public static final String CONTAINER_ID = "org.apache.ivyde.eclipse.cpcontainer.IVYDE_CONTAINER";

    private IClasspathEntry[] classpathEntries;

    private IPath path;

    private IvyResolveJob job;

    private IvyClasspathContainerConfiguration conf;

    private String jdtVersion;

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
     * @throws IOException
     * @throws ParseException
     * @throws MalformedURLException
     */
    public IvyClasspathContainer(IJavaProject javaProject, IPath path,
            IClasspathEntry[] classpathEntries) throws IOException {
        this.path = path;
        conf = new IvyClasspathContainerConfiguration(javaProject, path, false);
        this.classpathEntries = classpathEntries;
    }

    public IvyClasspathContainer(IvyClasspathContainer cp) {
        path = cp.path;
        conf = cp.conf;
        classpathEntries = cp.classpathEntries;
    }

    public IvyClasspathContainerConfiguration getConf() {
        return conf;
    }

    public String getDescription() {
        return conf.ivyXmlPath + " " + conf.confs;
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

    private static final ISchedulingRule RESOLVE_EVENT_RULE = new ISchedulingRule() {
        public boolean contains(ISchedulingRule rule) {
            return rule == this;
        }

        public boolean isConflicting(ISchedulingRule rule) {
            return rule == this;
        }
    };

    private IvyResolveJob computeClasspathEntries(final boolean usePreviousResolveIfExist,
            boolean isUser) {
        try {
            synchronized (this) {
                if (job != null) {
                    // resolve job already running
                    return job;
                }
                job = new IvyResolveJob(this, usePreviousResolveIfExist);
                job.setUser(isUser);
                job.setRule(RESOLVE_EVENT_RULE);
                return job;
            }
        } catch (Throwable e) {
            // IVYDE-79 : catch Throwable in order to catch java.lang.NoClassDefFoundError too
            Message.error(e.getMessage());
            IvyPlugin.log(IStatus.ERROR, "The creation of the job failed", e);
            MessageDialog.openError(IvyPlugin.getActiveWorkbenchShell(), "Resolve failed", e
                    .getMessage());
            return null;
        }
    }

    public void launchResolve(boolean usePreviousResolveIfExist, boolean isUser,
            IProgressMonitor monitor) {
        IvyResolveJob j = computeClasspathEntries(usePreviousResolveIfExist, isUser);
        if (j != null) {
            if (monitor != null) {
                j.run(monitor);
            } else {
                j.schedule();
            }
        }
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
        Display.getDefault().asyncExec(new Runnable() {
            public void run() {
                if (conf.isInheritedAlphaOrder()) {
                    Arrays.sort(entries, new Comparator() {
                        public int compare(Object o1, Object o2) {
                            return ((IClasspathEntry) o1).getPath().lastSegment().compareTo(
                                ((IClasspathEntry) o2).getPath().lastSegment());
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
            JavaCore.setClasspathContainer(path, new IJavaProject[] {conf.javaProject},
                new IClasspathContainer[] {new IvyClasspathContainer(IvyClasspathContainer.this)},
                null);

            // the following code was imported from:
            //http://svn.codehaus.org/m2eclipse/trunk/org.maven.ide.eclipse/src/org/maven/ide/eclipse
            // /embedder/BuildPathManager.java
            // revision: 370; function setClasspathContainer; line 215

            // XXX In Eclipse 3.3, changes to resolved classpath are not announced by JDT Core
            // and PackageExplorer does not properly refresh when we update Ivy
            // classpath container.
            // As a temporary workaround, send F_CLASSPATH_CHANGED notifications
            // to all PackageExplorerContentProvider instances listening to
            // java ElementChangedEvent.
            // Note that even with this hack, build clean is sometimes necessary to
            // reconcile PackageExplorer with actual classpath
            // See https://bugs.eclipse.org/bugs/show_bug.cgi?id=154071
            if (getJDTVersion().startsWith("3.3")) {
                DeltaProcessingState state = JavaModelManager.getJavaModelManager().deltaState;
                synchronized (state) {
                    IElementChangedListener[] listeners = state.elementChangedListeners;
                    for (int i = 0; i < listeners.length; i++) {
                        if (listeners[i] instanceof PackageExplorerContentProvider) {
                            JavaElementDelta delta = new JavaElementDelta(conf.javaProject);
                            delta.changed(IJavaElementDelta.F_CLASSPATH_CHANGED);
                            listeners[i].elementChanged(new ElementChangedEvent(delta,
                                    ElementChangedEvent.POST_CHANGE));
                        }
                    }
                }
            }
        } catch (JavaModelException e) {
            // unless there are some issues with the JDT, this should never happen
            IvyPlugin.log(e);
        }
    }

    private synchronized String getJDTVersion() {
        if (jdtVersion == null) {
            Bundle[] bundles = IvyPlugin.getDefault().getBundleContext().getBundles();
            for (int i = 0; i < bundles.length; i++) {
                if (JavaCore.PLUGIN_ID.equals(bundles[i].getSymbolicName())) {
                    jdtVersion = (String) bundles[i].getHeaders().get(Constants.BUNDLE_VERSION);
                    break;
                }
            }
        }
        return jdtVersion;
    }

    public URL getReportUrl() throws IvyDEException {
        Ivy ivy = conf.getIvy();
        ModuleDescriptor md = conf.getModuleDescriptor();
        String resolveId = ResolveOptions.getDefaultResolveId(md);
        try {
            return ivy.getResolutionCacheManager().getConfigurationResolveReportInCache(resolveId,
                md.getConfigurationsNames()[0]).toURL();
        } catch (MalformedURLException e) {
            // should never happen
            throw new RuntimeException(e);
        }
    }

    public void resetJob() {
        job = null;
    }

    public void reloadSettings() {
        conf.ivySettingsLastModified = -1;
        launchResolve(false, true, null);
    }

}
