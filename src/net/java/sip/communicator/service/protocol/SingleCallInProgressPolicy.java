/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import java.util.*;

import org.osgi.framework.*;

import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * Imposes the policy to have one call in progress i.e. to put existing calls on
 * hold when a new call enters in progress.
 *
 * @author Lubomir Marinov
 */
public class SingleCallInProgressPolicy
{

    /**
     * The name of the configuration property which specifies whether
     * <code>SingleCallInProgressPolicy</code> is enabled i.e. whether it should
     * put existing calls on hold when a new call enters in progress.
     */
    private static final String PNAME_SINGLE_CALL_IN_PROGRESS_POLICY_ENABLED
        = "net.java.sip.communicator.impl.protocol.SingleCallInProgressPolicy.enabled";

    /**
     * Implements the listeners interfaces used by this policy.
     */
    private class SingleCallInProgressPolicyListener
        implements CallChangeListener,
                   CallListener,
                   ServiceListener
    {
        /**
         * Stops tracking the state of a specific <code>Call</code> and no
         * longer tries to put it on hold when it ends.
         *
         * @see CallListener#callEnded(CallEvent)
         */
        public void callEnded(CallEvent callEvent)
        {
            SingleCallInProgressPolicy.this.handleCallEvent(
                CallEvent.CALL_ENDED, callEvent);
        }

        /**
         * Does nothing because adding <code>CallPeer<code>s to
         * <code>Call</code>s isn't related to the policy to put existing calls
         * on hold when a new call becomes in-progress and just implements
         * <code>CallChangeListener</code>.
         *
         * @see CallChangeListener#callPeerAdded(CallPeerEvent)
         */
        public void callPeerAdded( CallPeerEvent callPeerEvent)
        {

            /*
             * Not of interest, just implementing CallChangeListener in which
             * only #callStateChanged(CallChangeEvent) is of interest.
             */
        }

        /**
         * Does nothing because removing <code>CallPeer<code>s to
         * <code>Call</code>s isn't related to the policy to put existing calls
         * on hold when a new call becomes in-progress and just implements
         * <code>CallChangeListener</code>.
         *
         * @see CallChangeListener#callPeerRemoved(CallPeerEvent)
         */
        public void callPeerRemoved( CallPeerEvent callPeerEvent)
        {

            /*
             * Not of interest, just implementing CallChangeListener in which
             * only #callStateChanged(CallChangeEvent) is of interest.
             */
        }

        /**
         * Upon a <code>Call</code> changing its state to
         * <code>CallState.CALL_IN_PROGRESS</code>, puts the other existing
         * <code>Call</code>s on hold.
         *
         * @param callChangeEvent the <tt>CallChangeEvent</tt> that we are to
         * deliver.
         *
         * @see CallChangeListener#callStateChanged(CallChangeEvent)
         */
        public void callStateChanged(CallChangeEvent callChangeEvent)
        {
            // we are interested only in CALL_STATE_CHANGEs
            if(!callChangeEvent.getEventType().equals(CallChangeEvent.CALL_STATE_CHANGE))
                return;

            SingleCallInProgressPolicy.this.callStateChanged(callChangeEvent);
        }

        /**
         * Remembers an incoming <code>Call</code> so that it can put the other
         * existing <code>Call</code>s on hold when it changes its state to
         * <code>CallState.CALL_IN_PROGRESS</code>.
         *
         * @see CallListener#incomingCallReceived(CallEvent)
         */
        public void incomingCallReceived(CallEvent callEvent)
        {
            SingleCallInProgressPolicy.this.handleCallEvent(
                CallEvent.CALL_RECEIVED, callEvent);
        }

        /**
         * Remembers an outgoing <code>Call</code> so that it can put the other
         * existing <code>Call</code>s on hold when it changes its state to
         * <code>CallState.CALL_IN_PROGRESS</code>.
         *
         * @see CallListener#outgoingCallCreated(CallEvent)
         */
        public void outgoingCallCreated(CallEvent callEvent)
        {
            SingleCallInProgressPolicy.this.handleCallEvent(
                CallEvent.CALL_INITIATED, callEvent);
        }

        /**
         * Starts/stops tracking the new <code>Call</code>s originating from a
         * specific <code>ProtocolProviderService</code> when it
         * registers/unregisters in order to take them into account when putting
         * existing calls on hold upon a new call entering its in-progress
         * state.
         *
         * @param serviceEvent
         *            the <code>ServiceEvent</code> event describing a change in
         *            the state of a service registration which may be a
         *            <code>ProtocolProviderService</code> supporting
         *            <code>OperationSetBasicTelephony</code> and thus being
         *            able to create new <code>Call</code>s
         */
        public void serviceChanged(ServiceEvent serviceEvent)
        {
            SingleCallInProgressPolicy.this.serviceChanged(serviceEvent);
        }
    }

    /**
     * Our class logger
     */
    private static final Logger logger =
        Logger.getLogger(SingleCallInProgressPolicy.class);

    /**
     * The <code>BundleContext</code> to the Calls of which this policy applies.
     */
    private final BundleContext bundleContext;

    /**
     * The <code>Call</code>s this policy manages i.e. put on hold when one of
     * them enters in progress.
     */
    private final List<Call> calls = new ArrayList<Call>();

    /**
     * The listener utilized by this policy to discover new <code>Call</code>
     * and track their in-progress state.
     */
    private final SingleCallInProgressPolicyListener listener =
        new SingleCallInProgressPolicyListener();

    /**
     * Initializes a new <code>SingleCallInProgressPolicy</code> instance which
     * will apply to the <code>Call</code>s of a specific
     * <code>BundleContext</code>.
     *
     * @param bundleContext
     *            the <code>BundleContext</code> to the
     *            <code>Call<code>s of which the new policy should apply
     */
    public SingleCallInProgressPolicy(BundleContext bundleContext)
    {
        this.bundleContext = bundleContext;

        this.bundleContext.addServiceListener(listener);
    }

    /**
     * Registers a specific <code>Call</code> with this policy in order to have
     * the rules of the latter apply to the former.
     *
     * @param call
     *            the <code>Call</code> to register with this policy in order to
     *            have the rules of the latter apply to the former
     */
    private void addCallListener(Call call)
    {
        synchronized (calls)
        {
            if (!calls.contains(call))
            {
                CallState callState = call.getCallState();

                if ((callState != null)
                    && !callState.equals(CallState.CALL_ENDED))
                {
                    calls.add(call);
                }
            }
        }

        call.addCallChangeListener(listener);
    }

    /**
     * Registers a specific <code>OperationSetBasicTelephony</code> with this
     * policy in order to have the rules of the latter apply to the
     * <code>Call</code>s created by the former.
     *
     * @param telephony
     *            the <code>OperationSetBasicTelephony</code> to register with
     *            this policy in order to have the rules of the latter apply to
     *            the <code>Call</code>s created by the former
     */
    private void addOperationSetBasicTelephonyListener(
        OperationSetBasicTelephony<? extends ProtocolProviderService> telephony)
    {
        telephony.addCallListener(listener);
    }

    /**
     * Handles changes in the state of a <code>Call</code> this policy applies
     * to in order to detect when new calls become in-progress and when the
     * other calls should be put on hold.
     *
     * @param callChangeEvent
     *            a <code>CallChangeEvent</code> value which describes the
     *            <code>Call</code> and the change in its state
     */
    private void callStateChanged(CallChangeEvent callChangeEvent)
    {
        Call call = callChangeEvent.getSourceCall();

        if (CallState.CALL_INITIALIZATION.equals(callChangeEvent.getOldValue())
                && CallState.CALL_IN_PROGRESS.equals(call.getCallState())
                && ProtocolProviderActivator
                    .getConfigurationService()
                        .getBoolean(
                            PNAME_SINGLE_CALL_IN_PROGRESS_POLICY_ENABLED,
                            true))
        {
            synchronized (calls)
            {
                for (Call otherCall : calls)
                {
                    if (!call.equals(otherCall)
                            && CallState.CALL_IN_PROGRESS
                                    .equals(otherCall.getCallState()))
                    {
                        if((call.getCallGroup() != null ||
                            otherCall.getCallGroup() != null) &&
                            call.getCallGroup() != otherCall.getCallGroup())
                            putOnHold(otherCall);
                    }
                }
            }
        }
    }

    /**
     * Performs end-of-life cleanup associated with this instance e.g. removes
     * added listeners.
     */
    public void dispose()
    {
        bundleContext.removeServiceListener(listener);
    }

    /**
     * Handles the start and end of the <code>Call</code>s this policy applies
     * to in order to have them or stop having them put the other existing calls
     * on hold when the former change their states to
     * <code>CallState.CALL_IN_PROGRESS</code>.
     *
     * @param type
     *            one of {@link CallEvent#CALL_ENDED},
     *            {@link CallEvent#CALL_INITIATED} and
     *            {@link CallEvent#CALL_RECEIVED} which described the type of
     *            the event to be handled
     * @param callEvent
     *            a <code>CallEvent</code> value which describes the change and
     *            the <code>Call</code> associated with it
     */
    private void handleCallEvent(int type, CallEvent callEvent)
    {
        Call call = callEvent.getSourceCall();

        switch (type)
        {
        case CallEvent.CALL_ENDED:
            removeCallListener(call);
            break;

        case CallEvent.CALL_INITIATED:
        case CallEvent.CALL_RECEIVED:
            addCallListener(call);
            break;
        }
    }

    /**
     * Puts the <code>CallPeer</code>s of a specific <code>Call</code> on
     * hold.
     *
     * @param call
     *            the <code>Call</code> the <code>CallPeer</code>s of
     *            which are to be put on hold
     */
    private void putOnHold(Call call)
    {
        OperationSetBasicTelephony<?> telephony =
            call.getProtocolProvider()
                .getOperationSet(OperationSetBasicTelephony.class);

        if (telephony != null)
        {
            for (Iterator<? extends CallPeer> peerIter =
                call.getCallPeers(); peerIter.hasNext();)
            {
                CallPeer peer = peerIter.next();
                CallPeerState peerState = peer.getState();

                if (!CallPeerState.DISCONNECTED.equals(peerState)
                    && !CallPeerState.FAILED.equals(peerState)
                    && !CallPeerState.isOnHold(peerState))
                {
                    try
                    {
                        telephony.putOnHold(peer);
                    }
                    catch (OperationFailedException ex)
                    {
                        logger.error("Failed to put " + peer
                            + " on hold.", ex);
                    }
                }
            }
        }
    }

    /**
     * Unregisters a specific <code>Call</code> from this policy in order to
     * have the rules of the latter no longer applied to the former.
     *
     * @param call
     *            the <code>Call</code> to unregister from this policy in order
     *            to have the rules of the latter no longer apply to the former
     */
    private void removeCallListener(Call call)
    {
        call.removeCallChangeListener(listener);

        synchronized (calls)
        {
            calls.remove(call);
        }
    }

    /**
     * Unregisters a specific <code>OperationSetBasicTelephony</code> from this
     * policy in order to have the rules of the latter no longer apply to the
     * <code>Call</code>s created by the former.
     *
     * @param telephony
     *            the <code>OperationSetBasicTelephony</code> to unregister from
     *            this policy in order to have the rules of the latter apply to
     *            the <code>Call</code>s created by the former
     */
    private void removeOperationSetBasicTelephonyListener(
        OperationSetBasicTelephony<? extends ProtocolProviderService> telephony)
    {
        telephony.removeCallListener(listener);
    }

    /**
     * Handles the registering and unregistering of
     * <code>OperationSetBasicTelephony</code> instances in order to apply or
     * unapply the rules of this policy to the <code>Call</code>s originating
     * from them.
     *
     * @param serviceEvent
     *            a <code>ServiceEvent</code> value which described a change in
     *            a OSGi service and which is to be examined for the registering
     *            or unregistering of a <code>ProtocolProviderService</code> and
     *            thus a <code>OperationSetBasicTelephony</code>
     */
    private void serviceChanged(ServiceEvent serviceEvent)
    {
        Object service =
            bundleContext.getService(serviceEvent.getServiceReference());

        if (service instanceof ProtocolProviderService)
        {
            OperationSetBasicTelephony<?> telephony =
                ((ProtocolProviderService) service)
                    .getOperationSet(OperationSetBasicTelephony.class);

            if (telephony != null)
            {
                switch (serviceEvent.getType())
                {
                case ServiceEvent.REGISTERED:
                    addOperationSetBasicTelephonyListener(telephony);
                    break;
                case ServiceEvent.UNREGISTERING:
                    removeOperationSetBasicTelephonyListener(telephony);
                    break;
                }
            }
        }
    }
}
