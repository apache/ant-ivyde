package org.apache.ivyde.eclipse.ui.views;

import java.net.URL;

import org.apache.ivy.util.Message;
import org.apache.ivyde.eclipse.cpcontainer.IvyClasspathContainer;
import org.apache.ivyde.eclipse.cpcontainer.IvyClasspathUtil;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.ViewPart;

public class ReportView extends ViewPart implements ISelectionListener {
    private Browser _browser;

    public void createPartControl(Composite parent) {
        _browser = new Browser(parent, SWT.NONE);

        // add myself as a global selection listener
        getSite().getPage().addSelectionListener(this);

        // prime the selection
        selectionChanged(null, getSite().getPage().getSelection());

    }

    public void setFocus() {
    }

    public void selectionChanged(IWorkbenchPart part, ISelection sel) {
        if (sel instanceof IStructuredSelection) {
            IvyClasspathContainer ivycp;
            try {
                ivycp = IvyClasspathUtil.getIvyClasspathContainer((IStructuredSelection) sel);
            } catch (JavaModelException e) {
                Message.error(e.getMessage());
                return;
            }
            if (ivycp != null) {
                _browser.setUrl("");
                URL report = ivycp.getReportUrl();
                if (report != null) {
                    if (!_browser.setUrl(report.toExternalForm())) {
                        _browser.setUrl("");
                        Message.warn("impossible to set report view url to "
                                + report.toExternalForm());
                    }
                }
            }
        }
    }

}
