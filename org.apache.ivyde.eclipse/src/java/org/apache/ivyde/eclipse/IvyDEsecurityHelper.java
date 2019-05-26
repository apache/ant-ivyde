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
package org.apache.ivyde.eclipse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.ivy.util.url.CredentialsStore;
import org.apache.ivyde.eclipse.cp.SecuritySetup;
import org.apache.ivyde.internal.eclipse.IvyPlugin;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;

public final class IvyDEsecurityHelper {

    private static final String IVY_DE_CREDENTIALS_BASE_NODE = "org.apache.ivyde.credentials";

    private static final String HOST_KEY = "host";

    private static final String REALM_KEY = "realm";

    private static final String USERNAME_KEY = "username";

    private static final String PASSWORD_KEY = "pwd";

    private IvyDEsecurityHelper() {

    }

    public static void addCredentialsToIvyCredentialStorage(SecuritySetup setup) {
        CredentialsStore.INSTANCE.addCredentials(setup.getRealm(), setup.getHost(),
            setup.getUserName(), setup.getPwd());
        IvyPlugin.logInfo("Credentials " + setup.toString() + " added to IvyDE credential store");
    }

    public static void cpyCredentialsFromSecureToIvyStorage() {
        for (SecuritySetup entry : getCredentialsFromSecureStore()) {
            addCredentialsToIvyCredentialStorage(entry);
            IvyPlugin.logInfo("Credentials " + entry.toString()
                    + " from Eclipse secure storage copied to IvyDE credential store");
        }
    }

    public static void addCredentialsToSecureStorage(SecuritySetup setup) {
        ISecurePreferences preferences = SecurePreferencesFactory.getDefault();
        ISecurePreferences baseNode = preferences.node(IVY_DE_CREDENTIALS_BASE_NODE);
        ISecurePreferences childNode = baseNode.node(setup.getHost());
        ISecurePreferences childChildNode = childNode.node(setup.getRealm());

        try {
            childChildNode.put(HOST_KEY, setup.getHost(), false);
            childChildNode.put(REALM_KEY, setup.getRealm(), false);
            childChildNode.put(USERNAME_KEY, setup.getUserName(), true);
            childChildNode.put(PASSWORD_KEY, setup.getPwd(), true);
            childChildNode.flush();
            IvyPlugin.logInfo(
                "Credentials " + setup.toString() + " added to eclipse secure storage");
        } catch (StorageException | IOException e) {
            IvyPlugin.logError(e.getMessage(), e);
        }
    }

    public static List<SecuritySetup> getCredentialsFromSecureStore() {
        ISecurePreferences preferences = SecurePreferencesFactory.getDefault();
        List<SecuritySetup> setupValues = new ArrayList<>();
        if (preferences.nodeExists(IVY_DE_CREDENTIALS_BASE_NODE)) {
            ISecurePreferences node = preferences.node(IVY_DE_CREDENTIALS_BASE_NODE);
            for (String childName : node.childrenNames()) {
                ISecurePreferences childNode = node.node(childName);
                for (String childChildName : childNode.childrenNames()) {
                    ISecurePreferences childChildNode = childNode.node(childChildName);
                    try {
                        SecuritySetup toAdd = new SecuritySetup(
                                childChildNode.get(HOST_KEY, "localhost"),
                                childChildNode.get(REALM_KEY, "basic"),
                                childChildNode.get(USERNAME_KEY, null),
                                childChildNode.get(PASSWORD_KEY, null));
                        setupValues.add(toAdd);
                        IvyPlugin.logInfo("Credentials " + toAdd.toString()
                                + " loaded from Eclipse secure storage");
                    } catch (StorageException e1) {
                        IvyPlugin.logError(e1.getMessage(), e1);
                    }
                }
            }
        }
        Collections.sort(setupValues);
        return setupValues;
    }

    public static void removeCredentials(SecuritySetup setup) {
        removeCredentialsFromSecureStore(setup);
        invalidateIvyCredentials(setup);
    }

    public static boolean hostExistsInSecureStorage(String host, String realm) {
        ISecurePreferences preferences = SecurePreferencesFactory.getDefault();
        if (preferences.nodeExists(IVY_DE_CREDENTIALS_BASE_NODE)) {
            ISecurePreferences node = preferences.node(IVY_DE_CREDENTIALS_BASE_NODE);
            if (node.nodeExists(host)) {
                return node.node(host).nodeExists(realm);
            }
        }
        return false;
    }

    private static void removeCredentialsFromSecureStore(SecuritySetup setup) {
        String host = setup.getHost();
        String realm = setup.getRealm();
        ISecurePreferences preferences = SecurePreferencesFactory.getDefault();
        if (preferences.nodeExists(IVY_DE_CREDENTIALS_BASE_NODE)) {
            ISecurePreferences node = preferences.node(IVY_DE_CREDENTIALS_BASE_NODE);
            if (node.nodeExists(host)) {
                ISecurePreferences childNode = node.node(host);
                if (childNode.nodeExists(realm)) {
                    childNode.node(realm).removeNode();
                    try {
                        node.flush();
                        IvyPlugin.logInfo("Credentials " + setup.toString()
                                + "' removed from Eclipse secure storage");
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        IvyPlugin.logError(e.getMessage(), e);
                    }
                }
            }
        }
    }

    private static void invalidateIvyCredentials(SecuritySetup setup) {
        // need to invalidate => on credentialStore just add-ops allowed
        CredentialsStore.INSTANCE.addCredentials(setup.getHost(), setup.getRealm(), null, null);
        IvyPlugin.logInfo("Credentials " + setup
                + " invalidated on IvyDE credential store: Removed on next Eclipse startup.");
    }

    public static boolean credentialsInSecureStorage() {
        return SecurePreferencesFactory.getDefault().nodeExists(IVY_DE_CREDENTIALS_BASE_NODE);
    }

}
