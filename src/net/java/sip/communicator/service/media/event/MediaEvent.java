/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.media.event;

import net.java.sip.communicator.service.protocol.*;

/**
 * 
 * @author Martin Andre
 */
public class MediaEvent
    extends java.util.EventObject
{
    CallParticipant callParticipant;
    
    public MediaEvent(Object source)
    {
        super(source);
    }
}