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
package org.apache.ivyde.internal.eclipse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.ivyde.internal.eclipse.cpcontainer.ClasspathEntriesResolver;
import org.apache.ivyde.internal.eclipse.cpcontainer.IvyClasspathContainerImpl;
import org.apache.ivyde.internal.eclipse.resolve.IvyResolveJob;
import org.apache.ivyde.internal.eclipse.resolve.ResolveRequest;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry2;
import org.eclipse.jdt.launching.IRuntimeClasspathEntryResolver;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;

/**
 * Resolver that doesn't include the non exported library of the imported project in the IvyDE
 * container, contrary to the default behavior.
 * <p>
 * See also https://bugs.eclipse.org/bugs/show_bug.cgi?id=284150
 */
public class IvyDERuntimeClasspathEntryResolver implements IRuntimeClasspathEntryResolver {

    public IRuntimeClasspathEntry[] resolveRuntimeClasspathEntry(IRuntimeClasspathEntry entry,
            ILaunchConfiguration configuration) throws CoreException {
        if (entry == null) {
            // cannot resolve without entry or project context
            return new IRuntimeClasspathEntry[0];
        }

        IJavaProject project = entry.getJavaProject();

        return computeDefaultContainerEntries(entry, project);
    }

    private IRuntimeClasspathEntry[] computeDefaultContainerEntries(IRuntimeClasspathEntry entry,
            IJavaProject project) throws CoreException {
        IvyClasspathContainerImpl ivycp;

        if (project == null) {
            ivycp = new IvyClasspathContainerImpl(null, entry.getPath(), null, null);
        } else {
            IClasspathContainer container = JavaCore
                    .getClasspathContainer(entry.getPath(), project);
            if (container == null) {
                String message = "Could not resolve classpath container: "
                        + entry.getPath().toString();
                throw new CoreException(new Status(IStatus.ERROR, IvyPlugin.ID,
                        IJavaLaunchConfigurationConstants.ERR_INTERNAL_ERROR, message, null));
                // execution will not reach here - exception will be thrown
            }
            ivycp = (IvyClasspathContainerImpl) container;
        }

        return computeDefaultContainerEntries(ivycp, entry);
    }

    private static IRuntimeClasspathEntry[] computeDefaultContainerEntries(
            IvyClasspathContainerImpl ivycp, IRuntimeClasspathEntry entry) throws CoreException {
        IClasspathEntry[] cpes;
        if (ivycp.getClasspathEntries() == null
                || ivycp.getConf().getInheritedAdvancedSetup().isResolveBeforeLaunch()) {
            ClasspathEntriesResolver resolver = new ClasspathEntriesResolver(ivycp, false);
            ResolveRequest request = new ResolveRequest(resolver, ivycp.getState());
            request.setForceFailOnError(true);
            request.setInWorkspace(ivycp.getConf().getInheritedClasspathSetup()
                .isResolveInWorkspace());
            request.setTransitive(ivycp.getConf().getInheritedClasspathSetup()
                    .isTransitiveResolve());
            IvyResolveJob resolveJob = IvyPlugin.getDefault().getIvyResolveJob();
            IStatus status = resolveJob.launchRequest(request, new NullProgressMonitor());
            if (status.getCode() != IStatus.OK) {
                throw new CoreException(status);
            }
            cpes = resolver.getClasspathEntries();
        } else {
            cpes = ivycp.getClasspathEntries();
        }
        List<IRuntimeClasspathEntry> resolved = new ArrayList<>(cpes.length);
        List<IJavaProject> projects = new ArrayList<>();
        for (IClasspathEntry cpe : cpes) {
            if (cpe.getEntryKind() == IClasspathEntry.CPE_PROJECT) {
                IProject p = ResourcesPlugin.getWorkspace().getRoot()
                        .getProject(cpe.getPath().segment(0));
                IJavaProject jp = JavaCore.create(p);
                if (!projects.contains(jp)) {
                    projects.add(jp);
                    IRuntimeClasspathEntry classpath = JavaRuntime
                            .newProjectRuntimeClasspathEntry(jp);
                    resolved.add(classpath);
                    IRuntimeClasspathEntry[] entries = JavaRuntime.resolveRuntimeClasspathEntry(
                            classpath, jp);
                    for (IRuntimeClasspathEntry e : entries) {
                        if (!resolved.contains(e)) {
                            resolved.add(e);
                        }
                    }
                }
            } else if (cpe.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
                IRuntimeClasspathEntry e = JavaRuntime.newArchiveRuntimeClasspathEntry(cpe
                        .getPath());
                if (!resolved.contains(e)) {
                    resolved.add(e);
                }
            }
        }
        // set classpath property
        IRuntimeClasspathEntry[] result = new IRuntimeClasspathEntry[resolved.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = resolved.get(i);
            result[i].setClasspathProperty(IRuntimeClasspathEntry.USER_CLASSES);
        }
        return result;
    }

    public IRuntimeClasspathEntry[] resolveRuntimeClasspathEntry(IRuntimeClasspathEntry entry,
            IJavaProject project) throws CoreException {
        if (!(entry instanceof IRuntimeClasspathEntry2)) {
            if (entry.getType() == IRuntimeClasspathEntry.CONTAINER) {
                return computeDefaultContainerEntries(entry, project);
            }
            return new IRuntimeClasspathEntry[] {entry};
        }

        IRuntimeClasspathEntry[] entries = ((IRuntimeClasspathEntry2) entry).getRuntimeClasspathEntries(null);
        List<IRuntimeClasspathEntry> resolved = new ArrayList<>();
        for (IRuntimeClasspathEntry ent : entries) {
            IRuntimeClasspathEntry[] temp = JavaRuntime.resolveRuntimeClasspathEntry(ent, project);
            Collections.addAll(resolved, temp);
        }
        return resolved.toArray(new IRuntimeClasspathEntry[resolved.size()]);
    }

    public IVMInstall resolveVMInstall(IClasspathEntry entry) throws CoreException {
        return null;
    }

}
