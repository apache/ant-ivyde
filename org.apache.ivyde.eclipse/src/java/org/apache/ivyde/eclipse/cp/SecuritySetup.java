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

public class SecuritySetup implements Comparable<SecuritySetup> {

    private String id = "";

    private String host = "";

    private String realm = "";

    private String userName = "";

    private String pwd = "";

    public SecuritySetup() {

    }

    /**
     * @param host String
     * @param realm String
     * @param userName String
     * @param pwd String
     */
    public SecuritySetup(String host, String realm, String userName, String pwd) {
        this.id = host + "@" + realm;
        this.host = host;
        this.realm = realm;
        this.userName = userName;
        this.pwd = pwd;
    }

    public void setAllValues(SecuritySetup toSet) {
        this.id = toSet.getHost() + "@" + toSet.getRealm();
        this.host = toSet.getHost();
        this.realm = toSet.getRealm();
        this.userName = toSet.getUserName();
        this.pwd = toSet.getPwd();
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id
     *            the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the host
     */
    public String getHost() {
        return host;
    }

    /**
     * @param host
     *            the host to set
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * @return the realm
     */
    public String getRealm() {
        return realm;
    }

    /**
     * @param realm
     *            the realm to set
     */
    public void setRealm(String realm) {
        this.realm = realm;
    }

    /**
     * @return the userName
     */
    public String getUserName() {
        return userName;
    }

    /**
     * @param userName
     *            the userName to set
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * @return the pwd
     */
    public String getPwd() {
        return pwd;
    }

    /**
     * @param pwd
     *            the pwd to set
     */
    public void setPwd(String pwd) {
        this.pwd = pwd;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((host == null) ? 0 : host.hashCode());
        result = prime * result + ((realm == null) ? 0 : realm.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        SecuritySetup other = (SecuritySetup) obj;
        if (host == null) {
            if (other.host != null) {
                return false;
            }
        } else if (!host.equals(other.host)) {
            return false;
        }
        return realm == null ? other.realm == null : realm.equals(other.realm);
    }

    @Override
    public String toString() {
        return "[storageId: '" + this.host + "@" + this.realm + "', host='" + this.host
                + "', realm='" + this.realm + "', user='" + this.userName + "', password='******']";
    }

    public int compareTo(SecuritySetup o) {
        return this.host.compareTo(o.getHost());
    }
}
