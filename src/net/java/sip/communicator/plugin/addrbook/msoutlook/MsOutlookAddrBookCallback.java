/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.addrbook.msoutlook;

/**
 * Defines the interface for a callback function which is called by the Address
 * Book of Microsoft Outlook with a pointer to an <tt>IUnknown</tt> instance as
 * its argument.
 *
 * @author Lyubomir Marinov
 */
public interface MsOutlookAddrBookCallback
{
    /**
     * Notifies this <tt>MsOutlookAddrBookCallback</tt> about a specific
     * <tt>IUnknown</tt>.
     *
     * @param iUnknown the pointer to the <tt>IUnknown</tt> instance to notify
     * this <tt>MsOutlookAddrBookCallback</tt> about
     * @return <tt>true</tt> if this <tt>MsOutlookAddrBookCallback</tt> is to
     * continue being called; otherwise, <tt>false</tt>
     */
    boolean callback(long iUnknown);
}
