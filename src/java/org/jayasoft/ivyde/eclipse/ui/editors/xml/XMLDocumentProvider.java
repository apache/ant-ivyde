package org.jayasoft.ivyde.eclipse.ui.editors.xml;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.DefaultPartitioner;
import org.eclipse.ui.editors.text.FileDocumentProvider;

public class XMLDocumentProvider extends FileDocumentProvider {

	protected IDocument createDocument(Object element) throws CoreException {
		IDocument document = super.createDocument(element);
		if (document != null) {
			IDocumentPartitioner partitioner =
				new DefaultPartitioner(
					new XMLPartitionScanner(),
					new String[] {
						XMLPartitionScanner.XML_TAG,
						XMLPartitionScanner.XML_COMMENT });
			partitioner.connect(document);
			document.setDocumentPartitioner(partitioner);
//            document.addDocumentListener(new IDocumentListener() {
//                
//                public void documentAboutToBeChanged(DocumentEvent event) {
//                }
//
//                public void documentChanged(DocumentEvent event) {
//                    System.out.println("XMLDocumentProvider.documentChanged :"+event.getText());
//                }
//            });
		}
		return document;
	}
    
    protected void doSaveDocument(IProgressMonitor monitor, Object element, IDocument document, boolean overwrite) throws CoreException {
        // TODO Auto-generated method stub
        super.doSaveDocument(monitor, element, document, overwrite);
    }
}