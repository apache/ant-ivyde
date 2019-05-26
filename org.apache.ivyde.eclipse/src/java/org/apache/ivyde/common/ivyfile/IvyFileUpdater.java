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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.util.FileUtil;

public class IvyFileUpdater {
    private static final String NL = System.getProperty("line.separator");

    private static final class UpdateInfo {

        private int insertFromIndex = 0;

        private int insertToIndex = 0;

        private String prefix = "";

        private String suffix = "";

        private String insert = "";

        private UpdateInfo() {
            // nothing to do
        }
    }

    public void addDependency(File ivyFile, String org, String name, String revision,
            String confMapping) throws IOException {
        ModuleRevisionId depId = new ModuleRevisionId(new ModuleId(org, name), revision);
        addDependency(ivyFile, depId, confMapping);
    }

    public void addDependency(File ivyFile, ModuleRevisionId depId, String confMapping)
            throws IOException {
        String content = FileUtil.readEntirely(ivyFile);

        UpdateInfo info = findUpdateInfoToAddDependency(content, depId, confMapping);

        update(ivyFile, content, info);
    }

    /**
     * Removes a direct dependency in the given Ivy file, or excludes it if it isn't a direct
     * dependency, unless the Ivy file declares no dependency at all, in which case the file isn't
     * changed.
     *
     * @param ivyFile
     *            the file pointing to the Ivy file to update
     * @param depId
     *            the module id of the dependency to remove or exclude
     * @throws IOException failing to read the Ivy file
     */
    public void removeOrExcludeDependency(File ivyFile, ModuleId depId) throws IOException {
        String content = FileUtil.readEntirely(ivyFile);

        UpdateInfo info = findUpdateInfoToRemoveDependency(content, depId);
        if (info != null) {
            update(ivyFile, content, info);
        }
    }

    private void update(File ivyFile, String content, UpdateInfo info)
            throws FileNotFoundException {
        try (PrintWriter w = new PrintWriter(new FileOutputStream(ivyFile))) {
            w.print(content.substring(0, info.insertFromIndex));
            w.print(info.prefix);
            w.print(info.insert);
            w.print(info.suffix);
            w.print(content.substring(info.insertToIndex));
            w.flush();
        }
    }

    private UpdateInfo findUpdateInfoToAddDependency(String content, ModuleRevisionId depId,
            String confMapping) {
        UpdateInfo info = new UpdateInfo();
        info.insert = getDependencyToAdd(depId, confMapping);

        Pattern dependenciesClose = Pattern.compile("<\\s*/dependencies");
        Matcher depsCloseMatcher = dependenciesClose.matcher(content);
        if (depsCloseMatcher.find()) {
            info.insertFromIndex = findLastDependencyEnd(content, depsCloseMatcher.start());
            if (info.insertFromIndex == -1) {
                info.insertFromIndex = getLastMatchIndex(Pattern.compile("<\\s*dependencies.*?>"),
                    content, depsCloseMatcher.start());
                if (info.insertFromIndex == -1) {
                    info.insertFromIndex = depsCloseMatcher.start();
                } else {
                    info.prefix = NL;
                }
            } else {
                info.prefix = NL;
            }
            info.insertToIndex = info.insertFromIndex;
            return info;
        }
        Pattern depsOpenClose = Pattern.compile("<\\s*dependencies\\s*/>");
        Matcher depsOpenCloseMatcher = depsOpenClose.matcher(content);
        if (depsOpenCloseMatcher.find()) {
            info.insertFromIndex = depsOpenCloseMatcher.start();
            info.insertToIndex = depsOpenCloseMatcher.end();
            info.prefix = "<dependencies>" + NL;
            info.suffix = NL + "    </dependencies>";
            return info;
        }
        Pattern moduleClose = Pattern.compile("</\\s*ivy-module\\s*>");
        Matcher moduleCloseMatcher = moduleClose.matcher(content);
        if (moduleCloseMatcher.find()) {
            info.insertFromIndex = moduleCloseMatcher.start();
            info.insertToIndex = info.insertFromIndex;
            info.prefix = "    <dependencies>" + NL;
            info.suffix = NL + "    </dependencies>" + NL;
            return info;
        }
        return info;
    }

    private UpdateInfo findUpdateInfoToRemoveDependency(String content, ModuleId depId) {
        UpdateInfo info = new UpdateInfo();

        Matcher depsMatcher = Pattern.compile("<\\s*dependencies").matcher(content);
        if (!depsMatcher.find()) {
            // no dependencies at all, nothing to do
            return null;
        }
        Matcher depMatcher = Pattern.compile("<\\s*dependency\\s+.*name=[\"']([^\"']+)[\"']")
                .matcher(content);
        int start = depsMatcher.start();
        while (depMatcher.find(start)) {
            if (depId.getName().equals(depMatcher.group(1))) {
                // we have found the dependency to remove: let's remove it
                info.insertFromIndex = depMatcher.start();
                Matcher m = Pattern.compile("</\\s*dependency\\s*>").matcher(content);
                if (m.find(info.insertFromIndex)) {
                    info.insertToIndex = m.end();
                }
                m = Pattern.compile("<\\s*dependency[^<]*?\\/\\>").matcher(content);
                if (m.find(info.insertFromIndex)) {
                    info.insertToIndex = info.insertToIndex > 0 ? Math.min(info.insertToIndex, m
                            .end()) : m.end();
                }
                info.insertFromIndex = findStartOfBlock(content, info.insertFromIndex);
                info.insertToIndex = findEndOfBlock(content, info.insertToIndex);
                return info;
            }
            start = depMatcher.end();
        }

        // we haven't found the dependency in the list of declared dependencies

        if (start == depsMatcher.start()) {
            // no dependencies at all, nothing to do
            return null;
        }
        // there is at least one direct dependency, but not the one to remove, so we must exclude it
        Matcher depsCloseMatcher = Pattern.compile("<\\s*/dependencies").matcher(content);
        if (!depsCloseMatcher.find()) {
            // no closing tag for dependencies, probably malformed xml, nothing to do
            return null;
        }
        info.insertFromIndex = findLastDependencyEnd(content, depsCloseMatcher.start());
        info.insertToIndex = info.insertFromIndex;
        info.prefix = NL;
        info.insert = getDependencyToExclude(depId);
        return info;
    }

    private int findLastDependencyEnd(String content, int end) {
        int depCloseIndex = getLastMatchIndex(Pattern.compile("</\\s*dependency\\s*>"), content,
            end);
        int depOpCloseIndex = getLastMatchIndex(Pattern.compile("\\<\\s*dependency.*?\\/\\>"),
            content, end);
        return Math.max(depCloseIndex, depOpCloseIndex);
    }

    private String getDependencyToAdd(ModuleRevisionId depId, String confMapping) {
        String dep = "        <dependency org=\"" + depId.getOrganisation() + "\"";
        dep += " name=\"" + depId.getName() + "\"";
        dep += " rev=\"" + depId.getRevision() + "\"";
        if (confMapping != null) {
            dep += " conf=\"" + confMapping + "\"";
        }
        dep += " />";
        return dep;
    }

    private String getDependencyToExclude(ModuleId depId) {
        String dep = "        <exclude org=\"" + depId.getOrganisation() + "\"";
        dep += " module=\"" + depId.getName() + "\"";
        dep += " />";
        return dep;
    }

    private int getLastMatchIndex(Pattern pattern, String content, int end) {
        Matcher matcher = pattern.matcher(content);
        int index = -1;
        while (matcher.find(index + 1)) {
            if (matcher.end() > end) {
                return index;
            }

            index = matcher.end();
        }
        return index;
    }

    private int findStartOfBlock(String content, int index) {
        index--;
        while (index >= 0) {
            char c = content.charAt(index);
            if (c != ' ' && c != '\t') {
                return index + 1;
            }
            index--;
        }
        return 0;
    }

    private int findEndOfBlock(String content, int index) {
        while (index < content.length()) {
            char c = content.charAt(index);
            if (c != ' ' && c != '\t') {
                if (c == '\n' || c == '\r') {
                    return index + 1;
                }
                return index;
            }
            index++;
        }
        return index - 1;
    }

}
