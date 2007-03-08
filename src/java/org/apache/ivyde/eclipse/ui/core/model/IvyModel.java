package org.apache.ivyde.eclipse.ui.core.model;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.resolve.ResolveData;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivyde.eclipse.IvyPlugin;
import org.apache.ivyde.eclipse.ui.preferences.PreferenceConstants;
import org.eclipse.jdt.core.IJavaProject;

public class IvyModel {
    private IJavaProject _javaProject;

    private final Map MODEL = new HashMap();
    private Properties _defaults;
    
    public IvyModel(IJavaProject javaProject) {
        _javaProject = javaProject;
        
        loadDefaults();
        // ivy-module
        IvyTag ivyTag = new IvyTag("ivy-module", "root tag of ivy file");
        ivyTag.setDoc("Root tag of any ivy-file.");
        ivyTag.addAttribute(new IvyTagAttribute("version", "The version of the ivy file specification \nshould be '1.0' with current version of ivy", true));
        MODEL.put(ivyTag.getName(), ivyTag);

        // info
        IvyTagAttribute orgTagAttribute = new IvyTagAttribute("organisation", "the name of the organisation that is the owner of this module.", true);
        orgTagAttribute.setValueProvider(new PreferenceValueProvider(PreferenceConstants.ORGANISATION));
        IvyTagAttribute statusTagAttribute = new IvyTagAttribute("status", "the status of this module.");
        statusTagAttribute.setValueProvider(new ListValueProvider(_defaults.getProperty("status")));
        IvyTagAttribute pubTagAttribute = new IvyTagAttribute("publication", "the date of publication of this module. \nIt should be given in this format: yyyyMMddHHmmss");
        pubTagAttribute.setValueProvider(new IValueProvider() {        
            public String[] getValuesfor(IvyTagAttribute att, IvyFile ivyFile) {
                return new String[] {Ivy.DATE_FORMAT.format(new Date())};
            }        
        });
        IvyTagAttribute moduleTagAttribute = new IvyTagAttribute("module", "the name of the module described by this ivy file.", true);
        IValueProvider projectNameValueProvider = new IValueProvider() {        
            public String[] getValuesfor(IvyTagAttribute att, IvyFile ivyFile) {
                return new String[] {ivyFile.getProjectName()};
            }        
        };
        moduleTagAttribute.setValueProvider(projectNameValueProvider);
        IvyTag info = new IvyTag("info", "gives general information about the described module", new IvyTagAttribute[] { 
                orgTagAttribute,
                moduleTagAttribute, 
                new IvyTagAttribute("branch", "the branch of this module."), 
                new IvyTagAttribute("revision", "the revision of this module."), 
                statusTagAttribute,
                pubTagAttribute });
        MODEL.put(info.getName(), info);
        IvyTag child = new IvyTag("license", "gives information about the licenses of the described module");
        child.addAttribute(new IvyTagAttribute("name", "the name of the license. \nTry to respect spelling when using a classical license.", true, 
                new ListValueProvider(_defaults.getProperty("license"))));
        child.addAttribute(new IvyTagAttribute("url", "an url pointing to the license text.", false));
        MODEL.put(child.getName(), child);
        info.addChildIvyTag(child);
        child = new IvyTag("ivyauthor", "describes who has contributed to write the ivy file");
        child.addAttribute(new IvyTagAttribute("name", "the name of the author, as a person or a company.", true, 
                new PreferenceValueProvider(PreferenceConstants.ORGANISATION)));
        child.addAttribute(new IvyTagAttribute("url", "an url pointing to where the author can be reached.", false, 
                new PreferenceValueProvider(PreferenceConstants.ORGANISATION_URL)));
        MODEL.put(child.getName(), child);
        info.addChildIvyTag(child);
        child = new IvyTag("repository", "describes on which public repositories this module can be found");
        child.addAttribute(new IvyTagAttribute("name", "the name of the repository. \nTry to respect spelling for common repositories (ibiblio, ivyrep, ...)", true, 
                new IValueProvider() {        
            public String[] getValuesfor(IvyTagAttribute att, IvyFile ivyFile) {
                return new String[] {"ibiblio", "ivyrep"};
            }        
        }));
        child.addAttribute(new IvyTagAttribute("url", "an url pointing to the repository.", true, 
                new IValueProvider() {        
            public String[] getValuesfor(IvyTagAttribute att, IvyFile ivyFile) {
                Map allAttsValues = ivyFile.getAllAttsValues();
                if (allAttsValues != null) {
                    String name = (String)allAttsValues.get("name");
                    if ("ibiblio".equals(name)) {
                        return new String[] {"http://www.ibiblio.org/maven/"};
                    } else if ("ivyrep".equals(name)) {
                        return new String[] {"http://www.jayasoft.fr/org/ivyrep/"};
                    }
                }
                return null;
            }
        
        }));
        IvyTagAttribute reppatternTagAttribute = new IvyTagAttribute("pattern", "an ivy pattern to find modules on this repository", false);
        reppatternTagAttribute.setValueProvider(new IValueProvider() {        
            public String[] getValuesfor(IvyTagAttribute att, IvyFile ivyFile) {
                Map allAttsValues = ivyFile.getAllAttsValues();
                if (allAttsValues != null) {
                    String name = (String)allAttsValues.get("name");
                    if ("ibiblio".equals(name)) {
                        return new String[] {"[module]/[type]s/[artifact]-[revision].[ext]"};
                    } else if ("ivyrep".equals(name)) {
                        return new String[] {"[organisation]/[module]/[artifact]-[revision].[ext]"};
                    }
                }
                return null;
            }        
        });
        child.addAttribute(reppatternTagAttribute);
        child.addAttribute(new IvyBooleanTagAttribute("ivys", "true if ivy file can be found on this repository", false));
        child.addAttribute(new IvyBooleanTagAttribute("artifacts", "true if module artifacts can be found on this repository", false));
        MODEL.put(child.getName(), child);
        info.addChildIvyTag(child);
        child = new IvyTag("description", "gives general description about the module");
        child.addAttribute(new IvyTagAttribute("homepage", "the url of the homepage of the module", false,
                new PreferenceValueProvider(PreferenceConstants.ORGANISATION_URL)));
        MODEL.put(child.getName(), child);
        info.addChildIvyTag(child);
        ivyTag.addChildIvyTag(info);

        // configurations
        IvyTag configurations = new IvyTag("configurations", "container for configuration elements");
        IvyTag conf = new IvyTag("conf", "declares a configuration of this module");
        conf.addAttribute(new IvyTagAttribute("name", "the name of the declared configuration", true));
        conf.addAttribute(new IvyTagAttribute("description", "a short description for the declared configuration", false));
        IvyTagAttribute visibilityTagAttribute = new IvyTagAttribute("visibility", "the visibility of the declared configuration.\n'public' means that this configuration can be used by other modules, \nwhile 'private' means that this configuration is used only in the module itself, \nand is not exposed to other modules", false);
        visibilityTagAttribute.setValueProvider(new IValueProvider() {        
            public String[] getValuesfor(IvyTagAttribute att, IvyFile ivyFile) {
                return new String[] {"public", "private"};
            }        
        });
        conf.addAttribute(visibilityTagAttribute);
        IvyTagAttribute confExtTagAttribute = new IvyTagAttribute("extends", "a comma separated list of configurations of this module \nthat the current configuration extends", false);
        IValueProvider masterConfsValueProvider = new IValueProvider() {
            public String[] getValuesfor(IvyTagAttribute att, IvyFile ivyFile) {
                StringBuffer base = new StringBuffer();
                String qualifier = ivyFile.getAttributeValueQualifier();
                int comma = qualifier.lastIndexOf(",")+1;
                base.append(qualifier.substring(0, comma));
                qualifier = qualifier.substring(comma);
                while (qualifier.length() > 0 && qualifier.charAt(0) == ' ') {
                    base.append(' ');
                    qualifier = qualifier.substring(1);
                }
                String[] confs = ivyFile.getConfigurationNames();
                for (int i = 0; i < confs.length; i++) {
                    confs[i] = base + confs[i];
                }
                return confs;
            }
        };
        IValueProvider masterConfValueProvider = new IValueProvider() {
            public String[] getValuesfor(IvyTagAttribute att, IvyFile ivyFile) {
                return ivyFile.getConfigurationNames();
            }
        };
        confExtTagAttribute.setValueProvider(masterConfsValueProvider);
        conf.addAttribute(confExtTagAttribute);
        IvyTagAttribute deprecatedTagAttribute = new IvyTagAttribute("deprecated", "indicates that this conf has been deprecated \nby giving the date of the deprecation. \nIt should be given in this format: yyyyMMddHHmmss", false);
        deprecatedTagAttribute.setValueProvider(new IValueProvider() {        
            public String[] getValuesfor(IvyTagAttribute att, IvyFile ivyFile) {
                return new String[] {Ivy.DATE_FORMAT.format(new Date())};
            }        
        });
        conf.addAttribute(deprecatedTagAttribute);
        configurations.addChildIvyTag(conf);
        List allConf = new ArrayList();
        allConf.add(conf);
        MODEL.put(conf.getName(), allConf);
        ivyTag.addChildIvyTag(configurations);
        MODEL.put(configurations.getName(), configurations);

        // configurations
        IvyTag publications = new IvyTag("publications", "container for published artifact elements");
        IvyTag artifact = new IvyTag("artifact", "declares a published artifact for this module");
        artifact.addAttribute(new IvyTagAttribute("name", "the name of the published artifact. This name must not include revision.", true, projectNameValueProvider));
        artifact.addAttribute(new IvyTagAttribute("type", "the type of the published artifact. \nIt's usually its extension, but not necessarily. \nFor instance, ivy files are of type 'ivy' but have 'xml' extension", true, new ListValueProvider(_defaults.getProperty("type"))));
        artifact.addAttribute(new IvyTagAttribute("ext", "the extension of the published artifact", false, new ListValueProvider(_defaults.getProperty("ext"))));
        artifact.addAttribute(new IvyTagAttribute("conf", "comma separated list of public configurations in which this artifact is published. \n'*' wildcard can be used to designate all public configurations of this module", false, masterConfsValueProvider));
        IvyTag conf2 = new IvyTag("conf", "indicates a public configuration in which this artifact is published");
        conf2.addAttribute(new IvyTagAttribute("name", "the name of a module public configuration in which this artifact is published. \n'*' wildcard can be used to designate all public configurations of this module", true, masterConfValueProvider));
        allConf.add(conf2);
        artifact.addChildIvyTag(conf2);
        publications.addChildIvyTag(artifact);
        MODEL.put(publications.getName(), publications);
        MODEL.put(artifact.getName(), artifact);
        ivyTag.addChildIvyTag(publications);

        // dependencies
        IvyTag dependencies = new IvyTag("dependencies", "container for dependency elements");
        // dependency
        IvyTag dependency = new IvyTag("dependency", "declares a dependency for this module") {
        	public String[] getPossibleValuesForAttribute(String att, IvyFile ivyfile) {
        		String[] r = super.getPossibleValuesForAttribute(att, ivyfile);
        		if (r == null) { //listing can be used even for extra attributes
                	List ret = listDependencyTokenValues(att, ivyfile);
                    return (String[])ret.toArray(new String[ret.size()]);
        		} else {
        			return r;
        		}
        	}
        };
        IvyTagAttribute orgAtt = new IvyTagAttribute("org", "the name of the organisation of the dependency.", false);
        orgAtt.setValueProvider(new IValueProvider() {
            public String[] getValuesfor(IvyTagAttribute att, IvyFile ivyFile) {
            	List ret = listDependencyTokenValues(att.getName(), ivyFile);
                try {
                    ret.add(IvyPlugin.getDefault().getPreferenceStore().getString(PreferenceConstants.ORGANISATION));
                } catch (Exception ex) {                    
                }
                String org = ivyFile.getOrganisation();
                if (org != null) {
                    ret.add(org);
                }
                return (String[])ret.toArray(new String[ret.size()]);
            }

        });
        dependency.addAttribute(orgAtt);
        IvyTagAttribute module = new IvyTagAttribute("name", "the module name of the dependency", true);
        module.setValueProvider(new IValueProvider() {
            public String[] getValuesfor(IvyTagAttribute att, IvyFile ivyFile) {
            	List ret = listDependencyTokenValues(att.getName(), ivyFile);
                return (String[])ret.toArray(new String[ret.size()]);
            }
        });
        dependency.addAttribute(module);
        IvyTagAttribute branch = new IvyTagAttribute("branch", "the branch of the dependency. \nDo not set if not needed.", false);
        branch.setValueProvider(new IValueProvider() {
            public String[] getValuesfor(IvyTagAttribute att, IvyFile ivyFile) {
            	List ret = listDependencyTokenValues(att.getName(), ivyFile);
                return (String[])ret.toArray(new String[ret.size()]);
            }
        });
        dependency.addAttribute(branch);
        IvyTagAttribute rev = new IvyTagAttribute("rev", "the revision of the dependency. \nUse 'latest.integration' to get the latest version of the dependency. \nYou can also end the revision asked with a '+' to get the latest matching revision.", true);
        rev.setValueProvider(new IValueProvider() {
            public String[] getValuesfor(IvyTagAttribute att, IvyFile ivyFile) {
            	List ret = listDependencyTokenValues(att.getName(), ivyFile);
                ret.add("latest.integration");
                return (String[])ret.toArray(new String[ret.size()]);
            }
        });
        dependency.addAttribute(rev);
        dependency.addAttribute(new IvyBooleanTagAttribute("force", "a boolean to give an indication to conflict manager \nthat this dependency should be forced to this revision", false));
        dependency.addAttribute(new IvyBooleanTagAttribute("transitive", "a boolean indicating if this dependency should be resolved transitively or not", false));
        IvyTagAttribute confAtt = new IvyTagAttribute("conf", "an inline mapping configuration spec", false);
        dependency.addAttribute(confAtt);
        confAtt.setValueProvider(new IValueProvider() {
            public String[] getValuesfor(IvyTagAttribute att, IvyFile ivyFile) {                
                String qualifier = ivyFile.getAttributeValueQualifier();
                int index = qualifier.lastIndexOf(';') + 1;
                StringBuffer base = new StringBuffer(qualifier.substring(0, index));
                qualifier = qualifier.substring(index);
                int arrowIndex = qualifier.indexOf("->");
                if (arrowIndex > -1) {                        
                    // we are looking for a dep conf
                    String org = ivyFile.getDependencyOrganisation();
                    Map otherAttValues = ivyFile.getAllAttsValues();
                    if(org != null && otherAttValues != null && otherAttValues.get("name") != null && otherAttValues.get("rev") != null && getIvy() != null) {
                    	otherAttValues.remove("org");
                    	String branch = (String) otherAttValues.remove("branch");
                    	otherAttValues.remove("conf");
                    	otherAttValues.remove("force");
                    	otherAttValues.remove("transitive");
                    	otherAttValues.remove("changing");
                        return getDependencyConfs(org, (String)otherAttValues.remove("name"), branch, (String)otherAttValues.remove("rev"), otherAttValues, qualifier, base, arrowIndex);
                    }
                    
                } else {
                    // we are looking for a master conf
                    return getMasterConfs(ivyFile, qualifier, base, arrowIndex);
                }
            
                
                return null;
            }

            private String[] getMasterConfs(IvyFile ivyFile, String qualifier, StringBuffer base, int arrowIndex) {
                // search for word after last comma 
                int comma = qualifier.lastIndexOf(",")+1;
                base.append(qualifier.substring(0, comma));
                qualifier = qualifier.substring(comma);
                while (qualifier.length() > 0 && qualifier.charAt(0) == ' ') {
                    base.append(' ');
                    qualifier = qualifier.substring(1);
                }
                String[] confs = ivyFile.getConfigurationNames();
                for (int i = 0; i < confs.length; i++) {
                    confs[i] = base + confs[i];
                }
                List ret = new ArrayList(Arrays.asList(confs));
                ret.add("*");
                return (String[])ret.toArray(new String[ret.size()]);
            }
            
            private String[] getDependencyConfs(String org, String name, String branch, String rev, Map otherAtts, String qualifier, StringBuffer base, int arrowIndex) {
                base.append(qualifier.substring(0, arrowIndex + 2));
                qualifier = qualifier.substring(arrowIndex + 2);
                // search for word after last comma 
                int comma = qualifier.lastIndexOf(",")+1;
                base.append(qualifier.substring(0, comma));
                qualifier = qualifier.substring(comma);
                while (qualifier.length() > 0 && qualifier.charAt(0) == ' ') {
                    base.append(' ');
                    qualifier = qualifier.substring(1);
                }
                ResolveData data = new ResolveData(getIvy().getResolveEngine(), new ResolveOptions());
                ModuleRevisionId mrid = ModuleRevisionId.newInstance(org, name, branch, rev, otherAtts);
                DefaultDependencyDescriptor ddd = new DefaultDependencyDescriptor(mrid, false);
                try {
                    DependencyResolver resolver = getIvySettings().getResolver(mrid.getModuleId());
                    if (resolver == null) {
                        return null;
                    }
                    ResolvedModuleRevision dep = resolver.getDependency(ddd, data);
                    if (dep == null) {
                        return null;
                    }
                    String[] confs = dep.getDescriptor().getConfigurationsNames();
                    for (int i = 0; i < confs.length; i++) {
                        confs[i] = base + confs[i];
                    }
                    List ret = new ArrayList(Arrays.asList(confs));
                    ret.add("*");
                    return (String[])ret.toArray(new String[ret.size()]);
                } catch (ParseException e) {
                    System.err.println(e);
                    return null;
                }
            }
            
        });
        IvyTag conf3 = new IvyTag("conf", "defines configuration mapping has sub element");
        conf3.addAttribute(new IvyTagAttribute("name", "the name of the master configuration to map. \n'*' wildcard can be used to designate all configurations of this module", true, masterConfValueProvider));
        conf3.addAttribute(new IvyTagAttribute("mapped", "a comma separated list of dependency configurations \nto which this master configuration should be mapped", false,  
                new IValueProvider() {        
            public String[] getValuesfor(IvyTagAttribute att, IvyFile ivyFile) {
                int[] indexes = ivyFile.getParentTagIndex();
                if (indexes != null) {
                    Map otherAttValues = ivyFile.getAllAttsValues(indexes[0]+1);
                    String org = ivyFile.getDependencyOrganisation(otherAttValues);
                    if(org != null && otherAttValues != null && otherAttValues.get("name") != null && otherAttValues.get("rev") != null && getIvy() != null) {
                        StringBuffer base = new StringBuffer();
                        String qualifier = ivyFile.getAttributeValueQualifier();
                        // search for word after last comma 
                        int comma = qualifier.lastIndexOf(",")+1;
                        base.append(qualifier.substring(0, comma));
                        qualifier = qualifier.substring(comma);
                        while (qualifier.length() > 0 && qualifier.charAt(0) == ' ') {
                            base.append(' ');
                            qualifier = qualifier.substring(1);
                        }
                        ResolveData data = new ResolveData(getIvy().getResolveEngine(), new ResolveOptions());
                        ModuleRevisionId mrid = ModuleRevisionId.newInstance(org, (String)otherAttValues.get("name"), (String)otherAttValues.get("rev"));
                        DefaultDependencyDescriptor ddd = new DefaultDependencyDescriptor(mrid, false);
                        try {
                            String[] confs = getIvySettings().getResolver(mrid.getModuleId()).getDependency(ddd, data).getDescriptor().getConfigurationsNames();
                            for (int i = 0; i < confs.length; i++) {
                                confs[i] = base + confs[i];
                            }
                            List ret = new ArrayList(Arrays.asList(confs));
                            ret.add("*");
                            return (String[])ret.toArray(new String[ret.size()]);
                        } catch (ParseException e) {
                            System.err.println(e);
                            return new String[] {"*"};
                        }
                    }
                }
                return new String[] {"*"};
            }
        
        }));
        allConf.add(conf3);
        IvyTag mapped = new IvyTag("mapped", "map dependency configurations for this master configuration");
        mapped.addAttribute(new IvyTagAttribute("name", "the name of the dependency configuration mapped. \n'*' wildcard can be used to designate all configurations of this module", true, 
                new IValueProvider() {        
            public String[] getValuesfor(IvyTagAttribute att, IvyFile ivyFile) {
                int[] indexes = ivyFile.getParentTagIndex();
                if (indexes != null) {
                    indexes = ivyFile.getParentTagIndex(indexes[0]);
                    if (indexes != null) {
                        Map otherAttValues = ivyFile.getAllAttsValues(indexes[0]+1);
                        String org = ivyFile.getDependencyOrganisation(otherAttValues);
                        if(org != null && otherAttValues != null && otherAttValues.get("name") != null && otherAttValues.get("rev") != null && getIvy() != null) {
                            ResolveData data = new ResolveData(getIvy().getResolveEngine(), new ResolveOptions());
                            ModuleRevisionId mrid = ModuleRevisionId.newInstance(org, (String)otherAttValues.get("name"), (String)otherAttValues.get("rev"));
                            DefaultDependencyDescriptor ddd = new DefaultDependencyDescriptor(mrid, false);
                            try {
                                String[] confs = getIvySettings().getResolver(mrid.getModuleId()).getDependency(ddd, data).getDescriptor().getConfigurationsNames();
                                List ret = new ArrayList(Arrays.asList(confs));
                                ret.add("*");
                                return (String[])ret.toArray(new String[ret.size()]);
                            } catch (ParseException e) {
                                System.err.println(e);
                                return new String[] {"*"};
                            }
                        }
                    }
                }
                return new String[] {"*"};
            }
        
        }));
        conf3.addChildIvyTag(mapped);
        MODEL.put(mapped.getName(), mapped);
        
        ListValueProvider matcherNamesProvider = new ListValueProvider(
        		(String[])getIvy().getSettings().getMatcherNames().toArray(new String[0]));
        
        IvyTag artifact2 = new IvyTag("artifact", "defines artifacts restriction \nuse only if you do not control dependency ivy file");
        artifact2.addAttribute(new IvyTagAttribute("name", "the name of an artifact of the \ndependency module to add to the include list, \nor a regexp matching this name", false));
        artifact2.addAttribute(new IvyTagAttribute("type", "the type of the artifact of the \ndependency module to add to the include list, \nor a regexp matching this name", false,
                new ListValueProvider(_defaults.getProperty("type"))));
        artifact2.addAttribute(new IvyTagAttribute("ext", "the extension of the artifact of the \ndependency module to add to the include list, \nor a regexp matching this name", false,
                new ListValueProvider(_defaults.getProperty("ext"))));
        artifact2.addAttribute(new IvyTagAttribute("url", "an url where this artifact can be found \nif it isn't present at the standard \nlocation in the repository", false));
        artifact2.addAttribute(new IvyTagAttribute("conf", "comma separated list of the master configurations \nin which this artifact should be included. \n'*' wildcard can be used to designate all configurations of this module", false, 
                masterConfsValueProvider));
        IvyTag conf4 = new IvyTag("conf", "configuration in which the artifact should be included");
        conf4.addAttribute(new IvyTagAttribute("name", "the name of the master configuration in which \nthe enclosing artifact should be included", true, 
                masterConfValueProvider));
        allConf.add(conf4);
        artifact2.addChildIvyTag(conf4);
        MODEL.put(artifact2.getName(), artifact2);
        IvyTag include = new IvyTag("include", "defines artifacts restriction \nuse only if you do not control dependency ivy file");
        include.addAttribute(new IvyTagAttribute("name", "the name of an artifact of the \ndependency module to add to the include list, \nor a regexp matching this name", false));
        include.addAttribute(new IvyTagAttribute("type", "the type of the artifact of the \ndependency module to add to the include list, \nor a regexp matching this name", false, 
                new ListValueProvider(_defaults.getProperty("type"))));
        include.addAttribute(new IvyTagAttribute("ext",  "the extension of the artifact of the \ndependency module to add to the include list, \nor a regexp matching this name", false,
                new ListValueProvider(_defaults.getProperty("ext"))));
        include.addAttribute(new IvyTagAttribute("matcher",  "the matcher to use to match the modules to include", false,
                matcherNamesProvider));
        include.addAttribute(new IvyTagAttribute("conf", "comma separated list of the master configurations \nin which this artifact should be included. \n'*' wildcard can be used to designate all configurations of this module", false,
                masterConfsValueProvider));
        IvyTag conf5 = new IvyTag("conf", "configuration in which the artifact should be included");
        conf5.addAttribute(new IvyTagAttribute("name", "the name of the master configuration in which \nthe enclosing artifact should be included", true,  
                masterConfValueProvider));
        include.addChildIvyTag(conf5);
        MODEL.put(include.getName(), include);
        allConf.add(conf5);
        IvyTag exclude = new IvyTag("exclude", "defines artifacts restriction \nuse only if you do not control dependency ivy file");
        exclude.addAttribute(new IvyTagAttribute("org", "the organisation of the dependency \nmodule or artifact to exclude, \nor a pattern matching this organisation", false));
        exclude.addAttribute(new IvyTagAttribute("module", "the name of the dependency \nmodule or the artifact to exclude, \nor a pattern matching this module name", false));
        exclude.addAttribute(new IvyTagAttribute("name", "the name of an artifact of the \ndependency module to add to the exclude list, \nor a pattern matching this name", false));
        exclude.addAttribute(new IvyTagAttribute("type", "the type of the artifact of the \ndependency module to add to the exclude list, \nor a pattern matching this name", false, 
                new ListValueProvider(_defaults.getProperty("type"))));
        exclude.addAttribute(new IvyTagAttribute("ext",  "the extension of the artifact of the \ndependency module to add to the exclude list, \nor a pattern matching this name", false,
                new ListValueProvider(_defaults.getProperty("ext"))));
        exclude.addAttribute(new IvyTagAttribute("matcher",  "the matcher to use to match the modules to include", false,
                matcherNamesProvider));
        exclude.addAttribute(new IvyTagAttribute("conf", "comma separated list of the master configurations \nin which this artifact should be excluded. \n'*' wildcard can be used to designate all configurations of this module", false,
                masterConfsValueProvider));
        IvyTag conf6 = new IvyTag("conf", "configuration in which the artifact should be excluded");
        conf6.addAttribute(new IvyTagAttribute("name", "the name of the master configuration in which \nthe enclosing artifact should be excluded", true,
                masterConfValueProvider));
        allConf.add(conf6);
        exclude.addChildIvyTag(conf6);
        MODEL.put(exclude.getName(), exclude);
        dependency.addChildIvyTag(conf3);
        dependency.addChildIvyTag(artifact2);
        dependency.addChildIvyTag(include);
        dependency.addChildIvyTag(exclude);
        dependencies.addChildIvyTag(dependency);
        ivyTag.addChildIvyTag(dependencies);
        MODEL.put(dependency.getName(), dependency);
        MODEL.put(dependencies.getName(), dependencies);

        // dependencies

        IvyTag conflicts = new IvyTag("conflicts", "conflicts managers definition section");
        IvyTag manager = new IvyTag("manager", "declares a conflict manager for this module");
        manager.addAttribute(new IvyTagAttribute("org", "the name, or a regexp matching the name of organisation \nto which this conflict manager should apply", false));
        manager.addAttribute(new IvyTagAttribute("module", "the name, or a regexp matching the name of module \nto which this conflict manager should apply", false));
        manager.addAttribute(new IvyTagAttribute("name", "the name of the conflict manager to use", false));
        manager.addAttribute(new IvyTagAttribute("rev", "a comma separated list of revisions this conflict manager should select", false));
        conflicts.addChildIvyTag(manager);
        ivyTag.addChildIvyTag(conflicts);
        MODEL.put(conflicts.getName(), conflicts);
        MODEL.put(manager.getName(), manager);
    }

    public IvyTag getIvyTag(String tagName, String parentName) {
        Object tag = MODEL.get(tagName);
        if (tag instanceof List) {
            List all = (List) tag;
            for (Iterator iter = all.iterator(); iter.hasNext();) {
                IvyTag t = (IvyTag) iter.next();
                if (t.getParent() != null && t.getParent().getName().equals(parentName)) {
                    return t;
                }
            }
            return null;
        }
        return (IvyTag) tag;
    }

    private void loadDefaults() {
        _defaults = new Properties();
        try {
            _defaults.load(IvyModel.class.getResourceAsStream("defaults.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public IvyTag getRootIvyTag() {
        return (IvyTag) MODEL.get("ivy-module");
    }

    private Ivy getIvy() {
        return IvyPlugin.getIvy(_javaProject);
    }

    private IvySettings getIvySettings() {
        return getIvy().getSettings();
    }


	private List listDependencyTokenValues(String att, IvyFile ivyfile) {
		Map allAttsValues = ivyfile.getAllAttsValues();
        String org = ivyfile.getOrganisation();
        if (org != null && !allAttsValues.containsKey("org")) {
        	allAttsValues.put("org", org);
        }
		return listDependencyTokenValues(att, allAttsValues);
	}

	private List listDependencyTokenValues(String att, Map otherAttValues) {
		List ret = new ArrayList();
		if(getIvy() != null) {
			replaceToken(otherAttValues, "org", IvyPatternHelper.ORGANISATION_KEY);
			replaceToken(otherAttValues, "name", IvyPatternHelper.MODULE_KEY);
			replaceToken(otherAttValues, "rev", IvyPatternHelper.REVISION_KEY);

        	if (!otherAttValues.containsKey(IvyPatternHelper.BRANCH_KEY)) {
        		otherAttValues.put(IvyPatternHelper.BRANCH_KEY, getIvySettings().getDefaultBranch());
        	}

		    String stdAtt = standardiseDependencyAttribute(att);
		    otherAttValues.remove(stdAtt);
			String[] revs = getIvy().listTokenValues(stdAtt, otherAttValues);
		    if (revs != null) {
		        ret.addAll(Arrays.asList(revs));
		    }
		}
		return ret;
	}

	private void replaceToken(Map otherAttValues, String oldToken, String newToken) {
		String val = (String) otherAttValues.remove(oldToken); 
		if (val != null) {
			otherAttValues.put(newToken, val);
		}
	}

	private String standardiseDependencyAttribute(String att) {
		if ("org".equals(att)) {
			return IvyPatternHelper.ORGANISATION_KEY;
		}
		if ("name".equals(att)) {
			return IvyPatternHelper.MODULE_KEY;
		}
		if ("rev".equals(att)) {
			return IvyPatternHelper.REVISION_KEY;
		}
		return att;
	}

}
