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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Set;

import org.apache.ivyde.eclipse.resolvevisualizer.label.AllCallersAlgorithm;
import org.apache.ivyde.eclipse.resolvevisualizer.label.AllDependencyAlgorithm;
import org.apache.ivyde.eclipse.resolvevisualizer.label.AllRootPathsAlgorithm;
import org.apache.ivyde.eclipse.resolvevisualizer.label.SameModuleIdAlgorithm;
import org.apache.ivyde.eclipse.resolvevisualizer.label.ShortestRootPathAlgorithm;
import org.apache.ivyde.eclipse.resolvevisualizer.model.IvyNodeElement;
import org.apache.ivyde.eclipse.resolvevisualizer.model.IvyNodeElementFilterAdapter;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.draw2d.parts.ScrollableThumbnail;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IMessage;
import org.eclipse.ui.forms.ManagedForm;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;
import org.eclipse.zest.core.viewers.GraphViewer;
import org.eclipse.zest.core.widgets.Graph;

public class ResolveVisualizerForm {
    public static final String HEADER_TEXT = "Ivy Resolve Visualization";

    private final FormToolkit toolkit;
    private GraphViewer viewer;
    private final ScrolledForm form;
    private final ManagedForm managedForm;
    private final ResolveVisualizerView view;

    private Label searchLabel;
    private Text searchBox;

    private SashForm sash;

    // various auto-select options
    private Button showAllDependencies;
    private Button showAllCallers;
    private Button showShortestRootPath;
    private Button showAllRootPaths;
    private Button showSameModuleId;

    private Button evictionFilterEnablement;
    private Button depthLimitFilterEnablement;
    private Spinner depthLimit;

    private ThumbnailNavigator thumbnailNavigator;

    private final DepthFilter depthFilter = new DepthFilter();
    private final EvictionFilter evictionFilter = new EvictionFilter();

    public ResolveVisualizerForm(Composite parent, FormToolkit toolkit, ResolveVisualizerView view) {
        this.toolkit = toolkit;
        this.view = view;
        form = this.toolkit.createScrolledForm(parent);
        managedForm = new ManagedForm(this.toolkit, this.form);
        createHeaderRegion(form);
        FillLayout layout = new FillLayout();
        layout.marginHeight = 10;
        layout.marginWidth = 4;
        form.getBody().setLayout(layout);

        this.toolkit.decorateFormHeading(this.form.getForm());
        createSash(form.getBody());

        view.getContentProvider().addFilter(depthFilter);
        view.getContentProvider().addFilter(evictionFilter);
    }

    /**
     * Creates the section of the form where the graph is drawn
     *
     * @param parent Composite
     */
    private void createGraphSection(Composite parent) {
        Section section = this.toolkit.createSection(parent, Section.TITLE_BAR);
        thumbnailNavigator = new ThumbnailNavigator(section, SWT.NONE);
        viewer = new InternalGraphViewer(thumbnailNavigator, SWT.NONE);
        viewer.getGraphControl().setVerticalScrollBarVisibility(FigureCanvas.NEVER);
        viewer.getGraphControl().setHorizontalScrollBarVisibility(FigureCanvas.NEVER);
        thumbnailNavigator.setGraph((Graph) viewer.getControl());
        thumbnailNavigator.setSize(100, 25);
        section.setClient(thumbnailNavigator);
    }

    private void createHeaderRegion(ScrolledForm form) {
        Composite headClient = new Composite(form.getForm().getHead(), SWT.NULL);
        GridLayout glayout = new GridLayout();
        glayout.marginWidth = glayout.marginHeight = 0;
        glayout.numColumns = 3;
        headClient.setLayout(glayout);
        headClient.setBackgroundMode(SWT.INHERIT_DEFAULT);
        searchLabel = new Label(headClient, SWT.NONE);
        searchLabel.setText("Search:");
        searchBox = toolkit.createText(headClient, "");
        GridData data = new GridData();
        data.widthHint = 300;
        searchBox.setLayoutData(data);

        toolkit.paintBordersFor(headClient);
        form.setHeadClient(headClient);
        form.setText(HEADER_TEXT);
        enableSearchBox(false);

        form.getForm().addMessageHyperlinkListener(new HyperlinkAdapter() {
            @SuppressWarnings("unchecked")
            public void linkActivated(HyperlinkEvent e) {
                String title = e.getLabel();
                Object href = e.getHref();
                if (href instanceof IMessage[] && ((IMessage[]) href).length > 1) {
                    Point hl = ((Control) e.widget).toDisplay(0, 0);
                    hl.x += 10;
                    hl.y += 10;
                    final Shell shell = new Shell(ResolveVisualizerForm.this.form.getShell(), SWT.ON_TOP | SWT.TOOL);
                    shell.setImage(getImage(ResolveVisualizerForm.this.form.getMessageType()));
                    shell.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
                    shell.setBackgroundMode(SWT.INHERIT_DEFAULT);
                    GridLayout layout = new GridLayout();
                    layout.numColumns = 1;
                    layout.verticalSpacing = 0;
                    shell.setText(title);
                    shell.setLayout(layout);
                    Link link = new Link(shell, SWT.NONE);
                    link.setText("<A>close</A>");
                    GridData data = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
                    link.setLayoutData(data);
                    link.addSelectionListener(new SelectionAdapter() {
                        public void widgetSelected(SelectionEvent e) {
                            shell.close();
                        }
                    });
                    Group group = new Group(shell, SWT.NONE);
                    data = new GridData(SWT.LEFT, SWT.TOP, true, true);
                    group.setLayoutData(data);
                    group.setLayout(layout);
                    group.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
                    FormText text = toolkit.createFormText(group, true);
                    configureFormText(ResolveVisualizerForm.this.form.getForm(), text);
                    text.setText(createFormTextContent((IMessage[]) href), true, false);

                    shell.setLocation(hl);
                    shell.pack();
                    shell.open();
                } else if (href instanceof IMessage[]) {
                    IMessage oneMessage = ((IMessage[]) href)[0];
                    Set<IvyNodeElement> conflicts = (Set<IvyNodeElement>) oneMessage.getData();
                    if (conflicts != null) {
                        viewer.setSelection(new StructuredSelection(new ArrayList<>(conflicts)));
                    }
                }
            }
        });
    }

    public void enableSearchBox(boolean enable) {
        this.searchLabel.setEnabled(enable);
        this.searchBox.setEnabled(enable);
    }

    /**
     * Creates the sash form to separate the graph from the controls.
     *
     * @param parent Composite
     */
    private void createSash(Composite parent) {
        sash = new SashForm(parent, SWT.NONE);
        this.toolkit.paintBordersFor(parent);

        createGraphSection(sash);
        createOptionsSection(sash);
        sash.setWeights(new int[] { 10, 2 });
    }

    private void createOptionsSection(Composite parent) {
        Section controls = this.toolkit.createSection(parent, Section.TITLE_BAR | Section.EXPANDED);

        controls.setText("Options");
        Composite controlComposite = new Composite(controls, SWT.NONE) {
            public Point computeSize(int hint, int hint2, boolean changed) {
                return new Point(0, 0);
            }
        };
        this.toolkit.adapt(controlComposite);
        controlComposite.setLayout(new GridLayout());

        Section autoSelectOptions = this.toolkit.createSection(controlComposite, Section.EXPANDED);
        autoSelectOptions.setText("Auto Selection");
        autoSelectOptions.setLayout(new FillLayout());
        Composite autoSelectOptionsComposite = this.toolkit.createComposite(autoSelectOptions);
        autoSelectOptionsComposite.setLayout(new TableWrapLayout());

        showShortestRootPath = this.toolkit
                .createButton(autoSelectOptionsComposite, "Shortest path to root", SWT.RADIO);
        showShortestRootPath.setLayoutData(new TableWrapData(TableWrapData.FILL));
        showShortestRootPath.setSelection(true);
        showShortestRootPath.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                view.setAutoSelectDecorator(new ShortestRootPathAlgorithm());
            }
        });

        showAllRootPaths = this.toolkit.createButton(autoSelectOptionsComposite, "All paths to root", SWT.RADIO);
        showAllRootPaths.setLayoutData(new TableWrapData(TableWrapData.FILL));
        showAllRootPaths.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                view.setAutoSelectDecorator(new AllRootPathsAlgorithm());
            }
        });

        showAllCallers = this.toolkit.createButton(autoSelectOptionsComposite, "All callers", SWT.RADIO);
        showAllCallers.setLayoutData(new TableWrapData(TableWrapData.FILL));
        showAllCallers.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                view.setAutoSelectDecorator(new AllCallersAlgorithm());
            }
        });

        showAllDependencies = this.toolkit.createButton(autoSelectOptionsComposite, "All dependencies", SWT.RADIO);
        showAllDependencies.setLayoutData(new TableWrapData(TableWrapData.FILL));
        showAllDependencies.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                view.setAutoSelectDecorator(new AllDependencyAlgorithm());
            }
        });

        showSameModuleId = this.toolkit.createButton(autoSelectOptionsComposite, "Other revisions", SWT.RADIO);
        showSameModuleId.setLayoutData(new TableWrapData(TableWrapData.FILL));
        showSameModuleId.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                view.setAutoSelectDecorator(new SameModuleIdAlgorithm());
            }
        });

        autoSelectOptions.setClient(autoSelectOptionsComposite);

        Section filterOptions = this.toolkit.createSection(controlComposite, Section.EXPANDED);
        filterOptions.setText("Filter Options");
        filterOptions.setLayout(new FillLayout());
        Composite filterOptionsComposite = this.toolkit.createComposite(filterOptions);
        filterOptionsComposite.setLayout(new TableWrapLayout());

        evictionFilterEnablement = this.toolkit.createButton(filterOptionsComposite, "Hide evicted nodes", SWT.CHECK);
        evictionFilterEnablement.setLayoutData(new TableWrapData(TableWrapData.FILL));
        evictionFilterEnablement.setSelection(true);
        evictionFilter.setEnabled(true);
        evictionFilterEnablement.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                if (evictionFilterEnablement.getSelection()) {
                    evictionFilter.setEnabled(true);
                } else {
                    evictionFilter.setEnabled(false);
                }
                view.refresh();
            }
        });

        depthLimitFilterEnablement = this.toolkit.createButton(filterOptionsComposite, "Limit depth", SWT.CHECK);
        depthLimitFilterEnablement.setLayoutData(new TableWrapData(TableWrapData.FILL));
        depthLimitFilterEnablement.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                if (depthLimitFilterEnablement.getSelection()) {
                    depthFilter.setDepth(depthLimit.getSelection());
                    depthFilter.setEnabled(true);
                    view.refresh();
                    depthLimit.setEnabled(true);
                } else {
                    depthFilter.setEnabled(false);
                    view.refresh();
                    depthLimit.setEnabled(false);
                }
            }
        });

        depthLimit = new Spinner(filterOptionsComposite, 0);
        toolkit.adapt(depthLimit);
        depthLimit.setMinimum(1);
        depthLimit.setSelection(2);
        depthLimit.setIncrement(1);
        depthLimit.setSize(150, 40);
        depthLimit.setBackground(new Color(Display.getDefault(), 216, 228, 248));
        depthLimit.setEnabled(false);
        depthLimit.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                depthFilter.setDepth(depthLimit.getSelection());
                depthFilter.setEnabled(true);
                view.refresh();
            }
        });

        filterOptions.setClient(filterOptionsComposite);

        controls.setClient(controlComposite);
    }

    public GraphViewer getGraphViewer() {
        return viewer;
    }

    public ScrolledForm getForm() {
        return form;
    }

    public Text getSearchBox() {
        return this.searchBox;
    }

    private class InternalGraphViewer extends GraphViewer {
        public InternalGraphViewer(Composite parent, int style) {
            super(parent, style);
            Graph graph = new Graph(parent, style) {
                public Point computeSize(int hint, int hint2, boolean changed) {
                    return new Point(0, 0);
                }
            };
            setControl(graph);
        }
    }

    private static class ThumbnailNavigator extends Composite {
        FigureCanvas thumbnail;
        ScrollableThumbnail tb;

        public ThumbnailNavigator(Composite parent, int style) {
            super(parent, style);
            this.setLayout(new FormLayout());
            createZoomableCanvas(this);
        }

        public void setGraph(Graph graph) {
            if (graph.getParent() != this) {
                throw new AssertionError("Graph must be a child of this zoomable composite.");
            }
            createContents(graph);
            tb.setViewport(graph.getViewport());
            tb.setSource(graph.getContents());
        }

        private void createZoomableCanvas(Composite parent) {
            FormData data = new FormData();
            data.top = new FormAttachment(100, -100);
            data.left = new FormAttachment(100, -100);
            data.right = new FormAttachment(100, 0);
            data.bottom = new FormAttachment(100, 0);

            thumbnail = new FigureCanvas(parent, SWT.NONE);
            thumbnail.setBackground(ColorConstants.white);
            thumbnail.setLayoutData(data);

            tb = new ScrollableThumbnail();
            tb.setBorder(new LineBorder(1));
            thumbnail.setContents(tb);
        }

        private void createContents(Control control) {
            FormData data = new FormData();
            data.top = new FormAttachment(0, 0);
            data.left = new FormAttachment(0, 0);
            data.right = new FormAttachment(100, 0);
            data.bottom = new FormAttachment(100, 0);
            control.setParent(this);
            control.setLayoutData(data);
        }

    }

    private class DepthFilter extends IvyNodeElementFilterAdapter {
        private int depth = 2;

        public boolean accept(IvyNodeElement unfiltered) {
            return unfiltered.getDepth() - view.getCurrentRoot().getDepth() <= depth;
        }

        public void setDepth(int depth) {
            this.depth = depth;
        }
    }

    private class EvictionFilter extends IvyNodeElementFilterAdapter {
        public boolean accept(IvyNodeElement unfiltered) {
            return !unfiltered.isEvicted();
        }
    }

    public ManagedForm getManagedForm() {
        return managedForm;
    }

    private Image getImage(int type) {
        switch (type) {
        case IMessageProvider.ERROR:
            return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_ERROR_TSK);
        case IMessageProvider.WARNING:
            return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_WARN_TSK);
        case IMessageProvider.INFORMATION:
            return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_INFO_TSK);
        }
        return null;
    }

    private void configureFormText(final Form form, FormText text) {
        text.addHyperlinkListener(new HyperlinkAdapter() {
            @SuppressWarnings("unchecked")
            public void linkActivated(HyperlinkEvent e) {
                String is = (String) e.getHref();
                try {
                    ((FormText) e.widget).getShell().dispose();
                    int index = Integer.parseInt(is);
                    IMessage[] messages = form.getChildrenMessages();
                    IMessage message = messages[index];
                    Set<IvyNodeElement> conflicts = (Set<IvyNodeElement>) message.getData();
                    if (conflicts != null) {
                        viewer.setSelection(new StructuredSelection(new ArrayList<>(conflicts)));
                    }
                } catch (NumberFormatException ex) {
                }
            }
        });
        text.setImage("error", getImage(IMessageProvider.ERROR));
        text.setImage("warning", getImage(IMessageProvider.WARNING));
        text.setImage("info", getImage(IMessageProvider.INFORMATION));
    }

    String createFormTextContent(IMessage[] messages) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.println("<form>");
        for (int i = 0; i < messages.length; i++) {
            IMessage message = messages[i];
            pw.print("<li vspace=\"false\" style=\"image\" indent=\"16\" value=\"");
            switch (message.getMessageType()) {
            case IMessageProvider.ERROR:
                pw.print("error");
                break;
            case IMessageProvider.WARNING:
                pw.print("warning");
                break;
            case IMessageProvider.INFORMATION:
                pw.print("info");
                break;
            }
            pw.print("\"> <a href=\"");
            pw.print(i + "");
            pw.print("\">");
            if (message.getPrefix() != null) {
                pw.print(message.getPrefix());
            }
            pw.print(message.getMessage());
            pw.println("</a></li>");
        }
        pw.println("</form>");
        pw.flush();
        return sw.toString();
    }
}
