package org.apache.ivyde.eclipse.cpcontainer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.ivyde.eclipse.IvyPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
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
    public static IvyClasspathContainer getIvyClasspathContainer(IStructuredSelection selection)
            throws JavaModelException {
        if (selection == null) {
            return null;
        }
        for (Iterator it = selection.iterator(); it.hasNext();) {
            Object element = it.next();
            IvyClasspathContainer cp = null;
            if (element instanceof IvyClasspathContainer) {
                return (IvyClasspathContainer) element;
            }
            if (element instanceof IJavaProject) {
                return getIvyClassPathContainer((IJavaProject) element);
            }
            if (element instanceof IAdaptable) {
                cp = (IvyClasspathContainer) ((IAdaptable) element)
                        .getAdapter(IvyClasspathContainer.class);
                if (cp == null) {
                    IJavaProject p = (IJavaProject) ((IAdaptable) element)
                            .getAdapter(IJavaProject.class);
                    if (p != null) {
                        cp = getIvyClassPathContainer(p);
                    }
                }
            }
            if (cp != null) {
                return cp;
            }
            if (element instanceof ClassPathContainer) {
                // we shouldn't check against internal JDT API but there are not adaptable to useful
                // class
                return getIvyClassPathContainer(((ClassPathContainer) element).getJavaProject());
            }
        }
        return null;
    }

    /**
     * Search the Ivy classpath container within the specified Java project
     * 
     * @param javaProject
     *            the project to search into
     * @return the Ivy classpath container if found, otherwise return <code>null</code>
     * @throws JavaModelException
     */
    public static IvyClasspathContainer getIvyClassPathContainer(IJavaProject javaProject)
            throws JavaModelException {
        IClasspathEntry[] cpe = javaProject.getRawClasspath();
        for (int i = 0; i < cpe.length; i++) {
            IClasspathEntry entry = cpe[i];
            if (IvyClasspathContainer.isIvyClasspathContainer(entry.getPath())) {
                return (IvyClasspathContainer) JavaCore.getClasspathContainer(entry.getPath(),
                    javaProject);
            }
        }
        return null;
    }
}
