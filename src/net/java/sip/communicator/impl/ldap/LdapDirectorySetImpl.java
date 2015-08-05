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

import java.util.*;

import net.java.sip.communicator.service.ldap.*;
import net.java.sip.communicator.service.ldap.event.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.configuration.*;

/**
 * A thread-safe implementation of LdapDirectorySet, backed by a TreeMap
 *
 * @see net.java.sip.communicator.service.ldap
 *
 * @author Sebastien Mazy
 */
public class LdapDirectorySetImpl
    extends DefaultLdapEventManager
    implements LdapDirectorySet,
               LdapConstants
{
    /**
     * the logger for this class
     */
    private static Logger logger
        = Logger.getLogger(LdapDirectorySetImpl.class);

    /**
     * internal data structure used to store LdapDirectory(s)
     */
    private SortedMap<String,LdapDirectory> serverMap;

    /**
     * the configuration service
     */
    private ConfigurationService configService;

    /**
     * Stores the pending searches
     *
     * @see LdapPendingSearch
     */
    private HashMap<LdapQuery, LdapPendingSearch> pendingSearches =
        new HashMap<LdapQuery, LdapPendingSearch>();

    /**
     * A simple constructor for this class
     */
    public LdapDirectorySetImpl()
    {
        this(null);
    }

    /**
     * Use this constructor if you want every change to the LdapDirectorySet
     * to be propagated in the user preferences files
     *
     * @param configService the configuration service to use
     */
    public LdapDirectorySetImpl(ConfigurationService configService)
    {
        this.serverMap = Collections.
            synchronizedSortedMap(new TreeMap<String, LdapDirectory>());
        this.configService = configService;
    }

    /**
     * @return the LdapDirectory with name name or null
     * if it isn't present in the LdapDirectorySet
     */
    public LdapDirectory getServerWithName(String name)
    {
        return this.serverMap.get(name);
    }

    /**
     * Tries to remove an LdapDirectory from the LdapDirectorySet
     * using the name given by the getName method.
     *
     * @param name name of the LdapDirectory to remove
     *
     * @return LdapDirectory removed LdapDirectory or null if failed
     */
    public LdapDirectory removeServerWithName(String name)
    {
        LdapDirectory removed = this.serverMap.remove(name);

        if(configService != null)
            removed.getSettings().persistentRemove();

        return removed;
    }

    /**
     * Tries to add an LdapDirectory to the LdapDirectorySet
     *
     * @param server the server to be added
     *
     * @return whether it succeeded
     */
    public boolean addServer(LdapDirectory server)
    {
        /* TODO thread-safe */
        this.serverMap.put(server.getSettings().getName(), server);

        if(configService != null)
            server.getSettings().persistentSave();

        return true;
    }

    /**
     * @param name of the server to check presence in the LdapDirectorySet
     *
     * @return whether the server is in the LdapDirectorySet
     */
    public boolean containsServerWithName(String name)
    {
        /* TODO is this function useful ? */
        return this.serverMap.containsKey(name);
    }

    /**
     * Returns number of LdapDirectory(s) in the LdapDirectorySet.
     *
     * @return the number of LdapDirectory(s) in the LdapDirectorySet
     */
    public int size()
    {
        return this.serverMap.size();
    }

    /**
     * Required by LdapDirectorySet interface.
     * Returns a set of the marked enabled
     * LdapDirectory(s) alphabetically sorted
     *
     * @return a set of the enabled LdapDirectory(s)
     */
    public SortedSet<LdapDirectory> getEnabledServers()
    {
        SortedSet<LdapDirectory> enabledServers = new TreeSet<LdapDirectory>();

        for(LdapDirectory server : this)
        {
            if(server.isEnabled())
                enabledServers.add(server);
        }

        return enabledServers;
    }

    /**
     * Required by LdapDirectorySet interface.
     * Returns a set of the marked disabled
     * LdapDirectory(s) alphabetically sorted
     *
     * @return a set of the disabled LdapDirectory(s)
     */
    public SortedSet<LdapDirectory> getDisabledServers()
    {
        SortedSet<LdapDirectory> enabledServers = new TreeSet<LdapDirectory>();

        for(LdapDirectory server : this)
        {
            if(server.isEnabled())
                enabledServers.add(server);
        }

        return enabledServers;
    }

    /**
     * @return an iterator on all the
     * LdapDirectory(s) alphabetically sorted
     */
    public Iterator<LdapDirectory> iterator()
    {
        return this.serverMap.values().iterator();
    }

    /**
     * Performs a search on every LdapDirectory provided
     *
     * @param servers a set of LdapDirectory to search for the person
     * @param query the query to perform
     * @param caller the LdapListener that will receive the results
     * @param searchSettings the custom settings for this search,
     * or null for the defaults
     */
    public synchronized void searchPerson(
            Set<LdapDirectory> servers,
            LdapQuery query,
            LdapListener caller,
            LdapSearchSettings searchSettings
            )
    {
        if(servers == null)
            throw new NullPointerException("servers shouldn't be null!");
        if(query == null)
            throw new NullPointerException("query shouldn't be null!");
        if(caller == null)
            throw new NullPointerException("caller shouldn't be null!");

        this.pendingSearches.put(query, new LdapPendingSearch(servers, caller));
        for(LdapDirectory server : servers)
        {
            if(server == null)
                logger.info("server is null");
            server.searchPerson(query, this, searchSettings);
        }
    }

    /**
     * Performs a search on every enabled LdapDirectory of this set.
     *
     * @param query the query to perform
     * @param caller the LdapListener that will receive the results
     * @param searchSettings the custom settings for this search
     */
    public void searchPerson(
            LdapQuery query,
            LdapListener caller,
            LdapSearchSettings searchSettings
            )
    {
        this.searchPerson(this.getEnabledServers(), query, caller,
                searchSettings);
    }

    /**
     * Required by LdapListener.
     *
     * Dispatches event received from LdapDirectory-s to
     * real search initiators (the search dialog for example)
     *
     * @param event An LdapEvent probably sent by an LdapDirectory
     */
    public synchronized void ldapEventReceived(LdapEvent event)
    {
        LdapQuery query;
        switch(event.getCause())
        {
        case NEW_SEARCH_RESULT:
            LdapPersonFound result = (LdapPersonFound) event.getContent();
            query = result.getQuery();
            if(this.pendingSearches.get(query) != null)
            {
                this.fireLdapEvent(event, pendingSearches.get(query).
                        getCaller());
                logger.trace("result event for query \"" +
                        result.getQuery().toString() + "\" forwaded");
            }
            break;
        case SEARCH_ERROR:
        case SEARCH_CANCELLED:
        case SEARCH_ACHIEVED:
            query = (LdapQuery) event.getContent();
            if(this.pendingSearches.get(query) != null)
            {
                this.pendingSearches.get(query).getPendingServers().remove(
                        event.getSource());
                logger.trace("end event for query \"" + query.toString() +
                        "\" on directory \"" + event.getSource() + "\"");
                if(pendingSearches.get(query).getPendingServers().
                        size() == 0)
                {
                    fireLdapEvent(event, pendingSearches.get(query).
                            getCaller());
                    event = new LdapEvent(this,
                            LdapEvent.LdapEventCause.SEARCH_ACHIEVED,
                            query);
                    pendingSearches.remove(query);
                }
            }
            break;
        }
    }
}
