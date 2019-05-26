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
package org.apache.ivyde.eclipse.resolvevisualizer;

import java.awt.Dimension;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivyde.eclipse.cp.IvyClasspathContainer;
import org.apache.ivyde.eclipse.resolvevisualizer.label.ILabelDecoratorAlgorithm;
import org.apache.ivyde.eclipse.resolvevisualizer.model.IvyNodeElement;
import org.apache.ivyde.eclipse.resolvevisualizer.model.IvyNodeElementAdapter;
import org.apache.ivyde.eclipse.resolvevisualizer.model.IvyNodeElementFilterAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.zest.core.viewers.AbstractZoomableViewer;
import org.eclipse.zest.core.viewers.EntityConnectionData;
import org.eclipse.zest.core.viewers.GraphViewer;
import org.eclipse.zest.core.viewers.IZoomableWorkbenchPart;
import org.eclipse.zest.core.viewers.ZoomContributionViewItem;
import org.eclipse.zest.core.widgets.Graph;
import org.eclipse.zest.core.widgets.GraphItem;
import org.eclipse.zest.core.widgets.GraphNode;
import org.eclipse.zest.core.widgets.ZestStyles;
import org.eclipse.zest.layouts.LayoutAlgorithm;
import org.eclipse.zest.layouts.LayoutStyles;
import org.eclipse.zest.layouts.algorithms.CompositeLayoutAlgorithm;
import org.eclipse.zest.layouts.algorithms.DirectedGraphLayoutAlgorithm;
import org.eclipse.zest.layouts.algorithms.HorizontalShift;

public class ResolveVisualizerView extends ViewPart implements IZoomableWorkbenchPart {
    private GraphViewer viewer;

    private Action focusDialogAction;
    private Action focusDialogActionToolbar;
    private Action focusOnSelectionAction;
    private Action hideSelectionAction;
    private Action showHiddenAction;
    private Action applyDefaultLayoutAction;
    private Action historyAction;
    private Action forwardAction;
    private Action refreshAction;

    private ZoomContributionViewItem contextZoomContributionViewItem;
    private ZoomContributionViewItem toolbarZoomContributionViewItem;

    private final Stack<IvyNodeElement> historyStack;
    private final Stack<IvyNodeElement> forwardStack;

    private IvyNodeElement currentRoot;
    private IvyNodeElement currentSelection;
    private IvyClasspathContainer currentContainer;

    private final ResolveVisualizerContentProvider contentProvider = new ResolveVisualizerContentProvider();
    private final MessageContentProvider messageContentProvider = new MessageContentProvider();
    private IvyNodeLabelProvider labelProvider;
    private ResolveVisualizerForm visualizationForm;

    private final ForceHiddenFilter forceHiddenFilter;

    public ResolveVisualizerView() {
        historyStack = new Stack<>();
        forwardStack = new Stack<>();

        forceHiddenFilter = new ForceHiddenFilter();
        forceHiddenFilter.setEnabled(true);
        contentProvider.addFilter(forceHiddenFilter);
    }

    /**
     * This is a callback that will allow us to create the viewer and initialize it.
     *
     * @param parent Composite
     */
    public void createPartControl(Composite parent) {
        FormToolkit toolKit = new FormToolkit(parent.getDisplay());

        visualizationForm = new ResolveVisualizerForm(parent, toolKit, this);
        viewer = visualizationForm.getGraphViewer();

        this.labelProvider = new IvyNodeLabelProvider(this.viewer);
        viewer.setLabelProvider(labelProvider);
        viewer.setContentProvider(contentProvider);
        viewer.setInput(null);
        viewer.setConnectionStyle(ZestStyles.CONNECTIONS_DIRECTED);
        viewer.setLayoutAlgorithm(new CompositeLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING,
                new LayoutAlgorithm[] { new DirectedGraphLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING),
                                        new HorizontalShift(LayoutStyles.NO_LAYOUT_NODE_RESIZING) }));

        viewer.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                Object selectedElement = ((IStructuredSelection) event.getSelection()).getFirstElement();
                if (selectedElement instanceof EntityConnectionData) {
                    return;
                }
                ResolveVisualizerView.this.selectionChanged((IvyNodeElement) selectedElement);
            }
        });

        viewer.addDoubleClickListener(new IDoubleClickListener() {
            public void doubleClick(DoubleClickEvent event) {
                focusOnSelectionAction.run();
            }
        });

        visualizationForm.getSearchBox().addModifyListener(new ModifyListener() {
            @SuppressWarnings("unchecked")
            public void modifyText(ModifyEvent e) {
                String textString = visualizationForm.getSearchBox().getText();

                Map<String, GraphItem> figureListing = new HashMap<>();
                List<GraphItem> list = (List<GraphItem>) viewer.getGraphControl().getNodes();
                for (GraphItem item : list) {
                    figureListing.put(item.getText(), item);
                }
                list.clear();
                if (textString.length() > 0) {
                    for (Map.Entry<String, GraphItem> figure : figureListing.entrySet()) {
                        if (figure.getKey().toLowerCase().contains(textString.toLowerCase())) {
                            list.add(figure.getValue());
                        }
                    }
                }
                viewer.getGraphControl().setSelection(list.toArray(new GraphItem[list.size()]));
            }
        });

        messageContentProvider.setMessageManager(visualizationForm.getManagedForm().getMessageManager());
        contextZoomContributionViewItem = new ZoomContributionViewItem(this);
        toolbarZoomContributionViewItem = new ZoomContributionViewItem(this);

        // Create the help context id for the viewer's control
        makeActions();
        hookContextMenu();
        contributeToActionBars();
    }

    private void hookContextMenu() {
        MenuManager menuMgr = new MenuManager("#PopupMenu");
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(new IMenuListener() {
            public void menuAboutToShow(IMenuManager manager) {
                ResolveVisualizerView.this.fillContextMenu(manager);
            }
        });
        Menu menu = menuMgr.createContextMenu(viewer.getControl());
        viewer.getControl().setMenu(menu);
        getSite().registerContextMenu(menuMgr, viewer);
    }

    private void contributeToActionBars() {
        IActionBars bars = getViewSite().getActionBars();
        bars.getMenuManager().add(toolbarZoomContributionViewItem);
        fillLocalPullDown(bars.getMenuManager());
        fillLocalToolBar(bars.getToolBarManager());
    }

    private void fillLocalPullDown(IMenuManager manager) {
    }

    private void fillContextMenu(IMenuManager manager) {
        manager.add(new Separator());
        manager.add(focusDialogAction);
        manager.add(focusOnSelectionAction);
        manager.add(new Separator());
        manager.add(historyAction);
        manager.add(forwardAction);
        manager.add(new Separator());
        manager.add(hideSelectionAction);
        manager.add(showHiddenAction);
        manager.add(new Separator());
        manager.add(refreshAction);
        manager.add(applyDefaultLayoutAction);
        manager.add(new Separator());
        manager.add(contextZoomContributionViewItem);
    }

    private void fillLocalToolBar(IToolBarManager toolBarManager) {
        toolBarManager.add(refreshAction);
        toolBarManager.add(focusDialogActionToolbar);
        toolBarManager.add(new Separator());
        toolBarManager.add(historyAction);
        toolBarManager.add(forwardAction);
    }

    private void makeActions() {
        refreshAction = new Action() {
            public void run() {
                final IvyClasspathContainer container = currentContainer;

                if (container == null) {
                    // nothing as been actually selected
                    return;
                }

                ResolveReport report = container.getResolveReport();
                if (report == null) {
                    // TODO we might want to launch some resolve here
                    // or at least open a popup inviting the end user to launch one
                    return;
                }

                // a resolve report is already saved on the container's state, we will use it
                focusOnContainer(container);

                // When a new container is selected, disable the forward action
                // The forward action only stores history when the back button was used (much like a browser)
                forwardStack.clear();
                forwardAction.setEnabled(false);
            }
        };
        refreshAction.setText("Resolve");
        refreshAction.setEnabled(true);
        refreshAction.setImageDescriptor(Plugin.getImageDescriptor("icons/refresh.gif"));

        focusDialogAction = new Action() {
            public void run() {
                ClasspathContainerSelectionDialog dialog = new ClasspathContainerSelectionDialog(viewer.getControl()
                        .getShell());
                dialog.create();
                int dialogStatus = dialog.open();
                if (dialogStatus == Window.OK) {
                    currentContainer = (IvyClasspathContainer) dialog.getFirstResult();
                    refreshAction.run();
                }
            }
        };
        focusDialogAction.setText("Focus on ivy file...");

        focusDialogActionToolbar = new Action() {
            public void run() {
                focusDialogAction.run();
            }
        };
        focusDialogActionToolbar.setToolTipText("Focus on ivy file...");
        focusDialogActionToolbar.setImageDescriptor(ResolveVisualizerPlugin.getImageDescriptor("icons/focus.gif"));

        focusOnSelectionAction = new Action() {
            public void run() {
                if (currentSelection != null) {
                    if (currentRoot != currentSelection) {
                        if (currentRoot != null) {
                            historyStack.push(currentRoot);
                            historyAction.setEnabled(true);
                        }
                        focusOn(currentSelection);
                    }
                }
            }
        };
        focusOnSelectionAction.setText("Focus on selection");
        focusOnSelectionAction.setEnabled(false);

        historyAction = new Action() {
            public void run() {
                if (historyStack.size() > 0) {
                    IvyNodeElement element = historyStack.pop();
                    forwardStack.push(currentRoot);
                    forwardAction.setEnabled(true);
                    focusOn(element);
                    if (historyStack.size() <= 0) {
                        historyAction.setEnabled(false);
                    }
                }
            }
        };
        historyAction.setText("Back");
        historyAction.setToolTipText("Back");
        historyAction.setEnabled(false);
        historyAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages()
                .getImageDescriptor(ISharedImages.IMG_TOOL_BACK));

        forwardAction = new Action() {
            public void run() {
                if (forwardStack.size() > 0) {
                    IvyNodeElement element = forwardStack.pop();

                    historyStack.push(currentRoot);
                    historyAction.setEnabled(true);

                    focusOn(element);
                    if (forwardStack.size() <= 0) {
                        forwardAction.setEnabled(false);
                    }
                }
            }
        };

        forwardAction.setText("Forward");
        forwardAction.setToolTipText("Forward");
        forwardAction.setEnabled(false);
        forwardAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages()
                .getImageDescriptor(ISharedImages.IMG_TOOL_FORWARD));

        hideSelectionAction = new Action() {
            public void run() {
                forceHiddenFilter.addHidden(currentSelection);
                refresh();
            }
        };
        hideSelectionAction.setText("Hide");

        showHiddenAction = new Action() {
            public void run() {
                forceHiddenFilter.clearHidden();
                refresh();
            }
        };
        showHiddenAction.setText("Show hidden");

        applyDefaultLayoutAction = new Action() {
            public void run() {
                viewer.applyLayout();
            }
        };
        applyDefaultLayoutAction.setText("Apply default layout");
    }

    /**
     * Passing the focus request to the viewer's control.
     */
    public void setFocus() {
        viewer.getControl().setFocus();
    }

    private void focusOnContainer(IvyClasspathContainer container) {
        ResolveReport report = container.getResolveReport();

        if (report != null) {
            forceHiddenFilter.clearHidden();
            visualizationForm.getForm().setText(
                    ResolveVisualizerForm.HEADER_TEXT + " - " + container.getConf().getIvyXmlPath() + " in \""
                            + container.getConf().getJavaProject().getProject().getName() + "\"");

            IvyNodeElement nextRoot = IvyNodeElementAdapter.adapt(report);
            if (currentRoot != nextRoot) {
                if (currentRoot != null) {
                    historyStack.push(currentRoot);
                    historyAction.setEnabled(true);
                }
                focusOn(nextRoot);
            }
        }
    }

    /**
     * Update the view to focus on a particular bundle. If record history is set to true, and bundle does not equal the
     * current bundle, then the current bundle will be saved on the history stack
     *
     * @param focus IvyNodeElement
     */
    @SuppressWarnings("unchecked")
    public void focusOn(IvyNodeElement focus) {
        viewer.setSelection(new StructuredSelection(focus));
        viewer.setFilters(new ViewerFilter[] {});
        viewer.setInput(focus);

        Graph graph = viewer.getGraphControl();
        Dimension centre = new Dimension(graph.getBounds().width / 2, graph.getBounds().height / 2);
        List<GraphNode> list = (List<GraphNode>) viewer.getGraphControl().getNodes();
        for (GraphNode graphNode : list) {
            if (graphNode.getLocation().x <= 1 && graphNode.getLocation().y <= 1) {
                graphNode.setLocation(centre.width, centre.height);
            }
        }

        currentRoot = focus;

        if (viewer.getGraphControl().getNodes().size() > 0) {
            visualizationForm.enableSearchBox(true);
        } else {
            visualizationForm.enableSearchBox(false);
        }
        visualizationForm.enableSearchBox(true);
        focusOnSelectionAction.setEnabled(true);

        selectionChanged(focus);
    }

    /**
     * Handle the select changed. This will update the view whenever a selection occurs.
     *
     * @param selectedItem IvyNodeElement
     */
    private void selectionChanged(IvyNodeElement selectedItem) {
        currentSelection = selectedItem;
        labelProvider.setCurrentSelection(currentRoot, selectedItem);
        messageContentProvider.selectionChanged(currentRoot);
        viewer.update(contentProvider.getElements(currentRoot), null);
    }

    public AbstractZoomableViewer getZoomableViewer() {
        return viewer;
    }

    public void setAutoSelectDecorator(ILabelDecoratorAlgorithm algorithm) {
        labelProvider.setAutoSelectDecorator(algorithm);

        if (viewer.getSelection() != null) {
            Object selected = ((IStructuredSelection) viewer.getSelection()).getFirstElement();
            this.selectionChanged((IvyNodeElement) selected);
        }
    }

    public ResolveVisualizerContentProvider getContentProvider() {
        return contentProvider;
    }

    public void refresh() {
        viewer.refresh();
        viewer.applyLayout();
    }

    public IvyNodeElement getCurrentRoot() {
        return currentRoot;
    }

    private class ForceHiddenFilter extends IvyNodeElementFilterAdapter {
        private final Collection<IvyNodeElement> forceHidden = new HashSet<>();

        public boolean accept(IvyNodeElement unfiltered) {
            return !forceHidden.contains(unfiltered);
        }

        public void addHidden(IvyNodeElement hide) {
            forceHidden.addAll(Arrays.asList(hide.getDeepDependencies()));
        }

        public void clearHidden() {
            forceHidden.clear();
        }
    }
}
