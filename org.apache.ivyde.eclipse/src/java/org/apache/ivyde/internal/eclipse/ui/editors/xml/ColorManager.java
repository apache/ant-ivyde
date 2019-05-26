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
package org.apache.ivyde.internal.eclipse.ui.editors.xml;

import java.util.HashMap;
import java.util.Map;

import org.apache.ivyde.internal.eclipse.ui.preferences.PreferenceConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

public class ColorManager {

    private final Map<String, RGB> fKeyTable = new HashMap<>();

    private final Map<Display, Map<RGB, Color>> fDisplayTable = new HashMap<>();

    public void dispose(Display display) {
        Map<RGB, Color> colorTable = fDisplayTable.get(display);
        if (colorTable != null) {
            for (Color color : colorTable.values()) {
                if (color != null && !color.isDisposed()) {
                    color.dispose();
                }
            }
        }
    }

    public Color getColor(RGB rgb) {
        if (rgb == null) {
            return null;
        }

        final Display display = Display.getCurrent();
        Map<RGB, Color> colorTable = fDisplayTable.get(display);
        if (colorTable == null) {
            colorTable = new HashMap<>();
            fDisplayTable.put(display, colorTable);
            display.disposeExec(new Runnable() {
                public void run() {
                    dispose(display);
                }
            });
        }

        Color color = colorTable.get(rgb);
        if (color == null) {
            color = new Color(Display.getCurrent(), rgb);
            colorTable.put(rgb, color);
        }

        return color;
    }

    public Color getColor(String key) {
        if (key == null) {
            return null;
        }
        RGB rgb = fKeyTable.get(key);
        return getColor(rgb);
    }

    public void bindColor(String key, RGB rgb) {
        Object value = fKeyTable.get(key);
        if (value != null) {
            throw new UnsupportedOperationException();
        }

        fKeyTable.put(key, rgb);
    }

    public void unbindColor(String key) {
        fKeyTable.remove(key);
    }

    public void refreshFromStore(IPreferenceStore store) {
        rebind(store, PreferenceConstants.EDITOR_COLOR_XML_COMMENT);
        rebind(store, PreferenceConstants.EDITOR_COLOR_PROC_INSTR);
        rebind(store, PreferenceConstants.EDITOR_COLOR_STRING);
        rebind(store, PreferenceConstants.EDITOR_COLOR_DEFAULT);
        rebind(store, PreferenceConstants.EDITOR_COLOR_TAG);
    }

    private void rebind(IPreferenceStore store, String colorId) {
        unbindColor(colorId);
        bindColor(colorId, PreferenceConverter.getColor(store, colorId));
    }
}
