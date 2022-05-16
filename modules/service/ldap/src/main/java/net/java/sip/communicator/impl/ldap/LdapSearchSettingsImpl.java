/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.impl.ldap;

import net.java.sip.communicator.service.ldap.*;

/**
 * LDAP Settings.
 *
 * @author Sebastien Mazy
 */
public class LdapSearchSettingsImpl
    implements LdapSearchSettings
{
    private boolean isMaxResultsSet = false;
    private int maxResults;
    private boolean isScopeSet = false;
    private Scope scope;
    private boolean isDelaySet = false;
    private int delay;

    public boolean isMaxResultsSet()
    {
        return isMaxResultsSet;
    }

    /**
     * Sets the maximum number of results to fetch from the
     * directory when performing the search query.
     *
     * @param maxResults the maximum number of results
     */
    public void setMaxResults(int maxResults)
    {
        if(maxResults < 1)
            throw new IllegalArgumentException(
                    "number of max results should be > 0");
        this.maxResults = maxResults;
        this.isMaxResultsSet = true;
    }

    /**
     * Returns the maximum number of results to fetch from the
     * directory when performing the search query.
     *
     * @return the maximum number of results
     */
    public int getMaxResults()
    {
        return this.maxResults;
    }

    public boolean isScopeSet()
    {
        return isScopeSet;
    }

    public void setScope(Scope scope)
    {
        this.scope = scope;
        this.isScopeSet = true;
    }

    public Scope getScope()
    {
        return this.scope;
    }

    public boolean isDelaySet()
    {
        return this.isDelaySet;
    }

    public void setDelay(int delay)
    {
        this.delay = delay;
        this.isDelaySet = true;
    }

    public int getDelay()
    {
        return this.delay;
    }
}
