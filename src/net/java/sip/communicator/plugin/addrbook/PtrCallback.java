/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.addrbook;

/**
 * Defines the interface for a callback function which is called by the native
 * counterpart of the support for the OS-specific Address Book with a pointer as
 * its argument.
 *
 * @author Lyubomir Marinov
 */
public interface PtrCallback
{
    /**
     * Notifies this <tt>PtrCallback</tt> about a specific pointer.
     *
     * @param ptr the pointer to notify this <tt>PtrCallback</tt> about
     * @return <tt>true</tt> if this <tt>PtrCallback</tt> is to continue being
     * called; otherwise, <tt>false</tt>
     */
    boolean callback(long ptr);
}
