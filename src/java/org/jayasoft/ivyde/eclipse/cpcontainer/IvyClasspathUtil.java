package org.jayasoft.ivyde.eclipse.cpcontainer;

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
import org.jayasoft.ivyde.eclipse.cpcontainer.core.AddClasspathContainer;

public class IvyClasspathUtil {
	private static AddClasspathContainer addClasspathContainer = new AddClasspathContainer();
	
	public static AddClasspathContainer getAddClasspathContainer() {
		return addClasspathContainer;
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
