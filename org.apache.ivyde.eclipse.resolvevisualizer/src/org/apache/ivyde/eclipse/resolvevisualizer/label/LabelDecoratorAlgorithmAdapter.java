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
package org.apache.ivyde.eclipse.resolvevisualizer.label;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.swt.graphics.Color;

public abstract class LabelDecoratorAlgorithmAdapter implements ILabelDecoratorAlgorithm {
    protected Color entityColor = ColorConstants.orange;
    protected ConnectionStyle relationshipColor = new ConnectionStyle();

    /**
     * Specify custom colors for this algorithm instance.
     *
     * @param entityColor Color
     * @param relationshipColor ConnectionStyle
     */
    public void setStyles(Color entityColor, ConnectionStyle relationshipColor) {
        this.entityColor = entityColor;
        this.relationshipColor = relationshipColor;
    }
}
