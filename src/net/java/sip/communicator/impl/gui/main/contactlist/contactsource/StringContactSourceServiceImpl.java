/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist.contactsource;

import java.util.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>StringContactSourceServiceImpl</tt> is an implementation of the
 * <tt>ContactSourceService</tt> that returns the searched string as a result
 * contact.
 *
 * @author Yana Stamcheva
 */
public class StringContactSourceServiceImpl
    implements ContactSourceService
{
    /**
     * The protocol provider to be used with this string contact source.
     */
    private final ProtocolProviderService protocolProvider;

    /**
     * The operation set supported by this string contact source.
     */
    private final Class<? extends OperationSet> opSetClass;

    /**
     * Creates an instance of <tt>StringContactSourceServiceImpl</tt>.
     *
     * @param protocolProvider the protocol provider to be used with this string
     * contact source
     * @param opSet the operation set supported by this string contact source
     */
    public StringContactSourceServiceImpl(
        ProtocolProviderService protocolProvider,
        Class<? extends OperationSet> opSet)
    {
        this.protocolProvider = protocolProvider;
        this.opSetClass = opSet;
    }

    /**
     * Returns the type of this contact source.
     *
     * @return the type of this contact source
     */
    public int getType()
    {
        return SEARCH_TYPE;
    }

    /**
     * Returns a user-friendly string that identifies this contact source.
     *
     * @return the display name of this contact source
     */
    public String getDisplayName()
    {
        return GuiActivator.getResources().getI18NString(
            "service.gui.SEARCH_STRING_CONTACT_SOURCE");
    }

    /**
     * Queries this search source for the given <tt>queryString</tt>.
     *
     * @param queryString the string to search for
     * @return the created query
     */
    public ContactQuery queryContactSource(String queryString)
    {
        return queryContactSource(queryString, -1);
    }

    /**
     * Queries this search source for the given <tt>queryString</tt>.
     *
     * @param queryString the string to search for
     * @param contactCount the maximum count of result contacts
     * @return the created query
     */
    public ContactQuery queryContactSource( String queryString,
                                            int contactCount)
    {
        return new StringQuery(queryString);
    }

    /**
     * The query implementation.
     */
    private class StringQuery
        extends AbstractContactQuery<ContactSourceService>
    {
        /**
         * The query string.
         */
        private String queryString;

        /**
         * The query result list.
         */
        private final List<SourceContact> results;

        /**
         * Creates an instance of this query implementation.
         *
         * @param queryString the string to query
         */
        public StringQuery(String queryString)
        {
            super(StringContactSourceServiceImpl.this);

            this.queryString = queryString;
            this.results = new ArrayList<SourceContact>();

            results.add(getSourceContact());

            if (getStatus() != QUERY_CANCELED)
                setStatus(QUERY_COMPLETED);
        }

        /**
         * Returns the query string.
         *
         * @return the query string
         */
        public String getQueryString()
        {
            return queryString;
        }

        /**
         * Returns the list of query results.
         *
         * @return the list of query results
         */
        public List<SourceContact> getQueryResults()
        {
            return results;
        }

        /**
         * Returns the source contact corresponding to the query string.
         *
         * @return the source contact corresponding to the query string
         */
        private SourceContact getSourceContact()
        {
            ArrayList<ContactDetail> contactDetails
                = new ArrayList<ContactDetail>();

            ContactDetail contactDetail = new ContactDetail(queryString);

            // Init supported operation sets.
            ArrayList<Class<? extends OperationSet>>
                supportedOpSets
                = new ArrayList<Class<? extends OperationSet>>();
            supportedOpSets.add(opSetClass);
            contactDetail.setSupportedOpSets(supportedOpSets);

            // Init preferred protocol providers.
            Map<Class<? extends OperationSet>,ProtocolProviderService>
                providers = new HashMap<Class<? extends OperationSet>,
                                        ProtocolProviderService>();

            providers.put(opSetClass, protocolProvider);

            contactDetail.setPreferredProviders(providers);

            contactDetails.add(contactDetail);

            GenericSourceContact sourceContact
                = new GenericSourceContact( StringContactSourceServiceImpl.this,
                                            queryString,
                                            contactDetails);

            sourceContact.setDisplayDetails(
                GuiActivator.getResources().getI18NString(
                    "service.gui.CALL_VIA")
                    + " "
                    + protocolProvider.getAccountID().getDisplayName());

            return sourceContact;
        }
    }

    /**
     * Returns the index of the contact source in the result list.
     *
     * @return the index of the contact source in the result list
     */
    public int getIndex()
    {
        return 0;
    }
}
