package org.apache.ivyde.eclipse.cpcontainer.fragmentinfo;

import java.net.URL;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

public interface IPackageFragmentExtraInfo {
    
    public IPath getSourceAttachment(Path path);
    
    public IPath getSourceAttachmentRoot(Path path);
    
    public IPath getDocAttachment(Path path);

    public void setSourceAttachmentPath(IPath containerPath, String entryPath, IPath sourcePath);

    public void setSourceAttachmentRootPath(IPath containerPath, String entryPath, IPath rootPath);

    public void setJavaDocLocation(IPath containerPath, String entryPath, URL libraryJavadocLocation);
}
