package org.jayasoft.ivyde.eclipse.cpcontainer.core;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.corext.buildpath.ClasspathModifier;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.ClasspathModifierQueries.IAddLibrariesQuery;
import org.jayasoft.ivyde.eclipse.IvyPlugin;
import org.jayasoft.ivyde.eclipse.cpcontainer.IvyClasspathContainer;

public class AddClasspathContainer extends ClasspathModifier {

	/* (non-Javadoc)
	 * @see org.jayasoft.ivyde.eclipse.cpcontainer.core.v31.IAddClasspathContainer#addCPContainer(org.eclipse.jdt.core.IJavaProject, org.eclipse.core.runtime.IPath, java.lang.String)
	 */
	public void addCPContainer(IJavaProject project, IPath projectRelativePath, String confs) {
		try {
			addLibraries(getAddLibrariesQuery(projectRelativePath, confs), project, new NullProgressMonitor());
		} catch (CoreException e) {
			IvyPlugin.getDefault().log(e);
		}
	}

	private IAddLibrariesQuery getAddLibrariesQuery(final IPath projectRelativePath, final String confs) {
		return new IAddLibrariesQuery() {
			public IClasspathEntry[] doQuery(IJavaProject project,
					IClasspathEntry[] entries) {
				return new IClasspathEntry[] {JavaCore.newContainerEntry(
						new Path(IvyClasspathContainer.IVY_CLASSPATH_CONTAINER_ID)
						.append(projectRelativePath)
						.append(confs))
				};
			}
		
		};
	}
}
