/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.gui.event;

import net.java.sip.communicator.service.gui.Chat;

/**
 * Listens for the instantiation of new chats.
 *
 * @author Damian Johnson
 */
public interface ChatListener
{
    /**
     * Indicates how newly instantiated chats should be handled.
     * @param newChat chat that has just been instantiated
     */
    void chatCreated(Chat newChat);
}