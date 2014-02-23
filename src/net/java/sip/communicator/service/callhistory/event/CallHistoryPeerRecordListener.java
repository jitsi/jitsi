/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.callhistory.event;

/**
 * Interface for a listener that will be notified when new call peer is added to
 * the history.
 * @author Hristo Terezov
 */
public interface CallHistoryPeerRecordListener
{
    /**
     * This method is called when <tt>CallHistoryPeerRecordEvent</tt> occurred.
     * @param event the event
     */
    public void callPeerRecordReceived(CallHistoryPeerRecordEvent event);
}
