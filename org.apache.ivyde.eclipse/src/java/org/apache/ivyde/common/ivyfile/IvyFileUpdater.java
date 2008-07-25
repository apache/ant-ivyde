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
package org.apache.ivyde.common.ivyfile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.util.FileUtil;

public class IvyFileUpdater {
    private static final String NL = System.getProperty("line.separator");

    private static class UpdateInfo {
        int insertFromIndex = 0;
        int insertToIndex = 0;
        String prefix = "";
        String suffix = "";
    }
    
    public void addDependency(File ivyFile, ModuleRevisionId depId, String confMapping) throws IOException {
        String content = FileUtil.readEntirely(ivyFile);
        
        UpdateInfo info = findUpdateInfoToAddDependency(content);
        
        String dep = getDependencyToAdd(depId, confMapping);
        
        update(ivyFile, content, info, dep);
    }

    private void update(File ivyFile, String content, UpdateInfo info, String insert)
            throws FileNotFoundException {
        PrintWriter w = new PrintWriter(ivyFile);
        try {
            w.print(content.substring(0, info.insertFromIndex));
            w.print(info.prefix);
            w.print(insert);
            w.print(info.suffix);
            w.print(content.substring(info.insertToIndex));
            w.flush();
        } finally {
            w.close();
        }
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

    private UpdateInfo findUpdateInfoToAddDependency(String content) {
        UpdateInfo info = new UpdateInfo();
        
        String reversed = new StringBuffer(content).reverse().toString();
        int length = content.length();
        
        Pattern dependenciesClose = Pattern.compile("<\\s*/dependencies");
        Matcher depsCloseMatcher = dependenciesClose.matcher(content);
        if (depsCloseMatcher.find()) {
            info.insertFromIndex = findLastDependencyEnd(content, depsCloseMatcher.start());
            if (info.insertFromIndex == -1) {
                info.insertFromIndex = getLastEndIndex(Pattern.compile("<\\s*dependencies.*?>"), content, depsCloseMatcher.start());
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

    private int findLastDependencyEnd(String content, int end) {
        int depCloseIndex = getLastEndIndex(Pattern.compile("</\\s*dependency\\s*>"), content, end);
        int depOpCloseIndex = getLastEndIndex(Pattern.compile("\\<\\s*dependency.*?\\/\\>"), content, end);
        return Math.max(depCloseIndex, depOpCloseIndex);
    }

    private int getLastEndIndex(Pattern pattern, String content, int end) {
        Matcher matcher = pattern.matcher(content);
        int index = -1;
        while (matcher.find(index + 1)) {
            if (matcher.end() > end) {
                return index;
            } else {
                index = matcher.end();
            }
        }
        return index;
    }

    private int reverse(int index, int length) {
        return length - index;
    }

}
