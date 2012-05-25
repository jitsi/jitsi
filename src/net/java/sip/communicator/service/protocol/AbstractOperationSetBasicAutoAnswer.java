/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

import java.util.*;

/**
 * An Abstract Operation Set defining option to unconditionally auto answer
 * incoming calls.
 *
 * @author Damian Minkov
 * @author Vincent Lucas
 */
public abstract class AbstractOperationSetBasicAutoAnswer
    implements OperationSetBasicAutoAnswer
{
    /**
     * Our class logger.
     */
    private static final Logger logger
        = Logger.getLogger(AbstractOperationSetBasicAutoAnswer.class);

    /**
     * The parent Protocol Provider.
     */
    protected final ProtocolProviderService protocolProvider;

    /**
     * Should we unconditionally answer.
     */
    protected boolean answerUnconditional = false;

    /**
     * Should we answer video calls with video.
     */
    protected boolean answerWithVideo = false;

    /**
     * Creates this operation set, loads stored values, populating
     * local variable settings.
     *
     * @param protocolProvider the parent Protocol Provider.
     */
    public AbstractOperationSetBasicAutoAnswer(
            ProtocolProviderService protocolProvider)
    {
        this.protocolProvider = protocolProvider;

        // init values from account props
        load();
    }

    /**
     * Load values from account properties.
     */
    protected void load()
    {
        AccountID acc = protocolProvider.getAccountID();

        answerUnconditional
            = acc.getAccountPropertyBoolean(AUTO_ANSWER_UNCOND_PROP, false);
        answerWithVideo
            = acc.getAccountPropertyBoolean(AUTO_ANSWER_WITH_VIDEO_PROP, false);
    }

    /**
     * Saves values to account properties.
     */
    protected abstract void save();

    /**
     * Clear local settings.
     */
    protected void clearLocal()
    {
        this.answerUnconditional = false;
    }

    /**
     * Clear any previous settings.
     */
    public void clear()
    {
        clearLocal();

        this.answerWithVideo = false;

        save();
    }

    /**
     * Makes a check after creating call locally, should we answer it.
     *
     * @param call The new incoming call to auto-answer if needed.
     * @param isVideoCall Indicates if the remote peer which has created this
     * call wish to have a video call.
     *
     * @return <tt>true</tt> if we have processed and no further processing is
     *          needed, <tt>false</tt> otherwise.
     */
    public boolean autoAnswer(Call call, boolean isVideoCall)
    {
        if(answerUnconditional || satisfyAutoAnswerConditions(call))
        {
            this.answerCall(call, isVideoCall);
            return true;
        }
        return false;
    }


    /**
     * Answers call if peer in correct state or wait for it.
     *
     * @param call The new incoming call to auto-answer if needed.
     * @param isVideoCall Indicates if the remote peer which has created this
     * call wish to have a video call.
     */
    protected void answerCall(Call call, boolean isVideoCall)
    {
        // We are here because we satisfy the conditional, or unconditional is
        // true.
        Iterator<? extends CallPeer> peers = call.getCallPeers();

        while (peers.hasNext())
        {
            new AutoAnswerThread(peers.next(), isVideoCall);
        }
    }

    /**
     * Checks if the call satisfy the auto answer conditions.
     *
     * @param call The new incoming call to auto-answer if needed.
     *
     * @return <tt>true</tt> if the call satisfy the auto answer conditions.
     * <tt>False</tt> otherwise.
     */
    protected abstract boolean satisfyAutoAnswerConditions(Call call);

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
     *
     * @return is auto answer set to unconditional.
     */
    public boolean isAutoAnswerUnconditionalSet()
    {
        return answerUnconditional;
    }

    /**
     * Sets the auto answer with video to video calls.
     *
     * @param answerWithVideo A boolean set to true to activate the auto answer
     * with video when receiving a video call. False otherwise.
     */
    public void setAutoAnswerWithVideo(boolean answerWithVideo)
    {
        this.answerWithVideo = answerWithVideo;
        this.save();
    }

    /**
     * Returns if the auto answer with video to video calls is activated.
     *
     * @return A boolean set to true if the auto answer with video when
     * receiving a video call is activated. False otherwise.
     */
    public boolean isAutoAnswerWithVideoSet()
    {
        return this.answerWithVideo;
    }

    /**
     * Waits for peer to switch into INCOMING_CALL state, before
     * auto-answering the call in a new thread.
     */
    private class AutoAnswerThread
        extends CallPeerAdapter
        implements Runnable
    {
        /**
         * The call peer which has generated the call.
         */
        private CallPeer peer;

        /**
         * Indicates if the remote peer which has created this call wish to have
         * a video call.
         */
        private boolean isVideoCall;

        /**
         * Waits for peer to switch into INCOMING_CALL state, before
         * auto-answering the call in a new thread.
         *
         * @param peer The call peer which has generated the call.
         * @param isVideoCall Indicates if the remote peer which has created
         * this call wish to have a video call.
         */
        public AutoAnswerThread(CallPeer peer, boolean isVideoCall)
        {
            this.peer = peer;
            this.isVideoCall = isVideoCall;

            if (peer.getState() == CallPeerState.INCOMING_CALL)
            {
                new Thread(this).start();
            }
            else
            {
                peer.addCallPeerListener(this);
            }
                
        }

        /**
         * Answers the call.
         */
        public void run()
        {
            OperationSetBasicAutoAnswer opSetBasicAutoAnswer
                = protocolProvider.getOperationSet(
                        OperationSetBasicAutoAnswer.class);
            OperationSetBasicTelephony opSetBasicTelephony
                = protocolProvider.getOperationSet(
                        OperationSetBasicTelephony.class);
            OperationSetVideoTelephony opSetVideoTelephony
                = protocolProvider.getOperationSet(
                        OperationSetVideoTelephony.class);
            try
            {
                // If this is a video call and that the user has configured to
                // answer it with wideo, then create a video call.
                if(this.isVideoCall
                        && answerWithVideo
                        && opSetVideoTelephony != null)
                {
                    System.out.println("CHENZO: VIDEO");
                    opSetVideoTelephony.answerVideoCallPeer(peer);
                }
                // Else sends only audio to the repote peer (the remote peer is
                // still able to send us its video stream).
                else if(opSetBasicTelephony != null)
                {
                    System.out.println("CHENZO: AUDIO");
                    opSetBasicTelephony.answerCallPeer(peer);
                }
            }
            catch (OperationFailedException e)
            {
                logger.error("Could not answer to : " + peer
                    + " caused by the following exception: " + e);
            }
        }

        /**
         * If we peer was not in proper state wait for it and then answer.
         *
         * @param evt the <tt>CallPeerChangeEvent</tt> instance containing the
         */
        public void peerStateChanged(CallPeerChangeEvent evt)
        {
            CallPeerState newState = (CallPeerState) evt.getNewValue();

            if (newState == CallPeerState.INCOMING_CALL)
            {
                evt.getSourceCallPeer().removeCallPeerListener(this);
                new Thread(this).start();
            }
            else if (newState == CallPeerState.DISCONNECTED
                    || newState == CallPeerState.FAILED)
            {
                evt.getSourceCallPeer().removeCallPeerListener(this);
            }
        }
    }
}
