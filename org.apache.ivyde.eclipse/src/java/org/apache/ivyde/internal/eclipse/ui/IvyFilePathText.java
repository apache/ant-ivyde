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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

public class IvyFilePathText extends PathEditor {

    private Text ivyFilePathText;

    private DecoratedField ivyFilePathTextDeco;

    private IvyDEException ivyXmlError;

    private final List<IvyXmlPathListener> listeners = new ArrayList<>();

    private FieldDecoration errorDecoration;

    private Button defaultButton;

    public IvyFilePathText(Composite parent, int style, IProject project) {
        super(parent, SWT.NONE, "Ivy File:", project, "*.xml");
    }

    protected Text createText(Composite parent) {
        errorDecoration = FieldDecorationRegistry.getDefault().getFieldDecoration(
            FieldDecorationRegistry.DEC_ERROR);

        ivyFilePathTextDeco = new DecoratedField(this, SWT.LEFT | SWT.TOP, new IControlCreator() {
            public Control createControl(Composite parent, int style) {
                return new Text(parent, SWT.SINGLE | SWT.BORDER);
            }
        });
        ivyFilePathTextDeco.addFieldDecoration(errorDecoration, SWT.TOP | SWT.LEFT, false);
        ivyFilePathTextDeco.hideDecoration(errorDecoration);

        ivyFilePathText = (Text) ivyFilePathTextDeco.getControl();
        ivyFilePathTextDeco.getLayoutControl().setLayoutData(
            new GridData(GridData.FILL, GridData.CENTER, true, false));

        return ivyFilePathText;
    }

    protected boolean addButtons(Composite buttons) {
        defaultButton = new Button(buttons, SWT.NONE);
        defaultButton.setLayoutData(new GridData(GridData.END, GridData.CENTER, true, false));
        defaultButton.setText("Default");
        defaultButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                getText().setText("ivy.xml");
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
            IvyPlugin.logError("The file got from the file browser has an invalid URL", ex);
        }
    }

    protected void textUpdated() {
        ivyXmlPathUpdated();
    }

    public interface IvyXmlPathListener {
        void ivyXmlPathUpdated(String path);
    }

    public void addListener(IvyXmlPathListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    public void removeListener(IvyXmlPathListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    public String getIvyFilePath() {
        return ivyFilePathText.getText();
    }

    void ivyXmlPathUpdated() {
        synchronized (listeners) {
            for (IvyXmlPathListener listener : listeners) {
                listener.ivyXmlPathUpdated(ivyFilePathText.getText());
            }
        }
    }

    public void setIvyXmlError(IvyDEException error) {
        if (error == null) {
            ivyXmlError = null;
            ivyFilePathTextDeco.hideDecoration(errorDecoration);
            ivyFilePathTextDeco.hideHover();
        } else if (!error.equals(ivyXmlError)) {
            ivyXmlError = error;
            ivyFilePathTextDeco.showDecoration(errorDecoration);
            if (ivyFilePathText.isVisible()) {
                errorDecoration.setDescription(error.getShortMsg());
                ivyFilePathTextDeco.showHoverText(error.getShortMsg());
            }
        }
    }

    public void updateErrorMarker() {
        if (isVisible() && ivyXmlError != null) {
            errorDecoration.setDescription(ivyXmlError.getShortMsg());
            ivyFilePathTextDeco.showHoverText(ivyXmlError.getShortMsg());
        } else {
            ivyFilePathTextDeco.hideHover();
        }
    }

    public void init(String ivyXmlPath) {
        ivyFilePathText.setText(ivyXmlPath);
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        ivyFilePathText.setEnabled(enabled);
    }
}
