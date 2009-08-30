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
import org.apache.ivyde.eclipse.cpcontainer.IvyClasspathUtil;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.fieldassist.DecoratedField;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.fieldassist.IControlCreator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class SettingsEditor extends Composite {

    public static final String TOOLTIP_SETTINGS_PATH = "The url where your ivysettings file can be found. \n"
            + "Leave it empty to reference the default ivy settings. \n"
            + "Relative paths are handled relative to the project.\n"
            + " Example: 'project:///ivysettings.xml' or 'project://myproject/ivysettings.xml'.";

    public static final String TOOLTIP_PROPERTY_FILES = "Comma separated list of build property files.\n"
            + "Example: build.properties, override.properties";

    private Text settingsText;

    private DecoratedField settingsTextDeco;

    private final List listeners = new ArrayList();

    private IvyDEException settingsError;

    private FieldDecoration errorDecoration;

    private Text propFilesText;

    private Button loadOnDemandButton;

    public SettingsEditor(Composite parent, int style) {
        super(parent, style);
        GridLayout layout = new GridLayout(3, false);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        setLayout(layout);

        Label label = new Label(this, SWT.NONE);
        label.setText("Ivy settings path:");

        errorDecoration = FieldDecorationRegistry.getDefault().getFieldDecoration(
            FieldDecorationRegistry.DEC_ERROR);

        settingsTextDeco = new DecoratedField(this, SWT.LEFT | SWT.TOP, new IControlCreator() {
            public Control createControl(Composite parent, int style) {
                return new Text(parent, SWT.SINGLE | SWT.BORDER);
            }
        });
        settingsTextDeco.addFieldDecoration(errorDecoration, SWT.TOP | SWT.LEFT, false);
        // settingsTextDeco.setMarginWidth(2);
        settingsTextDeco.hideDecoration(errorDecoration);
        // this doesn't work well: we want the decoration image to be clickable, but it actually
        // hides the clickable area
        // settingsTextDeco.getLayoutControl().addMouseListener(new MouseAdapter() {
        // public void mouseDoubleClick(MouseEvent e) {
        // super.mouseDoubleClick(e);
        // }
        // public void mouseDown(MouseEvent e) {
        // if (settingsError != null) {
        // settingsError.show(IStatus.ERROR, "IvyDE configuration problem", null);
        // }
        // }
        // });

        settingsText = (Text) settingsTextDeco.getControl();
        settingsText.setToolTipText(TOOLTIP_SETTINGS_PATH);
        settingsTextDeco.getLayoutControl().setLayoutData(
            new GridData(GridData.FILL, GridData.FILL, true, false));
        settingsText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                settingsPathUpdated();
            }
        });

        Button browse = new Button(this, SWT.NONE);
        browse.setText("Browse");
        browse.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                File f = getFile(new File("/"));
                if (f != null) {
                    try {
                        settingsText.setText(f.toURI().toURL().toExternalForm());
                        settingsPathUpdated();
                    } catch (MalformedURLException ex) {
                        // this cannot happen
                        IvyPlugin.log(IStatus.ERROR,
                            "The file got from the file browser has not a valid URL", ex);
                    }
                }
            }
        });

        loadOnDemandButton = new Button(this, SWT.CHECK);
        loadOnDemandButton.setText("reload the settings only on demand");
        loadOnDemandButton.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 3, 1));

        label = new Label(this, SWT.NONE);
        label.setText("Property files:");

        propFilesText = new Text(this, SWT.BORDER);
        propFilesText.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
        propFilesText.setToolTipText(TOOLTIP_PROPERTY_FILES);
        propFilesText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                settingsPathUpdated();
            }
        });
    }

    public String getSettingsPath() {
        return settingsText.getText();
    }

    public boolean getLoadOnDemand() {
        return loadOnDemandButton.getSelection();
    }

    public List getPropertyFiles() {
        return IvyClasspathUtil.split(propFilesText.getText());
    }

    public interface SettingsEditorListener {
        void settingsEditorUpdated(String path);
    }

    public void addListener(SettingsEditorListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    public void remodeListener(SettingsEditorListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    void settingsPathUpdated() {
        synchronized (listeners) {
            Iterator it = listeners.iterator();
            while (it.hasNext()) {
                ((SettingsEditorListener) it.next()).settingsEditorUpdated(settingsText.getText());
            }
        }
    }

    public void setSettingsError(IvyDEException error) {
        if (error == null) {
            settingsError = null;
            settingsTextDeco.hideDecoration(errorDecoration);
            settingsTextDeco.hideHover();
        } else if (!error.equals(settingsError)) {
            settingsError = error;
            settingsTextDeco.showDecoration(errorDecoration);
            if (settingsText.isVisible()) {
                errorDecoration.setDescription(error.getShortMsg());
                settingsTextDeco.showHoverText(error.getShortMsg());
            }
        }
    }

    public void updateErrorMarker() {
        if (isVisible() && settingsError != null) {
            errorDecoration.setDescription(settingsError.getShortMsg());
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

    public void init(String ivySettingsPath, String propertyFiles, boolean loadOnDemand) {
        settingsText.setText(ivySettingsPath);
        propFilesText.setText(propertyFiles);
        loadOnDemandButton.setSelection(loadOnDemand);
    }

    public void init(String ivySettingsPath, List propertyFiles, boolean loadOnDemand) {
        init(ivySettingsPath, IvyClasspathUtil.concat(propertyFiles), loadOnDemand);
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        settingsText.setEnabled(enabled);
        propFilesText.setEnabled(enabled);
        loadOnDemandButton.setEnabled(enabled);
    }

}
