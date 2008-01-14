package org.apache.ivyde.eclipse.ui.actions;

import java.util.Collection;
import java.util.Iterator;

import org.apache.ivyde.eclipse.IvyPlugin;
import org.apache.ivyde.eclipse.cpcontainer.IvyClasspathContainer;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

public class ResolveAllAction implements IWorkbenchWindowActionDelegate {
    /**
     * The constructor.
     */
    public ResolveAllAction() {
    }

    /**
     * The action has been activated. The argument of the method represents the 'real' action
     * sitting in the workbench UI.
     * 
     * @see IWorkbenchWindowActionDelegate#run
     */
    public void run(IAction action) {
        Job resolveAllJob = new Job("Resolve all dependencies") {
            protected IStatus run(IProgressMonitor monitor) {
                Collection containers = IvyPlugin.getDefault().getAllContainers();
                monitor.beginTask("Resolve all dependencies", containers.size());
                for (Iterator iter = containers.iterator(); iter.hasNext();) {
                    if (monitor.isCanceled()) {
                        return Status.CANCEL_STATUS;
                    }
                    SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 1);
                    IvyClasspathContainer cp = (IvyClasspathContainer) iter.next();
                    cp.resolve(subMonitor);
                }

                return Status.OK_STATUS;
            }
        };

        resolveAllJob.setUser(true);
        resolveAllJob.schedule();
    }

    /**
     * Selection in the workbench has been changed. We can change the state of the 'real' action
     * here if we want, but this can only happen after the delegate has been created.
     * 
     * @see IWorkbenchWindowActionDelegate#selectionChanged
     */
    public void selectionChanged(IAction action, ISelection selection) {
    }

    /**
     * We can use this method to dispose of any system resources we previously allocated.
     * 
     * @see IWorkbenchWindowActionDelegate#dispose
     */
    public void dispose() {
    }

    /**
     * We will cache window object in order to be able to provide parent shell for the message
     * dialog.
     * 
     * @see IWorkbenchWindowActionDelegate#init
     */
    public void init(IWorkbenchWindow window) {
    }
}
