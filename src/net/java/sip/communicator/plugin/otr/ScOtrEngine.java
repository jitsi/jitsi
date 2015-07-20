/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.plugin.otr;

import java.security.*;
import java.util.*;

import net.java.otr4j.*;
import net.java.otr4j.session.*;
import net.java.sip.communicator.plugin.otr.OtrContactManager.OtrContact;
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
     * Initializes Smp negotiation.
     * @See <a href="http://en.wikipedia.org/wiki/Socialist_Millionaire_Problem"
     * >http://en.wikipedia.org/wiki/Socialist_Millionaire_Problem</a>
     * 
     * @param contact The contact with whom we want to start the Smp negotiation
     * @param question The question that is asked during the Smp negotiation
     * @param secret The secret answer for the question.
     */
    public abstract void initSmp(
        OtrContact contact, String question, String secret);

    /**
     * Responds to a question that is asked during the Smp negotiation process.
     * @See <a href="http://en.wikipedia.org/wiki/Socialist_Millionaire_Problem"
     * >http://en.wikipedia.org/wiki/Socialist_Millionaire_Problem</a>
     * 
     * @param contact The contact for whom we want to respond to a question
     *                  during the Smp negotiation process.
     * @param receiverTag The instance tag of the intended receiver of the SMP
     *                  response
     * @param question The question that is asked during the Smp negotiation.
     * @param secret The secret answer for the question.
     */
    public abstract void respondSmp(OtrContact contact,
                                    InstanceTag receiverTag,
                                    String question,
                                    String secret);

    /**
     * Aborts the Smp negotiation process.
     * @See <a href="http://en.wikipedia.org/wiki/Socialist_Millionaire_Problem"
     * >http://en.wikipedia.org/wiki/Socialist_Millionaire_Problem</a>
     * 
     * @param contact The contact with whom we want to abort the
     * Smp negotiation process.
     */
    public abstract void abortSmp(OtrContact contact);

    /**
     * Transforms an outgoing message.
     *
     * @param contact the destination {@link OtrContact}.
     * @param content the original message content.
     * @return the transformed message content.
     */
    public abstract String[] transformSending(OtrContact contact, String content);

    /**
     * Transforms an incoming message.
     *
     * @param contact the source {@link OtrContact}.
     * @param content the original message content.
     * @return the transformed message content.
     */
    public abstract String transformReceiving(OtrContact contact, String content);

    /**
     * Starts the Off-the-Record session for the given {@link OtrContact}, if it's
     * not already started.
     *
     * @param contact the {@link OtrContact} with whom we want to start an OTR
     *            session.
     */
    public abstract void startSession(OtrContact contact);

    /**
     * Ends the Off-the-Record session for the given {@link OtrContact}, if it is
     * not already started.
     *
     * @param contact the {@link OtrContact} with whom we want to end the OTR
     *            session.
     */
    public abstract void endSession(OtrContact contact);

    /**
     * Refreshes the Off-the-Record session for the given {@link OtrContact}. If
     * the session does not exist, a new session is established.
     *
     * @param contact the {@link OtrContact} with whom we want to refresh the OTR
     *            session.
     */
    public abstract void refreshSession(OtrContact contact);

    /**
     * Get the outgoing OTRv3 <tt>Session</tt>. This could be the 'master'
     * session as well as a 'slave' session.
     * This method could also be safely used for OTRv2 sessions. In the case of
     * version 2 the master session itself will always be returned.
     *
     * @param contact the {@link OtrContact} for whom we want to get the
     * outgoing OTR session.
     *
     * @return the <tt>Session</tt> that is currently transforming outgoing all
     *            messages.
     */
    public abstract Session getOutgoingSession(OtrContact contact);

    /**
     * Some IM networks always relay all messages to all sessions of a client
     * who is logged in multiple times. OTR version 3 deals with this problem
     * with introducing instance tags.
     * <a href="https://otr.cypherpunks.ca/Protocol-v3-4.0.0.html">
     * https://otr.cypherpunks.ca/Protocol-v3-4.0.0.html</a>
     * <p>
     * Returns a list containing all instances of a session. The 'master'
     * session is always first in the list.
     * 
     * @param contact the {@link OtrContact} for whom we want to get the instances
     * 
     * @return A list of all instances of the session for the specified contact.
     */
    public abstract List<Session> getSessionInstances(OtrContact contact);

    /**
     * Some IM networks always relay all messages to all sessions of a client
     * who is logged in multiple times. OTR version 3 deals with this problem
     * with introducing instance tags.
     * <a href="https://otr.cypherpunks.ca/Protocol-v3-4.0.0.html">
     * https://otr.cypherpunks.ca/Protocol-v3-4.0.0.html</a>
     * <p>
     * When the client wishes to start sending OTRv3 encrypted messages to a
     * specific session of his buddy who is logged in multiple times, he can set
     * the outgoing instance of his buddy by specifying his <tt>InstanceTag</tt>.
     * 
     * @param contact the {@link OtrContact} to whom we want to set the outgoing
     *          instance tag.
     * @param tag the outgoing {@link InstanceTag}
     *
     * @return true if an outgoing session with such {@link InstanceTag} exists
     *          . Otherwise false
     */
    public abstract boolean setOutgoingSession(OtrContact contact, InstanceTag tag);

    /**
     * Gets the {@link ScSessionStatus} for the given {@link OtrContact}.
     *
     * @param contact the {@link OtrContact} whose {@link ScSessionStatus} we are
     *            interested in.
     * @return the {@link ScSessionStatus}.
     */
    public abstract ScSessionStatus getSessionStatus(OtrContact contact);

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

    public abstract PublicKey getRemotePublicKey(OtrContact otrContact);

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
