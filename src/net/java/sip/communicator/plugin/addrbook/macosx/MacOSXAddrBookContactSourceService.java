/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.addrbook.macosx;

import java.util.regex.*;

import net.java.sip.communicator.plugin.addrbook.AddrBookActivator;
import net.java.sip.communicator.service.contactsource.*;

/**
 * Implements <tt>ContactSourceService</tt> for the Address Book of Mac OS X.
 *
 * @author Lyubomir Marinov
 */
public class MacOSXAddrBookContactSourceService
    extends AsyncContactSourceService
{
    /**
     * the Mac OS X address book prefix.
     */
    public static final String MACOSX_ADDR_BOOK_PREFIX
        = "net.java.sip.communicator.plugin.addrbook.MACOSX_ADDR_BOOK_PREFIX";

    static
    {
        System.loadLibrary("jmacosxaddrbook");
    }

    /**
     * The pointer to the native counterpart of this
     * <tt>MacOSXAddrBookContactSourceService</tt>.
     */
    private long ptr;

    /**
     * Initializes a new <tt>MacOSXAddrBookContactSourceService</tt> instance.
     */
    public MacOSXAddrBookContactSourceService()
    {
        ptr = start();
        if (0 == ptr)
            throw new IllegalStateException("ptr");
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
     * which match a specific <tt>query</tt> <tt>Pattern</tt>.
     *
     * @param query the <tt>Pattern</tt> which this
     * <tt>ContactSourceService</tt> is being queried for
     * @return a <tt>ContactQuery</tt> which represents the query of this
     * <tt>ContactSourceService</tt> implementation for the specified
     * <tt>Pattern</tt> and via which the matching <tt>SourceContact</tt>s (if
     * any) will be returned
     * @see ExtendedContactSourceService#queryContactSource(Pattern)
     */
    public ContactQuery queryContactSource(Pattern query)
    {
        MacOSXAddrBookContactQuery mosxabcq
            = new MacOSXAddrBookContactQuery(this, query);

        mosxabcq.start();
        return mosxabcq;
    }

    /**
     * Starts a new native <tt>MacOSXAddrBookContactSourceService</tt> instance.
     *
     * @return a pointer to the newly-started native
     * <tt>MacOSXAddrBookContactSourceService</tt> instance
     */
    private static native long start();

    /**
     * Stops this <tt>ContactSourceService</tt> implementation and prepares it
     * for garbage collection.
     *
     * @see AsyncContactSourceService#stop()
     */
    public synchronized void stop()
    {
        if (0 != ptr)
        {
            stop(ptr);
            ptr = 0;
        }
    }

    /**
     * Returns the global phone number prefix to be used when calling contacts
     * from this contact source.
     *
     * @return the global phone number prefix
     */
    public String getPhoneNumberPrefix()
    {
        return AddrBookActivator.getConfigService()
                .getString(MACOSX_ADDR_BOOK_PREFIX);
    }

    /**
     * Stops a native <tt>MacOSXAddrBookContactSourceService</tt>.
     *
     * @param ptr the pointer to the native
     * <tt>MacOSXAddrBookContactSourceService</tt> to stop
     */
    private static native void stop(long ptr);
}
