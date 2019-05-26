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

import java.util.HashMap;
import java.util.Map;

import org.apache.ivyde.eclipse.resolvevisualizer.label.ConfigurationConflictAlgorithm;
import org.apache.ivyde.eclipse.resolvevisualizer.label.ConnectionStyle;
import org.apache.ivyde.eclipse.resolvevisualizer.label.DirectDependenciesAlgorithm;
import org.apache.ivyde.eclipse.resolvevisualizer.label.ILabelDecoratorAlgorithm;
import org.apache.ivyde.eclipse.resolvevisualizer.label.ShortestRootPathAlgorithm;
import org.apache.ivyde.eclipse.resolvevisualizer.model.IvyNodeElement;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.zest.core.viewers.EntityConnectionData;
import org.eclipse.zest.core.viewers.GraphViewer;
import org.eclipse.zest.core.viewers.IConnectionStyleProvider;
import org.eclipse.zest.core.viewers.IEntityStyleProvider;
import org.eclipse.zest.core.widgets.ZestStyles;

/**
 * Alters the color of the labels and connections based on the selected analysis algorithm.
 */
public class IvyNodeLabelProvider implements ILabelProvider, IConnectionStyleProvider, IEntityStyleProvider {
    public final Color GRAY = new Color(Display.getDefault(), 128, 128, 128);
    public final Color LIGHT_GRAY = new Color(Display.getDefault(), 220, 220, 220);
    public final Color BLACK = new Color(Display.getDefault(), 0, 0, 0);
    public final Color RED = new Color(Display.getDefault(), 255, 0, 0);
    public final Color LIGHT_GREEN = new Color(Display.getDefault(), 96, 255, 96);

    private IvyNodeElement selected = null;
    private IvyNodeElement rootNode = null;
    private Map<EntityConnectionData, ConnectionStyle> highlightedRelationships = new HashMap<>();
    private Map<IvyNodeElement, Color> highlightedDependencies = new HashMap<>();
    private Color disabledColor = null;
    private IvyNodeElement pinnedNode = null;
    private final GraphViewer viewer;

    private ILabelDecoratorAlgorithm autoSelectDecorator = new ShortestRootPathAlgorithm();
    private final DirectDependenciesAlgorithm rootDirectDependenciesDecorator = new DirectDependenciesAlgorithm();
    private final ConfigurationConflictAlgorithm conflictDecorator = new ConfigurationConflictAlgorithm();

    private Color rootColor;
    private Color rootSelectedColor;

    public IvyNodeLabelProvider(GraphViewer viewer) {
        this.viewer = viewer;
        this.rootDirectDependenciesDecorator.setStyles(new Color(Display.getDefault(), 197, 237, 197),
                new ConnectionStyle(ZestStyles.CONNECTIONS_SOLID, new Color(Display.getDefault(), 175, 175, 175), 1,
                        false));
    }

    public Image getImage(Object element) {
        if (element instanceof IvyNodeElement) {
            IvyNodeElement node = (IvyNodeElement) element;
            if (node.isEvicted()) {
                return ResolveVisualizerPlugin.getImageDescriptor("icons/evicted.gif").createImage();
            }
        }

        return null;
    }

    public String getText(Object element) {
        if (element instanceof IvyNodeElement) {
            IvyNodeElement node = (IvyNodeElement) element;
            String text = node.getOrganization() + "#" + node.getName() + ";";
            if (node.getRevision().contains("working@")) {
                text += "WORKSPACE";
            } else {
                text += node.getRevision();
            }
            return text;
        }

        return "";
    }

    public void addListener(ILabelProviderListener listener) {
    }

    public void removeListener(ILabelProviderListener listener) {
    }

    public boolean isLabelProperty(Object element, String property) {
        return false;
    }

    /**
     * Colors all connections regardless of their selection status.
     *
     * @param rel Object
     * @return Color
     */
    public Color getColor(Object rel) {
        EntityConnectionData key = (EntityConnectionData) rel;
        if (highlightedRelationships.keySet().contains(key)) {
            ConnectionStyle style = highlightedRelationships.get(rel);
            return style.getHighlightColor();
        }
        return LIGHT_GRAY;
    }

    public int getConnectionStyle(Object rel) {
        return ZestStyles.CONNECTIONS_DIRECTED;
    }

    /**
     * Colors "highlighted" relationships. We want to differentiate between those highlighted
     * programmatically by the auto-select mechanism, and those hand-selected by the user.
     *
     * @param rel Object
     * @return Color
     */
    public Color getHighlightColor(Object rel) {
        EntityConnectionData key = (EntityConnectionData) rel;
        if (highlightedRelationships.keySet().contains(key)) {
            ConnectionStyle style = highlightedRelationships.get(rel);
            return style.getHighlightColor();
        }
        return ColorConstants.blue;
    }

    public Color getNodeHighlightColor(Object entity) {
        return null;
    }

    public int getLineWidth(Object rel) {
        EntityConnectionData key = (EntityConnectionData) rel;
        if (highlightedRelationships.keySet().contains(key)) {
            ConnectionStyle style = highlightedRelationships.get(rel);
            if (style.isRevealOnHighlight()) {
                return style.getLineWidth();
            }
        }
        return 1;
    }

    public Color getAdjacentEntityHighlightColor(Object entity) {
        return null;
    }

    public Color getBorderColor(Object node) {
        IvyNodeElement entity = (IvyNodeElement) node;
        if (this.selected != null || this.pinnedNode != null) {
            if (entity == this.selected || entity == this.pinnedNode) {
                return BLACK;
            } else if (highlightedDependencies.keySet().contains(entity)) {
                // If this entity is directly connected to the selected entity
                return BLACK;
            } else {
                return LIGHT_GRAY;
            }

        }
        return BLACK;
    }

    public Color getBorderHighlightColor(Object entity) {
        return null;
    }

    public int getBorderWidth(Object entity) {
        return 0;
    }

    public Color getBackgroundColour(Object node) {
        IvyNodeElement entity = (IvyNodeElement) node;
        if (entity == this.rootNode) {
            if (rootColor == null) {
                rootColor = LIGHT_GREEN;
            }
            return rootColor;
        }
        if (highlightedDependencies.keySet().contains(entity)) {
            return highlightedDependencies.get(entity); // viewer.getGraphControl().HIGHLIGHT_ADJACENT_COLOR;
        } else {
            return viewer.getGraphControl().DEFAULT_NODE_COLOR;
        }
    }

    public Color getForegroundColour(Object node) {
        IvyNodeElement entity = (IvyNodeElement) node;
        if (this.selected != null || this.pinnedNode != null) {
            if (entity == this.selected || this.pinnedNode == entity) {
                return BLACK;
            } else if (highlightedDependencies.keySet().contains(entity)) {
                // If this entity is directly connected to the selected entity
                return BLACK;
            } else {
                return GRAY;
            }

        }
        return BLACK;
    }

    public void setPinnedNode(IvyNodeElement pinnedNode) {
        this.pinnedNode = pinnedNode;
    }

    protected IvyNodeElement getSelected() {
        if (pinnedNode != null) {
            return pinnedNode;
        }
        return selected;
    }

    /**
     * Sets the current selection.
     *
     * @param root IvyNodeElement
     * @param currentSelection IvyNodeElement
     */
    public void setCurrentSelection(IvyNodeElement root, IvyNodeElement currentSelection) {
        for (Map.Entry<EntityConnectionData, ConnectionStyle> relationship : highlightedRelationships.entrySet()) {
            if (relationship.getValue().isRevealOnHighlight()) {
                viewer.unReveal(relationship.getKey());
            }
        }

        this.rootNode = root;
        this.selected = null;
        this.selected = currentSelection;

        highlightedRelationships = new HashMap<>();
        highlightedDependencies = new HashMap<>();

        rootDirectDependenciesDecorator.calculateHighlighted(root, root,
                highlightedRelationships, highlightedDependencies);
        conflictDecorator.calculateHighlighted(root, root, highlightedRelationships, highlightedDependencies);

        if (this.selected != null || this.pinnedNode != null) {
            autoSelectDecorator.calculateHighlighted(root, selected,
                    highlightedRelationships, highlightedDependencies);
        }

        for (Map.Entry<EntityConnectionData, ConnectionStyle> relationship : highlightedRelationships.entrySet()) {
            if (relationship.getValue().isRevealOnHighlight()) {
                viewer.reveal(relationship.getKey());
            }
        }

        for (Object connection : viewer.getConnectionElements()) {
            viewer.update(connection, null);
        }
    }

    public void dispose() {
        if (this.disabledColor != null) {
            this.disabledColor.dispose();
            this.disabledColor = null;
        }
        if (this.rootColor != null) {
            this.rootColor.dispose();
            this.rootColor = null;
        }
        if (this.rootSelectedColor != null) {
            this.rootSelectedColor.dispose();
            this.rootSelectedColor = null;
        }
    }

    public IFigure getTooltip(Object entity) {
        if (entity instanceof EntityConnectionData) {
            EntityConnectionData connection = (EntityConnectionData) entity;
            IvyNodeElement source = (IvyNodeElement) connection.source;
            IvyNodeElement dest = (IvyNodeElement) connection.dest;

            StringBuilder tooltipText = new StringBuilder();
            for (String conf : dest.getCallerConfigurations(source)) {
                tooltipText.append(conf).append(", ");
            }
            return new Label(tooltipText.substring(0, tooltipText.length() - 2));
        }

        return null;
    }

    public boolean fisheyeNode(Object entity) {
        return false;
    }

    public void setAutoSelectDecorator(ILabelDecoratorAlgorithm decoratorAlgorithm) {
        this.autoSelectDecorator = decoratorAlgorithm;
    }
}
