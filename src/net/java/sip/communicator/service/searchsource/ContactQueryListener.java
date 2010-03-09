/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.searchsource;

/**
 * The <tt>ContactQueryListener</tt> notifies interested parties of any change
 * in a <tt>ContactQuery</tt>, e.g. when a new contact has been received or a
 * the query status has changed.
 *
 * @author Yana Stamcheva
 */
public interface ContactQueryListener
{
    /**
     * Indicates that a new contact has been received for a search.
     * @param event the <tt>ContactQueryEvent</tt> containing information
     * about the received <tt>SearchSourceContact</tt>
     */
    public void contactReceived(ContactReceivedEvent event);

    /**
     * Indicates that the status of a search has been changed.
     * @param event the <tt>ContactQueryStatusEvent</tt> containing information
     * about the status change
     */
    public void queryStatusChanged(ContactQueryStatusEvent event);
}
