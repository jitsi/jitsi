/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.googlecontacts;

/**
 * Defines the interface for a callback function which is called by the
 * <tt>GoogleContactsService</tt> when a new <tt>GoogleContactsEntry</tt> has
 * been found during a search.
 */
public interface GoogleEntryCallback
{
    /**
     * Notifies this <tt>GoogleEntryCallback</tt> when a new
     * <tt>GoogleContactsEntry</tt> has been found.
     *
     * @param entry the <tt>GoogleContactsEntry</tt> found
     */
    void callback(GoogleContactsEntry entry);
}
