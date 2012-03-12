/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

import java.util.*;

/**
 * An Operation Set defining option
 * to unconditional auto answer incoming calls.
 *
 * @author Damian Minkov
 */
public class OperationSetAutoAnswerJabberImpl
    extends CallPeerAdapter
    implements OperationSetBasicAutoAnswer
{
    /**
     * Our class logger.
     */
    private static final Logger logger =
        Logger.getLogger(OperationSetAutoAnswerJabberImpl.class);

    /**
     * Should we unconditionally answer.
     */
    private boolean answerUnconditional = false;

    /**
     * The parent operation set.
     */
    private OperationSetBasicTelephonyJabberImpl telephonyJabber;

    /**
     * Creates this operation set, loads stored values, populating
     * local variable settings.
     *
     * @param telephonyJabber the parent opset.
     */
    OperationSetAutoAnswerJabberImpl(
        OperationSetBasicTelephonyJabberImpl telephonyJabber)
    {
        this.telephonyJabber = telephonyJabber;

        // init values from account props
        load();
    }

    /**
     * Load values from account properties.
     */
    private void load()
    {
        AccountID acc = telephonyJabber.getProtocolProvider().getAccountID();

        answerUnconditional =
            acc.getAccountPropertyBoolean(AUTO_ANSWER_UNCOND_PROP, false);
    }

    /**
     * Saves values to account properties.
     */
    private void save()
    {
        AccountID acc = telephonyJabber.getProtocolProvider().getAccountID();
        Map<String, String> accProps = acc.getAccountProperties();

        // lets clear anything before saving :)
        accProps.put(AUTO_ANSWER_UNCOND_PROP, null);

        if(answerUnconditional)
        {
            accProps.put(AUTO_ANSWER_UNCOND_PROP, Boolean.TRUE.toString());
        }

        acc.setAccountProperties(accProps);
        JabberActivator.getProtocolProviderFactory().storeAccount(acc);
    }

    /**
     * Sets the auto answer option to unconditionally answer all incoming calls.
     */
    public void setAutoAnswerUnconditional()
    {
        clearLocal();

        this.answerUnconditional = true;

        save();
    }

    /**
     * Is the auto answer option set to unconditionally
     * answer all incoming calls.
     * @return is auto answer set to unconditional.
     */
    public boolean isAutoAnswerUnconditionalSet()
    {
        return answerUnconditional;
    }

    /**
     * Clear local settings.
     */
    private void clearLocal()
    {
        this.answerUnconditional = false;
    }

    /**
     * Clear any previous settings.
     */
    public void clear()
    {
        clearLocal();

        save();
    }

    /**
     * Makes a check after creating call locally, should we answer it.
     * @param call the created call to answer if needed.
     * @return <tt>true</tt> if we have processed and no further processing is
     *          needed, <tt>false</tt> otherwise.
     */
    boolean followCallCheck(
        AbstractCall<CallPeer, ProtocolProviderServiceJabberImpl> call)
    {
        if(!answerUnconditional)
            return false;

        // we are here cause we satisfy the conditional,
        // or unconditional is true
        Iterator<? extends CallPeer> peers = call.getCallPeers();

        while (peers.hasNext())
        {
            final CallPeer peer = peers.next();

            answerPeer(peer);
        }

        return true;
    }

    /**
     * Answers call if peer in correct state or wait for it.
     * @param peer the peer to check and answer.
     */
    private void answerPeer(final CallPeer peer)
    {
        CallPeerState state = peer.getState();

        if (state == CallPeerState.INCOMING_CALL)
        {
            // answer in separate thread, don't block current
            // processing
            new Thread(new Runnable()
            {
                public void run()
                {
                    try
                    {
                        telephonyJabber.answerCallPeer(peer);
                    }
                    catch (OperationFailedException e)
                    {
                        logger.error("Could not answer to : " + peer
                            + " caused by the following exception: " + e);
                    }
                }
            }, getClass().getName()).start();
        }
        else
        {
            peer.addCallPeerListener(this);
        }
    }

    /**
     * If we peer was not in proper state wait for it and then answer.
     * @param evt the <tt>CallPeerChangeEvent</tt> instance containing the
     */
    public void peerStateChanged(CallPeerChangeEvent evt)
    {

        CallPeerState newState = (CallPeerState) evt.getNewValue();

        if (newState == CallPeerState.INCOMING_CALL)
        {
            CallPeer peer = evt.getSourceCallPeer();

            peer.removeCallPeerListener(this);

            answerPeer(peer);
        }
        else if (newState == CallPeerState.DISCONNECTED
                || newState == CallPeerState.FAILED)
        {
            evt.getSourceCallPeer().removeCallPeerListener(this);
        }
    }
}
