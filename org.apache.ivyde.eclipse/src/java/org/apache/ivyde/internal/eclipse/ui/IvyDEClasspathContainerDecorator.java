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
package org.apache.ivyde.internal.eclipse.ui;

import org.apache.ivyde.internal.eclipse.cpcontainer.IvyClasspathContainerImpl;
import org.apache.ivyde.internal.eclipse.cpcontainer.IvyClasspathUtil;
import org.eclipse.jdt.internal.ui.packageview.ClassPathContainer;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;

public class IvyDEClasspathContainerDecorator implements ILabelDecorator {

    public boolean isLabelProperty(Object element, String property) {
        return true;
    }

    public String decorateText(String text, Object element) {
        IvyClasspathContainerImpl ivycp = IvyClasspathUtil.jdt2IvyCPC((ClassPathContainer) element);
        if (ivycp == null) {
            return null;
        }
        return text + " " + ivycp.getConf().getIvyXmlPath() + " " + ivycp.getConf().getConfs();
    }

    public Image decorateImage(Image image, Object element) {
        return null;
    }

    public void addListener(ILabelProviderListener listener) {
        // nothing to do
    }

    public void dispose() {
        // nothing to do
    }

    public void removeListener(ILabelProviderListener listener) {
        // nothing to do
    }

}
