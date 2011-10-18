/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

/**
 * The <tt>CallInterfaceListener</tt> is notified when the call interface has
 * been started after the call was created.
 *
 * @author Yana Stamcheva
 */
public interface CallInterfaceListener
{
    /**
     * Indicates that the call interface was started.
     */
    public void callInterfaceStarted();
}
