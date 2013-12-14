/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.phonenumbercontactsource;

import net.java.sip.communicator.service.contactsource.*;

/**
 * The <tt>PhoneNumberContactSource</tt> is a source of phone numbers coming
 * from the server stored contact info of all contacts for all protocol
 * providers.
 *
 * @author Yana Stamcheva
 */
public class PhoneNumberContactSource
    implements ContactSourceService
{
    /**
     * Returns DEFAULT_TYPE to indicate that this contact source is a default
     * source.
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
        return PNContactSourceActivator.getResources().getI18NString(
            "plugin.phonenumbercontactsource.DISPLAY_NAME");
    }

    /**
     *  Creates query for the given <tt>queryString</tt>.
     *
     * @param queryString the string to search for
     * @param listener the listener that receives the found contacts.
     * @return the created query
     */
    public ContactQuery createContactQuery(String queryString)
    {
        return createContactQuery(queryString, -1);
    }

    /**
     *  Creates query for the given <tt>queryString</tt>.
     *
     * @param queryString the string to search for
     * @param contactCount the maximum count of result contacts
     * @param listener the listener that receives the found contacts.
     * @return the created query
     */
    public ContactQuery createContactQuery( String queryString,
                                            int contactCount)
    {
        if (queryString == null)
            queryString = "";

        PhoneNumberContactQuery contactQuery
            = new PhoneNumberContactQuery(this, queryString, contactCount);

        return contactQuery;
    }

    /**
     * Returns the index of the contact source in the result list.
     *
     * @return the index of the contact source in the result list
     */
    public int getIndex()
    {
        return -1;
    }
}
