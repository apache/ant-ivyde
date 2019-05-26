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
package org.apache.ivyde.internal.eclipse.workspaceresolver;

import java.util.Comparator;

import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.plugins.latest.ArtifactInfo;
import org.apache.ivy.plugins.latest.ComparatorLatestStrategy;
import org.apache.ivy.plugins.version.VersionMatcher;

/**
 * A dumb copy of the {@link org.apache.ivy.plugins.latest.LatestRevisionStrategy} but that take
 * into account the latest and working revision as superior to any other revision.
 */
public class IvyDEStrategy extends ComparatorLatestStrategy {

    final class MridComparator implements Comparator<ModuleRevisionId> {
        public int compare(ModuleRevisionId o1, ModuleRevisionId o2) {
            String rev1 = o1.getRevision();
            String rev2 = o2.getRevision();

            boolean latestRev1 = rev1.startsWith("latest") || rev1.startsWith("working");
            boolean latestRev2 = rev2.startsWith("latest") || rev2.startsWith("working");
            if (latestRev1 && !latestRev2) {
                return 1;
            } else if (latestRev2 && !latestRev1) {
                return -1;
            }

            rev1 = rev1.replaceAll("([a-zA-Z])(\\d)", "$1.$2");
            rev1 = rev1.replaceAll("(\\d)([a-zA-Z])", "$1.$2");
            rev2 = rev2.replaceAll("([a-zA-Z])(\\d)", "$1.$2");
            rev2 = rev2.replaceAll("(\\d)([a-zA-Z])", "$1.$2");

            String[] parts1 = rev1.split("[\\._\\-\\+]");
            String[] parts2 = rev2.split("[\\._\\-\\+]");

            int i = 0;
            for (; i < parts1.length && i < parts2.length; i++) {
                if (parts1[i].equals(parts2[i])) {
                    continue;
                }
                boolean is1Number = isNumber(parts1[i]);
                boolean is2Number = isNumber(parts2[i]);
                if (is1Number && !is2Number) {
                    return 1;
                }
                if (is2Number && !is1Number) {
                    return -1;
                }
                if (is1Number && is2Number) {
                    return Long.valueOf(parts1[i]).compareTo(Long.valueOf(parts2[i]));
                }
                return parts1[i].compareTo(parts2[i]);
            }
            if (i < parts1.length) {
                return isNumber(parts1[i]) ? 1 : -1;
            }
            if (i < parts2.length) {
                return isNumber(parts2[i]) ? -1 : 1;
            }
            return 0;
        }

        private boolean isNumber(String str) {
            return str.matches("\\d+");
        }
    }

    /**
     * Compares two ArtifactInfo by their revision. Revisions are compared using an algorithm
     * inspired by PHP version_compare one, unless a dynamic revision is given, in which case the
     * version matcher is used to perform the comparison.
     */
    final class ArtifactInfoComparator implements Comparator<ArtifactInfo> {
        public int compare(ArtifactInfo o1, ArtifactInfo o2) {
            String rev1 = o1.getRevision();
            String rev2 = o2.getRevision();

            /*
             * The revisions can still be not resolved, so we use the current version matcher to
             * know if one revision is dynamic, and in this case if it should be considered greater
             * or lower than the other one. Note that if the version matcher compare method returns
             * 0, it's because it's not possible to know which revision is greater. In this case we
             * consider the dynamic one to be greater, because most of the time it will then be
             * actually resolved and a real comparison will occur.
             */
            VersionMatcher vmatcher = IvyContext.getContext().getSettings().getVersionMatcher();
            ModuleRevisionId mrid1 = ModuleRevisionId.newInstance("", "", rev1);
            ModuleRevisionId mrid2 = ModuleRevisionId.newInstance("", "", rev2);
            if (vmatcher.isDynamic(mrid1)) {
                int c = vmatcher.compare(mrid1, mrid2, mridComparator);
                return c >= 0 ? 1 : -1;
            } else if (vmatcher.isDynamic(mrid2)) {
                int c = vmatcher.compare(mrid2, mrid1, mridComparator);
                return c >= 0 ? -1 : 1;
            }

            return mridComparator.compare(mrid1, mrid2);
        }
    }

    private final Comparator<ModuleRevisionId> mridComparator = new MridComparator();

    private final Comparator<ArtifactInfo> artifactInfoComparator = new ArtifactInfoComparator();

    public IvyDEStrategy() {
        setComparator(artifactInfoComparator);
        setName("ivyde-latest-revision");
    }

}
