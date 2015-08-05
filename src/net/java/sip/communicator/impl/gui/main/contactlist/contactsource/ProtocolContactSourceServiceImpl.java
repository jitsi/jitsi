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
package net.java.sip.communicator.impl.gui.main.contactlist.contactsource;

import java.util.*;
import java.util.regex.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>ProtocolContactSourceServiceImpl</tt>
 *
 * @author Yana Stamcheva
 */
public class ProtocolContactSourceServiceImpl
    implements ContactSourceService
{
    /**
     * The protocol provider, providing the contacts.
     */
    private final ProtocolProviderService protocolProvider;

    /**
     * The operation set class, we use to filter the capabilities of the
     * contacts.
     */
    private final Class<? extends OperationSet> opSetClass;

    /**
     * The <tt>MetaContactListService</tt>, providing the meta contact list.
     */
    MetaContactListService metaContactListService
        = GuiActivator.getContactListService();

    /**
     * The <tt>List</tt> of <tt>ProtocolContactQuery</tt> instances
     * which have been started and haven't stopped yet.
     */
    private final List<ProtocolCQuery> queries
        = new LinkedList<ProtocolCQuery>();

    /**
     * Creates an instance of <tt>ProtocolContactSourceServiceImpl</tt>.
     *
     * @param protocolProvider the protocol provider which is the contact source
     * @param opSetClass the <tt>OperationSet</tt> class that is supported by
     * source contacts
     */
    public ProtocolContactSourceServiceImpl(
            ProtocolProviderService protocolProvider,
            Class<? extends OperationSet> opSetClass)
    {
        this.protocolProvider = protocolProvider;
        this.opSetClass = opSetClass;
    }

    /**
     * Returns the type of this contact source.
     *
     * @return the type of this contact source
     */
    public int getType()
    {
        return DEFAULT_TYPE;
    }

    /**
     * Returns a user-friendly string that identifies this contact source.
     *
     * @return the display name of this contact source
     */
    public String getDisplayName()
    {
        return GuiActivator.getResources().getI18NString("service.gui.CONTACTS")
            + " " + protocolProvider.getAccountID().getDisplayName();
    }

    /**
     * Creates query for the given <tt>searchPattern</tt>.
     *
     * @param queryString the string to search for
     * @return the created query
     */
    public ContactQuery createContactQuery(String queryString)
    {
        return createContactQuery(queryString, -1);
    }

    /**
     * Creates query for the given <tt>searchPattern</tt>.
     *
     * @param queryString the string to search for
     * @param contactCount the maximum count of result contacts
     * @return the created query
     */
    public ContactQuery createContactQuery( String queryString,
                                            int contactCount)
    {
        if (queryString == null)
            queryString = "";

        ProtocolCQuery contactQuery
            = new ProtocolCQuery(queryString, contactCount);

        synchronized (queries)
        {
            queries.add(contactQuery);
        }

        return contactQuery;
    }

    /**
     * Removes query from the list.
     * @param contactQuery the query
     */
    public synchronized void removeQuery(ContactQuery contactQuery)
    {
        if (queries.remove(contactQuery))
            queries.notify();
    }

    /**
     * The <tt>ProtocolCQuery</tt> performing the query for this contact source.
     */
    private class ProtocolCQuery
        extends AsyncContactQuery<ProtocolContactSourceServiceImpl>
    {
        /**
         * The maximum number of contacts to return as result.
         */
        private int contactCount;

        /**
         * The query string used for filtering the results.
         */
        private final String queryString;

        /**
         * Creates an instance of <tt>ProtocolCQuery</tt>.
         *
         * @param queryString the query string
         * @param contactCount the maximum number of contacts to return as
         * result
         */
        public ProtocolCQuery(String queryString, int contactCount)
        {
            super(ProtocolContactSourceServiceImpl.this,
                Pattern.compile(queryString, Pattern.CASE_INSENSITIVE
                                | Pattern.LITERAL), true);

            this.queryString = queryString;
            this.contactCount = contactCount;
        }

        /**
         * {@inheritDoc}
         *
         * Always returns <tt>false</tt>.
         */
        @Override
        protected boolean phoneNumberMatches(String phoneNumber)
        {
            return false;
        }

        @Override
        public void run()
        {
            Iterator<MetaContact> contactListIter
                = metaContactListService.findAllMetaContactsForProvider(
                        protocolProvider);

            while (contactListIter.hasNext())
            {
                MetaContact metaContact = contactListIter.next();

                if (getStatus() == QUERY_CANCELED)
                    return;

                this.addResultContact(metaContact);
            }

            if (getStatus() != QUERY_CANCELED)
                setStatus(QUERY_COMPLETED);
        }

        @Override
        public synchronized void start()
        {
            boolean queryHasStarted = false;

            try
            {
                super.start();
                queryHasStarted = true;
            }
            finally
            {
                if (!queryHasStarted)
                {
                    getContactSource().removeQuery(this);
                }
            }

        }

        /**
         * Adds the result for the given group.
         *
         * @param metaContact the metaContact, which child protocol contacts
         * we'll be adding to the result
         */
        private void addResultContact(MetaContact metaContact)
        {
            Iterator<Contact> contacts
                = metaContact.getContactsForProvider(protocolProvider);

            while (contacts.hasNext())
            {
                if (getStatus() == QUERY_CANCELED)
                    return;

                if(contactCount > 0 && getQueryResultCount() > contactCount)
                    break;

                Contact contact = contacts.next();
                String contactAddress = contact.getAddress();
                String contactDisplayName = contact.getDisplayName();
                String queryLowerCase = queryString.toLowerCase();

                if (queryString == null
                    || queryString.length() <= 0
                    || metaContact.getDisplayName().toLowerCase().contains(
                        queryLowerCase)
                    || contactAddress.toLowerCase().contains(queryLowerCase)
                    || contactDisplayName.toLowerCase().contains(queryLowerCase))
                {
                    ContactDetail contactDetail
                        = new ContactDetail(contactAddress);
                    List<Class<? extends OperationSet>> supportedOpSets
                        = new ArrayList<Class<? extends OperationSet>>();

                    supportedOpSets.add(opSetClass);
                    contactDetail.setSupportedOpSets(supportedOpSets);

                    List<ContactDetail> contactDetails
                        = new ArrayList<ContactDetail>();

                    contactDetails.add(contactDetail);

                    SortedGenericSourceContact sourceContact
                        = new SortedGenericSourceContact(
                                this,
                                ProtocolContactSourceServiceImpl.this,
                                metaContact.getDisplayName(),
                                contactDetails);

                    if (!contactAddress.equals(contactDisplayName))
                        sourceContact.setDisplayDetails(contactAddress);

                    sourceContact.setImage(metaContact.getAvatar());
                    sourceContact.setPresenceStatus(
                            contact.getPresenceStatus());
                    sourceContact.setContactAddress(contactAddress);

                    addQueryResult(sourceContact);
                }
            }
        }
    }

    /**
     * Returns the index of the contact source in the result list.
     *
     * @return the index of the contact source in the result list
     */
    public int getIndex()
    {
        return 1;
    }
}
