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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.ui.editors.text.FileDocumentProvider;

public class XMLDocumentProvider extends FileDocumentProvider {

    protected IDocument createDocument(Object element) throws CoreException {
        IDocument document = super.createDocument(element);
        if (document != null) {
            IDocumentPartitioner partitioner = new FastPartitioner(new XMLPartitionScanner(),
                    new String[] {XMLPartitionScanner.XML_TAG, XMLPartitionScanner.XML_COMMENT});
            partitioner.connect(document);
            document.setDocumentPartitioner(partitioner);
            // document.addDocumentListener(new IDocumentListener() {
            //
            // public void documentAboutToBeChanged(DocumentEvent event) {
            // }
            //
            // public void documentChanged(DocumentEvent event) {
            // System.out.println("XMLDocumentProvider.documentChanged :"+event.getText());
            // }
            // });
        }
        return document;
    }

    protected void doSaveDocument(IProgressMonitor monitor, Object element, IDocument document,
            boolean overwrite) throws CoreException {
        // TODO Auto-generated method stub
        super.doSaveDocument(monitor, element, document, overwrite);
    }
}
