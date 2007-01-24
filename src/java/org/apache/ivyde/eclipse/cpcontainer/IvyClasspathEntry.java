package org.apache.ivyde.eclipse.cpcontainer;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IPackageFragmentRoot;


public class IvyClasspathEntry implements IClasspathEntry {

    Path path_;

    IvyClasspathEntry( Path path ) {
        path_ = path;
    }

    public int getContentKind() {
        return IPackageFragmentRoot.K_BINARY;
    }
    
    public int getEntryKind() {
        return CPE_LIBRARY;
    }
    
    public IPath[] getExclusionPatterns() {
        return null;
    }
    
    public IPath[] getInclusionPatterns() {
        return null;
    }
    
    public IPath getOutputLocation() {
        return null;
    }
    
    public IPath getPath() {
        return path_;
    }
    
    public IClasspathEntry getResolvedEntry() {
        return this;
    }
    
    public IPath getSourceAttachmentPath() {
        return null;
    }
    
    public IPath getSourceAttachmentRootPath() {
        return null;
    }
    
    public boolean isExported() {
        return false;
    }

    public boolean combineAccessRules() {
        // TODO Auto-generated method stub
        return false;
    }

    public IAccessRule[] getAccessRules() {
        // TODO Auto-generated method stub
        return null;
    }

    public IClasspathAttribute[] getExtraAttributes() {
        // TODO Auto-generated method stub
        return null;
    }
}
