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
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.ivy.core.module.descriptor.Configuration;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.TableColumn;

public class ConfTableViewer extends Composite {

    private CheckboxTableViewer confTableViewer;

    private ModuleDescriptor md;

    private Link select;

    private final List listeners = new ArrayList();

    public ConfTableViewer(Composite parent, int style) {
        super(parent, style);
        GridLayout layout = new GridLayout();
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        setLayout(layout);

        confTableViewer = CheckboxTableViewer.newCheckList(this, SWT.BORDER | SWT.H_SCROLL
                | SWT.V_SCROLL);
        confTableViewer.getTable().setHeaderVisible(true);
        confTableViewer.getTable().setLayoutData(
            new GridData(GridData.FILL, GridData.FILL, true, true));
        TableColumn col1 = new TableColumn(confTableViewer.getTable(), SWT.NONE);
        col1.setText("Name");
        col1.setWidth(100);
        TableColumn col2 = new TableColumn(confTableViewer.getTable(), SWT.NONE);
        col2.setText("Description");
        col2.setWidth(300);
        confTableViewer.setColumnProperties(new String[] {"Name", "Description"});
        confTableViewer.getTable().setLayoutData(
            new GridData(GridData.FILL, GridData.FILL, true, true));
        confTableViewer.setContentProvider(new IStructuredContentProvider() {
            public Object[] getElements(Object inputElement) {
                if (md != null) {
                    return md.getConfigurations();
                }
                return new Configuration[0];
            }

            public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
                // nothing to do
            }

            public void dispose() {
                // nothing to do
            }
        });
        confTableViewer.setLabelProvider(new ConfigurationLabelProvider());
        confTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                confTableUpdated();
            }
        });

        select = new Link(this, SWT.PUSH);
        select.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
        select.setText("<A>All</A>/<A>None</A>");
        select.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                if (e.text.equals("All") && md != null) {
                    confTableViewer.setCheckedElements(md.getConfigurations());
                } else {
                    confTableViewer.setCheckedElements(new Configuration[0]);
                }
            }
        });
    }

    public void setModuleDescriptor(ModuleDescriptor md) {
        this.md = md;
        confTableViewer.setInput(md);
    }

    public void init(List/* <String> */confs) {
        if (md != null) {
            Configuration[] configurations = md.getConfigurations();
            if (confs.size() != 0 && "*".equals(confs.get(0))) {
                confTableViewer.setCheckedElements(configurations);
            } else {
                for (int i = 0; i < confs.size(); i++) {
                    Configuration configuration = md.getConfiguration((String) confs.get(i));
                    if (configuration != null) {
                        confTableViewer.setChecked(configuration, true);
                    }
                }
            }
        }
    }

    public List getSelectedConfigurations() {
        Object[] confs = confTableViewer.getCheckedElements();
        int total = confTableViewer.getTable().getItemCount();
        if (confs.length == total) {
            return Arrays.asList(new String[] {"*"});
        }
        List confList = new ArrayList();
        for (int i = 0; i < confs.length; i++) {
            Configuration c = (Configuration) confs[i];
            confList.add(c.getName());
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
        void confTableUpdated(List confs);
    }

    public void addListener(ConfTableListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    public void remodeListener(ConfTableListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    void confTableUpdated() {
        synchronized (listeners) {
            Iterator it = listeners.iterator();
            while (it.hasNext()) {
                ((ConfTableListener) it.next()).confTableUpdated(getSelectedConfigurations());
            }
        }
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        confTableViewer.getTable().setEnabled(enabled);
        select.setEnabled(enabled);
    }
}
