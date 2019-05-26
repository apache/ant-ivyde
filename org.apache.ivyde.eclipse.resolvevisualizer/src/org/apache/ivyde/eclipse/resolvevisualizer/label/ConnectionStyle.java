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
package org.apache.ivyde.eclipse.resolvevisualizer.label;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.zest.core.widgets.ZestStyles;

public class ConnectionStyle {
    private static final Color DARK_RED = new Color(Display.getDefault(), 127, 0, 0);

    private int lineWidth = 1;
    private Color highlightColor = DARK_RED;
    private int connectionStyle = ZestStyles.CONNECTIONS_SOLID;
    private boolean revealOnHighlight = true;

    /**
     * Accept the defaults
     */
    public ConnectionStyle() {
    }

    public ConnectionStyle(int connectionStyle, Color highlightColor, int lineWidth, boolean revealOnHighlight) {
        super();
        this.connectionStyle = connectionStyle;
        this.highlightColor = highlightColor;
        this.lineWidth = lineWidth;
        this.revealOnHighlight = revealOnHighlight;
    }

    public static Color getDARK_RED() {
        return DARK_RED;
    }

    public int getLineWidth() {
        return lineWidth;
    }

    public Color getHighlightColor() {
        return highlightColor;
    }

    public int getConnectionStyle() {
        return connectionStyle;
    }

    public boolean isRevealOnHighlight() {
        return revealOnHighlight;
    }
}
