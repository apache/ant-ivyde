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
package org.apache.ivyde.common.ivyfile;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.resolve.ResolveData;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.util.DateUtil;
import org.apache.ivyde.common.model.IValueProvider;
import org.apache.ivyde.common.model.IvyBooleanTagAttribute;
import org.apache.ivyde.common.model.IvyFile;
import org.apache.ivyde.common.model.IvyModel;
import org.apache.ivyde.common.model.IvyModelSettings;
import org.apache.ivyde.common.model.IvyTag;
import org.apache.ivyde.common.model.IvyTagAttribute;
import org.apache.ivyde.common.model.ListValueProvider;

public class IvyModuleDescriptorModel extends IvyModel {
    public IvyModuleDescriptorModel(IvyModelSettings settings) {
        super(settings);

        // ivy-module
        IvyTag ivyTag = new IvyTag("ivy-module", "root tag of Ivy file");
        ivyTag.setDoc("Root tag of any Ivy file.");
        ivyTag.addAttribute(new IvyTagAttribute("version",
                "The version of the Ivy file specification \n"
                        + "should be '2.0' with current version of Ivy", true));
        addTag(ivyTag);

        IValueProvider defaultOrganizationProvider = new IValueProvider() {
            public String[] getValuesfor(IvyTagAttribute att, IvyFile ivyFile) {
                return new String[] {getSettings().getDefaultOrganization()};
            }
        };
        IValueProvider defaultOrganizationURLProvider = new IValueProvider() {
            public String[] getValuesfor(IvyTagAttribute att, IvyFile ivyFile) {
                return new String[] {getSettings().getDefaultOrganizationURL()};
            }
        };

        // info
        IvyTagAttribute orgTagAttribute = new IvyTagAttribute("organisation",
                "the name of the organisation that is the owner of this module.", true);
        orgTagAttribute.setValueProvider(defaultOrganizationProvider);
        IvyTagAttribute statusTagAttribute = new IvyTagAttribute("status",
                "the status of this module.");
        statusTagAttribute.setValueProvider(new ListValueProvider(getDefault("status")));
        IvyTagAttribute pubTagAttribute = new IvyTagAttribute("publication",
                "the date of publication of this module. \n"
                        + "It should be given in this format: yyyyMMddHHmmss");
        pubTagAttribute.setValueProvider(new IValueProvider() {
            public String[] getValuesfor(IvyTagAttribute att, IvyFile ivyFile) {
                return new String[] {DateUtil.format(new Date())};
            }
        });
        IvyTagAttribute moduleTagAttribute = new IvyTagAttribute("module",
                "the name of the module described by this Ivy file.", true);
        IValueProvider projectNameValueProvider = new IValueProvider() {
            public String[] getValuesfor(IvyTagAttribute att, IvyFile ivyFile) {
                return new String[] {ivyFile.getProjectName()};
            }
        };
        moduleTagAttribute.setValueProvider(projectNameValueProvider);
        IvyTag info = new IvyTag("info", "gives general information about the described module",
                new IvyTagAttribute[] {orgTagAttribute, moduleTagAttribute,
                        new IvyTagAttribute("branch", "the branch of this module."),
                        new IvyTagAttribute("revision", "the revision of this module."),
                        statusTagAttribute, pubTagAttribute});
        addTag(info);

        // extends
        IvyTag extend = new IvyTag("extends", "gives information about the parent module");
        IvyTagAttribute parentOrgAtt = new IvyTagAttribute("organisation",
                "the name of the organisation of the parent module.", false);
        parentOrgAtt.setValueProvider(new IValueProvider() {
            public String[] getValuesfor(IvyTagAttribute att, IvyFile ivyFile) {
                List<String> ret = listDependencyTokenValues(att.getName(), ivyFile);
                ret.add(getSettings().getDefaultOrganization());
                String org = ((IvyModuleDescriptorFile) ivyFile).getOrganisation();
                if (org != null) {
                    ret.add(org);
                }
                return ret.toArray(new String[ret.size()]);
            }
        });
        extend.addAttribute(parentOrgAtt);
        IvyTagAttribute parentModuleAtt = new IvyTagAttribute("module",
                "the module name of the parent module", true);
        parentModuleAtt.setValueProvider(new IValueProvider() {
            public String[] getValuesfor(IvyTagAttribute att, IvyFile ivyFile) {
                List<String> ret = listDependencyTokenValues(att.getName(), ivyFile);
                return ret.toArray(new String[ret.size()]);
            }
        });
        extend.addAttribute(parentModuleAtt);
        IvyTagAttribute parentRevAtt = new IvyTagAttribute("revision",
                "the revision of the parent module");
        extend.addAttribute(parentRevAtt);
        addTag(extend);
        info.addChildIvyTag(extend);

        // license
        IvyTag child = new IvyTag("license",
                "gives information about the licenses of the described module");
        child.addAttribute(new IvyTagAttribute("name", "the name of the license. \n"
                + "Try to respect spelling when using a classical license.", true,
                new ListValueProvider(getDefault("license"))));
        child
                .addAttribute(new IvyTagAttribute("url", "an URL pointing to the license text.",
                        false));
        addTag(child);
        info.addChildIvyTag(child);
        child = new IvyTag("ivyauthor", "describes who has contributed to write the Ivy file");
        child.addAttribute(new IvyTagAttribute("name",
                "the name of the author, as a person or a company.", true,
                defaultOrganizationProvider));
        child.addAttribute(new IvyTagAttribute("url",
                "an URL pointing to where the author can be reached.", false,
                defaultOrganizationURLProvider));
        addTag(child);
        info.addChildIvyTag(child);
        child = new IvyTag("repository",
                "describes on which public repositories this module can be found");
        child.addAttribute(new IvyTagAttribute("name", "the name of the repository. \n"
                + "Try to respect spelling for common repositories (ibiblio, ivyrep, ...)", true,
                new IValueProvider() {
                    public String[] getValuesfor(IvyTagAttribute att, IvyFile ivyFile) {
                        return new String[] {"ibiblio", "ivyrep"};
                    }
                }));
        child.addAttribute(new IvyTagAttribute("url", "an URL pointing to the repository.", true,
                new IValueProvider() {
                    public String[] getValuesfor(IvyTagAttribute att, IvyFile ivyFile) {
                        Map<String, String> allAttsValues = ivyFile.getAllAttsValues();
                        if (allAttsValues != null) {
                            String name = allAttsValues.get("name");
                            if ("ibiblio".equals(name)) {
                                return new String[] {"http://www.ibiblio.org/maven/"};
                            } else if ("ivyrep".equals(name)) {
                                return new String[] {"http://www.jayasoft.fr/org/ivyrep/"};
                            }
                        }
                        return null;
                    }

                }));
        IvyTagAttribute reppatternTagAttribute = new IvyTagAttribute("pattern",
                "an Ivy pattern to find modules on this repository", false);
        reppatternTagAttribute.setValueProvider(new IValueProvider() {
            public String[] getValuesfor(IvyTagAttribute att, IvyFile ivyFile) {
                Map<String, String> allAttsValues = ivyFile.getAllAttsValues();
                if (allAttsValues != null) {
                    String name = allAttsValues.get("name");
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
        child.addAttribute(new IvyBooleanTagAttribute("ivys",
                "true if Ivy file can be found on this repository", false));
        child.addAttribute(new IvyBooleanTagAttribute("artifacts",
                "true if module artifacts can be found on this repository", false));
        addTag(child);
        info.addChildIvyTag(child);
        child = new IvyTag("description", "gives general description about the module");
        child.addAttribute(new IvyTagAttribute("homepage", "The URL of the homepage of the module",
                false, defaultOrganizationURLProvider));
        addTag(child);
        info.addChildIvyTag(child);
        ivyTag.addChildIvyTag(info);

        // configurations
        IvyTag configurations = new IvyTag("configurations",
            "container for configuration elements");
        IvyTag conf = new IvyTag("conf", "declares a configuration of this module");
        conf.addAttribute(new IvyTagAttribute("name", "the name of the declared configuration",
                true));
        conf.addAttribute(new IvyTagAttribute("description",
                "a short description for the declared configuration", false));
        IvyTagAttribute visibilityTagAttribute = new IvyTagAttribute("visibility",
                "the visibility of the declared configuration.\n"
                        + "'public' means that this configuration can be used by other modules, \n"
                        + "while 'private' means that this configuration is used only in the\n"
                        + "module itself, and is not exposed to other modules", false);
        visibilityTagAttribute.setValueProvider(new IValueProvider() {
            public String[] getValuesfor(IvyTagAttribute att, IvyFile ivyFile) {
                return new String[] {"public", "private"};
            }
        });
        conf.addAttribute(visibilityTagAttribute);
        IvyTagAttribute confExtTagAttribute = new IvyTagAttribute("extends",
                "a comma separated list of configurations of this module \n"
                        + "that the current configuration extends", false);
        IValueProvider masterConfsValueProvider = new IValueProvider() {
            public String[] getValuesfor(IvyTagAttribute att, IvyFile ivyFile) {
                StringBuilder base = new StringBuilder();
                String qualifier = ivyFile.getAttributeValueQualifier();
                int comma = qualifier.lastIndexOf(",") + 1;
                base.append(qualifier, 0, comma);
                qualifier = qualifier.substring(comma);
                while (qualifier.length() > 0 && qualifier.charAt(0) == ' ') {
                    base.append(' ');
                    qualifier = qualifier.substring(1);
                }
                String[] confs = ((IvyModuleDescriptorFile) ivyFile).getConfigurationNames();
                for (int i = 0; i < confs.length; i++) {
                    confs[i] = base + confs[i];
                }
                return confs;
            }
        };
        IValueProvider masterConfValueProvider = new IValueProvider() {
            public String[] getValuesfor(IvyTagAttribute att, IvyFile ivyFile) {
                return ((IvyModuleDescriptorFile) ivyFile).getConfigurationNames();
            }
        };
        confExtTagAttribute.setValueProvider(masterConfsValueProvider);
        conf.addAttribute(confExtTagAttribute);
        IvyTagAttribute deprecatedTagAttribute = new IvyTagAttribute("deprecated",
                "indicates that this conf has been deprecated \n"
                        + "by giving the date of the deprecation. \n"
                        + "It should be given in this format: yyyyMMddHHmmss", false);
        deprecatedTagAttribute.setValueProvider(new IValueProvider() {
            public String[] getValuesfor(IvyTagAttribute att, IvyFile ivyFile) {
                return new String[] {DateUtil.format(new Date())};
            }
        });
        conf.addAttribute(deprecatedTagAttribute);
        configurations.addChildIvyTag(conf);
        List<IvyTag> allConf = new ArrayList<>();
        allConf.add(conf);
        addTag(conf.getName(), allConf);
        ivyTag.addChildIvyTag(configurations);
        addTag(configurations);

        // configurations
        IvyTag publications = new IvyTag("publications",
                "container for published artifact elements");
        IvyTag artifact = new IvyTag("artifact", "declares a published artifact for this module");
        artifact.addAttribute(new IvyTagAttribute("name",
                "the name of the published artifact. This name must not include revision.", true,
                projectNameValueProvider));
        artifact.addAttribute(new IvyTagAttribute("type", "the type of the published artifact. \n"
                + "It's usually its extension, but not necessarily. \n"
                + "For instance, Ivy files are of type 'ivy' but have 'xml' extension", true,
                new ListValueProvider(getDefault("type"))));
        artifact.addAttribute(new IvyTagAttribute("ext", "the extension of the published artifact",
                false, new ListValueProvider(getDefault("ext"))));
        artifact.addAttribute(new IvyTagAttribute("conf",
                "comma separated list of public configurations in which this artifact\n"
                        + "is published. '*' wildcard can be used to designate all public\n"
                        + "configurations of this module", false, masterConfsValueProvider));
        IvyTag conf2 = new IvyTag("conf",
                "indicates a public configuration in which this artifact is published");
        conf2.addAttribute(new IvyTagAttribute("name",
                "the name of a module public configuration in which this artifact\n"
                        + "is published. '*' wildcard can be used to designate all\n"
                        + "public configurations of this module", true, masterConfValueProvider));
        allConf.add(conf2);
        artifact.addChildIvyTag(conf2);
        publications.addChildIvyTag(artifact);
        addTag(publications);
        addTag(artifact);
        ivyTag.addChildIvyTag(publications);

        // dependencies
        IvyTag dependencies = new IvyTag("dependencies", "container for dependency elements");
        // dependency
        IvyTag dependency = new IvyTag("dependency", "declares a dependency for this module") {
            public String[] getPossibleValuesForAttribute(String att, IvyFile ivyfile) {
                String[] r = super.getPossibleValuesForAttribute(att, ivyfile);
                if (r == null) { // listing can be used even for extra attributes
                    List<String> ret = listDependencyTokenValues(att, ivyfile);
                    return ret.toArray(new String[ret.size()]);
                }
                return r;
            }
        };
        IvyTagAttribute orgAtt = new IvyTagAttribute("org",
                "the name of the organisation of the dependency.", false);
        orgAtt.setValueProvider(new IValueProvider() {
            public String[] getValuesfor(IvyTagAttribute att, IvyFile ivyFile) {
                List<String> ret = listDependencyTokenValues(att.getName(), ivyFile);
                ret.add(getSettings().getDefaultOrganization());
                String org = ((IvyModuleDescriptorFile) ivyFile).getOrganisation();
                if (org != null) {
                    ret.add(org);
                }
                return ret.toArray(new String[ret.size()]);
            }

        });
        dependency.addAttribute(orgAtt);
        IvyTagAttribute module = new IvyTagAttribute("name", "the module name of the dependency",
                true);
        module.setValueProvider(new IValueProvider() {
            public String[] getValuesfor(IvyTagAttribute att, IvyFile ivyFile) {
                List<String> ret = listDependencyTokenValues(att.getName(), ivyFile);
                return ret.toArray(new String[ret.size()]);
            }
        });
        dependency.addAttribute(module);
        IvyTagAttribute branch = new IvyTagAttribute("branch",
                "the branch of the dependency. \nDo not set if not needed.", false);
        branch.setValueProvider(new IValueProvider() {
            public String[] getValuesfor(IvyTagAttribute att, IvyFile ivyFile) {
                List<String> ret = listDependencyTokenValues(att.getName(), ivyFile);
                return ret.toArray(new String[ret.size()]);
            }
        });
        dependency.addAttribute(branch);
        IvyTagAttribute rev = new IvyTagAttribute("rev", "the revision of the dependency. \n"
                + "Use 'latest.integration' to get the latest version of the dependency. \n"
                + "You can also end the revision asked with a '+' to get the latest"
                + " matching revision.", true);
        rev.setValueProvider(new IValueProvider() {
            public String[] getValuesfor(IvyTagAttribute att, IvyFile ivyFile) {
                List<String> ret = listDependencyTokenValues(att.getName(), ivyFile);
                ret.add("latest.integration");
                return ret.toArray(new String[ret.size()]);
            }
        });
        dependency.addAttribute(rev);
        dependency.addAttribute(new IvyBooleanTagAttribute("force",
                "a boolean to give an indication to conflict manager \n"
                        + "that this dependency should be forced to this revision", false));
        dependency.addAttribute(new IvyBooleanTagAttribute("transitive",
                "a boolean indicating if this dependency should be resolved transitively or not",
                false));
        IvyTagAttribute confAtt = new IvyTagAttribute("conf",
                "an inline mapping configuration spec", false);
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
                    String org = ((IvyModuleDescriptorFile) ivyFile).getDependencyOrganisation();
                    Map<String, String> otherAttValues = ivyFile.getAllAttsValues();
                    if (org != null && otherAttValues != null && otherAttValues.get("name") != null
                            && otherAttValues.get("rev") != null) {
                        otherAttValues.remove("org");
                        String branch = otherAttValues.remove("branch");
                        otherAttValues.remove("conf");
                        otherAttValues.remove("force");
                        otherAttValues.remove("transitive");
                        otherAttValues.remove("changing");
                        return getDependencyConfs(org, otherAttValues.remove("name"),
                            branch, otherAttValues.remove("rev"), otherAttValues,
                            qualifier, base, arrowIndex);
                    }
                } else {
                    // we are looking for a master conf
                    return getMasterConfs(ivyFile, qualifier, base, arrowIndex);
                }

                return null;
            }

            private String[] getMasterConfs(IvyFile ivyFile, String qualifier, StringBuffer base,
                    int arrowIndex) {
                // search for word after last comma
                int comma = qualifier.lastIndexOf(",") + 1;
                base.append(qualifier, 0, comma);
                qualifier = qualifier.substring(comma);
                while (qualifier.length() > 0 && qualifier.charAt(0) == ' ') {
                    base.append(' ');
                    qualifier = qualifier.substring(1);
                }
                String[] confs = ((IvyModuleDescriptorFile) ivyFile).getConfigurationNames();
                for (int i = 0; i < confs.length; i++) {
                    confs[i] = base + confs[i];
                }
                List<String> ret = new ArrayList<>(Arrays.asList(confs));
                ret.add("*");
                return ret.toArray(new String[ret.size()]);
            }

            private String[] getDependencyConfs(String org, String name, String branch, String rev,
                    Map<String, String> otherAtts, String qualifier, StringBuffer base, int arrowIndex) {
                Ivy ivy = getIvy();
                if (ivy == null) {
                    return null;
                }
                base.append(qualifier, 0, arrowIndex + 2);
                qualifier = qualifier.substring(arrowIndex + 2);
                // search for word after last comma
                int comma = qualifier.lastIndexOf(",") + 1;
                base.append(qualifier, 0, comma);
                qualifier = qualifier.substring(comma);
                while (qualifier.length() > 0 && qualifier.charAt(0) == ' ') {
                    base.append(' ');
                    qualifier = qualifier.substring(1);
                }
                ResolveData data = new ResolveData(ivy.getResolveEngine(), new ResolveOptions());
                ModuleRevisionId mrid = ModuleRevisionId.newInstance(org, name, branch, rev, otherAtts);
                DefaultDependencyDescriptor ddd = new DefaultDependencyDescriptor(mrid, false);
                try {
                    DependencyResolver resolver = ivy.getSettings().getResolver(mrid);
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
                    List<String> ret = new ArrayList<>(Arrays.asList(confs));
                    ret.add("*");
                    return ret.toArray(new String[ret.size()]);
                } catch (ParseException e) {
                    getSettings().logError("The dependencies of " + mrid + " could not be parsed",
                        e);
                    return null;
                }
            }

        });
        IvyTag conf3 = new IvyTag("conf", "defines configuration mapping has sub element");
        conf3.addAttribute(new IvyTagAttribute(
            "name",
            "the name of the master configuration to map. \n"
            + "'*' wildcard can be used to designate all configurations of this module",
            true, masterConfValueProvider));
        conf3.addAttribute(new IvyTagAttribute("mapped",
                "a comma separated list of dependency configurations \n"
                        + "to which this master configuration should be mapped", false,
                new IValueProvider() {
                    public String[] getValuesfor(IvyTagAttribute att, IvyFile ivyFile) {
                        Ivy ivy = getIvy();
                        int[] indexes = ivyFile.getParentTagIndex();
                        if (indexes != null && ivy != null) {
                            Map<String, String> otherAttValues = ivyFile.getAllAttsValues(indexes[0] + 1);
                            String org = ((IvyModuleDescriptorFile) ivyFile)
                                    .getDependencyOrganisation(otherAttValues);
                            if (org != null && otherAttValues != null
                                    && otherAttValues.get("name") != null
                                    && otherAttValues.get("rev") != null) {
                                StringBuilder base = new StringBuilder();
                                String qualifier = ivyFile.getAttributeValueQualifier();
                                // search for word after last comma
                                int comma = qualifier.lastIndexOf(",") + 1;
                                base.append(qualifier, 0, comma);
                                qualifier = qualifier.substring(comma);
                                while (qualifier.length() > 0 && qualifier.charAt(0) == ' ') {
                                    base.append(' ');
                                    qualifier = qualifier.substring(1);
                                }
                                ResolveData data = new ResolveData(ivy.getResolveEngine(),
                                        new ResolveOptions());
                                ModuleRevisionId mrid = ModuleRevisionId.newInstance(org,
                                        otherAttValues.get("name"), otherAttValues.get("rev"));
                                DefaultDependencyDescriptor ddd = new DefaultDependencyDescriptor(
                                        mrid, false);
                                try {
                                    String[] confs = ivy.getSettings().getResolver(mrid)
                                            .getDependency(ddd, data).getDescriptor()
                                            .getConfigurationsNames();
                                    for (int i = 0; i < confs.length; i++) {
                                        confs[i] = base + confs[i];
                                    }
                                    List<String> ret = new ArrayList<>(Arrays.asList(confs));
                                    ret.add("*");
                                    return ret.toArray(new String[ret.size()]);
                                } catch (ParseException e) {
                                    getSettings().logError(
                                        "The dependencies of " + mrid + " could not be parsed", e);
                                    return new String[] {"*"};
                                }
                            }
                        }
                        return new String[] {"*"};
                    }

                }));
        allConf.add(conf3);
        IvyTag mapped = new IvyTag("mapped",
                "map dependency configurations for this master configuration");
        mapped.addAttribute(new IvyTagAttribute("name",
            "the name of the dependency configuration mapped. \n"
                + "'*' wildcard can be used to designate all configurations of this module",
            true, new IValueProvider() {
                public String[] getValuesfor(IvyTagAttribute att, IvyFile ivyFile) {
                    Ivy ivy = getIvy();
                    int[] indexes = ivyFile.getParentTagIndex();
                    if (indexes == null || ivy == null) {
                        return new String[] {"*"};
                    }
                    indexes = ivyFile.getParentTagIndex(indexes[0]);
                    if (indexes == null) {
                        return new String[] {"*"};
                    }
                    Map<String, String> otherAttValues = ivyFile.getAllAttsValues(indexes[0] + 1);
                    String org = ((IvyModuleDescriptorFile) ivyFile)
                        .getDependencyOrganisation(otherAttValues);
                    if (org == null || otherAttValues == null || otherAttValues.get("name") == null
                            || otherAttValues.get("rev") == null) {
                        return new String[] {"*"};
                    }
                    ResolveData data = new ResolveData(ivy.getResolveEngine(),
                        new ResolveOptions());
                    ModuleRevisionId mrid = ModuleRevisionId.newInstance(org,
                            otherAttValues.get("name"), otherAttValues.get("rev"));
                    DefaultDependencyDescriptor ddd = new DefaultDependencyDescriptor(mrid, false);
                    try {
                        String[] confs = ivy.getSettings().getResolver(mrid)
                            .getDependency(ddd, data).getDescriptor().getConfigurationsNames();
                        List<String> ret = new ArrayList<>(Arrays.asList(confs));
                        ret.add("*");
                        return ret.toArray(new String[ret.size()]);
                    } catch (ParseException e) {
                        getSettings().logError("The dependencies of " + mrid
                            + " could not be parsed", e);
                    }
                    return new String[] {"*"};
                }
            }));
        conf3.addChildIvyTag(mapped);
        addTag(mapped);

        Collection<String> matcherNames = Collections.emptySet();
        Ivy ivy = getIvy();
        if (ivy != null) {
            matcherNames = ivy.getSettings().getMatcherNames();
        }
        ListValueProvider matcherNamesProvider = new ListValueProvider(matcherNames.toArray(new String[matcherNames.size()]));

        IvyTag artifact2 = new IvyTag("artifact", "defines artifact restriction \n"
                + "use only if you do not control dependency Ivy file");
        artifact2.addAttribute(new IvyTagAttribute("name", "the name of an artifact of the \n"
                + "dependency module to add to the include list, \n"
                + "or a regexp matching this name", false));
        artifact2.addAttribute(new IvyTagAttribute("type", "the type of the artifact of the \n"
                + "dependency module to add to the include list, \n"
                + "or a regexp matching this name", false,
                new ListValueProvider(getDefault("type"))));
        artifact2
                .addAttribute(new IvyTagAttribute("ext", "the extension of the artifact of the \n"
                        + "dependency module to add to the include list, \n"
                        + "or a regexp matching this name", false, new ListValueProvider(
                        getDefault("ext"))));
        artifact2.addAttribute(new IvyTagAttribute("url",
                "an URL where this artifact can be found \n"
                        + "if it isn't present at the standard \n" + "location in the repository",
                false));
        artifact2.addAttribute(new IvyTagAttribute("conf",
            "comma separated list of the master configurations \n"
                + "in which this artifact should be included. \n"
                + "'*' wildcard can be used to designate all configurations of this module",
            false, masterConfsValueProvider));
        IvyTag conf4 = new IvyTag("conf", "configuration in which the artifact should be included");
        conf4.addAttribute(new IvyTagAttribute("name",
                "the name of the master configuration in which \n"
                        + "the enclosing artifact should be included", true,
                masterConfValueProvider));
        allConf.add(conf4);
        artifact2.addChildIvyTag(conf4);
        addTag(artifact2);
        IvyTag include = new IvyTag("include", "defines artifact restriction \n"
                + "use only if you do not control dependency Ivy file");
        include.addAttribute(new IvyTagAttribute("name", "the name of an artifact of the \n"
                + "dependency module to add to the include list, \n"
                + "or a regexp matching this name", false));
        include.addAttribute(new IvyTagAttribute("type", "the type of the artifact of the \n"
                + "dependency module to add to the include list, \n"
                + "or a regexp matching this name", false,
                new ListValueProvider(getDefault("type"))));
        include
                .addAttribute(new IvyTagAttribute("ext", "the extension of the artifact of the \n"
                        + "dependency module to add to the include list, \n"
                        + "or a regexp matching this name", false, new ListValueProvider(
                        getDefault("ext"))));
        include.addAttribute(new IvyTagAttribute("matcher",
                "the matcher to use to match the modules to include", false, matcherNamesProvider));
        include.addAttribute(new IvyTagAttribute("conf",
            "comma separated list of the master configurations \n"
                + "in which this artifact should be included. \n"
                + "'*' wildcard can be used to designate all configurations of this module",
            false, masterConfsValueProvider));
        IvyTag conf5 = new IvyTag("conf", "configuration in which the artifact should be included");
        conf5.addAttribute(new IvyTagAttribute("name",
                "the name of the master configuration in which \n"
                        + "the enclosing artifact should be included", true,
                masterConfValueProvider));
        include.addChildIvyTag(conf5);
        addTag(include);
        allConf.add(conf5);
        IvyTag exclude = new IvyTag("exclude", "defines artifacts restriction \n"
                + "use only if you do not control dependency Ivy file");
        exclude.addAttribute(new IvyTagAttribute("org", "the organisation of the dependency \n"
                + "module or artifact to exclude, \n" + "or a pattern matching this organisation",
                false));
        exclude.addAttribute(new IvyTagAttribute("module", "the name of the dependency \n"
                + "module or the artifact to exclude, \n"
                + "or a pattern matching this module name", false));
        exclude.addAttribute(new IvyTagAttribute("name", "the name of an artifact of the \n"
                + "dependency module to add to the exclude list, \n"
                + "or a pattern matching this name", false));
        exclude.addAttribute(new IvyTagAttribute("type", "the type of the artifact of the \n"
                + "dependency module to add to the exclude list, \n"
                + "or a pattern matching this name", false, new ListValueProvider(
                getDefault("type"))));
        exclude.addAttribute(new IvyTagAttribute("ext", "the extension of the artifact of the \n"
                + "dependency module to add to the exclude list, \n"
                + "or a pattern matching this name", false,
                new ListValueProvider(getDefault("ext"))));
        exclude.addAttribute(new IvyTagAttribute("matcher",
                "the matcher to use to match the modules to include", false, matcherNamesProvider));
        exclude.addAttribute(new IvyTagAttribute("conf",
                "comma separated list of the master configurations \n"
                        + "in which this artifact should be excluded. \n"
                        + "'*' wildcard can be used to designate all configurations of"
                        + " this module", false, masterConfsValueProvider));
        IvyTag conf6 = new IvyTag("conf", "configuration in which the artifact should be excluded");
        conf6.addAttribute(new IvyTagAttribute("name",
                "the name of the master configuration in which \n"
                        + "the enclosing artifact should be excluded", true,
                masterConfValueProvider));
        allConf.add(conf6);
        exclude.addChildIvyTag(conf6);
        addTag(exclude);
        dependency.addChildIvyTag(conf3);
        dependency.addChildIvyTag(artifact2);
        dependency.addChildIvyTag(include);
        dependency.addChildIvyTag(exclude);
        dependencies.addChildIvyTag(dependency);
        ivyTag.addChildIvyTag(dependencies);
        addTag(dependency);
        addTag(dependencies);

        // dependencies

        IvyTag conflicts = new IvyTag("conflicts", "conflicts managers definition section");
        IvyTag manager = new IvyTag("manager", "declares a conflict manager for this module");
        manager.addAttribute(new IvyTagAttribute("org",
                "the name, or a regexp matching the name of organisation \n"
                        + "to which this conflict manager should apply", false));
        manager.addAttribute(new IvyTagAttribute("module",
                "the name, or a regexp matching the name of module \n"
                        + "to which this conflict manager should apply", false));
        manager.addAttribute(new IvyTagAttribute("name", "the name of the conflict manager to use",
                false));
        manager.addAttribute(new IvyTagAttribute("rev",
                "a comma separated list of revisions this conflict manager should select", false));
        manager.addAttribute(new IvyTagAttribute("matcher",
                "the matcher to use to match the modules for which \n"
                        + "the conflict manager should be used", false));
        conflicts.addChildIvyTag(manager);
        ivyTag.addChildIvyTag(conflicts);
        addTag(conflicts);
        addTag(manager);
    }

    protected String getRootIvyTagName() {
        return "ivy-module";
    }

    private List<String> listDependencyTokenValues(String att, IvyFile ivyfile) {
        Map<String, String> allAttsValues = ivyfile.getAllAttsValues();
        String org = ((IvyModuleDescriptorFile) ivyfile).getOrganisation();
        if (org != null && !allAttsValues.containsKey("org")) {
            allAttsValues.put("org", org);
        }
        return listDependencyTokenValues(att, allAttsValues);
    }

    private List<String> listDependencyTokenValues(String att, Map<String, String> otherAttValues) {
        List<String> ret = new ArrayList<>();
        Ivy ivy = getIvy();
        if (ivy != null) {
            replaceToken(otherAttValues, "org", IvyPatternHelper.ORGANISATION_KEY);
            replaceToken(otherAttValues, "name", IvyPatternHelper.MODULE_KEY);
            replaceToken(otherAttValues, "rev", IvyPatternHelper.REVISION_KEY);

            if (!otherAttValues.containsKey(IvyPatternHelper.BRANCH_KEY)) {
                otherAttValues.put(IvyPatternHelper.BRANCH_KEY, ivy.getSettings()
                        .getDefaultBranch());
            }

            String stdAtt = standardiseDependencyAttribute(att);
            otherAttValues.remove(stdAtt);
            // transform otherAttValues into Map required by Ivy
            Map<String, Object> ivyAttrs = new HashMap<>();
            ivyAttrs.putAll(otherAttValues);
            String[] revs = ivy.listTokenValues(stdAtt, ivyAttrs);
            if (revs != null) {
                ret.addAll(Arrays.asList(revs));
            }
        }
        return ret;
    }

    private void replaceToken(Map<String, String> otherAttValues, String oldToken, String newToken) {
        String val = otherAttValues.remove(oldToken);
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

    public IvyFile newIvyFile(String name, String content, int documentOffset) {
        return new IvyModuleDescriptorFile(getSettings(), name, content, documentOffset);
    }

}
