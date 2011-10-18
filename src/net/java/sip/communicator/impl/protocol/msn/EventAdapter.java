/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.msn;

import net.sf.jml.*;

/**
 * Contactlist modification listener receives events
 * for successful changing
 *
 * @author Damian Minkov
 */
public class EventAdapter
        implements MsnContactListEventListener
{
    /**
     * Message is successfully delivered
     * @param transactionID int the transaction that send the message
     */
    public void messageDelivered(int transactionID){}
    /**
     * Message is not delivered
     * @param transactionID int the transaction that send the message
     */
    public void messageDeliveredFailed(int transactionID){}
    /**
     * Indicates that a group is successfully renamed
     * @param group MsnGroup the renamed group with the new name
     */
    public void groupRenamed(MsnGroup group){}

    /**
     * Indicates that we are logged out
     * because account logged in from other location
     */
    public void loggingFromOtherLocation(){}
}
