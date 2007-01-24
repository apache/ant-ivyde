/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package org.apache.ivyde.eclipse.cpcontainer;

import java.util.Locale;

import org.apache.ivyde.eclipse.IvyPlugin;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

public class FileExtFilter extends ViewerFilter {

    private String[] fTargetExtension;

    public FileExtFilter(String[] targetExtension) {
        fTargetExtension = targetExtension;
    }

    public boolean select(Viewer viewer, Object parent, Object element) {
        if (element instanceof IFile) {
            for (int i = 0; i < fTargetExtension.length; i++) {
                if (((IFile)element).getName().toLowerCase(Locale.ENGLISH).endsWith("." + fTargetExtension[i])) {
                    return true; 
                }
                return false;
            }
        }

        if (element instanceof IContainer){ // i.e. IProject, IFolder
            try {
                IResource[] resources = ((IContainer)element).members();
                for (int i = 0; i < resources.length; i++){
                    if (select(viewer, parent, resources[i]))
                        return true;
                }
            } catch (CoreException e) {
                IvyPlugin.log(e);
            }
        }
        return false;
    }

}
