/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.addrbook.macosx;

import net.java.sip.communicator.plugin.addrbook.*;

/**
 * Implements <tt>ContactQuery</tt> for the Address Book of Mac OS X.
 *
 * @author Lyubomir Marinov
 */
public class MacOSXAddrBookContactQuery
    extends AsyncContactQuery<MacOSXAddrBookContactSourceService>
{

    /**
     * Initializes a new <tt>MacOSXAddrBookContactQuery</tt> which is to perform
     * a specific <tt>query</tt> in the Address Book of Mac OS X on behalf of a
     * specific <tt>MacOSXAddrBookContactSourceService</tt>.
     *
     * @param contactSource the <tt>MacOSXAddrBookContactSourceService</tt>
     * which is to perform the new <tt>ContactQuery</tt> instance
     * @param query the <tt>String</tt> for which <tt>contactSource</tt> i.e.
     * the Address Book of Mac OS X is being queried
     */
    public MacOSXAddrBookContactQuery(
            MacOSXAddrBookContactSourceService contactSource,
            String query)
    {
        super(contactSource, query);
    }

    /**
     * Performs this <tt>AsyncContactQuery</tt> in a background <tt>Thread</tt>.
     *
     * @see AsyncContactQuery#run()
     */
    protected void run()
    {
        // TODO Auto-generated method stub
    }
}
