package org.jayasoft.ivyde.eclipse.ui.core;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IStorageEditorInput;

public class IvyFileEditorInput implements IPathEditorInput, IStorageEditorInput, IFileEditorInput {
    private IFile _ivyFile;
    
    public IvyFileEditorInput(IFile input) {
        super();
        _ivyFile = input;
    }

    public boolean exists() {
        return _ivyFile.exists();
    }

    public ImageDescriptor getImageDescriptor() {
        return null;
    }

    public String getName() {
        return _ivyFile.getName();
    }
    
    /* (non-Javadoc)
     * Method declared on IEditorInput.
     */
    public String getToolTipText() {
        return _ivyFile.getFullPath().makeRelative().toString();
    }

    /* (non-Javadoc)
     * Method declared on IPathEditorInput
     * @since 3.0
     * @issue consider using an internal adapter for IPathEditorInput rather than adding this as API
     */
    public IPath getPath() {
        return _ivyFile.getLocation();
    }
    

    public String toString() {
        return getClass().getName() + "(" + _ivyFile.getFullPath() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
    }
    
    public Object getAdapter(Class adapter) {
        return null;
    }

    public IPersistableElement getPersistable() {
        return null;
    }

    public IStorage getStorage() throws CoreException {
        return _ivyFile;
    }
    
    public IFile getFile() {
        return _ivyFile;
    }
    
    public boolean equals(Object obj) {
        if (! (obj instanceof IFileEditorInput)) {
            return false;
        }
        IFileEditorInput o = (IFileEditorInput)obj;
        return getFile().equals(o.getFile());
    }
    
    public int hashCode() {
        return getFile().hashCode();
    }
}
