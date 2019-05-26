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
package org.apache.ivyde.internal.eclipse.ui.editors.xml;

import org.apache.ivyde.internal.eclipse.IvyPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.presentation.IPresentationDamager;
import org.eclipse.jface.text.presentation.IPresentationRepairer;
import org.eclipse.swt.custom.StyleRange;

public class NonRuleBasedDamagerRepairer implements IPresentationDamager, IPresentationRepairer {

    /** The document this object works on */
    private IDocument fDocument;

    /** The default text attribute if non is returned as data by the current token */
    private final TextAttribute fDefaultTextAttribute;

    /**
     * Constructor for NonRuleBasedDamagerRepairer.
     *
     * @param defaultTextAttribute TextAttribute
     */
    public NonRuleBasedDamagerRepairer(TextAttribute defaultTextAttribute) {
        Assert.isNotNull(defaultTextAttribute);

        fDefaultTextAttribute = defaultTextAttribute;
    }

    /**
     * @see IPresentationRepairer#setDocument(IDocument)
     * @param document IDocument
     */
    public void setDocument(IDocument document) {
        fDocument = document;
    }

    /**
     * Returns the end offset of the line that contains the specified offset or if the offset is
     * inside a line delimiter, the end offset of the next line.
     *
     * @param offset
     *            the offset whose line end offset must be computed
     * @return the line end offset for the given offset
     * @throws BadLocationException
     *                if offset is invalid in the current document
     */
    protected int endOfLineOf(int offset) throws BadLocationException {

        IRegion info = fDocument.getLineInformationOfOffset(offset);
        if (offset <= info.getOffset() + info.getLength()) {
            return info.getOffset() + info.getLength();
        }

        int line = fDocument.getLineOfOffset(offset);
        try {
            info = fDocument.getLineInformation(line + 1);
            return info.getOffset() + info.getLength();
        } catch (BadLocationException x) {
            return fDocument.getLength();
        }
    }

    /**
     * @see IPresentationDamager#getDamageRegion(ITypedRegion, DocumentEvent, boolean)
     * @param partition ITypedRegion
     * @param event DocumentEvent
     * @param documentPartitioningChanged boolean
     * @return IRegion
     */
    public IRegion getDamageRegion(ITypedRegion partition, DocumentEvent event,
            boolean documentPartitioningChanged) {
        if (!documentPartitioningChanged) {
            try {

                IRegion info = fDocument.getLineInformationOfOffset(event.getOffset());
                int start = Math.max(partition.getOffset(), info.getOffset());

                int end = event.getOffset()
                        + (event.getText() == null ? event.getLength() : event.getText().length());

                if (info.getOffset() <= end && end <= info.getOffset() + info.getLength()) {
                    // optimize the case of the same line
                    end = info.getOffset() + info.getLength();
                } else {
                    end = endOfLineOf(end);
                }

                end = Math.min(partition.getOffset() + partition.getLength(), end);
                return new Region(start, end - start);

            } catch (BadLocationException x) {
                // FIXME hu ? needs some comment
                IvyPlugin.logWarn("Something bad happened", x);
            }
        }

        return partition;
    }

    /**
     * @see IPresentationRepairer#createPresentation(TextPresentation, ITypedRegion)
     * @param presentation TextPresentation
     * @param region ITypedRegion
     */
    public void createPresentation(TextPresentation presentation, ITypedRegion region) {
        addRange(presentation, region.getOffset(), region.getLength(), fDefaultTextAttribute);
    }

    /**
     * Adds style information to the given text presentation.
     *
     * @param presentation
     *            the text presentation to be extended
     * @param offset
     *            the offset of the range to be styled
     * @param length
     *            the length of the range to be styled
     * @param attr
     *            the attribute describing the style of the range to be styled
     */
    protected void addRange(TextPresentation presentation, int offset, int length,
            TextAttribute attr) {
        if (attr != null) {
            presentation.addStyleRange(new StyleRange(offset, length, attr.getForeground(), attr
                    .getBackground(), attr.getStyle()));
        }
    }
}
