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
package org.apache.ivyde.common.model;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Map;

import org.apache.ivyde.common.ivyfile.IvyModuleDescriptorFile;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class IvyFileUtilTest {
    private static String hibContentStr;

    @BeforeClass
    public static void setUp() {
        try (RandomAccessFile accessFile = new RandomAccessFile(IvyFileUtilTest.class.getResource(
                "/ivy-hibernate.xml").getFile(), "r")) {
            byte[] content = new byte[(int) accessFile.length()];
            accessFile.read(content);
            hibContentStr = new String(content);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Test
    public void testInTag() {
        IvyFile ivyFile = new IvyModuleDescriptorFile(null, "", hibContentStr);
        assertTrue(ivyFile.inTag(1000));
    }

    @Test
    public void testGetTagName() {
        IvyFile ivyFile = new IvyModuleDescriptorFile(null, "", hibContentStr);
        String tag = ivyFile.getTagName(1000);
        assertEquals("info", tag);
        tag = ivyFile.getTagName(1600);
        assertEquals("description", tag);
        // tag = IvyFileUtil.getTagName(1000);
    }

    @Test
    public void testGetAllAttsValues() {
        String test = "<test att1=\"value1\" att2 =\"value 2 \"  att3 =\" value3 \" att4   =   \"  4  \"";
        IvyFile ivyFile = new IvyModuleDescriptorFile(null, "", test);
        Map<String, String> all = ivyFile.getAllAttsValues(1);
        assertNotNull(all);
        assertEquals(4, all.size());
        assertEquals("value1", all.get("att1"));
        assertEquals("value 2 ", all.get("att2"));
        assertEquals(" value3 ", all.get("att3"));
        assertEquals("  4  ", all.get("att4"));
    }

    @Test
    public void testGetAttributeName() {
        String test = "<test nospace=\"\" 1Space =\"\"  2Space = \"\" lotofSpace   =   \"    \"";
        IvyFile ivyFile = new IvyModuleDescriptorFile(null, "", test);
        String name = ivyFile.getAttributeName(15);
        assertEquals("nospace", name);
        name = ivyFile.getAttributeName(26);
        assertEquals("1Space", name);
        name = ivyFile.getAttributeName(39);
        assertEquals("2Space", name);
        name = ivyFile.getAttributeName(60);
        assertEquals("lotofSpace", name);
    }

    @Test
    public void testGetParentTagName() {
        IvyFile ivyFile = new IvyModuleDescriptorFile(null, "", hibContentStr);
        String tag = ivyFile.getParentTagName(1000);
        assertEquals("ivy-module", tag);
        tag = ivyFile.getParentTagName(2600);
        assertEquals("configurations", tag);
        tag = ivyFile.getParentTagName(2493);
        assertEquals("configurations", tag);
        tag = ivyFile.getParentTagName(1700);
        assertEquals("description", tag);
        tag = ivyFile.getParentTagName(5585);
        assertNull(tag);
    }

    @Test
    @Ignore
    public void testReadyForValue() {
    }

    @Test
    @Ignore
    public void testGetStringIndex() {
    }

    @Test
    @Ignore
    public void testGetQualifier() {
    }
}
