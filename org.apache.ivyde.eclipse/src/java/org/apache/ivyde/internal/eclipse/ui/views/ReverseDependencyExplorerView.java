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
package org.apache.ivyde.internal.eclipse.ui.views;

import org.apache.ivyde.eclipse.cp.IvyClasspathContainer;
import org.apache.ivyde.internal.eclipse.IvyPlugin;
import org.apache.ivyde.internal.eclipse.handlers.OpenIvyFileHandler;
import org.apache.ivyde.internal.eclipse.revdepexplorer.IvyUtil;
import org.apache.ivyde.internal.eclipse.revdepexplorer.MultiRevDependencyDescriptor;
import org.apache.ivyde.internal.eclipse.revdepexplorer.SyncIvyFilesJob;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

/**
 * This is a view to manage synchronizing Ivy files in a workspace.
 */
public class ReverseDependencyExplorerView extends ViewPart {

    private static final int COLUMN_MIN_WITH = 75;

    private static final int COLUMN_DEFAULT_WEIGHT = 50;

    private static final int COLUMN_LIGHT_WEIGHT = 25;

    private static final RGB LIGHT_GREEEN = new RGB(50, 150, 50);

    private static TreeViewer viewer;

    private static MultiRevDependencyDescriptor[] dependencies;

    private static Display display;

    private static IProject[] selectedProjects;

    private static final String NEW_REVISION = "New Revision";

    private static final String[] PROPS = new String[] {"Organization", "Module", "Revision",
            NEW_REVISION};

    /**
     * This is a callback that will allow us to create the viewer and initialize it.
     *
     * @param parent Composite
     */
    public void createPartControl(Composite parent) {
        display = parent.getDisplay();
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(1, false));

        ISharedImages sharedImages = PlatformUI.getWorkbench().getSharedImages();

        Action syncAction = new Action() {
            public void run() {
                if (MessageDialog.openConfirm(Display.getCurrent().getActiveShell(),
                    "Fix dependencies",
                    "Alter dependencies?\n\nAnything marked in green will be synchronized.")) {
                    Job job = new SyncIvyFilesJob(dependencies);
                    job.addJobChangeListener(new JobChangeAdapter() {
                        public void done(IJobChangeEvent arg0) {
                            refresh(true);
                        }
                    });
                    job.schedule();
                }
            }
        };
        syncAction.setToolTipText("Synchronize Ivy dependencies");
        syncAction.setImageDescriptor(IvyPlugin.getImageDescriptor("icons/synced.gif"));

        Action refreshAction = new Action() {
            public void run() {
                refresh(true);
            }
        };
        refreshAction.setToolTipText("Refresh");
        refreshAction.setImageDescriptor(IvyPlugin.getImageDescriptor("icons/refresh.gif"));

        Action refreshAllAction = new Action() {
            public void run() {
                ReverseDependencyExplorerView.setSelectedProjects(null);
                refresh(true);
            }
        };
        refreshAllAction.setToolTipText("Show all projects in workspace");
        refreshAllAction.setImageDescriptor(sharedImages
                .getImageDescriptor(ISharedImages.IMG_TOOL_UP));

        IToolBarManager toolbar = getViewSite().getActionBars().getToolBarManager();
        toolbar.add(syncAction);
        toolbar.add(refreshAction);
        toolbar.add(refreshAllAction);

        newTreeViewer(composite);
        refresh(true);
    }

    private void newTreeViewer(Composite composite) {
        viewer = new TreeViewer(composite, SWT.FULL_SELECTION);
        IvyRevisionProvider ivyRevisionProvider = new IvyRevisionProvider();

        viewer.setContentProvider(ivyRevisionProvider);
        viewer.setLabelProvider(ivyRevisionProvider);
        viewer.setColumnProperties(PROPS);
        viewer.addDoubleClickListener(new IDoubleClickListener() {
            public void doubleClick(DoubleClickEvent event) {
                IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
                Object element = selection.getFirstElement();
                if (element instanceof CPDependencyDescriptor) {
                    IvyClasspathContainer container = ((CPDependencyDescriptor) element).container;
                    OpenIvyFileHandler.open(container);
                }
            }
        });

        Tree tree = viewer.getTree();
        tree.setLayoutData(new GridData(GridData.FILL_BOTH));

        TableLayout layout = new TableLayout();
        layout.addColumnData(new ColumnWeightData(COLUMN_DEFAULT_WEIGHT, COLUMN_MIN_WITH, true));
        layout.addColumnData(new ColumnWeightData(COLUMN_DEFAULT_WEIGHT, COLUMN_MIN_WITH, true));
        layout.addColumnData(new ColumnWeightData(COLUMN_LIGHT_WEIGHT, COLUMN_MIN_WITH, true));
        layout.addColumnData(new ColumnWeightData(COLUMN_DEFAULT_WEIGHT, COLUMN_MIN_WITH, true));
        tree.setLayout(layout);

        new TreeColumn(tree, SWT.LEFT).setText("Organization");
        new TreeColumn(tree, SWT.LEFT).setText("Module");
        new TreeColumn(tree, SWT.LEFT).setText("Revision");
        new TreeColumn(tree, SWT.LEFT).setText("New Revision");

        for (int i = 0, n = tree.getColumnCount(); i < n; i++) {
            tree.getColumn(i).pack();
        }

        tree.setHeaderVisible(true);
        tree.setLinesVisible(false);

        CellEditor[] editors = new CellEditor[PROPS.length];
        editors[0] = new TextCellEditor(tree);
        editors[1] = new TextCellEditor(tree);
        editors[2] = new TextCellEditor(tree);
        // CheckStyle:MagicNumber| OFF
        editors[3] = new TextCellEditor(tree);
        // CheckStyle:MagicNumber| ON

        viewer.setCellModifier(new CellModifier());
        viewer.setCellEditors(editors);
    }

    public static void refresh(final boolean reloadData) {
        display.syncExec(new Runnable() {
            public void run() {
                if (reloadData) {
                    if (selectedProjects == null) {
                        dependencies = IvyUtil.getAllDependencyDescriptorsInWorkspace();
                    } else {
                        dependencies = IvyUtil.getDependencyDescriptorsByProjects(selectedProjects);
                    }
                    viewer.setInput(dependencies);
                }

                viewer.refresh();

                for (TreeItem item : viewer.getTree().getItems()) {
                    MultiRevDependencyDescriptor multiRD = (MultiRevDependencyDescriptor) item
                            .getData();

                    if (multiRD.hasMultipleRevisions() && !multiRD.hasNewRevision()) {
                        item.setForeground(display.getSystemColor(SWT.COLOR_RED));
                    } else if (multiRD.hasNewRevision()) {
                        item.setForeground(new Color(Display.getDefault(), LIGHT_GREEEN));
                    } else {
                        item.setForeground(display.getSystemColor(SWT.COLOR_BLACK));
                    }
                }
            }
        });
    }

    /**
     * Passing the focus request to the viewer's control.
     */
    public void setFocus() {
        viewer.getControl().setFocus();
    }

    class IvyRevisionProvider extends LabelProvider implements ITableLabelProvider,
            ITreeContentProvider {

        public void inputChanged(Viewer v, Object oldInput, Object newInput) {
            // nothing to do
        }

        public Object[] getElements(Object parent) {
            return dependencies;
        }

        public String getColumnText(Object obj, int index) {
            if (obj instanceof MultiRevDependencyDescriptor) {
                MultiRevDependencyDescriptor mrdd = (MultiRevDependencyDescriptor) obj;

                switch (index) {
                    case 0:
                        return mrdd.getOrganization();
                    case 1:
                        return mrdd.getModule();
                    case 2:
                        return toRevisionList(mrdd.getRevisions());
                        // CheckStyle:MagicNumber| OFF
                    case 3:
                        // CheckStyle:MagicNumber| ON
                        return mrdd.getNewRevision();
                    default:
                        break;
                }
            } else if (obj instanceof CPDependencyDescriptor) {
                CPDependencyDescriptor containerDescriptorComposite = (CPDependencyDescriptor) obj;
                switch (index) {
                    case 0:
                        IJavaProject javaProject = containerDescriptorComposite
                                .getIvyClasspathContainer().getConf().getJavaProject();
                        return containerDescriptorComposite.getIvyClasspathContainer()
                                .getDescription()
                                + (javaProject == null ? "" : (" in \""
                                        + javaProject.getElementName() + "\""));
                    case 2:
                        return toRevisionList(containerDescriptorComposite.getRevisions());
                    default:
                        break;
                }

                return null;
            }

            return null;
        }

        private String toRevisionList(String[] revisions) {
            StringBuilder buffer = new StringBuilder();

            for (String revision : revisions) {
                if (buffer.length() > 0) {
                    buffer.append(", ");
                }
                buffer.append(revision);
            }

            return buffer.toString();
        }

        public Image getColumnImage(Object obj, int index) {
            if (index == 0) {
                return getImage(obj);
            }

            return null;
        }

        public Image getImage(Object obj) {
            ISharedImages sharedImages = PlatformUI.getWorkbench().getSharedImages();
            if (obj instanceof MultiRevDependencyDescriptor) {
                MultiRevDependencyDescriptor mrdd = (MultiRevDependencyDescriptor) obj;

                if (mrdd.hasMultipleRevisions() && !mrdd.hasNewRevision()) {
                    return sharedImages.getImage(ISharedImages.IMG_OBJS_WARN_TSK);
                }

                return IvyPlugin.getImageDescriptor("icons/synced.gif").createImage();
            } else if (obj instanceof CPDependencyDescriptor) {
                return JavaUI.getSharedImages().getImage(
                    org.eclipse.jdt.ui.ISharedImages.IMG_OBJS_LIBRARY);
            }

            return null;
        }

        public Object[] getChildren(Object parent) {
            if (parent instanceof MultiRevDependencyDescriptor) {
                MultiRevDependencyDescriptor mrdd = (MultiRevDependencyDescriptor) parent;
                IvyClasspathContainer[] containers = mrdd.getIvyClasspathContainers();

                Object[] wrappedProjects = new Object[containers.length];
                for (int i = 0; i < containers.length; i++) {
                    wrappedProjects[i] = new CPDependencyDescriptor(containers[i], mrdd);
                }

                return wrappedProjects;
            }

            return new Object[0];
        }

        public Object getParent(Object parent) {
            return null;
        }

        public boolean hasChildren(Object parent) {
            if (parent instanceof MultiRevDependencyDescriptor) {
                MultiRevDependencyDescriptor mrdd = (MultiRevDependencyDescriptor) parent;
                return mrdd.getIvyClasspathContainers().length > 0;
            }

            return false;
        }
    }

    class CPDependencyDescriptor {
        private final IvyClasspathContainer container;

        private final MultiRevDependencyDescriptor multiRevisionDescriptor;

        public CPDependencyDescriptor(IvyClasspathContainer container,
                MultiRevDependencyDescriptor multiRevisionDescriptor) {
            this.container = container;
            this.multiRevisionDescriptor = multiRevisionDescriptor;
        }

        /**
         * @return revisions for a container
         */
        public String[] getRevisions() {
            return multiRevisionDescriptor.getRevisions(container);
        }

        public IvyClasspathContainer getIvyClasspathContainer() {
            return container;
        }

        public MultiRevDependencyDescriptor getMultiRevisionDescriptor() {
            return multiRevisionDescriptor;
        }
    }

    class CellModifier implements ICellModifier {

        public boolean canModify(Object element, String property) {
            return property.equals(NEW_REVISION);
        }

        public Object getValue(Object element, String property) {
            if (!property.equals(NEW_REVISION)
                    || !(element instanceof MultiRevDependencyDescriptor)) {
                return null;
            }
            MultiRevDependencyDescriptor mrdd = (MultiRevDependencyDescriptor) element;
            String revision = mrdd.getNewRevision();
            if (revision == null) {
                return "";
            }
            return revision;
        }

        public void modify(Object element, String property, Object value) {
            if (element instanceof Item) {
                element = ((Item) element).getData();
            }

            if (element instanceof MultiRevDependencyDescriptor && property.equals(NEW_REVISION)) {
                ((MultiRevDependencyDescriptor) element).setNewRevision((String) value);

                refresh(false);
            }
        }
    }

    public static void setSelectedProjects(IProject[] projects) {
        selectedProjects = projects;
    }
}
