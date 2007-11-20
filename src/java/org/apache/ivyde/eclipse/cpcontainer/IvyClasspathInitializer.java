package org.apache.ivyde.eclipse.cpcontainer;


import org.apache.ivyde.eclipse.IvyPlugin;
import org.apache.ivyde.eclipse.cpcontainer.fragmentinfo.IPackageFragmentExtraInfo;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.corext.javadoc.JavaDocLocations;
import org.eclipse.swt.widgets.Display;

/**
 * Initializer the ivy class path container. It will create a container from the persisted class
 * path entries, and then schedule the refresh of the container.
 */
public class IvyClasspathInitializer extends ClasspathContainerInitializer {

    /**
     * Initialize the container with the "persisted" class path entries, and then schedule the
     * refresh
     */
	public void initialize(IPath containerPath, IJavaProject project) throws CoreException {
        if (IvyClasspathContainer.isIvyClasspathContainer(containerPath)) {
            String ivyFilePath = IvyClasspathContainer.getIvyFilePath(containerPath);
            String[] confs = IvyClasspathContainer.getConfigurations(containerPath);

            // try to get an existing one
            IClasspathContainer container = null;
            try {
                container = JavaCore.getClasspathContainer(containerPath, project);
            } catch (JavaModelException ex) {
                IvyPlugin.log(IStatus.ERROR, "Unable to get container for "
                        + containerPath.toString(), ex);
                return;
            }

            if (container == null) {
                container = new IvyClasspathContainer(project, containerPath, ivyFilePath, confs,
                        new IClasspathEntry[0]);
            } else if (!(container instanceof IvyClasspathContainer)) {
                // this might be the persisted one : reuse the persisted entries
                container = new IvyClasspathContainer(project, containerPath, ivyFilePath, confs,
                        container.getClasspathEntries());
            }

            try {
                JavaCore.setClasspathContainer(containerPath, new IJavaProject[] {project},
                    new IClasspathContainer[] {container}, null);
            } catch (JavaModelException ex) {
                IvyPlugin.log(IStatus.ERROR, "Unable to set container for "
                        + containerPath.toString(), ex);
            }

            // now refresh the container to be synchronized with the ivy.xml
            ((IvyClasspathContainer) container).refresh(false);
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


