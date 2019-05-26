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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.ivy.core.module.descriptor.Configuration;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableColumn;

public class ConfTableViewer extends Composite {

    private CheckboxTableViewer confTableViewer;

    private final List<ConfTableListener> listeners = new ArrayList<>();

    private final Button selectAll;

    private final Button up;

    private final Button down;

    private Configuration[] orderedConfigurations = new Configuration[0];

    private final Button all;

    private final Button none;

    public ConfTableViewer(Composite parent, int style) {
        super(parent, style);
        GridLayout layout = new GridLayout(2, false);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        setLayout(layout);

        selectAll = new Button(this, SWT.CHECK);
        selectAll.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 2, 1));
        selectAll.setText("Select every configuration");
        selectAll.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                boolean enabled = !selectAll.getSelection();
                confTableViewer.getTable().setEnabled(enabled);
                updateAllNoneEnableButtons(enabled);
                updateUpDownEnableButtons(enabled);
                confTableUpdated();
            }
        });

        confTableViewer = CheckboxTableViewer.newCheckList(this, SWT.BORDER | SWT.H_SCROLL
                | SWT.V_SCROLL);
        confTableViewer.getTable().setHeaderVisible(true);
        confTableViewer.getTable().setLayoutData(
            new GridData(GridData.FILL, GridData.FILL, true, true));
        TableColumn col1 = new TableColumn(confTableViewer.getTable(), SWT.NONE);
        col1.setText("Name");
        // CheckStyle:MagicNumber| OFF
        col1.setWidth(100);
        TableColumn col2 = new TableColumn(confTableViewer.getTable(), SWT.NONE);
        col2.setText("Description");
        col2.setWidth(300);
        // CheckStyle:MagicNumber| ON
        confTableViewer.setColumnProperties(new String[] {"Name", "Description"});
        confTableViewer.getTable().setLayoutData(
            new GridData(GridData.FILL, GridData.FILL, true, true));
        confTableViewer.setContentProvider(ArrayContentProvider.getInstance());
        confTableViewer.setLabelProvider(new ConfigurationLabelProvider());
        confTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                updateUpDownEnableButtons(true);
                updateAllNoneEnableButtons(true);
                confTableUpdated();
            }
        });

        Composite upDownButtons = new Composite(this, SWT.NONE);
        upDownButtons.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, true));
        upDownButtons.setLayout(new GridLayout());

        all = new Button(upDownButtons, SWT.PUSH);
        all.setText("All");
        all.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false));
        all.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                confTableViewer.setCheckedElements(orderedConfigurations);
                updateAllNoneEnableButtons(true);
            }
        });

        none = new Button(upDownButtons, SWT.PUSH);
        none.setText("None");
        none.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false));
        none.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                confTableViewer.setCheckedElements(new Configuration[0]);
                updateAllNoneEnableButtons(true);
            }
        });

        up = new Button(upDownButtons, SWT.PUSH);
        up.setText("Up");
        up.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false));
        up.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                int i = getSelectedConfigurationIndex();
                Configuration c = orderedConfigurations[i];
                orderedConfigurations[i] = orderedConfigurations[i - 1];
                orderedConfigurations[i - 1] = c;
                confTableViewer.refresh();
                updateUpDownEnableButtons(true);
            }
        });

        down = new Button(upDownButtons, SWT.PUSH);
        down.setText("Down");
        down.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false));
        down.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                int i = getSelectedConfigurationIndex();
                Configuration c = orderedConfigurations[i];
                orderedConfigurations[i] = orderedConfigurations[i + 1];
                orderedConfigurations[i + 1] = c;
                confTableViewer.refresh();
                updateUpDownEnableButtons(true);
            }
        });
    }

    private int getSelectedConfigurationIndex() {
        IStructuredSelection selection = (IStructuredSelection) confTableViewer.getSelection();
        Configuration c = (Configuration) selection.getFirstElement();
        for (int i = 0; i < orderedConfigurations.length; i++) {
            if (orderedConfigurations[i] == c) {
                return i;
            }
        }
        return -1;
    }

    private void updateUpDownEnableButtons(boolean enabled) {
        boolean selected = confTableViewer.getTable().getSelectionCount() != 0;
        int i = getSelectedConfigurationIndex();
        up.setEnabled(enabled && selected && i > 0);
        down.setEnabled(enabled &&  selected && i < orderedConfigurations.length - 1);
    }

    private void updateAllNoneEnableButtons(boolean enabled) {
        int nbChecked = confTableViewer.getCheckedElements().length;
        all.setEnabled(!selectAll.getSelection() && nbChecked != orderedConfigurations.length);
        none.setEnabled(!selectAll.getSelection() && nbChecked > 0);
    }

    public void setModuleDescriptor(ModuleDescriptor md) {
        if (md == null) {
            orderedConfigurations = new Configuration[0];
        } else {
            orderedConfigurations = md.getConfigurations();
        }
        confTableViewer.setInput(orderedConfigurations);
    }

    public void init(List<String> confs) {
        boolean enabled;
        if (confs.size() == 1 && "*".equals(confs.get(0))) {
            enabled = true;
            selectAll.setSelection(true);
            for (Configuration orderedConfiguration : orderedConfigurations) {
                confTableViewer.setChecked(orderedConfiguration, true);
            }
        } else {
            enabled = false;
            selectAll.setSelection(false);
            for (String conf : confs) {
                for (Configuration orderedConfiguration : orderedConfigurations) {
                    if (orderedConfiguration.getName().equals(conf)) {
                        confTableViewer.setChecked(orderedConfiguration, true);
                        break;
                    }
                }
            }
        }
        confTableViewer.getTable().setEnabled(!selectAll.getSelection());
        updateAllNoneEnableButtons(enabled);
        updateUpDownEnableButtons(enabled);
    }

    public List<String> getSelectedConfigurations() {
        if (selectAll.getSelection()) {
            return Collections.singletonList("*");
        }
        List<String> confList = new ArrayList<>();
        for (Object conf : confTableViewer.getCheckedElements()) {
            confList.add(((Configuration) conf).getName());
        }
        return confList;
    }

    static class ConfigurationLabelProvider extends LabelProvider implements ITableLabelProvider {

        public Image getColumnImage(Object element, int columnIndex) {
            return null;
        }

        public String getColumnText(Object element, int columnIndex) {
            if (columnIndex == 0) {
                return ((Configuration) element).getName();
            }
            return ((Configuration) element).getDescription();
        }
    }

    public interface ConfTableListener {
        void confTableUpdated(List<String> confs);
    }

    public void addListener(ConfTableListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    public void removeListener(ConfTableListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    private void confTableUpdated() {
        synchronized (listeners) {
            for (ConfTableListener listener : listeners) {
                listener.confTableUpdated(getSelectedConfigurations());
            }
        }
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        selectAll.setEnabled(enabled);
        updateUpDownEnableButtons(enabled);
        updateAllNoneEnableButtons(enabled);
    }
}
