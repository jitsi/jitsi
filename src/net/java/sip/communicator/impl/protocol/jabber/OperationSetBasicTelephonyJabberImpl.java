/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import java.util.*;

import net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.*;
import net.java.sip.communicator.impl.protocol.jabber.extensions.mailnotification.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.provider.*;
import org.jivesoftware.smackx.*;
import org.jivesoftware.smackx.packet.*;

/**
 * Implements all call management logic and exports basic telephony support by
 * implementing OperationSetBasicTelephony.
 *
 * @author Symphorien Wanko
 */
public class OperationSetBasicTelephonyJabberImpl
   extends AbstractOperationSetBasicTelephony
   implements RegistrationStateChangeListener
{

    /**
     * The logger used by this class
     */
    private static final Logger logger
            = Logger.getLogger(OperationSetBasicTelephonyJabberImpl.class);

    /**
     * A reference to the <tt>ProtocolProviderServiceJabberImpl</tt> instance
     * that created us.
     */
    private ProtocolProviderServiceJabberImpl protocolProvider = null;

    /**
     * Contains references for all currently active (non ended) calls.
     */
    private ActiveCallsRepository activeCallsRepository
            = new ActiveCallsRepository(this);

    /**
     * Creates a new instance.
     *
     * @param protocolProvider a reference to the
     * <tt>ProtocolProviderServiceJabberImpl</tt> instance that created us.
     */
    public OperationSetBasicTelephonyJabberImpl(
            ProtocolProviderServiceJabberImpl protocolProvider)
    {

        this.protocolProvider = protocolProvider;
        protocolProvider.addRegistrationStateChangeListener(this);

        //register our home grown Jingle Provider.
        //ProviderManager providerManager = ProviderManager.getInstance();
        //providerManager.addIQProvider(
        //    JingleIQ.ELEMENT_NAME, JingleIQ.NAMESPACE, new JingleIQProvider());

    }

    /**
     * Implementation of method <tt>registrationStateChange</tt> from
     * interface RegistrationStateChangeListener for setting up (or down)
     * our <tt>JingleManager</tt> when an <tt>XMPPConnection</tt> is available
     *
     * @param evt the event received
     */
    public void registrationStateChanged(RegistrationStateChangeEvent evt)
    {
        if ((evt.getNewState() == RegistrationState.REGISTERED))
        {
            // TODO: plug jingle registraion
            if (logger.isInfoEnabled())
                logger.info("Jingle : ON ");
        }
        else if ((evt.getNewState() == RegistrationState.UNREGISTERED))
        {
            // TODO: plug jingle unregistraion
            if (logger.isInfoEnabled())
                logger.info("Jingle : OFF ");
        }
    }

    /**
     * Create a new call and invite the specified CallPeer to it.
     *
     * @param callee the jabber address of the callee that we should invite to a
     * new call.
     * @return CallPeer the CallPeer that will represented by
     *   the specified uri. All following state change events will be
     *   delivered through that call peer. The Call that this
     *   peer is a member of could be retrieved from the
     *   CallParticipatn instance with the use of the corresponding method.
     * @throws OperationFailedException with the corresponding code if we fail
     * to create the call.
     */
    public Call createCall(String callee)
            throws OperationFailedException
    {
        return createOutgoingCall(callee);
    }

    /**
     * Create a new call and invite the specified CallPeer to it.
     *
     * @param callee the address of the callee that we should invite to a
     *   new call.
     * @return CallPeer the CallPeer that will represented by
     *   the specified uri. All following state change events will be
     *   delivered through that call peer. The Call that this
     *   peer is a member of could be retrieved from the
     *   CallParticipatn instance with the use of the corresponding method.
     * @throws OperationFailedException with the corresponding code if we fail
     * to create the call.
     */
    public Call createCall(Contact callee)
            throws OperationFailedException
    {

        return createOutgoingCall(callee.getAddress());
    }

    /**
     * Init and establish the specified call.
     *
     * @param calleeAddress the address of the callee that we'd like to connect
     * with.
     *
     * @return CallPeer the CallPeer that represented by
     *   the specified uri. All following state change events will be
     *   delivered through that call peer. The Call that this
     *   peer is a member of could be retrieved from the
     *   CallParticipatn instance with the use of the corresponding method.
     *
     * @throws OperationFailedException with the corresponding code if we fail
     * to create the call.
     */
    private CallJabberImpl createOutgoingCall(String calleeAddress)
            throws OperationFailedException
    {
        if (logger.isInfoEnabled())
            logger.info("creating outgoing call...");
        if (protocolProvider.getConnection() == null) {
            throw new OperationFailedException(
                    "Failed to create OutgoingJingleSession.\n"
                    + "we don't have a valid XMPPConnection."
                    , OperationFailedException.INTERNAL_ERROR);
        }

        // we determine on which resource the remote user is connected if the
        // resource isn't already provided
        String fullCalleeURI = null;
        if (calleeAddress.indexOf('/') > 0)
        {
            fullCalleeURI = calleeAddress;
        }
        else
        {
            fullCalleeURI = protocolProvider.getConnection().
                    getRoster().getPresence(calleeAddress).getFrom();
        }
        if (fullCalleeURI.indexOf('/') < 0)
        {
            throw new OperationFailedException(
                    "Failed to create OutgoingJingleSession.\n"
                    + "User " + calleeAddress + " is unknown to us."
                    , OperationFailedException.INTERNAL_ERROR);
        }

        try
        {
            // check if the remote client supports telephony.
            DiscoverInfo di = ServiceDiscoveryManager
                    .getInstanceFor(protocolProvider.getConnection())
                    .discoverInfo(fullCalleeURI);
            if (di.containsFeature(ProtocolProviderServiceJabberImpl
                            .URN_XMPP_JINGLE))
            {
                if (logger.isInfoEnabled())
                    logger.info(fullCalleeURI + ": jingle supported ");
            }
            else
            {
                logger.info(calleeAddress + ": jingle not supported ??? ");
                /* FIXME: this is only temporarily disabled
                throw new OperationFailedException(
                        "Failed to create OutgoingJingleSession.\n"
                        + fullCalleeURI + " does not support jingle"
                        , OperationFailedException.INTERNAL_ERROR);
                */
            }
        }
        catch (XMPPException ex)
        {
            logger.warn("could not retrieve info for " + fullCalleeURI, ex);
        }

        //create the actual jingle call
        return null;
    }

    /**
     * Returns an iterator over all currently active calls.
     *
     * @return an iterator over all currently active calls.
     */
    public Iterator<CallJabberImpl> getActiveCalls()
    {
        return activeCallsRepository.getActiveCalls();
    }

    /**
     * Resumes communication with a call peer previously put on hold.
     *
     * @param peer the call peer to put on hold.
     */
    public void putOffHold(CallPeer peer)
    {
        /** @todo implement putOffHold() */
    }

    /**
     * Puts the specified CallPeer "on hold".
     *
     * @param peer the peer that we'd like to put on hold.
     */
    public void putOnHold(CallPeer peer)
    {
        /** @todo implement putOnHold() */
    }

    /**
     * Implements method <tt>hangupCallPeer</tt>
     * from <tt>OperationSetBasicTelephony</tt>.
     *
     * @param peer the peer that we'd like to hang up on.
     * @throws ClassCastException if peer is not an instance of
     * CallPeerJabberImpl.
     *
     * @throws OperationFailedException if we fail to terminate the call.
     *
     * // TODO: ask for suppression of OperationFailedException from the interface.
     * // what happens if hangup fails ? are we forced to continue to talk ? :o)
     */
    public void hangupCallPeer(CallPeer peer)
            throws ClassCastException, OperationFailedException
    {
        CallPeerJabberImpl callPeer
                = (CallPeerJabberImpl)peer;
        /**
         * @todo implement hangupCallPeer
         */
    }



    /**
     * Implements method <tt>answerCallPeer</tt>
     * from <tt>OperationSetBasicTelephony</tt>.
     *
     * @param peer the call peer that we want to answer
     * @throws OperationFailedException if we fails to answer
     */
    public void answerCallPeer(CallPeer peer)
            throws OperationFailedException
    {
        CallPeerJabberImpl callPeer
                = (CallPeerJabberImpl)peer;

        /**
         * @todo implement
         */
    }

    /**
     * Closes all active calls. And releases resources.
     */
    public void shutdown()
    {
        if (logger.isTraceEnabled())
            logger.trace("Ending all active calls. ");
        Iterator<CallJabberImpl> activeCalls
            = this.activeCallsRepository.getActiveCalls();

        // this is fast, but events aren't triggered ...
        //jingleManager.disconnectAllSessions();

        //go through all active calls.
        while(activeCalls.hasNext())
        {
            CallJabberImpl call = activeCalls.next();
            Iterator<CallPeer> callPeers = call.getCallPeers();

            //go through all call peers and say bye to every one.
            while (callPeers.hasNext())
            {
                CallPeer peer = callPeers.next();
                try
                {
                    this.hangupCallPeer(peer);
                }
                catch (Exception ex)
                {
                    logger.warn("Failed to properly hangup peer "
                            + peer
                            , ex);
                }
            }
        }
    }

    /**
     * Implements method <tt>transportClosedOnError</tt> from JingleTransportListener.
     *
     * @param ex the exception accompanying this error
     */
    public void transportClosedOnError(XMPPException ex)
    {
        logger.error("transport closed on error ", ex);
    }
}

