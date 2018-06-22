/*
 *  Copyright 2012-2013 E.Hooijmeijer
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.javaswift.cloudie.login;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.apache.commons.lang.ObjectUtils;
import org.javaswift.joss.client.factory.AccountConfig;
import org.javaswift.joss.client.factory.AuthenticationMethod;
import org.slf4j.LoggerFactory;

/**
 * Stores credentials (in plain/encoded) text using the Java Preferences API.
 * @author E. Hooijmeier
 */
public class CredentialsStore {

    private static final String GARBLESRC = "qpwoeirutyalskdjfhgzmxncbvQPWOEIRUTYALSKDJFHGZMXNCVB";

    public static class Credentials {
        public AuthenticationMethod method;
        public String authUrl;
        public String tenantId;
        public String tenantName;
        public String username;
        public char[] password;
        public String preferredRegion;
        
        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Credentials) {
                Credentials cr = (Credentials) obj;
                return ObjectUtils.equals(tenantName, cr.tenantName) && ObjectUtils.equals(username, cr.username) && ObjectUtils.equals(tenantId, cr.tenantId)
                        && ObjectUtils.equals(method, cr.method);
            } else {
                return super.equals(obj);
            }
        }

        @Override
        public int hashCode() {
            return tenantName.hashCode() + 31 * username.hashCode() + 31 * 31 * (tenantId == null ? 0 : tenantId.hashCode()) + method.hashCode();
        }

        public String toString() {
            return tenantName + "(" + tenantId + ") -" + username;
        }

        public AccountConfig toAccountConfig() {
            AccountConfig account = new AccountConfig();
            account.setAuthenticationMethod(method);
            account.setAuthUrl(authUrl);
            account.setTenantId(tenantId);
            account.setTenantName(tenantName);
            account.setUsername(username);
            account.setPassword(new String(password));
            account.setPreferredRegion(preferredRegion);
            return account;
        }
    }

    /**
     * lists the available credentials for this user.
     * @return the credentials.
     */
    public List<Credentials> getAvailableCredentials() {
        List<Credentials> results = new ArrayList<Credentials>();
        try {
            Preferences prefs = Preferences.userNodeForPackage(CredentialsStore.class);
            for (String node : prefs.childrenNames()) {
                Preferences cred = prefs.node(node);
                try {
                    results.add(toCredentials(cred));
                } catch (IllegalArgumentException ex) {
                    LoggerFactory.getLogger(getClass()).warn("Bad preferences node - skipping");
                }
            }
        } catch (BackingStoreException ex) {
            throw new RuntimeException(ex);
        }
        return results;
    }

    /**
     * converts a preferences node to credentials.
     * @param node the node.
     * @return the credentials.
     */
    private Credentials toCredentials(Preferences node) {
        Credentials cr = new Credentials();
        cr.method = AuthenticationMethod.valueOf(node.get("method", AuthenticationMethod.KEYSTONE.name()));
        cr.authUrl = node.get("authUrl", "");
        cr.tenantName = node.get("tenantName", "");
        cr.tenantId = node.get("tenantId", "");
        cr.username = node.get("username", "");
        cr.password = garble(node.get("password", ""));
        cr.preferredRegion = node.get("preferredRegion", "");
        return cr;
    }

    /**
     * saves the given credentials.
     * @param cr the credentials.
     */
    public void save(Credentials cr) {
        try {
            Preferences prefs = Preferences.userNodeForPackage(CredentialsStore.class);
            saveCredentials(prefs.node(cr.toString()), cr);
            prefs.flush();
        } catch (BackingStoreException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * deletes the credentials.
     * @param cr the credentials.
     */
    public void delete(Credentials cr) {
        try {
            Preferences prefs = Preferences.userNodeForPackage(CredentialsStore.class);
            prefs.node(cr.toString()).removeNode();
            prefs.flush();
        } catch (BackingStoreException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void saveCredentials(Preferences node, Credentials cr) {
        node.put("method", String.valueOf(cr.method));
        node.put("authUrl", cr.authUrl);
        node.put("tenantId", cr.tenantId);
        node.put("tenantName", cr.tenantName);
        node.put("username", cr.username);
        node.put("password", String.valueOf(garble(cr.password)));
        node.put("preferredRegion", cr.preferredRegion);
    }

    //
    // Encode the password just to prevent it from being readable by accidental visitors.
    //

    /**
     * Garbles the password so that it cannot be just read. To de-garble the password invoke the method again. Its a
     * simple exclusive-or encoding.
     * @param string the string to garble.
     * @return the garbled string.
     */
    protected char[] garble(String string) {
        return garble(string.toCharArray());
    }

    private char[] garble(char[] chars) {
        char[] garble = GARBLESRC.toCharArray();
        for (int t = 0; t < chars.length; t++) {
            chars[t] = (char) (chars[t] ^ garble[t % garble.length]);
        }
        return chars;
    }

}
