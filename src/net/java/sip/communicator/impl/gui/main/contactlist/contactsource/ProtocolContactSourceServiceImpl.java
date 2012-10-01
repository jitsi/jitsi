/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist.contactsource;

import java.util.*;
import java.util.regex.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * 
 * @author Yana Stamcheva
 */
public class ProtocolContactSourceServiceImpl
    implements ContactSourceService
{
    private final ProtocolProviderService protocolProvider;

    private final Class<? extends OperationSet> opSetClass;

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

    public int getType()
    {
        return DEFAULT_TYPE;
    }

    public String getDisplayName()
    {
        return GuiActivator.getResources().getI18NString("service.gui.CONTACTS")
            + " " + protocolProvider.getAccountID().getDisplayName();
    }

    public ContactQuery queryContactSource(String queryString)
    {
        return queryContactSource(queryString, -1);
    }

    public ContactQuery queryContactSource( String queryString,
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

        boolean queryHasStarted = false;

        try
        {
            contactQuery.start();
            queryHasStarted = true;
        }
        finally
        {
            if (!queryHasStarted)
            {
                synchronized (queries)
                {
                    if (queries.remove(contactQuery))
                        queries.notify();
                }
            }
        }
        return contactQuery;
    }

    private class ProtocolCQuery
        extends AsyncContactQuery<ProtocolContactSourceServiceImpl>
    {
        private int contactCount;

        private final String queryString;

        public ProtocolCQuery(String queryString, int contactCount)
        {
            super(ProtocolContactSourceServiceImpl.this,
                Pattern.compile(queryString, Pattern.CASE_INSENSITIVE
                                | Pattern.LITERAL));

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

        /**
         * Adds the result for the given group.
         *
         * @param group the group
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

                if (queryString == null
                    || queryString.length() <= 0
                    || metaContact.getDisplayName().startsWith(queryString)
                    || contactAddress.startsWith(queryString)
                    || contactDisplayName.startsWith(queryString))
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

                    GenericSourceContact sourceContact
                        = new GenericSourceContact(
                                ProtocolContactSourceServiceImpl.this,
                                contactDisplayName,
                                contactDetails);

                    if (!contactAddress.equals(contactDisplayName))
                        sourceContact.setDisplayDetails(contactAddress);

                    sourceContact.setImage(metaContact.getAvatar());
                    sourceContact.setPresenceStatus(
                            contact.getPresenceStatus());

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
