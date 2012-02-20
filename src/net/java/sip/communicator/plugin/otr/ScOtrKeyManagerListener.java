/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.otr;

import net.java.sip.communicator.service.protocol.*;

/**
 * 
 * @author George Politis
 *
 */
public interface ScOtrKeyManagerListener
{
    public abstract void contactVerificationStatusChanged(Contact contact);
}
