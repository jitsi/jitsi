/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.otr;

import net.java.otr4j.*;
import net.java.otr4j.session.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * This interface must be implemented by classes that provide the Off-the-Record
 * functionality.
 * 
 * @author George Politis
 */
public interface ScOtrEngine
{
    // Proxy methods OtrEngine.

    /**
     * Transforms an outgoing message.
     * 
     * @param contact the destination {@link Contact}.
     * @param content the original message content.
     * @return the transformed message content.
     */
    public abstract String transformSending(Contact contact, String content);

    /**
     * Transforms an incoming message.
     * 
     * @param contact the source {@link Contact}.
     * @param content the original message content.
     * @return the transformed message content.
     */
    public abstract String transformReceiving(Contact contact, String content);

    /**
     * Starts the Off-the-Record session for the given {@link Contact}, if it's
     * not already started.
     * 
     * @param contact the {@link Contact} with whom we want to start an OTR
     *            session.
     */
    public abstract void startSession(Contact contact);

    /**
     * Ends the Off-the-Record session for the given {@link Contact}, if it is
     * not already started.
     * 
     * @param contact the {@link Contact} with whom we want to end the OTR
     *            session.
     */
    public abstract void endSession(Contact contact);

    /**
     * Refreshes the Off-the-Record session for the given {@link Contact}. If
     * the session does not exist, a new session is established.
     * 
     * @param contact the {@link Contact} with whom we want to refresh the OTR
     *            session.
     */
    public abstract void refreshSession(Contact contact);

    /**
     * Gets the {@link SessionStatus} for the given {@link Contact}.
     * 
     * @param contact the {@link Contact} whose {@link SessionStatus} we are
     *            interested in.
     * @return the {@link SessionStatus}.
     */
    public abstract SessionStatus getSessionStatus(Contact contact);

    // New Methods (Misc)

    /**
     * Gets weather the passed in messageUID is injected by the engine or not.
     * If it is injected, it shouldn't be re-transformed.
     *
     * @param messageUID the messageUID which is to be determined whether it is
     * injected by the engine or not
     * @return <tt>true</tt> if the passed in messageUID is injected by the
     * engine; <tt>false</tt>, otherwise
     */
    public abstract boolean isMessageUIDInjected(String messageUID);

    /**
     * Registers an {@link ScOtrEngineListener}.
     * 
     * @param listener the {@link ScOtrEngineListener} to register.
     */
    public abstract void addListener(ScOtrEngineListener listener);

    /**
     * Unregisters an {@link ScOtrEngineListener}.
     * 
     * @param listener the {@link ScOtrEngineListener} to unregister.
     */
    public abstract void removeListener(ScOtrEngineListener listener);

    // New Methods (Policy management)
    /**
     * Gets the global {@link OtrPolicy}.
     *
     * @return the global {@link OtrPolicy}
     */
    public abstract OtrPolicy getGlobalPolicy();

    /**
     * Gets a {@link Contact} specific policy.
     * 
     * @param contact the {@link Contact} whose policy we want.
     * @return The {@link Contact} specific OTR policy. If the specified
     *         {@link Contact} has no policy, the global policy is returned.
     */
    public abstract OtrPolicy getContactPolicy(Contact contact);

    /**
     * Sets the global policy.
     * 
     * @param policy the global policy
     */
    public abstract void setGlobalPolicy(OtrPolicy policy);

    /**
     * Sets the contact specific policy
     * 
     * @param contact the {@link Contact} whose policy we want to set
     * @param policy the {@link OtrPolicy}
     */
    public abstract void setContactPolicy(Contact contact, OtrPolicy policy);

    /**
     * Launches the help page.
     */
    public abstract void launchHelp();
}
