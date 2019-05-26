/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.ivyde.internal.eclipse;

import java.io.File;
import java.util.Collections;
import java.util.Enumeration;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ivy.Ivy;
import org.apache.ivyde.common.ivyfile.IvyFileResourceListener;
import org.apache.ivyde.eclipse.IvyDEsecurityHelper;
import org.apache.ivyde.eclipse.cp.IvyClasspathContainer;
import org.apache.ivyde.eclipse.cp.IvyClasspathContainerHelper;
import org.apache.ivyde.internal.eclipse.cpcontainer.IvyAttachmentManager;
import org.apache.ivyde.internal.eclipse.cpcontainer.IvyClasspathContainerSerializer;
import org.apache.ivyde.internal.eclipse.resolve.IvyResolveJob;
import org.apache.ivyde.internal.eclipse.retrieve.RetrieveSetupManager;
import org.apache.ivyde.internal.eclipse.ui.console.IvyConsole;
import org.apache.ivyde.internal.eclipse.ui.console.IvyConsoleFactory;
import org.apache.ivyde.internal.eclipse.ui.editors.xml.ColorManager;
import org.apache.ivyde.internal.eclipse.ui.preferences.IvyDEPreferenceStoreHelper;
import org.apache.ivyde.internal.eclipse.ui.preferences.PreferenceConstants;
import org.apache.ivyde.internal.eclipse.workspaceresolver.WorkspaceResourceChangeListener;
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
import org.eclipse.swt.widgets.Display;
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

    private static final Pattern IVY_VERSION_PATTERN = Pattern
            .compile("([0-9]+)\\.([0-9]+)\\.([0-9]+).*");

    /** The ID of IvyDE plugin. */
    public static final String ID = "org.apache.ivyde.eclipse";

    /** The ID of IvyDE problem markers. */
    public static final String MARKER_ID = ID + ".marker";

    // The shared instance.
    private static volatile IvyPlugin plugin;

    // Resource bundle.
    private ResourceBundle resourceBundle;

    private IvyConsole console;

    private IvyDEPreferenceStoreHelper prefStoreHelper;

    private BundleContext bundleContext;

    private ColorManager colorManager;

    private IvyResolveJob ivyResolveJob;

    private RetrieveSetupManager retrieveSetupManager;

    private WorkspaceResourceChangeListener workspaceListener;

    private IvyFileResourceListener ivyFileListener;

    private IPropertyChangeListener propertyListener;

    private IvyMarkerManager ivyMarkerManager;

    private boolean osgiAvailable;

    private boolean osgiClasspathAvailable;

    private IvyClasspathContainerSerializer ivyCpcSerializer;

    private IvyAttachmentManager ivyAttachmentManager;

    private int ivyVersionMajor = 0;

    private int ivyVersionMinor = 0;

    private int ivyVersionPatch = 0;

    /**
     * The constructor.
     */
    public IvyPlugin() {
        super();
        plugin = this;
    }

    /**
     * This method is called upon plug-in activation.
     *
     * @param context BundleContext
     * @throws Exception if something goes wrong
     */
    public void start(BundleContext context) throws Exception {
        super.start(context);
        this.bundleContext = context;
        logInfo("starting IvyDE plugin");

        if (IvyDEsecurityHelper.credentialsInSecureStorage()) {
            IvyDEsecurityHelper.cpyCredentialsFromSecureToIvyStorage();
            logInfo("Credentials loaded from secure storage");
        } else {
            logInfo("No credentials stored in secure storage");
        }

        Matcher matcher = IVY_VERSION_PATTERN.matcher(Ivy.getIvyVersion());
        if (matcher.matches()) {
            ivyVersionMajor = Integer.parseInt(matcher.group(1));
            ivyVersionMinor = Integer.parseInt(matcher.group(2));
            ivyVersionPatch = Integer.parseInt(matcher.group(3));
        }

        ivyResolveJob = new IvyResolveJob();

        retrieveSetupManager = new RetrieveSetupManager();

        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        workspace.addSaveParticipant(ID, retrieveSetupManager);

        colorManager = new ColorManager();
        Display.getDefault().asyncExec(new Runnable() {
            public void run() {
                colorManager.refreshFromStore(getPreferenceStore());
            }
        });

        prefStoreHelper = new IvyDEPreferenceStoreHelper(getPreferenceStore());

        propertyListener = new IPropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent event) {
                try {
                    if (PreferenceConstants.ALL.contains(event.getProperty())) {
                        prefStoreChanged();
                    }
                } catch (JavaModelException e) {
                    MessageDialog.openError(IvyPlugin.getDefault().getWorkbench()
                            .getActiveWorkbenchWindow().getShell(),
                        "Unable to trigger the update the IvyDE classpath containers",
                        e.getMessage());
                }
            }
        };
        getPreferenceStore().addPropertyChangeListener(propertyListener);

        Display.getDefault().asyncExec(new Runnable() {
            public void run() {
                try {
                    console = new IvyConsole();
                    if (prefStoreHelper.isOpenIvyConsoleOnStartup()) {
                        IvyConsoleFactory.showConsole();
                    }
                } catch (RuntimeException e) {
                    // Don't let the console bring down the IvyDE UI
                    logError("Errors occurred starting the Ivy console", e);
                }
            }
        });

        // Listen for project open/close events to auto-update inter-project dependencies
        workspaceListener = new WorkspaceResourceChangeListener();
        workspace.addResourceChangeListener(workspaceListener);
        ivyFileListener = new IvyFileResourceListener();
        workspace.addResourceChangeListener(ivyFileListener, IResourceChangeEvent.PRE_BUILD);

        ivyMarkerManager = new IvyMarkerManager();

        File stateLocation = getStateLocation().toFile();
        ivyAttachmentManager = new IvyAttachmentManager(
                new File(stateLocation, "attachment.properties"));
        File containersStateDir = new File(stateLocation, "cpstates");
        if (!containersStateDir.exists()) {
            containersStateDir.mkdirs();
        }
        ivyCpcSerializer = new IvyClasspathContainerSerializer(containersStateDir,
                ivyAttachmentManager);

        try {
            Class.forName("org.apache.ivy.osgi.core.ManifestParser");
            osgiAvailable = true;
            try {
                Class.forName("org.apache.ivy.osgi.core.BundleInfo")
                        .getDeclaredMethod("getClasspath");
                osgiClasspathAvailable = true;
            } catch (Exception e) {
                osgiClasspathAvailable = false;
            }
        } catch (Exception e) {
            osgiAvailable = false;
            osgiClasspathAvailable = false;
        }

        logInfo("IvyDE plugin started");
    }

    /**
     * This method is called when the plug-in is stopped.
     *
     * @param context BundleContext
     * @throws Exception if something goes wrong
     */
    public void stop(BundleContext context) throws Exception {
        super.stop(context);
        ivyCpcSerializer = null;
        ivyAttachmentManager = null;
        resourceBundle = null;
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        workspace.removeSaveParticipant(ID);
        colorManager = null;
        ivyMarkerManager = null;
        ivyResolveJob = null;
        retrieveSetupManager = null;
        workspace.removeResourceChangeListener(workspaceListener);
        workspaceListener = null;
        workspace.removeResourceChangeListener(ivyFileListener);
        ivyFileListener = null;

        getPreferenceStore().removePropertyChangeListener(propertyListener);
        propertyListener = null;

        if (console != null) {
            console.destroy();
        }
        plugin = null;
    }

    void prefStoreChanged() throws JavaModelException {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IJavaModel javaModel = JavaCore.create(workspace.getRoot());
        for (IJavaProject project : javaModel.getJavaProjects()) {
            for (IvyClasspathContainer container : IvyClasspathContainerHelper
                    .getContainers(project)) {
                if (!container.getConf().isSettingsProjectSpecific()) {
                    container.launchResolve(false, null);
                }
            }
        }
    }

    /**
     * Convenience method for logging statuses to the plugin log.
     *
     * @param status
     *            the status to log
     */
    public static void log(IStatus status) {
        getDefault().getLog().log(status);
        switch (status.getCode()) {
            case IStatus.ERROR:
                IvyDEMessage.error(status.getMessage(), status.getException());
                break;
            case IStatus.CANCEL:
            case IStatus.WARNING:
                IvyDEMessage.warn(status.getMessage(), status.getException());
                break;
            case IStatus.OK:
            case IStatus.INFO:
                IvyDEMessage.info(status.getMessage(), status.getException());
                break;
        }
    }

    public static void log(CoreException e) {
        log(e.getStatus().getSeverity(), "IvyDE internal error", e);
    }

    /**
     * Log the given exception along with the provided message and severity indicator.
     *
     * @param severity int
     * @param message String
     * @param e Throwable
     */
    public static void log(int severity, String message, Throwable e) {
        log(new Status(severity, ID, 0, message, e));
    }

    public static void logInfo(String message) {
        log(new Status(IStatus.INFO, ID, 0, message, null));
    }

    public static void logWarn(String message) {
        logWarn(message, null);
    }

    public static void logWarn(String message, Throwable e) {
        log(new Status(IStatus.WARNING, ID, 0, message, e));
    }

    public static void logError(String message) {
        logError(message, null);
    }

    public static void logError(String message, Throwable e) {
        log(new Status(IStatus.ERROR, ID, 0, message, e));
    }

    /**
     * Returns the shared instance.
     *
     * @return the plugin instance
     */
    public static IvyPlugin getDefault() {
        return plugin;
    }

    public boolean isOsgiAvailable() {
        return osgiAvailable;
    }

    public boolean isOsgiClasspathAvailable() {
        return osgiClasspathAvailable;
    }

    /**
     * Returns the active workbench shell.
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
     * Returns the active workbench window.
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
     *
     * @param key String
     * @return String
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
     * @param <T> type
     * @param object
     *            the object to adapt
     * @param type
     *            the class to adapt to
     * @return the adapted object
     */

    @SuppressWarnings("unchecked")
    public static <T> T adapt(Object object, Class<T> type) {
        if (type.isInstance(object)) {
            return (T) object;
        } else if (object instanceof IAdaptable) {
            return (T) ((IAdaptable) object).getAdapter(type);
        }
        return (T) Platform.getAdapterManager().getAdapter(object, type);
    }

    /**
     * Returns the plugin's resource bundle.
     *
     * @return ResourceBundle
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

                public Enumeration<String> getKeys() {
                    return Collections.emptyEnumeration();
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

    /**
     * @return the helper around the plugin preference store
     */
    public static IvyDEPreferenceStoreHelper getPreferenceStoreHelper() {
        return plugin.prefStoreHelper;
    }

    public IvyConsole getConsole() {
        return console;
    }

    public BundleContext getBundleContext() {
        return this.bundleContext;
    }

    public ColorManager getColorManager() {
        return colorManager;
    }

    public IvyMarkerManager getIvyMarkerManager() {
        return ivyMarkerManager;
    }

    public IvyClasspathContainerSerializer getIvyClasspathContainerSerializer() {
        return ivyCpcSerializer;
    }

    public IvyAttachmentManager getIvyAttachmentManager() {
        return ivyAttachmentManager;
    }

    public IvyResolveJob getIvyResolveJob() {
        return ivyResolveJob;
    }

    public RetrieveSetupManager getRetrieveSetupManager() {
        return retrieveSetupManager;
    }

    public boolean isIvyVersionGreaterOrEqual(int major, int minor, int patch) {
        if (ivyVersionMajor > major) {
            return true;
        } else if (ivyVersionMajor < major) {
            return false;
        }
        // ivyVersionMajor == major
        if (ivyVersionMinor > minor) {
            return true;
        } else if (ivyVersionMinor < minor) {
            return false;
        }
        // ivyVersionMinor == minor
        return ivyVersionPatch >= patch;
    }
}
