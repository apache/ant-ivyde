package org.jayasoft.ivyde.eclipse.ui.actions;

import java.util.Iterator;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.jayasoft.ivyde.eclipse.IvyPlugin;
import org.jayasoft.ivyde.eclipse.cpcontainer.IvyClasspathContainer;


public class ResolveAllAction implements IWorkbenchWindowActionDelegate {
	/**
	 * The constructor.
	 */
	public ResolveAllAction() {
	}

	/**
	 * The action has been activated. The argument of the
	 * method represents the 'real' action sitting
	 * in the workbench UI.
	 * @see IWorkbenchWindowActionDelegate#run
	 */
	public void run(IAction action) {
        for (Iterator iter = IvyPlugin.getDefault().getAllContainers().iterator(); iter.hasNext();) {
			IvyClasspathContainer cp = (IvyClasspathContainer) iter.next();
			cp.resolve();
		}
	}
	
	/**
	 * Selection in the workbench has been changed. We 
	 * can change the state of the 'real' action here
	 * if we want, but this can only happen after 
	 * the delegate has been created.
	 * @see IWorkbenchWindowActionDelegate#selectionChanged
	 */
	public void selectionChanged(IAction action, ISelection selection) {
	}

	/**
	 * We can use this method to dispose of any system
	 * resources we previously allocated.
	 * @see IWorkbenchWindowActionDelegate#dispose
	 */
	public void dispose() {
	}

	/**
	 * We will cache window object in order to
	 * be able to provide parent shell for the message dialog.
	 * @see IWorkbenchWindowActionDelegate#init
	 */
	public void init(IWorkbenchWindow window) {
	}
}