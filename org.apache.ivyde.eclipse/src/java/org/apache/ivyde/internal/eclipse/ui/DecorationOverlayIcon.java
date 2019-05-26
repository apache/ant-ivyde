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

import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;

/**
 * extracted from org.eclipse.jface.viewers.DecorationOverlayIcon of Eclipse 3.3
 */
public class DecorationOverlayIcon extends CompositeImageDescriptor {

    private final Image baseImage;

    private final Point size;

    private final ImageDescriptor overlayImage;

    public DecorationOverlayIcon(Image baseImage, ImageDescriptor overlayImage, int quadrant) {
        this.baseImage = baseImage;
        this.overlayImage = overlayImage;
        this.size = new Point(baseImage.getBounds().width, baseImage.getBounds().height);
    }

    protected void drawCompositeImage(int width, int height) {
        drawImage(baseImage.getImageData(), 0, 0);
        ImageData overlayData = overlayImage.getImageData();
        // Use the missing descriptor if it is not there.
        if (overlayData == null) {
            overlayData = ImageDescriptor.getMissingImageDescriptor().getImageData();
        }
        drawImage(overlayData, 0, size.y - overlayData.height);
    }

    protected Point getSize() {
        return size;
    }

    protected int getTransparentPixel() {
        return baseImage.getImageData().transparentPixel;
    }

}
