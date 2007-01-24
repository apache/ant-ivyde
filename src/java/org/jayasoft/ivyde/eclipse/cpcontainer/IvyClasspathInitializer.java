package org.jayasoft.ivyde.eclipse.cpcontainer;


import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.corext.javadoc.JavaDocLocations;
import org.eclipse.swt.widgets.Display;
import org.jayasoft.ivyde.eclipse.IvyPlugin;
import org.jayasoft.ivyde.eclipse.cpcontainer.fragmentinfo.IPackageFragmentExtraInfo;

/**
 *
 */
public class IvyClasspathInitializer extends ClasspathContainerInitializer {

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.ClasspathContainerInitializer#initialize(org.eclipse.core.runtime.IPath, org.eclipse.jdt.core.IJavaProject)
	 */
	public void initialize(IPath containerPath, IJavaProject project) throws CoreException {
        if (IvyClasspathContainer.isIvyClasspathContainer(containerPath)) {
    	    try {
                String ivyFilePath = IvyClasspathContainer.getIvyFilePath(containerPath);
                String[] confs = IvyClasspathContainer.getConfigurations(containerPath);
                IClasspathContainer ivyClasspathContainer = (IClasspathContainer)JavaCore.getClasspathContainer(containerPath, project);
                if (!(ivyClasspathContainer instanceof IvyClasspathContainer)) {
                    ivyClasspathContainer = new IvyClasspathContainer( project, containerPath, ivyFilePath, confs );
                }
                JavaCore.setClasspathContainer(containerPath, new IJavaProject[] { project },   new IClasspathContainer[] { ivyClasspathContainer }, null);
            } catch (JavaModelException e) {
                e.printStackTrace();
            }
        }
	}



    /* (non-Javadoc)
	 * @see org.eclipse.jdt.core.ClasspathContainerInitializer#canUpdateClasspathContainer(org.eclipse.core.runtime.IPath, org.eclipse.jdt.core.IJavaProject)
	 */
	public boolean canUpdateClasspathContainer(IPath containerPath, IJavaProject project) {
		return true;
	}

	/**
	 * @see org.eclipse.jdt.core.ClasspathContainerInitializer#requestClasspathContainerUpdate(org.eclipse.core.runtime.IPath, org.eclipse.jdt.core.IJavaProject, org.eclipse.jdt.core.IClasspathContainer)
	 */
	public void requestClasspathContainerUpdate(IPath containerPath, final IJavaProject project, IClasspathContainer containerSuggestion) throws CoreException {
	    if (IvyClasspathContainer.isIvyClasspathContainer(containerPath)) {
	        IClasspathEntry ice[] = containerSuggestion.getClasspathEntries();
            IPackageFragmentExtraInfo ei = IvyPlugin.getDefault().getPackageFragmentExtraInfo();
	        for (int i = 0; i < ice.length; i++) {
	            IClasspathEntry entry = ice[i];
	            IPath path = entry.getSourceAttachmentPath();
	            String entryPath = entry.getPath().toPortableString();
	            ei.setSourceAttachmentPath(containerPath, entryPath, path);
	            ei.setSourceAttachmentRootPath(containerPath, entryPath, path);
                ei.setJavaDocLocation(containerPath, entryPath, JavaDocLocations.getLibraryJavadocLocation(entry));
	        }
            //force refresh of ivy classpath entry in ui thread 
            Display.getDefault().asyncExec(new Runnable() {
                public void run() {
                    IvyClasspathUtil.refreshContainer(project);
                }
            });
	    }
	}

	/**
	 * @see org.eclipse.jdt.core.ClasspathContainerInitializer#getDescription(org.eclipse.core.runtime.IPath, org.eclipse.jdt.core.IJavaProject)
	 */
	public String getDescription(IPath containerPath, IJavaProject project) {
		return "my description";
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.ClasspathContainerInitializer#getComparisonID(org.eclipse.core.runtime.IPath, org.eclipse.jdt.core.IJavaProject)
	 */
	public Object getComparisonID(IPath containerPath, IJavaProject project) {
		return project.getProject().getName()+"/"+containerPath;
	}
}


