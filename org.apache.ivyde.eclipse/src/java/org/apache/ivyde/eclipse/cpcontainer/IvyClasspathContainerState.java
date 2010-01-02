/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.ivyde.eclipse.cpcontainer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.cache.DefaultRepositoryCacheManager;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.parser.ModuleDescriptorParserRegistry;
import org.apache.ivy.util.Message;
import org.apache.ivyde.eclipse.FakeProjectManager;
import org.apache.ivyde.eclipse.IvyDEException;
import org.apache.ivyde.eclipse.IvyPlugin;
import org.apache.ivyde.eclipse.workspaceresolver.WorkspaceIvySettings;
import org.apache.ivyde.eclipse.workspaceresolver.WorkspaceResolver;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.BundleContext;

/**
 * This class is a front end to the container configuration. It computes the configuration status,
 * build the Ivy bean on demand and can cache it.
 */
public class IvyClasspathContainerState {

    private Ivy ivy;

    private long ivySettingsLastModified = -1;

    private ModuleDescriptor md;

    private IvyClasspathContainerConfiguration conf;

    public IvyClasspathContainerState(IvyClasspathContainerConfiguration conf) {
        this.conf = conf;
    }

    public void setConf(IvyClasspathContainerConfiguration conf) {
        this.conf = conf;
        md = null;
        ivy = null;
        ivySettingsLastModified = -1;
    }

    public void setIvySettingsLastModified(long ivySettingsLastModified) {
        this.ivySettingsLastModified = ivySettingsLastModified;
    }

    private void setConfStatus(IvyDEException ex) {
        if (ex != null) {
            setResolveStatus(new Status(IStatus.ERROR, IvyPlugin.ID, IStatus.ERROR,
                    ex.getMessage(), ex.getCause()));
        } else {
            setResolveStatus(Status.OK_STATUS);
        }
    }

    public void setResolveStatus(IStatus status) {
        if (FakeProjectManager.isFake(conf.getJavaProject())) {
            return;
        }
        IProject p = conf.getJavaProject().getProject();
        try {
            p.deleteMarkers(IvyPlugin.MARKER_ID, true, IResource.DEPTH_INFINITE);
            if (status == Status.OK_STATUS) {
                return;
            }
            IResource r = conf.getJavaProject().getProject().getFile(conf.getIvyXmlPath());
            if (!r.exists()) {
                r = p;
            }
            IMarker marker = r.createMarker(IvyPlugin.MARKER_ID);
            marker.setAttribute(IMarker.MESSAGE, status.getMessage());
            switch (status.getSeverity()) {
                case IStatus.ERROR:
                    marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
                    break;
                case IStatus.WARNING:
                    marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
                    break;
                case IStatus.INFO:
                    marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO);
                    break;
                default:
                    IvyPlugin.log(IStatus.WARNING, "Unsupported resolve status: "
                            + status.getSeverity(), null);
            }
        } catch (CoreException e) {
            IvyPlugin.log(e);
        }
    }

    public Ivy getCachedIvy() throws IvyDEException {
        if (ivy != null) {
            return ivy;
        }
        return getIvy();
    }

    public Ivy getIvy() throws IvyDEException {
        try {
            return doGetIvy();
        } catch (IvyDEException e) {
            e.contextualizeMessage("Error while resolving the ivy instance for " + this.toString());
            throw e;
        }
    }

    private Ivy doGetIvy() throws IvyDEException {
        String settingsPath = conf.getInheritedIvySettingsPath();
        if (settingsPath == null || settingsPath.trim().length() == 0) {
            // no settings specified, so take the default one
            if (ivy == null) {
                IvySettings ivySettings = createIvySettings();
                try {
                    ivySettings.loadDefault();
                } catch (ParseException e) {
                    IvyDEException ex = new IvyDEException(
                            "Parsing error of the default Ivy settings",
                            "The default Ivy settings file could not be parsed: " + e.getMessage(),
                            e);
                    setConfStatus(ex);
                    throw ex;
                } catch (IOException e) {
                    IvyDEException ex = new IvyDEException(
                            "Read error of the default Ivy settings",
                            "The default Ivy settings file could not be read: "
                            + e.getMessage(), e);
                    setConfStatus(ex);
                    throw ex;
                }
                ivy = Ivy.newInstance(ivySettings);
            }
            setConfStatus(null);
            return ivy;
        }

        // before returning the found ivy, try to refresh it if the settings changed
        URL url;
        try {
            url = new URL(settingsPath);
        } catch (MalformedURLException e) {
            IvyDEException ex = new IvyDEException("Incorrect url of the Ivy settings",
                    "The Ivy settings url '" + settingsPath + "' is incorrect: " + e.getMessage(),
                    e);
            setConfStatus(ex);
            throw ex;
        }
        if (url.getProtocol().startsWith("file")) {
            File file = new File(url.getPath());
            return getIvy(file);
        } else {
            // an URL but not a file
            if (ivy == null || ivySettingsLastModified == -1) {
                IvySettings ivySettings = createIvySettings();
                try {
                    ivySettings.load(url);
                    ivySettingsLastModified = 0;
                } catch (ParseException e) {
                    IvyDEException ex = new IvyDEException("Parsing error of the Ivy settings",
                            "The ivy settings file '" + settingsPath + "' could not be parsed: "
                                    + e.getMessage(), e);
                    setConfStatus(ex);
                    throw ex;
                } catch (IOException e) {
                    IvyDEException ex = new IvyDEException("Read error of the Ivy settings",
                            "The ivy settings file '" + settingsPath + "' could not be read: "
                                    + e.getMessage(), e);
                    setConfStatus(ex);
                    throw ex;
                }
                ivy = Ivy.newInstance(ivySettings);
            }
        }
        setConfStatus(null);
        return ivy;
    }

    private Ivy getIvy(File file) throws IvyDEException {
        String ivySettingsPath = conf.getInheritedIvySettingsPath();
        if (!file.exists()) {
            IvyDEException ex = new IvyDEException("Ivy settings file not found",
                    "The Ivy settings file '" + ivySettingsPath + "' cannot be found", null);
            setConfStatus(ex);
            throw ex;
        }

        if (file.lastModified() != ivySettingsLastModified
                || !conf.getInheritedLoadSettingsOnDemandPath()) {
            IvySettings ivySettings = createIvySettings();
            if (ivySettingsLastModified == -1) {
                Message.info("\n\n");
            } else {
                Message.info("\n\nIVYDE: ivysettings has changed, configuring ivy again\n");
            }
            try {
                ivySettings.load(file);
            } catch (ParseException e) {
                IvyDEException ex = new IvyDEException("Parsing error of the Ivy settings",
                        "The ivy settings file '" + ivySettingsPath + "' could not be parsed: "
                                + e.getMessage(), e);
                setConfStatus(ex);
                throw ex;
            } catch (IOException e) {
                IvyDEException ex = new IvyDEException("Read error of the Ivy settings",
                        "The ivy settings file '" + ivySettingsPath + "' could not be read: "
                                + e.getMessage(), e);
                setConfStatus(ex);
                throw ex;
            }
            ivy = Ivy.newInstance(ivySettings);
            ivySettingsLastModified = file.lastModified();
        }
        return ivy;
    }

    private IvySettings createIvySettings() throws IvyDEException {
        IvySettings ivySettings;
        if (conf.isInheritedResolveInWorkspace()) {
            ivySettings = new WorkspaceIvySettings(conf.getJavaProject());
            DefaultRepositoryCacheManager cacheManager = new DefaultRepositoryCacheManager();
            BundleContext bundleContext = IvyPlugin.getDefault().getBundleContext();
            cacheManager.setBasedir(bundleContext.getDataFile("ivyde-workspace-resolver-cache"));
            cacheManager.setCheckmodified(true);
            cacheManager.setUseOrigin(true);
            cacheManager.setName(WorkspaceResolver.CACHE_NAME);
            ivySettings.addRepositoryCacheManager(cacheManager);
        } else {
            ivySettings = new IvySettings();
        }
        IPath location = conf.getJavaProject().getProject().getLocation();
        if (location != null) {
            ivySettings.setBaseDir(location.toFile());
        }
        Collection propFiles = conf.getInheritedPropertyFiles();
        if (propFiles != null) {
            Iterator iter = propFiles.iterator();
            while (iter.hasNext()) {
                String file = (String) iter.next();
                InputStream is;
                Path p = new Path(file);
                if (conf.getJavaProject() != null && !p.isAbsolute()) {
                    try {
                        is = new FileInputStream(conf.getJavaProject().getProject().getLocation()
                                .append(file).toFile());
                    } catch (FileNotFoundException e) {
                        IvyDEException ex = new IvyDEException("Property file not found",
                                "The property file '" + file + "' could not be found", e);
                        setConfStatus(ex);
                        throw ex;
                    }
                } else {
                    try {
                        is = new FileInputStream(file);
                    } catch (FileNotFoundException e) {
                        IvyDEException ex = new IvyDEException("Property file not found",
                                "The property file '" + file + "' was not found", e);
                        setConfStatus(ex);
                        throw ex;
                    }
                }
                Properties props = new Properties();
                try {
                    props.load(is);
                } catch (IOException e) {
                    IvyDEException ex = new IvyDEException("Not a property file",
                            "The property file '" + file + "' could not be loaded", e);
                    setConfStatus(ex);
                    throw ex;
                }
                try {
                    is.close();
                } catch (IOException e) {
                    // don't care
                }

                Iterator keys = props.keySet().iterator();
                while (keys.hasNext()) {
                    String key = (String) keys.next();
                    String value = props.getProperty(key);
                    ivySettings.setVariable(key, value);
                }
            }
        }
        return ivySettings;
    }

    public File getIvyFile() {
        File file = new File(conf.getIvyXmlPath());
        if (!file.isAbsolute() && !FakeProjectManager.isFake(conf.getJavaProject())) {
            Path ivyPath = new Path(conf.getIvyXmlPath());
            // get the file location in Eclipse "space"
            IFile ivyfile = conf.getJavaProject().getProject().getFile(ivyPath);
            // compute the actual file system location, following Eclipse's linked folders (see
            // IVYDE-211)
            IPath ivyLocation = ivyfile.getLocation();
            // get the corresponding java.io.File instance
            file = ivyLocation.toFile();
        }
        return file;
    }

    public ModuleDescriptor getCachedModuleDescriptor() throws IvyDEException {
        if (md != null) {
            return md;
        }
        return getModuleDescriptor(getCachedIvy());
    }

    public ModuleDescriptor getModuleDescriptor() throws IvyDEException {
        return getModuleDescriptor(getIvy());
    }

    public ModuleDescriptor getModuleDescriptor(Ivy i) throws IvyDEException {
        File file = getIvyFile();
        if (!file.exists()) {
            IvyDEException ex = new IvyDEException("Ivy file not found", "The ivy.xml file '"
                    + file.getAbsolutePath() + "' was not found", null);
            setConfStatus(ex);
            throw ex;
        }
        try {
            md = ModuleDescriptorParserRegistry.getInstance().parseDescriptor(i.getSettings(),
                file.toURL(), false);
            setConfStatus(null);
            return md;
        } catch (MalformedURLException e) {
            IvyDEException ex = new IvyDEException("Incorrect URL of the Ivy file",
                    "The URL to the ivy.xml file is incorrect: '" + file.getAbsolutePath()
                    + "'", e);
            setConfStatus(ex);
            throw ex;
        } catch (ParseException e) {
            IvyDEException ex = new IvyDEException("Parsing error of the Ivy file",
                    "The ivy file '" + file.getAbsolutePath() + "' could not be parsed: "
                            + e.getMessage(), e);
            setConfStatus(ex);
            throw ex;
        } catch (IOException e) {
            IvyDEException ex = new IvyDEException("Read error of the Ivy file", "The ivy file '"
                    + file.getAbsolutePath() + "' could not be read: " + e.getMessage(), e);
            setConfStatus(ex);
            throw ex;
        }
    }

    public String toString() {
        return conf.toString();
    }

}
