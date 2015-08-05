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
package net.java.sip.communicator.impl.protocol.jabber;

import java.util.*;

import org.jivesoftware.smack.*;
import org.jivesoftware.smackx.*;
import org.jivesoftware.smackx.ReportedData.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * This operation set provides utility methods for user search implementation.
 *
 * @author Hristo Terezov
 */
public class OperationSetUserSearchJabberImpl
    implements OperationSetUserSearch, RegistrationStateChangeListener
{
    /**
     * The logger.
     */
    private static final Logger logger =
        Logger.getLogger(OperationSetUserSearchJabberImpl.class);

    /**
     * The <tt>UserSearchManager</tt> instance which actually implements the
     * user search.
     */
    private UserSearchManager searchManager = null;

    /**
     * The <tt>ProtocolProviderService</tt> instance.
     */
    private ProtocolProviderServiceJabberImpl provider;

    /**
     * The user search service name.
     */
    private String serviceName = null;

    /**
     * Whether the user search service is enabled or not.
     */
    private Boolean userSearchEnabled = false;

    /**
     * A list of <tt>UserSearchProviderListener</tt> listeners which will be
     * notified when the provider user search feature is enabled or disabled.
     */
    private List<UserSearchProviderListener> listeners
        = new ArrayList<UserSearchProviderListener>();

    /**
     * The property name of the user search service name.
     */
    private static final String USER_SEARCH_SERVICE_NAME
        = "USER_SEARCH_SERVICE_NAME";

    /**
     * Constructs new <tt>OperationSetUserSearchJabberImpl</tt> instance.
     * @param provider the provider associated with the operation set.
     */
    protected OperationSetUserSearchJabberImpl(
        ProtocolProviderServiceJabberImpl provider)
    {
        this.provider = provider;
        serviceName = provider.getAccountID().getAccountPropertyString(
            USER_SEARCH_SERVICE_NAME, "");
        if(serviceName.equals(""))
        {
            provider.addRegistrationStateChangeListener(this);
        }
        else
        {
            setUserSearchEnabled(true);
        }
    }

    /**
     * Sets the <tt>userSearchEnabled</tt> property and fires
     * <tt>UserSearchProviderEvent</tt> event.
     *
     * @param isEnabled the value to be set.
     */
    private void setUserSearchEnabled(boolean isEnabled)
    {
        userSearchEnabled = isEnabled;
        int type = (isEnabled? UserSearchProviderEvent.PROVIDER_ADDED
            : UserSearchProviderEvent.PROVIDER_REMOVED);
        fireUserSearchProviderEvent(new UserSearchProviderEvent(provider, type));
    }

    /**
     * Fires <tt>UserSearchProviderEvent</tt> event.
     * @param event the event to be fired.
     */
    private void fireUserSearchProviderEvent(UserSearchProviderEvent event)
    {
        List<UserSearchProviderListener> tmpListeners;
        synchronized (listeners)
        {
            tmpListeners = new ArrayList<UserSearchProviderListener>(listeners);
        }
        for(UserSearchProviderListener l : tmpListeners)
            l.onUserSearchProviderEvent(event);
    }

    @Override
    public void registrationStateChanged(RegistrationStateChangeEvent evt)
    {
        if(evt.getNewState().equals(RegistrationState.REGISTERED))
        {
            discoverSearchService();
        }
        else if(evt.getNewState() == RegistrationState.UNREGISTERED
            || evt.getNewState() == RegistrationState.AUTHENTICATION_FAILED
            || evt.getNewState() == RegistrationState.CONNECTION_FAILED)
       {
            synchronized (userSearchEnabled)
            {
                setUserSearchEnabled(false);
            }
       }
    }

    /**
     * Tries to discover the user search service name.
     */
    private void discoverSearchService()
    {
        new Thread()
        {
            public void run()
            {
                synchronized (userSearchEnabled)
                {
                    List<String> serviceNames
                        = UserSearchManager.getAvailableServiceNames(
                            provider);
                    if(!serviceNames.isEmpty())
                    {
                        serviceName = serviceNames.get(0);
                        setUserSearchEnabled(true);
                    }
                    else
                    {
                        setUserSearchEnabled(false);
                    }
                }

            };
        }.start();
    }

    /**
     * Creates the <tt>UserSearchManager</tt> instance.
     */
    public void createSearchManager()
    {
        if(searchManager == null)
        {
            searchManager
                = new UserSearchManager(provider.getConnection(), serviceName);
        }
    }

    /**
     * Releases the <tt>UserSearchManager</tt> instance.
     */
    public void removeSearchManager()
    {
        searchManager = null;
    }

    /**
     * Performs user search for the searched string and returns the JIDs of the
     * found contacts.
     *
     * @param searchedString the text we want to query the server.
     * @return the list of found JIDs
     */
    public List<String> search(String searchedString)
    {
        ReportedData data = null;
        try
        {
            data = searchManager.searchForString(searchedString);
        }
        catch (XMPPException e)
        {
           logger.error(e);
           return null;
        }

        if(data == null)
        {
            logger.error("No data have been received from server.");
            return null;
        }
        Iterator<Column> columns = data.getColumns();
        Iterator<Row> rows = data.getRows();
        if(columns == null || rows == null)
        {
            logger.error("The received data is corrupted.");
            return null;
        }

        Column jidColumn = null;
        while(columns.hasNext())
        {
            Column tmpCollumn = columns.next();
            if(tmpCollumn.getType().equals(FormField.TYPE_JID_SINGLE))
            {
                jidColumn = tmpCollumn;
                break;
            }
        }

        if(jidColumn == null)
        {
            logger.error("No jid collumn provided by the server.");
            return null;
        }
        List<String> result = new ArrayList<String>();
        while(rows.hasNext())
        {
            Row row = rows.next();
            result.add((String)row.getValues(jidColumn.getVariable()).next());
        }
        return result;
    }

    /**
     * Adds <tt>UserSearchProviderListener</tt> instance to the list of
     * listeners.
     *
     * @param l the listener to be added
     */
    public void addUserSearchProviderListener(UserSearchProviderListener l)
    {
        synchronized (listeners)
        {
            if(!listeners.contains(l))
                listeners.add(l);
        }
    }

    /**
     * Removes <tt>UserSearchProviderListener</tt> instance from the list of
     * listeners.
     *
     * @param l the listener to be removed
     */
    public void removeUserSearchProviderListener(UserSearchProviderListener l)
    {
        synchronized (listeners)
        {
            listeners.remove(l);
        }
    }

    /**
     * Returns <tt>true</tt> if the user search service is enabled.
     *
     * @return  <tt>true</tt> if the user search service is enabled.
     */
    public boolean isEnabled()
    {
        return userSearchEnabled;
    }
}
