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

import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.apache.ivyde.common.ivyfile.IvyFileResourceListener;
import org.apache.ivyde.eclipse.cpcontainer.IvyClasspathContainer;
import org.apache.ivyde.eclipse.cpcontainer.IvyClasspathUtil;
import org.apache.ivyde.eclipse.cpcontainer.IvyResolveJob;
import org.apache.ivyde.eclipse.cpcontainer.fragmentinfo.IPackageFragmentExtraInfo;
import org.apache.ivyde.eclipse.cpcontainer.fragmentinfo.PreferenceStoreInfo;
import org.apache.ivyde.eclipse.retrieve.RetrieveSetupManager;
import org.apache.ivyde.eclipse.ui.console.IvyConsole;
import org.apache.ivyde.eclipse.ui.editors.xml.ColorManager;
import org.apache.ivyde.eclipse.ui.preferences.IvyDEPreferenceStoreHelper;
import org.apache.ivyde.eclipse.ui.preferences.PreferenceConstants;
import org.apache.ivyde.eclipse.workspaceresolver.WorkspaceResourceChangeListener;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
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

    /** The ID of IvyDE problem markers */
    public static final String MARKER_ID = ID + ".marker";

    // The shared instance.
    private static IvyPlugin plugin;

    // Resource bundle.
    private ResourceBundle resourceBundle;

    private IvyConsole console;

    private IvyDEPreferenceStoreHelper prefStoreHelper;

    private IJavaModel javaModel;

    private BundleContext bundleContext;

    private ColorManager colorManager;

    private IvyResolveJob ivyResolveJob;

    private RetrieveSetupManager retrieveSetupManager;

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
        ivyResolveJob = new IvyResolveJob();
        
        retrieveSetupManager = new RetrieveSetupManager();

        javaModel = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot());
        
        colorManager = new ColorManager();
        colorManager.refreshFromStore(getPreferenceStore());

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
                            || event.getProperty() == PreferenceConstants.DO_RETRIEVE_DEPRECATED
                           || event.getProperty() == PreferenceConstants.RETRIEVE_PATTERN_DEPRECATED
                            || event.getProperty() == PreferenceConstants.DO_RETRIEVE
                            || event.getProperty() == PreferenceConstants.RETRIEVE_PATTERN
                            || event.getProperty() == PreferenceConstants.RETRIEVE_SYNC
                            || event.getProperty() == PreferenceConstants.ALPHABETICAL_ORDER
                            || event.getProperty() == PreferenceConstants.RESOLVE_IN_WORKSPACE) {
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

        try {
            console = new IvyConsole();
        } catch (RuntimeException e) {
            // Don't let the console bring down the IvyDE UI
            log(IStatus.ERROR, "Errors occurred starting the Ivy console", e);
        }

        // Listen for project open/close events to auto-update inter-project dependencies
        IWorkspace workspace = ResourcesPlugin.getWorkspace();

        workspace.addResourceChangeListener(new WorkspaceResourceChangeListener());
        workspace.addResourceChangeListener(new IvyFileResourceListener(),
            IResourceChangeEvent.PRE_BUILD);

        log(IStatus.INFO, "IvyDE plugin started", null);
    }

    /**
     * This method is called when the plug-in is stopped
     */
    public void stop(BundleContext context) throws Exception {
        super.stop(context);
        plugin = null;
        resourceBundle = null;
        colorManager = null;
        ivyResolveJob = null;
        retrieveSetupManager = null;
        // if (console != null)
        // console.shutdown();
    }

    void prefStoreChanged() throws JavaModelException {
        IJavaProject[] projects = plugin.javaModel.getJavaProjects();
        for (int i = 0; i < projects.length; i++) {
            List/* <IvyClasspathContainer> */containers = IvyClasspathUtil
                    .getIvyClasspathContainers(projects[i]);
            Iterator/* <IvyClasspathContainer> */itContainers = containers.iterator();
            while (itContainers.hasNext()) {
                IvyClasspathContainer ivycp = (IvyClasspathContainer) itContainers.next();
                if (!ivycp.getConf().isSettingsProjectSpecific()) {
                    ivycp.launchResolve(false, null);
                }
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
     * Utility class that tries to adapt a non null object to the specified type
     * 
     * @param object
     *            the object to adapt
     * @param type
     *            the class to adapt to
     * @return the adapted object
     */

    public static /*<T> T*/ Object adapt(Object object, Class/*<T>*/ type) {
        if (type.isInstance(object)) {
            return /*(T)*/ object;
        } else if (object instanceof IAdaptable) {
            return /*(T)*/ ((IAdaptable) object).getAdapter(type);
        }
        return /*(T)*/ Platform.getAdapterManager().getAdapter(object, type);
    }

    /**
     * Returns the plugin's resource bundle,
     */
    public ResourceBundle getResourceBundle() {
        try {
            if (resourceBundle == null) {
                resourceBundle = ResourceBundle
                        .getBundle("org.apache.ivyde.eclipse.IvyPluginResources");
            }
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

    private PreferenceStoreInfo packageExtraInfo;

    /**
     * @return the helper around the plugin preference store
     */
    public static IvyDEPreferenceStoreHelper getPreferenceStoreHelper() {
        return plugin.prefStoreHelper;
    }

    public IvyConsole getConsole() {
        return console;
    }

    public IPackageFragmentExtraInfo getPackageFragmentExtraInfo() {
        if (packageExtraInfo == null) {
            packageExtraInfo = new PreferenceStoreInfo(getPreferenceStore());
        }
        return packageExtraInfo;
    }

    public BundleContext getBundleContext() {
        return this.bundleContext;
    }

    public ColorManager getColorManager() {
        return colorManager;
    }

    public IvyResolveJob getIvyResolveJob() {
        return ivyResolveJob;
    }

    public RetrieveSetupManager getRetrieveSetupManager() {
        return retrieveSetupManager;
    }
}
