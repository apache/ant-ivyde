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
package org.apache.ivyde.eclipse.cp;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;

public class ResolvedPath {

    private final String inputPath;

    private String resolvedPath;

    private File file;

    private URL url;

    private Exception e;

    public ResolvedPath(String inputPath, IProject project) {
        this.inputPath = inputPath;
        try {
            resolvePath(inputPath, project);
        } catch (MalformedURLException | CoreException e) {
            this.e = e;
        }
    }

    public boolean isSet() {
        return file != null || url != null;
    }

    public Exception getError() {
        return e;
    }

    public File getFile() {
        return file;
    }

    public URL getUrl() {
        return url;
    }

    public String getInputPath() {
        return inputPath;
    }

    public String getResolvedPath() {
        return resolvedPath;
    }

    private void resolvePath(String inputPath, IProject project) throws CoreException,
            MalformedURLException {
        IStringVariableManager manager = VariablesPlugin.getDefault().getStringVariableManager();
        if (project != null) {
            resolvedPath = inputPath.replaceAll("\\$\\{ivyproject_loc\\}", "\\${workspace_loc:"
                    + project.getName() + "}");
        } else {
            resolvedPath = inputPath;
        }
        resolvedPath = manager.performStringSubstitution(resolvedPath, false);
        resolvedPath = resolvedPath.trim();
        if (resolvedPath.length() == 0) {
            // no file or url to set
            return;
        }
        try {
            url = new URL(resolvedPath);
            if (url.getProtocol().equals("file")) {
                try {
                    // first try the standard way
                    file = new File(new URI(url.toString()));
                } catch (URISyntaxException | IllegalArgumentException e) {
                    // probably a badly constructed url: "file://" + filename
                    file = new File(url.getPath());
                }
            }
        } catch (MalformedURLException e) {
            // probably a file
            file = new File(resolvedPath);
            if (!file.isAbsolute()) {
                if (project != null) {
                    Path path = new Path(resolvedPath);
                    // get the file location in Eclipse "space"
                    IFile ifile = project.getFile(path);
                    // compute the actual file system location, following Eclipse's linked folders
                    // (see IVYDE-211)
                    IPath ipath = ifile.getLocation();
                    // get the corresponding java.io.File instance
                    file = ipath.toFile();
                } else {
                    file = file.getAbsoluteFile();
                }
            }
            url = file.toURI().toURL();
        }
    }

}
