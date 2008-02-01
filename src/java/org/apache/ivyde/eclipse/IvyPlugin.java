package org.apache.ivyde.eclipse;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.util.Message;
import org.apache.ivyde.eclipse.cpcontainer.IvyClasspathContainer;
import org.apache.ivyde.eclipse.cpcontainer.fragmentinfo.IPackageFragmentExtraInfo;
import org.apache.ivyde.eclipse.cpcontainer.fragmentinfo.PreferenceStoreInfo;
import org.apache.ivyde.eclipse.ui.console.IvyConsole;
import org.apache.ivyde.eclipse.ui.preferences.PreferenceConstants;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.service.prefs.BackingStoreException;

/**
 * The main plugin class to be used in the desktop.
 */
public class IvyPlugin extends AbstractUIPlugin {
    public static final String ID = "org.apache.ivyde.eclipse";

    public static final String PREF_CONSOLE_DEBUG_COLOR = ID + ".console.color.debug";

    public static final String PREF_CONSOLE_VERBOSE_COLOR = ID + ".console.color.verbose";

    public static final String PREF_CONSOLE_INFO_COLOR = ID + ".console.color.info";

    public static final String PREF_CONSOLE_WARN_COLOR = ID + ".console.color.warn";

    public static final String PREF_CONSOLE_ERROR_COLOR = ID + ".console.color.error";

    // The shared instance.
    private static IvyPlugin plugin;

    // Resource bundle.
    private ResourceBundle resourceBundle;

    private IvyConsole console;

    private IJavaModel javaModel;

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
        log(IStatus.INFO, "starting IvyDE plugin", null);
        try {
            console = new IvyConsole();
        } catch (RuntimeException e) {
            // Don't let the console bring down the CVS UI
            log(IStatus.ERROR, "Errors occurred starting the Ivy console", e);
        }
        getPreferenceStore().addPropertyChangeListener(new IPropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent event) {
                if (event.getProperty() == PreferenceConstants.IVYCONF_PATH) {
                    ivyConfPathChanged();
                }
                if (event.getProperty() == PreferenceConstants.ACCEPTED_TYPES
                        || event.getProperty() == PreferenceConstants.SOURCES_TYPES
                        || event.getProperty() == PreferenceConstants.JAVADOC_TYPES) {
                    typesChanged(event.getProperty());
                }
            }
        });
        javaModel = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot());
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
     */
    public static IvyPlugin getDefault() {
        return plugin;
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
                resourceBundle = ResourceBundle.getBundle("org.apache.ivyde.IvyPluginResources");
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
        return AbstractUIPlugin.imageDescriptorFromPlugin("org.apache.ivyde", path);
    }

    private static class IvyConfig {
        Ivy ivy;

        long configTime = -1;

        public IvyConfig() {
        }

        public IvyConfig(Ivy ivy) {
            this.ivy = ivy;
        }
    }

    private static Map _ivysByProject = new HashMap(); // Map (IJavaProject -> IvyConfig)

    private static Map _ivysByConf = new HashMap(); // Map (configPath -> IvyConfig)

    private static boolean _inChange;

    private static Set _todo = new HashSet();

    private PreferenceStoreInfo _packageExtraInfo;

    private Map _containers = new HashMap();

    public static Ivy getIvy(IJavaProject javaProject) {
        Ivy ivy = refreshIvyConfiguration(javaProject, getIvyconfURL(javaProject));
        return ivy == null ? new Ivy() : ivy;
    }

    public static void ivyConfPathChanged() {
        try {
            IJavaProject[] projects = plugin.javaModel.getJavaProjects();
            String defaultConfURL = getIvyconfURL();
            for (int i = 0; i < projects.length; i++) {
                if (getStrictIvyconfURL(projects[i]) == null) {
                    resolve(projects[i]);
                }
            }
        } catch (JavaModelException e) {
        }
    }

    public static void typesChanged(String typesCode) {
        try {
            IJavaProject[] projects = plugin.javaModel.getJavaProjects();
            String defaultConfURL = getIvyconfURL();
            for (int i = 0; i < projects.length; i++) {
                if ("[inherited]".equals(getInheritablePreferenceString(projects[i], typesCode))) {
                    resolve(projects[i]);
                }
            }
        } catch (JavaModelException e) {
        }
    }

    private static synchronized Ivy refreshIvyConfiguration(IJavaProject javaProject,
            String configPath) {
        try {
            if (configPath == null || configPath.trim().length() == 0
                    || "default".equals(configPath)) {
                return defaultIvyConfigure(javaProject);
            } else {
                IvyConfig ic = (IvyConfig) _ivysByProject.get(javaProject);
                if (ic == null) {
                    ic = (IvyConfig) _ivysByConf.get(configPath);
                    if (ic == null) {
                        ic = new IvyConfig();
                        _ivysByProject.put(javaProject, ic);
                        _ivysByConf.put(configPath, ic);
                    }
                }

                URL url = new URL(configPath);
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
                                    PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
                                    "No ivyConf found",
                                    configPath
                                            + " ivyconf cannot be found.\nPlease set your ivy conf url in the preference or in your project properties to be able to use IvyDE");
                        if (ic.ivy == null) {
                            ic.ivy = new Ivy();
                        }
                    } else {
                        if (file.lastModified() != ic.configTime) {
                            ic.ivy = new Ivy();
                            if (ic.configTime == -1) {
                                Message.info("\n\n");
                            } else {
                                Message
                                        .info("\n\nIVYDE: ivysettings has changed, configuring ivy again\n");
                            }
                            ic.ivy.configure(file);
                            ic.configTime = file.lastModified();
                        }
                    }
                } else {
                    if (ic.ivy == null) {
                        ic.ivy = new Ivy();
                        ic.ivy.configure(url);
                    }
                }
                return ic.ivy;
            }
        } catch (Exception e) {
            try {
                MessageDialog
                        .openWarning(
                            PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
                            "Bad ivyConf found",
                            "Problem occured while using "
                                    + configPath
                                    + " to configure Ivy.\nPlease set your ivy conf url properly in the preference or in the project properties to be able to use IvyDE.\nException message: "
                                    + e.getMessage());
            } catch (Exception ex) {
            }
            log(IStatus.WARNING, "Problem occured while using " + configPath + " to configure Ivy",
                e);
            Message.warn("IVYDE: Problem occured while using " + configPath
                    + " to configure Ivy. See error log for details");
            _ivysByProject.remove(javaProject);
            _ivysByConf.remove(configPath);
            return new Ivy();
        }
    }

    private static Ivy defaultIvyConfigure(IJavaProject javaProject) {
        IvyConfig ic = (IvyConfig) _ivysByProject.get(javaProject);
        if (ic == null) {
            ic = (IvyConfig) _ivysByConf.get("default");
        }
        Ivy ivy = ic == null ? null : ic.ivy;
        if (ivy == null) {
            ivy = new Ivy();
            try {
                ivy.configureDefault();
                IvyConfig ivyConfig = new IvyConfig(ivy);
                _ivysByProject.put(javaProject, ivyConfig);
                _ivysByConf.put("default", ivyConfig);
            } catch (Exception ex) {
                MessageDialog
                        .openWarning(
                            PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
                            "Impossible to configure Ivy",
                            "Problem occured while configuring Ivy with its default settings.\nPlease set an ivy settings url properly in the preference or in the project properties to be able to use IvyDE.\nException message: "
                                    + ex.getMessage());
                log(IStatus.WARNING,
                    "Problem occured while configuring Ivy with its default settings.", ex);
                Message
                        .warn("IVYDE: Problem occured while configuring Ivy with its default settings. See error log for details");
            }
        }
        return ivy;
    }

    public static String getIvyconfURL(IJavaProject project) {
        String ivyconf = getStrictIvyconfURL(project);
        if (ivyconf == null) {
            return getIvyconfURL();
        } else {
            return ivyconf;
        }
    }

    public static String getStrictIvyconfURL(IJavaProject project) {
        if (project == null) {
            return null;
        }
        String opt = IvyPlugin.getDefault().getProjectPreferences(project).get(
            PreferenceConstants.IVYCONF_PATH, null);
        if (opt == null || opt.trim().length() == 0 || "inherited".equals(opt)) {
            return null;
        } else {
            return opt.trim();
        }
    }

    public static String getIvyconfURL() {
        String configPath = IvyPlugin.getDefault().getPreferenceStore().getString(
            PreferenceConstants.IVYCONF_PATH);
        if (configPath == null || configPath.trim().length() == 0) {
            return "default";
        } else {
            return configPath.trim();
        }
    }

    public static void setIvyconfURL(IJavaProject project, String ivyconfURL) {
        _ivysByProject.remove(project);
        if (ivyconfURL == null) {
            IvyPlugin.getDefault().getProjectPreferences(project).put(
                PreferenceConstants.IVYCONF_PATH, "inherited");
        } else {
            IvyPlugin.getDefault().getProjectPreferences(project).put(
                PreferenceConstants.IVYCONF_PATH, ivyconfURL);
        }
        flushProjectPreferences(project);

        resolve(project);
    }

    /**
     * Check if the artifact is an artifact which can be added to the classpath container
     * 
     * @param project
     *            the project containing the ivy container
     * @param artifact
     *            the artifact to check
     * @return <code>true</code> if the artifact can be added
     */
    public static boolean accept(IJavaProject project, Artifact artifact) {
        return getAcceptedTypes(project).contains(artifact.getType())
                && !getSourcesTypes(project).contains(artifact.getType())
                && !getJavadocTypes(project).contains(artifact.getType());
    }

    private static Collection getAcceptedTypes(IJavaProject project) {
        return getInheritablePreferenceList(project, PreferenceConstants.ACCEPTED_TYPES, "jar");
    }

    public static String getAcceptedTypesString(IJavaProject project) {
        return getInheritablePreferenceString(project, PreferenceConstants.ACCEPTED_TYPES);
    }

    public static void setAcceptedTypes(IJavaProject project, String types) {
        setInheritablePreferenceString(project, types, PreferenceConstants.ACCEPTED_TYPES);
    }

    public static boolean isSources(IJavaProject project, Artifact artifact) {
        return getSourcesTypes(project).contains(artifact.getType());
    }

    private static Collection getSourcesTypes(IJavaProject project) {
        return getInheritablePreferenceList(project, PreferenceConstants.SOURCES_TYPES, "source");
    }

    public static String getSourcesTypesString(IJavaProject project) {
        return getInheritablePreferenceString(project, PreferenceConstants.SOURCES_TYPES);
    }

    public static void setSourcesTypes(IJavaProject project, String types) {
        setInheritablePreferenceString(project, types, PreferenceConstants.SOURCES_TYPES);
    }

    public static boolean isSourceArtifactName(IJavaProject project, String jar, String source) {
        if (source.equals(jar)) {
            return true;
        }
        Iterator it = getSourcesSuffixes(project).iterator();
        while (it.hasNext()) {
            if (source.equals(jar + it.next())) {
                return true;
            }
        }
        return false;
    }

    private static Collection getSourcesSuffixes(IJavaProject project) {
        return getInheritablePreferenceList(project, PreferenceConstants.SOURCES_SUFFIXES, "-source, -sources, -src");
    }

    public static String getSourcesSuffixesString(IJavaProject project) {
        return getInheritablePreferenceString(project, PreferenceConstants.SOURCES_SUFFIXES);
    }

    public static void setSourcesSuffixes(IJavaProject project, String types) {
        setInheritablePreferenceString(project, types, PreferenceConstants.SOURCES_SUFFIXES);
    }

    public static boolean isJavadoc(IJavaProject project, Artifact artifact) {
        return getJavadocTypes(project).contains(artifact.getType());
    }

    private static Collection getJavadocTypes(IJavaProject project) {
        return getInheritablePreferenceList(project, PreferenceConstants.JAVADOC_TYPES, "javadoc");
    }

    public static String getJavadocTypesString(IJavaProject project) {
        return getInheritablePreferenceString(project, PreferenceConstants.JAVADOC_TYPES);
    }

    public static void setJavadocTypes(IJavaProject project, String types) {
        setInheritablePreferenceString(project, types, PreferenceConstants.JAVADOC_TYPES);
    }

    public static boolean isJavadocArtifactName(IJavaProject project, String jar, String javadoc) {
        if (javadoc.equals(jar)) {
            return true;
        }
        Iterator it = getJavadocSuffixes(project).iterator();
        while (it.hasNext()) {
            if (javadoc.equals(jar + it.next())) {
                return true;
            }
        }
        return false;
    }

    private static Collection getJavadocSuffixes(IJavaProject project) {
        return getInheritablePreferenceList(project, PreferenceConstants.JAVADOC_SUFFIXES, "-javadoc, -javadocs, -doc, -docs");
    }

    public static String getJavadocSuffixesString(IJavaProject project) {
        return getInheritablePreferenceString(project, PreferenceConstants.JAVADOC_SUFFIXES);
    }

    public static void setJavadocSuffixes(IJavaProject project, String types) {
        setInheritablePreferenceString(project, types, PreferenceConstants.JAVADOC_SUFFIXES);
    }

    private static Collection getInheritablePreferenceList(IJavaProject project, String code, String defaultValues) {
        String types = getInheritablePreferenceString(project, code);

        if ("[inherited]".equals(types)) {
            String workspaceTypes = IvyPlugin.getDefault().getPreferenceStore()
                    .getString(code);
            if (workspaceTypes == null || workspaceTypes.trim().length() == 0) {
                types = defaultValues;
            } else {
                types = workspaceTypes.trim();
            }
        }
        return split(types);
    }

    private static String getInheritablePreferenceString(IJavaProject project, String code) {
        String values = IvyPlugin.getDefault().getProjectPreferences(project).get(code, null);
        if (values == null || values.trim().length() == 0) {
            return "[inherited]";
        }
        return values.trim();
    }

    private static void setInheritablePreferenceString(IJavaProject project, String values, String code) {
        if (values == null || values.trim().length() == 0 || values.trim().startsWith("[inherited]")) {
            IvyPlugin.getDefault().getProjectPreferences(project).put(code, "[inherited]");
        } else {
            IvyPlugin.getDefault().getProjectPreferences(project).put(code, values);
        }
        flushProjectPreferences(project);
        resolve(project);
    }

    private static Collection split(String str) {
        String[] t = str.split(",");
        Collection ret = new ArrayList();
        for (int i = 0; i < t.length; i++) {
            if (t[i].trim().length() > 0) {
                ret.add(t[i].trim());
            }
        }
        return ret;
    }

    public IvyConsole getConsole() {
        return console;
    }

    public static String getRetreivePattern() {
        if (IvyPlugin.getDefault().getPreferenceStore().getBoolean(PreferenceConstants.DO_RETRIEVE)) {
            String pattern = IvyPlugin.getDefault().getPreferenceStore().getString(
                PreferenceConstants.RETRIEVE_PATTERN);
            return pattern == null ? "" : pattern.trim();
        } else {
            return "";
        }
    }

    public static String getRetreivePattern(IJavaProject project) {
        String pattern = IvyPlugin.getDefault().getProjectPreferences(project).get(
            PreferenceConstants.RETRIEVE_PATTERN, null);

        return pattern == null ? "" : pattern.trim();
    }

    public static void setRetreivePattern(String pattern) {
        IvyPlugin.getDefault().getPreferenceStore().putValue(PreferenceConstants.RETRIEVE_PATTERN,
            pattern);
    }

    public static void setRetreivePattern(IJavaProject project, String pattern) {
        if (pattern == null || pattern.trim().length() == 0) {
            pattern = "none";
        } else if (pattern.startsWith("[inherited]")) {
            pattern = "[inherited]";
        }
        IvyPlugin.getDefault().getProjectPreferences(project).put(
            PreferenceConstants.RETRIEVE_PATTERN, pattern);
        flushProjectPreferences(project);
    }

    private static void flushProjectPreferences(IJavaProject project) {
        try {
            IvyPlugin.getDefault().getProjectPreferences(project).flush();
        } catch (BackingStoreException e) {
            log(IStatus.WARNING, "impossible to store IvyDE project preferences", e);
        }
    }

    public IPackageFragmentExtraInfo getPackageFragmentExtraInfo() {
        if (_packageExtraInfo == null) {
            _packageExtraInfo = new PreferenceStoreInfo(getPreferenceStore());
        }
        return _packageExtraInfo;
    }

    public static String getRetrievePatternHerited(IJavaProject project) {
        String retreivePattern = getRetreivePattern(project);
        if ("".equals(retreivePattern) || retreivePattern.startsWith("[inherited]")) {
            retreivePattern = getRetreivePattern();
            if (!"".equals(retreivePattern)) {
                retreivePattern = "[inherited] " + retreivePattern;
            }
        } else if ("none".equals(retreivePattern)) {
            retreivePattern = "";
        }
        return retreivePattern.trim();
    }

    public static String getFullRetrievePatternHerited(IJavaProject project) {
        String retreivePattern = getRetrievePatternHerited(project);
        if (!"".equals(retreivePattern)) {
            if (retreivePattern.startsWith("[inherited] ")) {
                retreivePattern = retreivePattern.substring("[inherited] ".length());
            }
            return project.getProject().getLocation().toPortableString() + "/" + retreivePattern;
        } else {
            return "";
        }
    }

    public static boolean shouldDoRetrieve(IJavaProject project) {
        return !"".equals(getRetrievePatternHerited(project));
    }

    public static boolean isAlphaOrder(IJavaProject javaProject) {
        return IvyPlugin.getDefault().getProjectPreferences(javaProject).getBoolean(
            PreferenceConstants.APHABETICAL_ORDER, false);
    }

    public static void setAlphaOrder(IJavaProject project, boolean alphaOrder) {
        IvyPlugin.getDefault().getProjectPreferences(project).putBoolean(
            PreferenceConstants.APHABETICAL_ORDER, alphaOrder);
    }

    public static boolean shouldTestNonDeclaredSources(IJavaProject project) {
        return true; // TODO: add settings for that
    }

    public static boolean shouldTestNonDeclaredJavadocs(IJavaProject project) {
        return true; // TODO: add settings for that
    }

    public IEclipsePreferences getProjectPreferences(final IJavaProject project) {
        IScopeContext projectScope = new ProjectScope(project.getProject());
        IEclipsePreferences projectNode = projectScope.getNode(ID);
        return projectNode;

    }

    public static void beginChanges() {
        _inChange = true;
    }

    public static void commitChanges() {
        _inChange = false;
        for (Iterator iter = _todo.iterator(); iter.hasNext();) {
            IJavaProject project = (IJavaProject) iter.next();
            IvyClasspathContainer.resolve(project);
        }
    }

    private static void resolve(IJavaProject project) {
        if (_inChange) {
            _todo.add(project);
        } else {
            IvyClasspathContainer.resolve(project);
        }
    }

    public void register(IvyClasspathContainer container) {
        _containers.put(container.getProject().getProject().getName() + "/" + container.getPath(),
            container);
    }

    public Collection getAllContainers() {
        Message.debug("all known ivy classpath containers are: " + _containers.keySet());
        return _containers.values();
    }

}
