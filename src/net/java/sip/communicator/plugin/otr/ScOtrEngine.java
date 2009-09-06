/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.otr;

import net.java.otr4j.*;
import net.java.otr4j.session.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * 
 * @author George Politis
 *
 */
public interface ScOtrEngine
{
    // Proxy methods OtrEngine.
    public abstract String transformSending(Contact contact, String content);

    public abstract String transformReceiving(Contact contact, String content);

    public abstract void startSession(Contact contact);

    public abstract void endSession(Contact contact);

    public abstract void refreshSession(Contact contact);

    public abstract SessionStatus getSessionStatus(Contact contact);

    // New Methods (Misc)
    public abstract boolean isMessageUIDInjected(String messageUID);

    public abstract void addListener(ScOtrEngineListener listener);

    public abstract void removeListener(ScOtrEngineListener listener);

    // New Methods (Policy management)
    public abstract OtrPolicy getGlobalPolicy();

    public abstract OtrPolicy getContactPolicy(Contact contact);

    public abstract void setGlobalPolicy(OtrPolicy policy);

    public abstract void setContactPolicy(Contact contact, OtrPolicy policy);

    public abstract void launchHelp();

}
