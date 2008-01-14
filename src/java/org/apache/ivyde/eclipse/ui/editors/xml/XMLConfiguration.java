package org.apache.ivyde.eclipse.ui.editors.xml;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.swt.widgets.Shell;

public class XMLConfiguration extends SourceViewerConfiguration {
    private XMLDoubleClickStrategy doubleClickStrategy;

    private XMLTagScanner tagScanner;

    private XMLScanner scanner;

    private ColorManager colorManager;

    private IProject project;

    private IvyContentAssistProcessor _processor;

    private IFile _file;

    public XMLConfiguration(ColorManager colorManager) {
        this.colorManager = colorManager;
    }

    public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
        return new String[] {IDocument.DEFAULT_CONTENT_TYPE, XMLPartitionScanner.XML_COMMENT,
                XMLPartitionScanner.XML_TAG};
    }

    public ITextDoubleClickStrategy getDoubleClickStrategy(ISourceViewer sourceViewer,
            String contentType) {
        if (doubleClickStrategy == null)
            doubleClickStrategy = new XMLDoubleClickStrategy();
        return doubleClickStrategy;
    }

    protected XMLScanner getXMLScanner() {
        if (scanner == null) {
            scanner = new XMLScanner(colorManager);
            scanner.setDefaultReturnToken(new Token(new TextAttribute(colorManager
                    .getColor(IXMLColorConstants.DEFAULT))));
        }
        return scanner;
    }

    protected XMLTagScanner getXMLTagScanner() {
        if (tagScanner == null) {
            tagScanner = new XMLTagScanner(colorManager);
            tagScanner.setDefaultReturnToken(new Token(new TextAttribute(colorManager
                    .getColor(IXMLColorConstants.TAG))));
        }
        return tagScanner;
    }

    public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
        PresentationReconciler reconciler = new PresentationReconciler();

        DefaultDamagerRepairer dr = new DefaultDamagerRepairer(getXMLTagScanner());
        reconciler.setDamager(dr, XMLPartitionScanner.XML_TAG);
        reconciler.setRepairer(dr, XMLPartitionScanner.XML_TAG);

        dr = new DefaultDamagerRepairer(getXMLScanner());
        reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
        reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

        NonRuleBasedDamagerRepairer ndr = new NonRuleBasedDamagerRepairer(new TextAttribute(
                colorManager.getColor(IXMLColorConstants.XML_COMMENT)));
        reconciler.setDamager(ndr, XMLPartitionScanner.XML_COMMENT);
        reconciler.setRepairer(ndr, XMLPartitionScanner.XML_COMMENT);

        return reconciler;
    }

    public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
        // Create content assistant
        ContentAssistant assistant = new ContentAssistant();

        // required to display additional info
        assistant.setInformationControlCreator(new IInformationControlCreator() {
            public IInformationControl createInformationControl(Shell parent) {
                return new DefaultInformationControl(parent);
            }
        });

        _processor = new IvyContentAssistProcessor();
        _processor.setFile(_file);
        // Set this processor for each supported content type
        assistant.setContentAssistProcessor(_processor, XMLPartitionScanner.XML_TAG);
        assistant.setContentAssistProcessor(_processor, XMLPartitionScanner.XML_DEFAULT);
        assistant.setContentAssistProcessor(_processor, IDocument.DEFAULT_CONTENT_TYPE);

        // Return the content assistant
        return assistant;
    }

    public void setFile(IFile file) {
        if (_processor != null) {
            _processor.setFile(file);
        }
        _file = file;
    }
}
