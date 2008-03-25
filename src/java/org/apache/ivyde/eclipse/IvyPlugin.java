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
package org.apache.ivyde.eclipse;

import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.apache.ivy.Ivy;
import org.apache.ivy.util.Message;
import org.apache.ivyde.eclipse.cpcontainer.IvyClasspathContainer;
import org.apache.ivyde.eclipse.cpcontainer.IvyClasspathUtil;
import org.apache.ivyde.eclipse.cpcontainer.fragmentinfo.IPackageFragmentExtraInfo;
import org.apache.ivyde.eclipse.cpcontainer.fragmentinfo.PreferenceStoreInfo;
import org.apache.ivyde.eclipse.ui.console.IvyConsole;
import org.apache.ivyde.eclipse.ui.preferences.IvyDEPreferenceStoreHelper;
import org.apache.ivyde.eclipse.ui.preferences.PreferenceConstants;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The main plugin class to be used in the desktop.
 */
public class IvyPlugin extends AbstractUIPlugin {

    /** The ID of IvyDE plugin */
    public static final String ID = "org.apache.ivyde.eclipse";

    // The shared instance.
    private static IvyPlugin plugin;

    // Resource bundle.
    private ResourceBundle resourceBundle;

    private IvyConsole console;

    private IvyDEPreferenceStoreHelper prefStoreHelper;

    private IJavaModel javaModel;

    private BundleContext bundleContext;

    /**
     * The constructor.
     */
    public IvyPlugin() {
        super();
        plugin = this;
    }

    /**
     * This method is called upon plug-in activation
     */
    public void start(BundleContext context) throws Exception {
        super.start(context);
        this.bundleContext = context;
        log(IStatus.INFO, "starting IvyDE plugin", null);
        try {
            console = new IvyConsole();
        } catch (RuntimeException e) {
            // Don't let the console bring down the CVS UI
            log(IStatus.ERROR, "Errors occurred starting the Ivy console", e);
        }
        javaModel = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot());
        prefStoreHelper = new IvyDEPreferenceStoreHelper(getPreferenceStore());
        getPreferenceStore().addPropertyChangeListener(new IPropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent event) {
                try {
                    if (event.getProperty() == PreferenceConstants.IVYSETTINGS_PATH
                            || event.getProperty() == PreferenceConstants.ACCEPTED_TYPES
                            || event.getProperty() == PreferenceConstants.SOURCES_TYPES
                            || event.getProperty() == PreferenceConstants.JAVADOC_TYPES
                            || event.getProperty() == PreferenceConstants.SOURCES_SUFFIXES
                            || event.getProperty() == PreferenceConstants.JAVADOC_SUFFIXES
                            || event.getProperty() == PreferenceConstants.DO_RETRIEVE
                            || event.getProperty() == PreferenceConstants.RETRIEVE_PATTERN
                            || event.getProperty() == PreferenceConstants.ALPHABETICAL_ORDER) {
                        prefStoreChanged();
                    }
                } catch (JavaModelException e) {
                    MessageDialog.openError(IvyPlugin.getDefault().getWorkbench()
                            .getActiveWorkbenchWindow().getShell(),
                        "Unable to trigger the update the IvyDE classpath containers", e
                                .getMessage());
                }
            }
        });
        log(IStatus.INFO, "IvyDE plugin started", null);
    }

    /**
     * This method is called when the plug-in is stopped
     */
    public void stop(BundleContext context) throws Exception {
        super.stop(context);
        plugin = null;
        resourceBundle = null;
        // if (console != null)
        // console.shutdown();
    }

    void prefStoreChanged() throws JavaModelException {
        IJavaProject[] projects = plugin.javaModel.getJavaProjects();
        for (int i = 0; i < projects.length; i++) {
            IvyClasspathContainer cp = IvyClasspathUtil.getIvyClasspathContainer(projects[i]);
            if (cp != null && !cp.getConf().isProjectSpecific()) {
                cp.scheduleRefresh(false);
            }
        }
    }

    /**
     * Convenience method for logging statuses to the plugin log
     * 
     * @param status
     *            the status to log
     */
    public static void log(IStatus status) {
        getDefault().getLog().log(status);
    }

    public static void log(CoreException e) {
        log(e.getStatus().getSeverity(), "IvyDE internal error", e);
    }

    /**
     * Log the given exception along with the provided message and severity indicator
     */
    public static void log(int severity, String message, Throwable e) {
        log(new Status(severity, ID, 0, message, e));
    }

    /**
     * Returns the shared instance.
     * 
     * @return the plugin instance
     */
    public static IvyPlugin getDefault() {
        return plugin;
    }

    /**
     * Returns the active workbench shell
     * 
     * @return the active workbench shell
     */
    public static Shell getActiveWorkbenchShell() {
        IWorkbenchWindow workBenchWindow = getActiveWorkbenchWindow();
        if (workBenchWindow == null) {
            return null;
        }
        return workBenchWindow.getShell();
    }

    /**
     * Returns the active workbench page or <code>null</code> if none.
     * 
     * @return the active workbench page
     */
    public static IWorkbenchPage getActivePage() {
        IWorkbenchWindow window = getActiveWorkbenchWindow();
        if (window != null) {
            return window.getActivePage();
        }
        return null;
    }

    /**
     * Returns the active workbench window
     * 
     * @return the active workbench window
     */
    public static IWorkbenchWindow getActiveWorkbenchWindow() {
        if (getDefault() == null) {
            return null;
        }
        IWorkbench workBench = getDefault().getWorkbench();
        if (workBench == null) {
            return null;
        }
        return workBench.getActiveWorkbenchWindow();
    }

    /**
     * Returns the string from the plugin's resource bundle, or 'key' if not found.
     */
    public static String getResourceString(String key) {
        ResourceBundle bundle = IvyPlugin.getDefault().getResourceBundle();
        try {
            return (bundle != null) ? bundle.getString(key) : key;
        } catch (MissingResourceException e) {
            return key;
        }
    }

    /**
     * Returns the plugin's resource bundle,
     */
    public ResourceBundle getResourceBundle() {
        try {
            if (resourceBundle == null)
                resourceBundle = ResourceBundle.getBundle("org.apache.ivyde.eclipse.IvyPluginResources");
        } catch (MissingResourceException x) {
            resourceBundle = new ResourceBundle() {
                protected Object handleGetObject(String key) {
                    return null;
                }

                public Enumeration getKeys() {
                    return Collections.enumeration(Collections.EMPTY_LIST);
                }

            };
        }
        return resourceBundle;
    }

    /**
     * Returns an image descriptor for the image file at the given plug-in relative path.
     * 
     * @param path
     *            the path
     * @return the image descriptor
     */
    public static ImageDescriptor getImageDescriptor(String path) {
        return AbstractUIPlugin.imageDescriptorFromPlugin(ID, path);
    }

    private static class IvyConfig {
        Ivy ivy;

        long configTime = -1;
    }

    private Map/* <String, IvyConfig> */ivyBySettings = new HashMap();

    private PreferenceStoreInfo _packageExtraInfo;

    /**
     * @return the helper around the plugin preference store
     */
    public static IvyDEPreferenceStoreHelper getPreferenceStoreHelper() {
        return plugin.prefStoreHelper;
    }

    /**
     * Get the Ivy instance for the specified project and the settings of the configured container
     * on the project.
     * 
     * @param javaProject
     *            the Java project
     * @param ivySettingsPath
     *            the settings to use
     * @return the configured Ivy instance, <code>null</code> if it failed
     */
    // TODO: check that every caller of this function can properly handle a returned null
    public static synchronized Ivy getIvy(IJavaProject javaProject) {
        IvyClasspathContainer cp;
        try {
            cp = IvyClasspathUtil.getIvyClassPathContainer(javaProject);
        } catch (JavaModelException e) {
            // TODO log and better handle the error
            return null;
        }
        if (cp == null) {
            return null;
        }
        return getIvy(javaProject, cp.getConf().getIvySettingsPath());
    }

    /**
     * Get the Ivy instance for the specified project and the specified settings
     * <p>
     * 
     * @param javaProject
     *            the Java project
     * @param ivySettingsPath
     *            the settings to use
     * @return the configured Ivy instance, <code>null</code> if it failed
     */
    // TODO: check that every caller of this function can properly handle a returned null
    public static synchronized Ivy getIvy(IJavaProject javaProject, String ivySettingsPath) {
        IvyConfig ic;
        try {
            if (ivySettingsPath == null || ivySettingsPath.trim().length() == 0) {
                // no settings specified, so take the default one
                return getDefaultIvy();
            }

            ic = (IvyConfig) plugin.ivyBySettings.get(ivySettingsPath);
            if (ic == null) {
                ic = new IvyConfig();
                plugin.ivyBySettings.put(ivySettingsPath, ic);
            }

            // before returning the found ivy, try to refresh it if the settings changed

            URL url = new URL(ivySettingsPath);
            if (url.getProtocol().startsWith("file")) {
                File file = new File(url.getPath());

                // BEGIN - JIRA: IVYDE-25 by Peter Chanthamynavong
                // Getting an Absolute Filename Path from a Relative Filename Path for the
                // current project
                if (!file.exists()) {
                    IProject project = javaProject.getProject();
                    File loc = project.getLocation().toFile();
                    file = new File(loc, url.getPath());
                    Message.info("\n\nIVYDE: ivysettings from relative path: "
                            + file.getAbsolutePath());
                }
                // END - JIRA: IVYDE-25

                if (!file.exists()) {
                    MessageDialog
                            .openWarning(
                                getActiveWorkbenchShell(),
                                "No ivyConf found",
                                ivySettingsPath
                                        + " ivyconf cannot be found.\nPlease set your ivy conf url in the preference or in your project properties to be able to use IvyDE");
                    return null;
                }

                if (file.lastModified() != ic.configTime) {
                    ic.ivy = new Ivy();
                    if (ic.configTime == -1) {
                        Message.info("\n\n");
                    } else {
                        Message.info("\n\nIVYDE: ivysettings has changed, configuring ivy again\n");
                    }
                    ic.ivy.configure(file);
                    ic.configTime = file.lastModified();
                }

            } else {
                // an URL but not a file
                if (ic.ivy == null) {
                    ic.ivy = new Ivy();
                    ic.ivy.configure(url);
                }
            }
            return ic.ivy;
        } catch (Exception e) {
            MessageDialog
                    .openWarning(
                        getActiveWorkbenchShell(),
                        "Bad ivySetting found",
                        "Problem occured while using "
                                + ivySettingsPath
                                + " to configure Ivy.\n"
                                + "Please set your ivy settings url properly in the preference or in the project properties to be able to use IvyDE.\n"
                                + "Exception message: " + e.getMessage());
            log(IStatus.WARNING, "Problem occured while using " + ivySettingsPath
                    + " to configure Ivy", e);
            Message.warn("IVYDE: Problem occured while using " + ivySettingsPath
                    + " to configure Ivy. See error log for details");
            plugin.ivyBySettings.remove(ivySettingsPath);
            return null;
        }
    }

    private static Ivy getDefaultIvy() {
        IvyConfig ic = (IvyConfig) plugin.ivyBySettings.get(null);
        if (ic == null) {
            ic = new IvyConfig();
            ic.ivy = new Ivy();
            try {
                ic.ivy.configureDefault();
                plugin.ivyBySettings.put(null, ic);
            } catch (Exception ex) {
                MessageDialog
                        .openWarning(
                            getActiveWorkbenchShell(),
                            "Impossible to configure Ivy",
                            "Problem occured while configuring Ivy with its default settings.\n"
                                    + "Please set an ivy settings url properly in the preference or in the project properties to be able to use IvyDE.\n"
                                    + "Exception message: " + ex.getMessage());
                log(IStatus.WARNING,
                    "Problem occured while configuring Ivy with its default settings.", ex);
                Message
                        .warn("IVYDE: Problem occured while configuring Ivy with its default settings. See error log for details");
                return null;
            }
        }
        return ic.ivy;
    }

    public IvyConsole getConsole() {
        return console;
    }

    public IPackageFragmentExtraInfo getPackageFragmentExtraInfo() {
        if (_packageExtraInfo == null) {
            _packageExtraInfo = new PreferenceStoreInfo(getPreferenceStore());
        }
        return _packageExtraInfo;
    }

    public BundleContext getBundleContext() {
      return this.bundleContext;
    }
}
