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
import java.io.IOException;

import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.util.FileUtil;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class IvyFileUpdaterTest {
    private final IvyFileUpdater updater = new IvyFileUpdater();

    @Test
    public void testAddDependency() throws IOException {
        testAddDependency("addDependency1");
        testAddDependency("addDependency2");
        testAddDependency("addDependency3");
        testAddDependency("addDependency4");
        testAddDependency("addDependency5");
        testAddDependency("addDependency6");
        testAddDependency("addDependency7");
        testAddDependency("addDependency8");
    }

    private void testAddDependency(String test) throws IOException {
        File dest = File.createTempFile("ivy", ".xml");
        dest.deleteOnExit();
        FileUtil.copy(IvyFileUpdaterTest.class.getResourceAsStream(test + "/ivy.xml"), dest, null);
        updater.addDependency(dest, ModuleRevisionId.parse("apache#newdep;1.0"), "default->default");
        assertEquals(
            test + " failed",
            FileUtil.readEntirely(IvyFileUpdaterTest.class.getResourceAsStream(test + "/expected.xml")),
            FileUtil.readEntirely(dest));
    }
    @Test
    public void testRemoveDependency() throws IOException {
        testRemoveDependency("removeDependency1");
        testRemoveDependency("removeDependency2");
        testRemoveDependency("removeDependency3");
        testRemoveDependency("removeDependency4");
    }

    private void testRemoveDependency(String test) throws IOException {
        File dest = File.createTempFile("ivy", ".xml");
        dest.deleteOnExit();
        FileUtil.copy(IvyFileUpdaterTest.class.getResourceAsStream(test + "/ivy.xml"), dest, null);
        updater.removeOrExcludeDependency(dest, new ModuleId("apache", "newdep"));
        assertEquals(
            test + " failed",
            FileUtil.readEntirely(IvyFileUpdaterTest.class.getResourceAsStream(test + "/expected.xml")),
            FileUtil.readEntirely(dest));
    }
}
