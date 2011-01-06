/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.addrbook.macosx;

import net.java.sip.communicator.service.contactsource.*;

/**
 * Implements <tt>ContactSourceService</tt> for the Address Book of Mac OS X.
 *
 * @author Lyubomir Marinov
 */
public class MacOSXAddrBookContactSourceService
    implements ContactSourceService
{
    static
    {
        /*
         * Attempt to load the JNI counterpart as close to the startup of the
         * addrbook bundle as possible so that any possible problem is
         * discovered and reported as early as possible and outside of the UI
         * which uses MacOSXAddrBookContactSourceService later on.
         */
        System.loadLibrary("jmacosxaddrbook");
    }

    /**
     * Initializes a new <tt>MacOSXAddrBookContactSourceService</tt> instance.
     */
    public MacOSXAddrBookContactSourceService()
    {
    }

    /**
     * Gets a human-readable <tt>String</tt> which names this
     * <tt>ContactSourceService</tt> implementation.
     *
     * @return a human-readable <tt>String</tt> which names this
     * <tt>ContactSourceService</tt> implementation
     * @see ContactSourceService#getDisplayName()
     */
    public String getDisplayName()
    {
        return "Address Book";
    }

    /**
     * Gets a <tt>String</tt> which uniquely identifies the instances of the
     * <tt>MacOSXAddrBookContactSourceService</tt> implementation.
     *
     * @return a <tt>String</tt> which uniquely identifies the instances of the
     * <tt>MacOSXAddrBookContactSourceService</tt> implementation
     * @see ContactSourceService#getIdentifier()
     */
    public String getIdentifier()
    {
        return "MacOSXAddressBook";
    }

    /**
     * Queries this <tt>ContactSourceService</tt> for <tt>SourceContact</tt>s
     * which match a specific <tt>query</tt> <tt>String</tt>.
     *
     * @param query the <tt>String</tt> which this <tt>ContactSourceService</tt>
     * is being queried for
     * @return a <tt>ContactQuery</tt> which represents the query of this
     * <tt>ContactSourceService</tt> implementation for the specified
     * <tt>String</tt> and via which the matching <tt>SourceContact</tt>s (if
     * any) will be returned
     * @see ContactSourceService#queryContactSource(String)
     */
    public ContactQuery queryContactSource(String query)
    {
        MacOSXAddrBookContactQuery mosxabcq
            = new MacOSXAddrBookContactQuery(this, query);

        mosxabcq.start();
        return mosxabcq;
    }
}
