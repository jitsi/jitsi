/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol;

import java.util.*;

import org.osgi.framework.*;

import net.java.sip.communicator.service.protocol.*;
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
     * Implements the listeners interfaces used by this policy.
     */
    private class SingleCallInProgressPolicyListener
        implements CallChangeListener, CallListener, ServiceListener
    {
        /*
         * (non-Javadoc)
         * 
         * @see net.java.sip.communicator.service.protocol.event.CallListener
         * #callEnded(net.java.sip.communicator.service.protocol.event
         * .CallEvent)
         */
        public void callEnded(CallEvent callEvent)
        {
            SingleCallInProgressPolicy.this.handleCallEvent(
                CallEvent.CALL_ENDED, callEvent);
        }

        /*
         * (non-Javadoc)
         * 
         * @see net.java.sip.communicator.service.protocol.event
         * .CallChangeListener#callParticipantAdded(net.java.sip.communicator
         * .service.protocol.event.CallParticipantEvent)
         */
        public void callParticipantAdded(
            CallParticipantEvent callParticipantEvent)
        {

            /*
             * Not of interest, just implementing CallChangeListener in which
             * only #callStateChanged(CallChangeEvent) is of interest.
             */
        }

        /*
         * (non-Javadoc)
         * 
         * @see net.java.sip.communicator.service.protocol.event
         * .CallChangeListener#callParticipantRemoved(net.java.sip.communicator
         * .service.protocol.event.CallParticipantEvent)
         */
        public void callParticipantRemoved(
            CallParticipantEvent callParticipantEvent)
        {

            /*
             * Not of interest, just implementing CallChangeListener in which
             * only #callStateChanged(CallChangeEvent) is of interest.
             */
        }

        /*
         * (non-Javadoc)
         * 
         * @see net.java.sip.communicator.service.protocol.event
         * .CallChangeListener#callStateChanged(net.java.sip.communicator
         * .service.protocol.event.CallChangeEvent)
         */
        public void callStateChanged(CallChangeEvent callChangeEvent)
        {
            SingleCallInProgressPolicy.this.callStateChanged(callChangeEvent);
        }

        /*
         * (non-Javadoc)
         * 
         * @see net.java.sip.communicator.service.protocol.event.CallListener
         * #incomingCallReceived(net.java.sip.communicator.service.protocol
         * .event.CallEvent)
         */
        public void incomingCallReceived(CallEvent callEvent)
        {
            SingleCallInProgressPolicy.this.handleCallEvent(
                CallEvent.CALL_RECEIVED, callEvent);
        }

        /*
         * (non-Javadoc)
         * 
         * @see net.java.sip.communicator.service.protocol.event.CallListener
         * #outgoingCallCreated(net.java.sip.communicator.service.protocol
         * .event.CallEvent)
         */
        public void outgoingCallCreated(CallEvent callEvent)
        {
            SingleCallInProgressPolicy.this.handleCallEvent(
                CallEvent.CALL_INITIATED, callEvent);
        }

        public void serviceChanged(ServiceEvent serviceEvent)
        {
            SingleCallInProgressPolicy.this.serviceChanged(serviceEvent);
        }
    }

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
     * The listener utilized by this policty to discover new <code>Call</code>
     * and track their in-progress state.
     */
    private final SingleCallInProgressPolicyListener listener =
        new SingleCallInProgressPolicyListener();

    /**
     * Initializes a new <code>SingleCallInProgressPolicy</code> instance which
     * will apply to the <code>Call</code>s of a specific
     * <code>BundleContext</code>.
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
     * @param call the <code>Call</code> to register with this policy in order
     *            to have the rules of the latter apply to the former 
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
     * @param telephony the <code>OperationSetBasicTelephony</code> to register
     *            with this policy in order to have the rules of the latter
     *            apply to the <code>Call</code>s created by the former
     */
    private void addOperationSetBasicTelephonyListener(
        OperationSetBasicTelephony telephony)
    {
        telephony.addCallListener(listener);
    }

    /**
     * Handles changes in the state of a <code>Call</code> this policy applies
     * to.
     *
     * @param callChangeEvent a <code>CallChangeEvent</code> value which
     *            describes the <code>Call</code> and the change in its state
     */
    private void callStateChanged(CallChangeEvent callChangeEvent)
    {
        Call call = callChangeEvent.getSourceCall();

        if (CallState.CALL_IN_PROGRESS.equals(call.getCallState())
            && CallState.CALL_INITIALIZATION.equals(callChangeEvent
                .getOldValue()))
        {
            synchronized (calls)
            {
                for (Iterator<Call> callIter = calls.iterator(); callIter
                    .hasNext();)
                {
                    Call otherCall = callIter.next();

                    if (!call.equals(otherCall)
                        && CallState.CALL_IN_PROGRESS.equals(otherCall
                            .getCallState()))
                    {
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
     * to.
     *
     * @param type one of {@link CallEvent#CALL_ENDED},
     *            {@link CallEvent#CALL_INITIATED} and
     *            {@link CallEvent#CALL_RECEIVED} which described the type of
     *            the event to be handled
     * @param callEvent a <code>CallEvent</code> value which describes the
     *            change and the <code>Call</code> associated with it
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
     * Puts the <code>CallParticipant</code>s of a specific <code>Call</code>
     * on hold.
     *
     * @param call the <code>Call</code> the <code>CallParticipant</code>s of
     *            which are to be put on hold.
     */
    private void putOnHold(Call call)
    {
        OperationSetBasicTelephony telephony =
            (OperationSetBasicTelephony) call.getProtocolProvider()
                .getOperationSet(OperationSetBasicTelephony.class);

        if (telephony != null)
        {
            for (Iterator<CallParticipant> participantIter =
                call.getCallParticipants(); participantIter.hasNext();)
            {
                CallParticipant participant = participantIter.next();
                CallParticipantState participantState = participant.getState();

                if (!CallParticipantState.DISCONNECTED.equals(participantState)
                    && !CallParticipantState.FAILED.equals(participantState)
                    && !CallParticipantState.isOnHold(participantState))
                {
                    try
                    {
                        telephony.putOnHold(participant);
                    }
                    catch (OperationFailedException ex)
                    {
                        logger.error("Failed to put " + participant
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
     * @param call the <code>Call</code> to unregister from this policy in order
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
     * @param telephony the <code>OperationSetBasicTelephony</code> to
     *            unregister from this policy in order to have the rules of the
     *            latter apply to the <code>Call</code>s created by the former
     */
    private void removeOperationSetBasicTelephonyListener(
        OperationSetBasicTelephony telephony)
    {
        telephony.removeCallListener(listener);
    }

    /**
     * Handles the registering and unregistering of
     * <code>OperationSetBasicTelephony</code> instances in order to apply or
     * unapply the rules of this policy to them.
     *
     * @param serviceEvent a <code>ServiceEvent</code> value which described
     *            a change in a OSGi service and which is to be examined for the
     *            registering or unregistering of a
     *            <code>ProtocolProviderService</code> and thus a
     *            <code>OperationSetBasicTelephony</code>
     */
    private void serviceChanged(ServiceEvent serviceEvent)
    {
        Object service =
            bundleContext.getService(serviceEvent.getServiceReference());

        if (service instanceof ProtocolProviderService)
        {
            OperationSetBasicTelephony telephony =
                (OperationSetBasicTelephony) ((ProtocolProviderService) service)
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
