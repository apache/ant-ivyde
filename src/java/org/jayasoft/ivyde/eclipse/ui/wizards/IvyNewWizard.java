package org.jayasoft.ivyde.eclipse.ui.wizards;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWizard;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.jayasoft.ivyde.eclipse.ui.core.IvyFileEditorInput;
import org.jayasoft.ivyde.eclipse.ui.editors.IvyEditor;


public class IvyNewWizard extends Wizard implements INewWizard {
    private IvyNewWizardPage page;

    private ISelection selection;

    /**
     * Constructor for IvyNewWizard.
     */
    public IvyNewWizard() {
        super();
        setNeedsProgressMonitor(true);
    }

    /**
     * Adding the page to the wizard.
     */

    public void addPages() {
        page = new IvyNewWizardPage(selection);
        addPage(page);
    }

    /**
     * This method is called when 'Finish' button is pressed in the wizard. We will create an operation and run it using wizard as execution context.
     */
    public boolean performFinish() {
        final String containerName = page.getContainerName();
        final String fileName = page.getFileName();
        final String orgName = page.getOrganisationName();
        final String moduleName = page.getModuleName();
        final String status = page.getStatus();

        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IResource resource = root.findMember(new Path(containerName));
        if (!resource.exists() || !(resource instanceof IContainer)) {
            MessageDialog.openError(getShell(), "Error", "Container \"" + containerName + "\" does not exist.");
        }
        IContainer container = (IContainer) resource;
        final IFile file = container.getFile(new Path(fileName));
        if (file.exists() && !MessageDialog.openConfirm(getShell(), "overwrite existing ?", "The file you selected already exist. Do you want to overwrite its content ?")) {
            return false;
        }
        
        IRunnableWithProgress op = new IRunnableWithProgress() {
            public void run(IProgressMonitor monitor) throws InvocationTargetException {
                try {
                    doFinish(file, orgName, moduleName, status, monitor);
                } catch (CoreException e) {
                    throw new InvocationTargetException(e);
                } finally {
                    monitor.done();
                }
            }
        };
        try {
            getContainer().run(true, false, op);
        } catch (InterruptedException e) {
            return false;
        } catch (InvocationTargetException e) {
            Throwable realException = e.getTargetException();
            MessageDialog.openError(getShell(), "Error", realException.getMessage());
            return false;
        }
        return true;
    }

    /**
     * The worker method. It will find the container, create the file if missing or just replace its contents, and open the editor on the newly created file.
     */

    private void doFinish(final IFile file, String org, String module, String status, final IProgressMonitor monitor) throws CoreException {
        // create a sample file
        monitor.beginTask("Creating " + file.getName(), 2);
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(openContentStream()));
            final StringBuffer buf = new StringBuffer();
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                line = line.replaceAll("@ORGANISATION@", org);
                line = line.replaceAll("@MODULE@", module);
                line = line.replaceAll("@STATUS@", status);
                buf.append(line).append(System.getProperty("line.separator", "\n"));
            }
            reader.close();
            InputStream stream = new ByteArrayInputStream(buf.toString().getBytes());
            if (file.exists()) {
                file.setContents(stream, true, true, monitor);
            } else {
                file.create(stream, true, monitor);
            }
            stream.close();
        } catch (IOException e) {
        }
        monitor.worked(1);
        monitor.setTaskName("Opening file for editing...");
        getShell().getDisplay().asyncExec(new Runnable() {
            public void run() {
                IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
                try {
                    page.openEditor(new IvyFileEditorInput(file), IvyEditor.ID, true);
//                    IDE.openEditor(page, file, IvyEditor.ID, true);
                } catch (PartInitException e) {
                }
            }
        });
        monitor.worked(1);
    }

    /**
     * We will initialize file contents with a sample text.
     */

    private InputStream openContentStream() {
        return getClass().getResourceAsStream("ivy-template.xml");
    }

    private void throwCoreException(String message) throws CoreException {
        IStatus status = new Status(IStatus.ERROR, "org.jayasoft.ivyde", IStatus.OK, message, null);
        throw new CoreException(status);
    }

    /**
     * We will accept the selection in the workbench to see if we can initialize from it.
     * 
     * @see IWorkbenchWizard#init(IWorkbench, IStructuredSelection)
     */
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        this.selection = selection;
    }
//    public Image getDefaultPageImage() {
//        return IvyPlugin.getImageDescriptor("icons/logo16x16.gif").createImage();
//    }
}