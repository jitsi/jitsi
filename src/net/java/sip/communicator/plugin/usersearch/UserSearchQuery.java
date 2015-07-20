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
package net.java.sip.communicator.plugin.usersearch;

import java.util.*;
import java.util.regex.*;

import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>UserSearchQuery</tt> is a query over
 * <tt>UserSearchContactSource</tt>.
 */
public class UserSearchQuery
    extends AsyncContactQuery<ContactSourceService>
    implements UserSearchSupportedProviderListener
{
    /**
     * The query string.
     */
    private String queryString;

    /**
     * A list with the found contacts.
     */
    private List<UserSearchContact> contacts
        = new LinkedList<UserSearchContact>();

    /**
     * A list of providers that are already displayed.
     */
    private List<ProtocolProviderService> displayedProviders
        = new LinkedList<ProtocolProviderService>();

    /**
     * The number of the listeners added to the query.
     */
    private int contactQueryListenersCount = 0;

    /**
     * Creates an instance of <tt>ChatRoomQuery</tt> by specifying
     * the parent contact source, the query string to match and the maximum
     * result contacts to return.
     *
     * @param queryString the query string to match
     * @param contactSource the parent contact source
     */
    public UserSearchQuery(String queryString,
        UserSearchContactSource contactSource)
    {
        super(contactSource,
            Pattern.compile(queryString, Pattern.CASE_INSENSITIVE
                            | Pattern.LITERAL), true);
        this.queryString = queryString;

    }

    @Override
    protected void run()
    {
        try
        {
            Thread.sleep(UserSearchContactSource.QUERY_DELAY);
        }
        catch (InterruptedException e)
        { }
        if(getStatus() == QUERY_CANCELED)
            return;

        for(ProtocolProviderService provider :
            UserSearchActivator.getSupportedProviders())
        {
            providerAdded(provider);
        }
    }

    /**
     * Handles provider addition.
     *
     * @param provider the provider that was added.
     */
    public void providerAdded(ProtocolProviderService provider)
    {
        synchronized (displayedProviders)
        {
            if(displayedProviders.contains(provider))
                return;
            displayedProviders.add(provider);
        }

        OperationSetUserSearch opSetUserSearch
            = provider.getOperationSet(
                OperationSetUserSearch.class);
        if(opSetUserSearch == null)
        {
            return;
        }
        opSetUserSearch.createSearchManager();
        List<String> jidList = opSetUserSearch.search(queryString);
        if(jidList == null || jidList.isEmpty())
            return;

        List<Class<? extends OperationSet>> supportedOpSets
            = new ArrayList<Class<? extends OperationSet>>(2);
        supportedOpSets.add(OperationSetPersistentPresence.class);
        supportedOpSets.add(OperationSetBasicInstantMessaging.class);

        ContactDetail detail = new ContactDetail(queryString);

        for(String jid : jidList)
        {
            List<ContactDetail> contactDetails
                = new LinkedList<ContactDetail>();

            ContactDetail jidDetail = new ContactDetail(jid);
            jidDetail.setSupportedOpSets(supportedOpSets);

            contactDetails.add(jidDetail);

            contactDetails.add(detail);

            UserSearchContact contact
                = new UserSearchContact(
                    this,
                    getContactSource(),
                    jid,
                    contactDetails,
                    provider);
            contact.setContactAddress(jid);
            synchronized (contacts)
            {
                contacts.add(contact);
            }

            fireContactReceived(contact);
        }
    }

    /**
     * Handles provider removal.
     *
     * @param provider the provider that was removed.
     */
    public void providerRemoved(ProtocolProviderService provider)
    {
        synchronized (displayedProviders)
        {
            displayedProviders.remove(provider);
        }

        OperationSetUserSearch opSetUserSearch
            = provider.getOperationSet(
                OperationSetUserSearch.class);
        if(opSetUserSearch == null)
            return;
        opSetUserSearch.removeSearchManager();
        List<UserSearchContact> tmpContacts;
        synchronized (contacts)
        {
            tmpContacts = new LinkedList<UserSearchContact>(contacts);
        }
        for(UserSearchContact contact : tmpContacts)
        {
            if(contact.getProvider().equals(provider))
            {
                synchronized (contacts)
                {
                    contacts.remove(contact);
                }
                fireContactRemoved(contact);
            }
        }
    }

    /**
     * Releases the resources of the query.
     */
    private void dispose()
    {
        UserSearchActivator.removeUserSearchSupportedProviderListener(this);

        synchronized (contacts)
        {
            contacts.clear();
        }

        synchronized (displayedProviders)
        {
            displayedProviders.clear();
        }
    }

    /**
     * Cancels this <tt>ContactQuery</tt>.
     *
     * @see ContactQuery#cancel()
     */
    public void cancel()
    {
        dispose();

        super.cancel();
    }

    /**
     * If query has status changed to cancel, let's clear listeners.
     * @param status {@link ContactQuery#QUERY_CANCELED},
     * {@link ContactQuery#QUERY_COMPLETED}
     */
    public void setStatus(int status)
    {
        if(status == QUERY_CANCELED)
            dispose();

        super.setStatus(status);
    }

    @Override
    public void addContactQueryListener(ContactQueryListener l)
    {
        super.addContactQueryListener(l);
        contactQueryListenersCount++;
        if(contactQueryListenersCount == 1)
        {
            UserSearchActivator.addUserSearchSupportedProviderListener(this);
        }
    }

    @Override
    public void removeContactQueryListener(ContactQueryListener l)
    {
        super.removeContactQueryListener(l);
        contactQueryListenersCount--;
        if(contactQueryListenersCount == 0)
        {
            dispose();
        }
    }

    /**
     * A specific user search source contact.
     */
    private class UserSearchContact
        extends SortedGenericSourceContact
    {
        /**
         * The provider associated with the contact.
         */
        private ProtocolProviderService provider;

        public UserSearchContact(ContactQuery parentQuery,
            ContactSourceService cSourceService, String displayName,
            List<ContactDetail> contactDetails,
            ProtocolProviderService provider)
        {
            super(parentQuery, cSourceService, displayName, contactDetails);
            this.provider = provider;
        }

        /**
         * Returns the provider associated with the contact.
         * @return the provider associated with the contact.
         */
        public ProtocolProviderService getProvider()
        {
            return provider;
        }

    }
    @Override
    public List<SourceContact> getQueryResults()
    {
        List<SourceContact> queryResults;
        synchronized (contacts)
        {
            queryResults = new LinkedList<SourceContact>(contacts);
        }
        return queryResults;
    }

    @Override
    public int getQueryResultCount()
    {
        synchronized (contacts)
        {
            return contacts.size();
        }
    }

}

