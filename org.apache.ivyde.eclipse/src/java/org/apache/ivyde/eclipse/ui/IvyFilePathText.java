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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.ivyde.eclipse.IvyDEException;
import org.apache.ivyde.eclipse.IvyPlugin;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.window.Window;
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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.views.navigator.ResourceSorter;

public class IvyFilePathText extends Composite {

    private Text ivyFilePathText;

    private ControlDecoration ivyFilePathTextDeco;

    private IvyDEException ivyXmlError;

    private final IJavaProject project;

    private final List listeners = new ArrayList();

    private Button browseButton;

    public IvyFilePathText(Composite parent, int style, IJavaProject project) {
        super(parent, style);
        GridLayout layout = new GridLayout(2, false);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        setLayout(layout);
        this.project = project;

        Image errorDecoImage = FieldDecorationRegistry.getDefault().getFieldDecoration(
            FieldDecorationRegistry.DEC_ERROR).getImage();

        ivyFilePathText = new Text(this, SWT.SINGLE | SWT.BORDER);
        ivyFilePathText.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
        ivyFilePathText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent ev) {
                ivyXmlPathUpdated();
            }
        });
        ivyFilePathTextDeco = new ControlDecoration(ivyFilePathText, SWT.LEFT | SWT.TOP);
        ivyFilePathTextDeco.setMarginWidth(2);
        ivyFilePathTextDeco.setImage(errorDecoImage);
        ivyFilePathTextDeco.hide();
        ivyFilePathTextDeco.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                if (ivyXmlError != null) {
                    ivyXmlError.show(IStatus.ERROR, "IvyDE configuration problem", null);
                }
            }
        });

        browseButton = new Button(this, SWT.NONE);
        browseButton.setText("Browse");
        browseButton.addSelectionListener(new BrowseButtonListener());
    }

    public interface IvyXmlPathListener {
        void ivyXmlPathUpdated(String path);
    }

    public void addListener(IvyXmlPathListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    public void remodeListener(IvyXmlPathListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    public String getIvyFilePath() {
        return ivyFilePathText.getText();
    }

    void ivyXmlPathUpdated() {
        synchronized (listeners) {
            Iterator it = listeners.iterator();
            while (it.hasNext()) {
                ((IvyXmlPathListener) it.next()).ivyXmlPathUpdated(ivyFilePathText.getText());
            }
        }
    }

    public void setIvyXmlError(IvyDEException error) {
        if (error == null) {
            ivyXmlError = null;
            ivyFilePathTextDeco.hide();
            ivyFilePathTextDeco.hideHover();
        } else if (!error.equals(ivyXmlError)) {
            ivyXmlError = error;
            ivyFilePathTextDeco.show();
            if (ivyFilePathText.isVisible()) {
                ivyFilePathTextDeco.showHoverText(error.getShortMsg());
            }
        }
    }

    public void updateErrorMarker() {
        if (isVisible() && ivyXmlError != null) {
            ivyFilePathTextDeco.showHoverText(ivyXmlError.getShortMsg());
        } else {
            ivyFilePathTextDeco.hideHover();
        }
    }

    private class BrowseButtonListener extends SelectionAdapter {
        public void widgetSelected(SelectionEvent e) {
            String path = null;
            if (project != null) {
                ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(Display
                        .getDefault().getActiveShell(), new WorkbenchLabelProvider(),
                        new WorkbenchContentProvider());
                dialog.setValidator(new ISelectionStatusValidator() {
                    private final IStatus errorStatus = new Status(IStatus.ERROR, IvyPlugin.ID, 0,
                            "", null);

                    public IStatus validate(Object[] selection) {
                        if (selection.length == 0) {
                            return errorStatus;
                        }
                        for (int i = 0; i < selection.length; i++) {
                            Object o = selection[i];
                            if (!(o instanceof IFile)) {
                                return errorStatus;
                            }
                        }
                        return Status.OK_STATUS;
                    }

                });
                dialog.setTitle("choose ivy file");
                dialog.setMessage("choose the ivy file to use to resolve dependencies");
                dialog.setInput(project.getProject());
                dialog.setSorter(new ResourceSorter(ResourceSorter.NAME));

                if (dialog.open() == Window.OK) {
                    Object[] elements = dialog.getResult();
                    if (elements.length > 0 && elements[0] instanceof IFile) {
                        IPath p = ((IFile) elements[0]).getProjectRelativePath();
                        path = p.toString();
                    }
                }
            } else {
                FileDialog dialog = new FileDialog(IvyPlugin.getActiveWorkbenchShell(), SWT.OPEN);
                dialog.setText("Choose an ivy.xml");
                path = dialog.open();
            }

            if (path != null) {
                ivyFilePathText.setText(path);
                ivyXmlPathUpdated();
            }
        }
    }

    public void init(String ivyXmlPath) {
        ivyFilePathText.setText(ivyXmlPath);
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        ivyFilePathText.setEnabled(enabled);
        browseButton.setEnabled(enabled);
    }
}
