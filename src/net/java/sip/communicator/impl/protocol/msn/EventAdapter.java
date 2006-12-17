/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.msn;

import net.sf.jml.*;

/**
 * Contactlist modification listener receives events
 * for successful chngings
 *
 * @author Damian Minkov
 */
public class EventAdapter
        implements EventListener
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
     * Indicates that a contact is successfully added
     * @param contact MsnContact the contact
     */
    public void contactAdded(MsnContact contact){}
    /**
     * Indicates that a contact is successfully added to the group
     * @param contact MsnContact the contact
     * @param group MsnGroup the group
     */
    public void contactAddedInGroup(MsnContact contact, MsnGroup group){}
    /**
     * Indicates successful removing of a contact
     * @param contact MsnContact the removed contact
     */
    public void contactRemoved(MsnContact contact){}
    /**
     * Indicates successful removing of a contact from a group
     * @param contact MsnContact the contact removed
     * @param group MsnGroup the group
     */
    public void contactRemovedFromGroup(MsnContact contact, MsnGroup group){}
    /**
     * Indicates that a group is successfully added
     * @param group MsnGroup the added group
     */
    public void groupAdded(MsnGroup group){}
    /**
     * Indicates that a group is successfully renamed
     * @param group MsnGroup the renmaed group with the new name
     */
    public void groupRenamed(MsnGroup group){}
    /**
     * Indicates successful removing of a group
     * @param id String the id of the removed group
     */
    public void groupRemoved(String id){}

    /**
     * Indicates that we are logged out
     * beacuse account logged in from other location
     */
    public void loggingFromOtherLocation(){}
}
