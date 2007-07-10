package org.apache.ivyde.eclipse.cpcontainer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.ivyde.eclipse.IvyPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
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
	 * Adds an IvyDE classpath container to the list of existing classpath entries in the 
	 * given project.
	 * 
	 * @param project 
	 * 			the project to which the cp container should be added
	 * @param projectRelativePath 
	 * 			the path relative to the project of the module descriptor file
	 * 			to use for the classpath container
	 * @param confs 
	 * 			the configurations to use in the classpath container.
	 */
	public static void addCPContainer(
			IJavaProject project, IPath projectRelativePath, String confs) {
		try {
			IClasspathEntry newEntry = JavaCore.newContainerEntry(
					new Path(IvyClasspathContainer.IVY_CLASSPATH_CONTAINER_ID)
					.append(projectRelativePath)
					.append(confs));
			
			IClasspathEntry[] entries= project.getRawClasspath();
			
			List newEntries = new ArrayList(Arrays.asList(entries));
			newEntries.add(newEntry);
			entries = (IClasspathEntry[]) newEntries
					.toArray(new IClasspathEntry[newEntries.size()]);
			
			project.setRawClasspath(entries, project.getOutputLocation(), null);
		} catch (CoreException e) {
			IvyPlugin.getDefault().log(e);
		}
	}
	
    public static void refreshContainer() {
        IWorkbenchWindow activeWorkbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if(activeWorkbenchWindow != null) {
            ISelection sel = activeWorkbenchWindow.getSelectionService().getSelection();
            if(!(sel instanceof IStructuredSelection)) {
            		sel = activeWorkbenchWindow.getSelectionService().getSelection("org.eclipse.jdt.ui.PackageExplorer");
            }
            if (sel instanceof IStructuredSelection) {
                IStructuredSelection s = (IStructuredSelection)sel;
                try {
                    IClasspathContainer fContainer= getClassPathContainer(s.getFirstElement());
                    if (fContainer instanceof IvyClasspathContainer) {
                        IvyClasspathContainer ivycp = (IvyClasspathContainer)fContainer;
                        ivycp.refresh();
                    }
                } catch (JavaModelException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    public static void refreshContainer(IJavaProject project) {
	    	IvyClasspathContainer ivycp;
			try {
				ivycp = (IvyClasspathContainer)getIvyClassPathContainer(project);
				if(ivycp != null) {
					ivycp.refresh();
				}
			} catch (JavaModelException e) {
			}
    }
    
    private static IClasspathContainer getClassPathContainer(Object o) throws JavaModelException {
        if (o instanceof ClassPathContainer) {
            ClassPathContainer cp = (ClassPathContainer) o;
            IJavaProject project = cp.getJavaProject();
            return JavaCore.getClasspathContainer(cp.getClasspathEntry().getPath(), project);
        }
        if (o instanceof IJavaProject) {
            return getIvyClassPathContainer((IJavaProject) o);
        }
        if (o instanceof IJavaElement) {
            return getClassPathContainer(((IJavaElement) o).getParent());
        }
        return null;
    }
    
	public  static IvyClasspathContainer getIvyClassPathContainer(IJavaProject javaProject) throws JavaModelException {
		IClasspathEntry[] cpe = javaProject.getRawClasspath();
		for (int i = 0; i < cpe.length; i++) {
		    IClasspathEntry entry = cpe[i];
		    if (IvyClasspathContainer.isIvyClasspathContainer(entry.getPath())) {
		        return (IvyClasspathContainer) JavaCore.getClasspathContainer(entry.getPath(), javaProject);
		    }
		}
		return null;
	}
}
