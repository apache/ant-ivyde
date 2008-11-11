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
package org.apache.ivyde.eclipse.ui;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.ivyde.eclipse.IvyDEException;
import org.apache.ivyde.eclipse.IvyPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Text;

public class SettingsPathText extends Composite {

    public static final String TOOLTIP_SETTINGS = "The url where your ivysettings file can be found. \n"
            + "Leave it empty to reference the default ivy settings. \n"
            + "Relative paths are handled relative to the project.\n"
            + " Example: 'project:///ivysettings.xml' or 'project://myproject/ivysettings.xml'.";

    private Text settingsText;

    private ControlDecoration settingsTextDeco;

    private final List listeners = new ArrayList();

    private IvyDEException settingsError;

    public SettingsPathText(Composite parent, int style) {
        super(parent, style);
        GridLayout layout = new GridLayout(2, false);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        setLayout(layout);

        Image errorDecoImage = FieldDecorationRegistry.getDefault().getFieldDecoration(
            FieldDecorationRegistry.DEC_ERROR).getImage();

        settingsText = new Text(this, SWT.SINGLE | SWT.BORDER);
        settingsText.setToolTipText(TOOLTIP_SETTINGS);
        settingsText.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
        settingsText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                settingsPathUpdated();
            }
        });
        settingsTextDeco = new ControlDecoration(settingsText, SWT.LEFT | SWT.TOP);
        settingsTextDeco.setMarginWidth(2);
        settingsTextDeco.setImage(errorDecoImage);
        settingsTextDeco.hide();
        settingsTextDeco.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                if (settingsError != null) {
                    settingsError.show(IStatus.ERROR, "IvyDE configuration problem", null);
                }
            }
        });

        Button browse = new Button(this, SWT.NONE);
        browse.setText("Browse");
        browse.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                File f = getFile(new File("/"));
                if (f != null) {
                    try {
                        settingsText.setText(f.toURL().toExternalForm());
                        settingsPathUpdated();
                    } catch (MalformedURLException ex) {
                        // this cannot happen
                        IvyPlugin.log(IStatus.ERROR,
                            "The file got from the file browser has not a valid URL", ex);
                    }
                }
            }
        });
    }

    public String getSettingsPath() {
        return settingsText.getText();
    }

    public interface SettingsPathListener {
        void settingsPathUpdated(String path);
    }

    public void addListener(SettingsPathListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    public void remodeListener(SettingsPathListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    void settingsPathUpdated() {
        synchronized (listeners) {
            Iterator it = listeners.iterator();
            while (it.hasNext()) {
                ((SettingsPathListener) it.next()).settingsPathUpdated(settingsText.getText());
            }
        }
    }

    public void setSettingsError(IvyDEException error) {
        if (error == null) {
            settingsError = null;
            settingsTextDeco.hide();
            settingsTextDeco.hideHover();
        } else if (!error.equals(settingsError)) {
            settingsError = error;
            settingsTextDeco.show();
            if (settingsText.isVisible()) {
                settingsTextDeco.showHoverText(error.getShortMsg());
            }
        }
    }

    public void updateErrorMarker() {
        if (isVisible() && settingsError != null) {
            settingsTextDeco.showHoverText(settingsError.getShortMsg());
        } else {
            settingsTextDeco.hideHover();
        }
    }

    File getFile(File startingDirectory) {
        FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
        if (startingDirectory != null) {
            dialog.setFileName(startingDirectory.getPath());
        }
        dialog.setFilterExtensions(new String[] {"*.xml", "*"});
        String file = dialog.open();
        if (file != null) {
            file = file.trim();
            if (file.length() > 0) {
                return new File(file);
            }
        }
        return null;
    }

    public void init(String ivySettingsPath) {
        settingsText.setText(ivySettingsPath);
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        settingsText.setEnabled(enabled);
    }
}
