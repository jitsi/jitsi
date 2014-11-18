/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.msghistory;

import net.java.sip.communicator.service.protocol.*;

import java.util.*;

/**
 * Adds advanced operation to the message service like inserting/editing
 * messages. Can be used to insert messages when synchronizing history with
 * external source.
 * @author Damian Minkov
 */
public interface MessageHistoryAdvancedService
{
    /**
     * Inserts message to the history. Allows to update the already saved
     * history.
     * @param direction String direction of the message in or out.
     * @param source The source Contact
     * @param destination The destination Contact
     * @param message Message message to be written
     * @param messageTimestamp Date this is the timestamp when was message
     * received that came from the protocol provider
     * @param isSmsSubtype whether message to write is an sms
     */
    public void insertMessage(
        String direction,
        Contact source,
        Contact destination,
        Message message,
        Date messageTimestamp,
        boolean isSmsSubtype);
}
