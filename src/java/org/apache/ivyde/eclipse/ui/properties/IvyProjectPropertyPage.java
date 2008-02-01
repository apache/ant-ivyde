package org.apache.ivyde.eclipse.ui.properties;

import java.io.File;
import java.net.MalformedURLException;

import org.apache.ivyde.eclipse.IvyPlugin;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PropertyPage;

public class IvyProjectPropertyPage extends PropertyPage {

    private static final String PATH_TITLE = "Ivy settings url:";

    private static final String ACCEPTED_TYPES_TITLE = "Accepted types:";

    private static final String SOURCES_TYPES_TITLE = "Sources types:";

    private static final String SOURCES_SUFFIXES_TITLE = "Sources suffixes:";

    private static final String JAVADOC_TYPES_TITLE = "Javadoc types:";

    private static final String JAVADOC_SUFFIXES_TITLE = "Javadoc suffixes:";

    private Text _pathValueText;

    private Button _retreiveB;

    private Text _patternT;

    private Text _acceptedTypesText;

    private Text _sourcesTypesText;

    private Text _javadocTypesText;

    private Text _sourcesSuffixesText;

    private Text _javadocSuffixesText;

    public IvyProjectPropertyPage() {
        super();
    }

    private void addMainSection(Composite parent) {
        Composite composite = createDefaultComposite(parent);

        // Label for path field
        Label pathLabel = new Label(composite, SWT.NONE);
        pathLabel.setText(PATH_TITLE);

        _pathValueText = new Text(composite, SWT.SINGLE | SWT.BORDER);
        String ivyconfURL = IvyPlugin.getStrictIvyconfURL(getJavaProject());
        if (ivyconfURL == null) {
            ivyconfURL = getDefaultIvyconfURLForDisplay();
        }
        _pathValueText.setText(ivyconfURL);
        _pathValueText.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false,
                2, 1));

        Button btn = new Button(composite, SWT.NONE);
        btn.setText("Browse");
        btn.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                File f = getFile(new File("/"));
                if (f != null) {
                    try {
                        _pathValueText.setText(f.toURL().toExternalForm());
                    } catch (MalformedURLException e1) {
                    }
                }
            }
        });

        new Label(composite, SWT.NONE); // space
        Label explanation = new Label(composite, SWT.NONE);
        explanation.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, false, false, 3,
                1));
        explanation
                .setText("The url where your ivysettings file can be found. \nUse 'default' to reference the default ivy settings. \nUse '[inherited]' to use your general eclipse setting.\nRelative paths are handled relative to the project. Example: 'file://./ivysettings.xml'.");
        new Label(composite, SWT.NONE).setLayoutData(new GridData(GridData.FILL,
                GridData.BEGINNING, false, false, 4, 1)); // space

        Label acceptedTypesLabel = new Label(composite, SWT.NONE);
        acceptedTypesLabel.setText(ACCEPTED_TYPES_TITLE);

        _acceptedTypesText = new Text(composite, SWT.SINGLE | SWT.BORDER);
        _acceptedTypesText.setText(IvyPlugin.getAcceptedTypesString(getJavaProject()));
        _acceptedTypesText.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true,
                false, 3, 1));

        new Label(composite, SWT.NONE); // space
        explanation = new Label(composite, SWT.NONE);
        explanation.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, false, false, 3,
                1));
        explanation
                .setText("Comma separated list of artifact types to use in IvyDE Managed Dependencies Library.\nExample: jar, zip\nUse [inherited] to use your general eclise setting.");

        Label sourcesTypesLabel = new Label(composite, SWT.NONE);
        sourcesTypesLabel.setText(SOURCES_TYPES_TITLE);

        _sourcesTypesText = new Text(composite, SWT.SINGLE | SWT.BORDER);
        _sourcesTypesText.setText(IvyPlugin.getSourcesTypesString(getJavaProject()));
        _sourcesTypesText
                .setToolTipText("Example: source, src\nUse [inherited] to use your general eclise setting.");
        _sourcesTypesText.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true,
                false, 3, 1));

        new Label(composite, SWT.NONE); // space
        explanation = new Label(composite, SWT.NONE);
        explanation.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, false, false, 3,
                1));
        explanation.setText("Comma separated list of artifact types to be used as sources.");

        Label sourcesSuffixesLabel = new Label(composite, SWT.NONE);
        sourcesSuffixesLabel.setText(SOURCES_SUFFIXES_TITLE);

        _sourcesSuffixesText = new Text(composite, SWT.SINGLE | SWT.BORDER);
        _sourcesSuffixesText.setText(IvyPlugin.getSourcesSuffixesString(getJavaProject()));
        _sourcesSuffixesText
                .setToolTipText("Example: -source, -src\nUse [inherited] to use your general eclise setting.");
        _sourcesSuffixesText.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true,
                false, 3, 1));

        new Label(composite, SWT.NONE); // space
        explanation = new Label(composite, SWT.NONE);
        explanation.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, false, false, 3,
                1));
        explanation.setText("Comma separated list of suffixes to match sources to artifacts.");

        Label javadocTypesLabel = new Label(composite, SWT.NONE);
        javadocTypesLabel.setText(JAVADOC_TYPES_TITLE);

        _javadocTypesText = new Text(composite, SWT.SINGLE | SWT.BORDER);
        _javadocTypesText.setText(IvyPlugin.getJavadocTypesString(getJavaProject()));
        _javadocTypesText
                .setToolTipText("Example: javadoc\nUse [inherited] to use your general eclise setting.");
        _javadocTypesText.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true,
                false, 3, 1));

        new Label(composite, SWT.NONE); // space
        explanation = new Label(composite, SWT.NONE);
        explanation.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, false, false, 3,
                1));
        explanation.setText("Comma separated list of artifact types to be used as javadoc.");

        Label javadocSuffixesLabel = new Label(composite, SWT.NONE);
        javadocSuffixesLabel.setText(JAVADOC_TYPES_TITLE);

        _javadocSuffixesText = new Text(composite, SWT.SINGLE | SWT.BORDER);
        _javadocSuffixesText.setText(IvyPlugin.getJavadocSuffixesString(getJavaProject()));
        _javadocSuffixesText
                .setToolTipText("Example: -javadoc, -doc\nUse [inherited] to use your general eclise setting.");
        _javadocSuffixesText.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true,
                false, 3, 1));

        new Label(composite, SWT.NONE); // space
        explanation = new Label(composite, SWT.NONE);
        explanation.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, false, false, 3,
                1));
        explanation.setText("Comma separated list of suffixes to match javadocs to artifacts.");

        new Label(composite, SWT.NONE).setLayoutData(new GridData(GridData.FILL,
                GridData.BEGINNING, false, false, 4, 1)); // space

        _retreiveB = new Button(composite, SWT.CHECK);
        _retreiveB.setText("Do retrieve after resolve");
        _retreiveB
                .setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, false, false, 4, 1));

        new Label(composite, SWT.NONE).setText("Pattern:");
        _patternT = new Text(composite, SWT.SINGLE | SWT.BORDER);
        _patternT.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false, 3, 1));
        _retreiveB.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                _patternT.setEnabled(_retreiveB.getSelection());
            }
        });
        _retreiveB.setSelection(IvyPlugin.shouldDoRetrieve(getJavaProject()));
        _patternT.setEnabled(_retreiveB.getSelection());
        _patternT.setText(IvyPlugin.getRetrievePatternHerited(getJavaProject()));

        new Label(composite, SWT.NONE); // space
        explanation = new Label(composite, SWT.NONE);
        explanation.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, false, false, 3,
                1));
        explanation
                .setText("Example: lib/[conf]/[artifact].[ext]\nTo copy artifacts in folder named lib without revision by folder named like configurations\nUse [inherited] to use your general eclipse setting.");
        new Label(composite, SWT.NONE).setLayoutData(new GridData(GridData.FILL,
                GridData.BEGINNING, false, false, 4, 1)); // space
    }

    /**
     * Try to get a JavaProject from the getElement() result. Throws a IllegalStateException if it
     * can't succeed.
     * 
     * @return
     */
    private IJavaProject getJavaProject() {
        IAdaptable adaptable = getElement();
        IJavaProject project = null;
        if (adaptable instanceof IJavaProject) {
            project = (IJavaProject) adaptable;
        } else if (adaptable instanceof IProject) {
            project = JavaCore.create((IProject) adaptable);
        } else {
            throw new IllegalStateException("Attempting a IProject element ! Not "
                    + adaptable.getClass().getName() + " element");
        }
        return project;
    }

    /**
     * Helper to open the file chooser dialog.
     * 
     * @param startingDirectory
     *            the directory to open the dialog on.
     * @return File The File the user selected or <code>null</code> if they do not.
     */
    private File getFile(File startingDirectory) {

        FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
        if (startingDirectory != null)
            dialog.setFileName(startingDirectory.getPath());
        dialog.setFilterExtensions(new String[] {"*.xml", "*"});
        String file = dialog.open();
        if (file != null) {
            file = file.trim();
            if (file.length() > 0)
                return new File(file);
        }

        return null;
    }

    /**
     * @see PreferencePage#createContents(Composite)
     */
    protected Control createContents(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        composite.setLayout(layout);
        GridData data = new GridData(GridData.FILL);
        data.grabExcessHorizontalSpace = true;
        composite.setLayoutData(data);

        addMainSection(composite);
        return composite;
    }

    private Composite createDefaultComposite(Composite parent) {
        Composite composite = new Composite(parent, SWT.NULL);
        GridLayout layout = new GridLayout();
        layout.numColumns = 4;
        composite.setLayout(layout);

        GridData data = new GridData();
        data.grabExcessHorizontalSpace = true;
        data.verticalAlignment = GridData.FILL;
        data.horizontalAlignment = GridData.FILL;
        composite.setLayoutData(data);

        return composite;
    }

    protected void performDefaults() {
        _pathValueText.setText(getDefaultIvyconfURLForDisplay());
        _retreiveB.setSelection(false);
        _patternT.setText("");
        _acceptedTypesText.setText("[inherited]");
        _sourcesTypesText.setText("[inherited]");
        _sourcesSuffixesText.setText("[inherited]");
        _javadocTypesText.setText("[inherited]");
        _javadocSuffixesText.setText("[inherited]");
    }

    private String getDefaultIvyconfURLForDisplay() {
        return "[inherited] " + IvyPlugin.getIvyconfURL();
    }

    public boolean performOk() {
        IvyPlugin.beginChanges();
        try {
            // store the value in the owner text field
            String text = _pathValueText.getText();
            if (text.startsWith("[inherited] ") || text.trim().length() == 0) {
                text = null;
            }
            IvyPlugin.setIvyconfURL(getJavaProject(), text);

            // retreive per project
            if (_retreiveB.getSelection()) {
                IvyPlugin.setRetreivePattern(getJavaProject(), _patternT.getText());
            } else {
                IvyPlugin.setRetreivePattern(getJavaProject(), "");
            }

            IvyPlugin.setAcceptedTypes(getJavaProject(), _acceptedTypesText.getText());
            IvyPlugin.setSourcesTypes(getJavaProject(), _sourcesTypesText.getText());
            IvyPlugin.setSourcesSuffixes(getJavaProject(), _sourcesSuffixesText.getText());
            IvyPlugin.setJavadocTypes(getJavaProject(), _javadocTypesText.getText());
            IvyPlugin.setJavadocSuffixes(getJavaProject(), _javadocSuffixesText.getText());
            return true;
        } finally {
            IvyPlugin.commitChanges();
        }
    }

}
