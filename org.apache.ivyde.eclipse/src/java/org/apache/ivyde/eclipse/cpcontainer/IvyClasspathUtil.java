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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.ivyde.eclipse.IvyPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.corext.javadoc.JavaDocLocations;
import org.eclipse.jdt.internal.ui.packageview.ClassPathContainer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

public class IvyClasspathUtil {

    /**
     * Adds an Ivy classpath container to the list of existing classpath entries in the given
     * project.
     * 
     * @param project
     *            the project to which the classpath container should be added
     * @param projectRelativePath
     *            the path relative to the project of the module descriptor file to use for the
     *            classpath container
     * @param confs
     *            the configurations to use in the classpath container.
     */
    public static void addCPContainer(IJavaProject project, IPath projectRelativePath, String confs) {
        try {
            IClasspathEntry newEntry = JavaCore.newContainerEntry(new Path(
                    IvyClasspathContainer.IVY_CLASSPATH_CONTAINER_ID).append(projectRelativePath)
                    .append(confs));

            IClasspathEntry[] entries = project.getRawClasspath();

            List newEntries = new ArrayList(Arrays.asList(entries));
            newEntries.add(newEntry);
            entries = (IClasspathEntry[]) newEntries
                    .toArray(new IClasspathEntry[newEntries.size()]);

            project.setRawClasspath(entries, project.getOutputLocation(), null);
        } catch (CoreException e) {
            // unless there are issues with the JDT, this should never happen
            IvyPlugin.log(e);
        }
    }

    /**
     * Get the current selection in the Java package view
     * 
     * @return the selection, <code>null</code> if unsuccessful
     */
    public static IStructuredSelection getSelectionInJavaPackageView() {
        IWorkbenchWindow activeWorkbenchWindow = PlatformUI.getWorkbench()
                .getActiveWorkbenchWindow();
        if (activeWorkbenchWindow == null) {
            return null;
        }
        ISelection sel = activeWorkbenchWindow.getSelectionService().getSelection();
        IStructuredSelection selection;
        if (sel instanceof IStructuredSelection) {
            selection = (IStructuredSelection) sel;
        } else {
            sel = activeWorkbenchWindow.getSelectionService().getSelection(
                "org.eclipse.jdt.ui.PackageExplorer");
            if (sel instanceof IStructuredSelection) {
                selection = (IStructuredSelection) sel;
            } else {
                return null;
            }
        }
        return selection;
    }

    /**
     * Get the Ivy classpath container from the selection in the Java package view
     * 
     * @param selection
     *            the selection
     * @return
     * @throws JavaModelException
     */
    public static IvyClasspathContainer getIvyClasspathContainer(IStructuredSelection selection) {
        if (selection == null) {
            return null;
        }
        for (Iterator it = selection.iterator(); it.hasNext();) {
            Object element = it.next();
            IvyClasspathContainer cp = (IvyClasspathContainer) IvyPlugin.adapt(element,
                IvyClasspathContainer.class);
            if (cp != null) {
                return cp;
            }
            IJavaProject project = (IJavaProject) IvyPlugin.adapt(element, IJavaProject.class);
            if (project != null) {
                return getIvyClasspathContainer(project);
            }
            if (element instanceof ClassPathContainer) {
                // FIXME: we shouldn't check against internal JDT API but there are not adaptable to
                // useful class
                return jdt2IvyCPC((ClassPathContainer) element);
            }
        }
        return null;
    }

    /**
     * Work around the non adaptability of ClassPathContainer
     * 
     * @param cpc
     *            the container to transform into an IvyClasspathContainer
     * @return the IvyClasspathContainer is such, null, if not
     */
    public static IvyClasspathContainer jdt2IvyCPC(ClassPathContainer cpc) {
        IClasspathEntry entry = cpc.getClasspathEntry();
        try {
            IClasspathContainer icp = JavaCore.getClasspathContainer(entry.getPath(), cpc
                    .getJavaProject());
            if (icp instanceof IvyClasspathContainer) {
                return (IvyClasspathContainer) icp;
            }
        } catch (JavaModelException e) {
            // unless there are issues with the JDT, this should never happen
            IvyPlugin.log(e);
        }
        return null;
    }

    public static boolean isIvyClasspathContainer(IPath containerPath) {
        return containerPath.segment(0).equals(IvyClasspathContainer.IVY_CLASSPATH_CONTAINER_ID);
    }

    /**
     * Search the Ivy classpath container within the specified Java project
     * 
     * @param javaProject
     *            the project to search into
     * @return the Ivy classpath container if found, otherwise return <code>null</code>
     */
    public static IvyClasspathContainer getIvyClasspathContainer(IJavaProject javaProject) {
        try {
            IClasspathEntry[] entries = javaProject.getRawClasspath();
            for (int i = 0; i < entries.length; i++) {
                IClasspathEntry entry = entries[i];
                if (entry != null && entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
                    IPath path = entry.getPath();
                    if (isIvyClasspathContainer(path)) {
                        IClasspathContainer cp = JavaCore.getClasspathContainer(path, javaProject);
                        if (cp instanceof IvyClasspathContainer) {
                            return (IvyClasspathContainer) cp;
                        }
                    }
                }
            }
        } catch (JavaModelException e) {
            // unless there are issues with the JDT, this should never happen
            IvyPlugin.log(e);
        }
        return null;
    }

    public static List split(String str) {
        String[] terms = str.split(",");
        List ret = new ArrayList();
        for (int i = 0; i < terms.length; i++) {
            String t = terms[i].trim();
            if (t.length() > 0) {
                ret.add(t);
            }
        }
        return ret;
    }

    public static String concat(Collection/* <String> */list) {
        StringBuffer b = new StringBuffer();
        Iterator it = list.iterator();
        while (it.hasNext()) {
            b.append(it.next());
            if (it.hasNext()) {
                b.append(",");
            }
        }
        return b.toString();
    }

    /**
     * Just a verbatim copy of the internal Eclipse function:
     * {@link JavaDocLocations#getLibraryJavadocLocation(IClasspathEntry)}
     * 
     * @param entry
     * @return
     */
    public static URL getLibraryJavadocLocation(IClasspathEntry entry) {
        if (entry == null) {
            throw new IllegalArgumentException("Entry must not be null"); //$NON-NLS-1$
        }

        int kind = entry.getEntryKind();
        if (kind != IClasspathEntry.CPE_LIBRARY && kind != IClasspathEntry.CPE_VARIABLE) {
            throw new IllegalArgumentException("Entry must be of kind CPE_LIBRARY or CPE_VARIABLE"); //$NON-NLS-1$
        }

        IClasspathAttribute[] extraAttributes = entry.getExtraAttributes();
        for (int i = 0; i < extraAttributes.length; i++) {
            IClasspathAttribute attrib = extraAttributes[i];
            if (IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME.equals(attrib.getName())) {
                try {
                    return new URL(attrib.getValue());
                } catch (MalformedURLException e) {
                    return null;
                }
            }
        }
        return null;
    }

}
