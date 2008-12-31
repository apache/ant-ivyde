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
package org.apache.ivyde.eclipse.cpcontainer;

import org.apache.ivyde.eclipse.IvyPlugin;
import org.apache.ivyde.eclipse.ui.DecorationOverlayIcon;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.packageview.ClassPathContainer;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.swt.graphics.Image;

public class IvyClasspathContainerDecorator implements ILabelDecorator {

    private ListenerList listenerList;

    public IvyClasspathContainerDecorator() {
        IvyPlugin.getDefault().setContainerDecorator(this);
    }

    public String decorateText(String text, Object element) {
        return text;
    }

    public Image decorateImage(Image image, Object obj) {
        ImageDescriptor overlay = getOverlay(obj);
        if (overlay == null) {
            return null;
        }
        return new DecorationOverlayIcon(image, overlay, IDecoration.BOTTOM_LEFT).createImage();
    }

    protected ImageDescriptor getOverlay(Object obj) {
        if (obj instanceof ClassPathContainer) {
            IvyClasspathContainer ivycp = IvyClasspathUtil.jdt2IvyCPC((ClassPathContainer) obj);
            if (ivycp != null) {
                if (!ivycp.getConf().confOk) {
                    return JavaPluginImages.DESC_OVR_ERROR;
                }
            }
        }
        return null;
    }

    public void dispose() {
        // nothing to do
    }

    public boolean isLabelProperty(Object element, String property) {
        return true;
    }

    public void addListener(ILabelProviderListener listener) {
        if (listenerList == null) {
            listenerList = new ListenerList();
        }
        listenerList.add(listener);
    }

    public void removeListener(ILabelProviderListener listener) {
        if (listenerList != null) {
            listenerList.remove(listener);
        }
    }

    public void statusChanged(IvyClasspathContainerConfiguration conf) {
        if (listenerList != null && !listenerList.isEmpty()) {
            ClassPathContainer elem = new ClassPathContainer(conf.javaProject, JavaCore
                    .newContainerEntry(conf.getPath()));
            LabelProviderChangedEvent event = new LabelProviderChangedEvent(this, elem);
            Object[] listeners = listenerList.getListeners();
            for (int i = 0; i < listeners.length; i++) {
                ((ILabelProviderListener) listeners[i]).labelProviderChanged(event);
            }
        }
    }

}
