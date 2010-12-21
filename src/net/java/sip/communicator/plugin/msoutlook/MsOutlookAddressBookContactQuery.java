/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.msoutlook;

import java.util.*;

import net.java.sip.communicator.service.contactsource.*;

/**
 * Implements <tt>ContactQuery</tt> for the Address Book of Microsoft Outlook.
 *
 * @author Lyubomir Marinov
 */
public class MsOutlookAddressBookContactQuery
    extends AbstractContactQuery<MsOutlookAddressBookContactSourceService>
{
    /**
     * Initializes a new <tt>MsOutlookAddressBookContactQuery</tt> instance to
     * be performed by a specific
     * <tt>MsOutlookAddressBookContactSourceService</tt>.
     *
     * @param msoabcss the <tt>MsOutlookAddressBookContactSourceService</tt>
     * which is to perform the new <tt>ContactQuery</tt>
     */
    public MsOutlookAddressBookContactQuery(
            MsOutlookAddressBookContactSourceService msoabcss)
    {
        super(msoabcss);
    }

    /**
     * Cancels this <tt>ContactQuery</tt>.
     *
     * @see ContactQuery#cancel()
     */
    public void cancel()
    {
        // TODO Auto-generated method stub
    }

    /**
     * Gets the <tt>List</tt> of <tt>SourceContact</tt>s which match this
     * <tt>ContactQuery</tt>.
     *
     * @return the <tt>List</tt> of <tt>SourceContact</tt>s which match this
     * <tt>ContactQuery</tt>
     * @see ContactQuery#getQueryResults()
     */
    public List<SourceContact> getQueryResults()
    {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the status of this <tt>ContactQuery</tt> which can be one of the
     * <tt>QUERY_XXX</tt> constants defined by <tt>ContactQuery</tt>.
     *
     * @return the status of this <tt>ContactQuery</tt> which can be one of the
     * <tt>QUERY_XXX</tt> constants defined by <tt>ContactQuery</tt>
     * @see ContactQuery#getStatus()
     */
    public int getStatus()
    {
        // TODO Auto-generated method stub
        return 0;
    }
}
