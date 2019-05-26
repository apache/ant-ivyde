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

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.ivyde.eclipse.IvyDEException;
import org.apache.ivyde.eclipse.cp.SettingsSetup;
import org.apache.ivyde.internal.eclipse.IvyPlugin;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.fieldassist.DecoratedField;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.fieldassist.IControlCreator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Text;

public class SettingsSetupEditor extends Composite {

    public static final String TOOLTIP_SETTINGS_PATH = "The URL where your Ivy settings file can be"
            + " found. \nLeave it empty to reference the default Ivy settings. \n"
            + "Relative paths are handled relative to the project.";

    public static final String TOOLTIP_PROPERTY_FILES = "Comma separated list of build property"
            + " files.\nExample: build.properties, override.properties";

    private final List<SettingsEditorListener> listeners = new ArrayList<>();

    private IvyDEException settingsError;

    private FieldDecoration errorDecoration;

    private final FileListEditor propFilesEditor;

    private DecoratedField settingsTextDeco;

    private final Button loadOnDemandButton;

    private final PathEditor settingsEditor;

    private Button defaultButton;

    private final PathEditor ivyUserDirEditor;

    public SettingsSetupEditor(Composite parent, int style, IProject project) {
        super(parent, style);

        GridLayout layout = new GridLayout();
        setLayout(layout);

        loadOnDemandButton = new Button(this, SWT.CHECK);
        loadOnDemandButton.setText("reload the settings only on demand");
        loadOnDemandButton.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));

        settingsEditor = new PathEditor(this, SWT.NONE, "Ivy settings path:", project, "*.xml") {

            protected Text createText(Composite parent) {
                errorDecoration = FieldDecorationRegistry.getDefault().getFieldDecoration(
                    FieldDecorationRegistry.DEC_ERROR);

                settingsTextDeco = new DecoratedField(parent, SWT.LEFT | SWT.TOP,
                        new IControlCreator() {
                            public Control createControl(Composite p, int s) {
                                return new Text(p, SWT.SINGLE | SWT.BORDER);
                            }
                        });
                settingsTextDeco.addFieldDecoration(errorDecoration, SWT.TOP | SWT.LEFT, false);
                // settingsTextDeco.setMarginWidth(2);
                settingsTextDeco.hideDecoration(errorDecoration);
                // this doesn't work well: we want the decoration image to be clickable, but it
                // actually
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

                Text settingsText = (Text) settingsTextDeco.getControl();
                settingsText.setToolTipText(TOOLTIP_SETTINGS_PATH);
                settingsTextDeco.getLayoutControl().setLayoutData(
                    new GridData(GridData.FILL, GridData.CENTER, true, false));

                return settingsText;
            }

            protected boolean addButtons(Composite buttons) {
                defaultButton = new Button(buttons, SWT.NONE);
                defaultButton
                        .setLayoutData(new GridData(GridData.END, GridData.CENTER, true, false));
                defaultButton.setText("Default");
                defaultButton.addSelectionListener(new SelectionAdapter() {
                    public void widgetSelected(SelectionEvent e) {
                        getText().setText("");
                    }
                });
                return true;
            }

            protected void setFile(String f) {
                try {
                    getText().setText(new File(f).toURI().toURL().toExternalForm());
                    textUpdated();
                } catch (MalformedURLException ex) {
                    // this cannot happen
                    IvyPlugin
                            .logError("The file got from the file browser has an invalid URL", ex);
                }
            }

            protected void textUpdated() {
                settingsUpdated();
            }
        };
        settingsEditor.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));

        ivyUserDirEditor = new PathEditor(this, SWT.NONE, "Ivy user dir:", project, null) {
            protected void textUpdated() {
                settingsUpdated();
            }
        };
        ivyUserDirEditor.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));

        propFilesEditor = new FileListEditor(this, SWT.NONE, "Property files:", "Property file:",
                project, "*.properties") {
            protected void fileListUpdated() {
                settingsUpdated();
            }
        };
        propFilesEditor.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
    }

    public SettingsSetup getIvySettingsSetup() {
        SettingsSetup setup = new SettingsSetup();
        setup.setIvySettingsPath(settingsEditor.getText().getText());
        setup.setLoadSettingsOnDemand(loadOnDemandButton.getSelection());
        setup.setIvyUserDir(ivyUserDirEditor.getText().getText());
        setup.setPropertyFiles(propFilesEditor.getFiles());
        return setup;
    }

    public interface SettingsEditorListener {
        void settingsEditorUpdated(SettingsSetup setup);
    }

    public void addListener(SettingsEditorListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    public void removeListener(SettingsEditorListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    void settingsUpdated() {
        synchronized (listeners) {
            SettingsSetup setup = getIvySettingsSetup();
            for (SettingsEditorListener listener : listeners) {
                listener.settingsEditorUpdated(setup);
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
            if (settingsEditor.getText().isVisible()) {
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

    public void init(SettingsSetup setup) {
        settingsEditor.getText().setText(setup.getRawIvySettingsPath());
        propFilesEditor.init(setup.getRawPropertyFiles());
        ivyUserDirEditor.getText().setText(setup.getRawIvyUserDir());
        loadOnDemandButton.setSelection(setup.isLoadSettingsOnDemand());
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        settingsEditor.setEnabled(enabled);
        defaultButton.setEnabled(enabled);
        ivyUserDirEditor.setEnabled(enabled);
        propFilesEditor.setEnabled(enabled);
        loadOnDemandButton.setEnabled(enabled);
    }

}
