package org.apache.ivyde.eclipse.ui.views;

import java.net.URL;

import org.apache.ivyde.eclipse.cpcontainer.IvyClasspathContainer;
import org.apache.ivyde.eclipse.ui.core.IvyFileEditorInput;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.packageview.ClassPathContainer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import fr.jayasoft.ivy.util.Message;

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
            IStructuredSelection s = (IStructuredSelection)sel;
            Object o = s.getFirstElement();
            if (o instanceof ClassPathContainer) {
                IPath path = ((ClassPathContainer)o).getClasspathEntry().getPath();
                IJavaProject project = ((ClassPathContainer)o).getJavaProject();
                try {
                    IClasspathContainer fContainer= JavaCore.getClasspathContainer(path, project);
                    if (fContainer instanceof IvyClasspathContainer) {
                    	_browser.setUrl("");
                        IvyClasspathContainer ivycp = (IvyClasspathContainer)fContainer;
                        URL report = ivycp.getReportUrl();
                        if (report != null) {
                        	if (!_browser.setUrl(report.toExternalForm())) {
                        		_browser.setUrl("");
                        		Message.warn("impossible to set report view url to "+report.toExternalForm());
                        	}
                        }
                    }
                } catch (JavaModelException e) {                    
                }

            }
        }
	}

}
