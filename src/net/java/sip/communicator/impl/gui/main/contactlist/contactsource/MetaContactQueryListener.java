/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist.contactsource;

/**
 * The <tt>MetaContactQueryListener</tt> listens for events coming from a
 * <tt>MetaContactListService</tt> filtering.
 *
 * @author Yana Stamcheva
 */
public interface MetaContactQueryListener
{
    /**
     * Indicates that a <tt>MetaContact</tt> has been received for a search in
     * the <tt>MetaContactListService</tt>.
     * @param event the received <tt>MetaContactQueryEvent</tt>
     */
    public void metaContactReceived(MetaContactQueryEvent event);

    /**
     * Indicates that a <tt>MetaGroup</tt> has been received from a search in
     * the <tt>MetaContactListService</tt>.
     * @param event the <tt>MetaGroupQueryEvent</tt> that has been received
     */
    public void metaGroupReceived(MetaGroupQueryEvent event);

    /**
     * Indicates that a query has changed its status.
     * @param event the <tt>MetaContactQueryStatusEvent</tt> that notified us
     */
    public void metaContactQueryStatusChanged(MetaContactQueryStatusEvent event);
}
