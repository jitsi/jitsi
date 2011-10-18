/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist;

/**
 * The <tt>FilterQueryListener</tt> is notified when a filter query finishes.
 *
 * @author Yana Stamcheva
 */
public interface FilterQueryListener
{
    /**
     * Indicates that the given <tt>query</tt> has finished with success, i.e.
     * the filter has returned results.
     * @param query the <tt>FilterQuery</tt>, where this listener is registered
     */
    public void filterQuerySucceeded(FilterQuery query);

    /**
     * Indicates that the given <tt>query</tt> has finished with failure, i.e.
     * no results for the filter were found.
     * @param query the <tt>FilterQuery</tt>, where this listener is registered
     */
    public void filterQueryFailed(FilterQuery query);
}
