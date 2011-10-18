/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

/**
 * A listener notified when the title of a call changes.
 *
 * @author Yana Stamcheva
 */
public interface CallTitleListener
{
    /**
     * Called when the title of the given <tt>CallContainer</tt> changes.
     *
     * @param callContainer the <tt>CallContainer</tt>, which title has changed
     */
    public void callTitleChanged(CallPanel callContainer);
}
