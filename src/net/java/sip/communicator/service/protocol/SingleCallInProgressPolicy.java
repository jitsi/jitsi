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
package net.java.sip.communicator.service.protocol;

import static net.java.sip.communicator.service.protocol.OperationSetBasicTelephony.*;

import java.util.*;
import java.util.regex.*;

import net.java.sip.communicator.service.calendar.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.configuration.*;
import org.osgi.framework.*;

/**
 * Imposes the policy to have one call in progress i.e. to put existing calls on
 * hold when a new call enters in progress.
 *
 * @author Lyubomir Marinov
 * @author Damian Minkov
 * @author Hristo Terezov
 */
public class SingleCallInProgressPolicy
{
    /**
     * Account property to enable per account rejecting calls if the account
     * presence is in DND or OnThePhone status.
     */
    private static final String ACCOUNT_PROPERTY_REJECT_IN_CALL_ON_DND
        = "RejectIncomingCallsWhenDnD";

    /**
     * Our class logger
     */
    private static final Logger logger
        = Logger.getLogger(SingleCallInProgressPolicy.class);

    /**
     * The name of the configuration property which specifies whether call
     * waiting is disabled i.e. whether it should reject new incoming calls when
     * there are other calls already in progress.
     */
    private static final String PNAME_CALL_WAITING_DISABLED
        = "net.java.sip.communicator.impl.protocol.CallWaitingDisabled";

    /**
     * The name of the configuration property which specifies whether
     * <tt>OnThePhoneStatusPolicy</tt> is enabled i.e. whether it should set the
     * presence statuses of online accounts to &quot;On the phone&quot; when at
     * least one <tt>Call</tt> is in progress.
     */
    private static final String PNAME_ON_THE_PHONE_STATUS_ENABLED
        = "net.java.sip.communicator.impl.protocol.OnThePhoneStatusPolicy"
            + ".enabled";

    /**
     * Global property which will enable rejecting incoming calls for all
     * accounts, if the account is in DND or OnThePhone status.
     */
    private static final String PNAME_REJECT_IN_CALL_ON_DND
        = "net.java.sip.communicator.impl.protocol."
            + ACCOUNT_PROPERTY_REJECT_IN_CALL_ON_DND;

    /**
     * The name of the configuration property which specifies whether
     * <tt>SingleCallInProgressPolicy</tt> is enabled i.e. whether it should put
     * existing calls on hold when a new call enters in progress.
     */
    private static final String PNAME_SINGLE_CALL_IN_PROGRESS_POLICY_ENABLED
        = "net.java.sip.communicator.impl.protocol.SingleCallInProgressPolicy"
            + ".enabled";

    /**
     * The <tt>BundleContext</tt> to the Calls of which this policy applies.
     */
    private final BundleContext bundleContext;

    /**
     * The <tt>Call</tt>s this policy manages i.e. put on hold when one of them
     * enters in progress.
     */
    private final List<Call> calls = new ArrayList<Call>();

    /**
     * The listener utilized by this policy to discover new <tt>Call</tt> and
     * track their in-progress state.
     */
    private final SingleCallInProgressPolicyListener listener
        = new SingleCallInProgressPolicyListener();

    /**
     * The implementation of the policy to have the presence statuses of online
     * accounts (i.e. registered <tt>ProtocolProviderService</tt>s) set to
     * &quot;On the phone&quot; when at least one <tt>Call</tt> is in progress.
     */
    private final OnThePhoneStatusPolicy onThePhoneStatusPolicy
        = new OnThePhoneStatusPolicy();

    /**
     * Initializes a new <tt>SingleCallInProgressPolicy</tt> instance which
     * will apply to the <tt>Call</tt>s of a specific <tt>BundleContext</tt>.
     *
     * @param bundleContext the <tt>BundleContext</tt> to the <tt>Call<tt>s of
     * which the new policy should apply
     */
    public SingleCallInProgressPolicy(BundleContext bundleContext)
    {
        this.bundleContext = bundleContext;

        this.bundleContext.addServiceListener(listener);
    }

    /**
     * Registers a specific <tt>Call</tt> with this policy in order to have the
     * rules of the latter apply to the former.
     *
     * @param call the <tt>Call</tt> to register with this policy in order to
     * have the rules of the latter apply to the former
     */
    private void addCallListener(Call call)
    {
        if(logger.isTraceEnabled())
        {
            logger.trace("Add call change listener");
        }

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
     * Registers a specific <tt>OperationSetBasicTelephony</tt> with this policy
     * in order to have the rules of the latter apply to the <tt>Call</tt>s
     * created by the former.
     *
     * @param telephony the <tt>OperationSetBasicTelephony</tt> to register with
     * this policy in order to have the rules of the latter apply to the
     * <tt>Call</tt>s created by the former
     */
    private void addOperationSetBasicTelephonyListener(
            OperationSetBasicTelephony<? extends ProtocolProviderService>
                telephony)
    {
        if(logger.isTraceEnabled())
        {
            logger.trace("Call listener added to provider.");
        }
        telephony.addCallListener(listener);
    }

    /**
     * Handles changes in the state of a <tt>Call</tt> this policy applies to in
     * order to detect when new calls become in-progress and when the other
     * calls should be put on hold.
     *
     * @param ev a <tt>CallChangeEvent</tt> value which describes the
     * <tt>Call</tt> and the change in its state
     */
    private void callStateChanged(CallChangeEvent ev)
    {
        Call call = ev.getSourceCall();
        if(logger.isTraceEnabled())
        {
            logger.trace("Call state changed.");
        }


        if (CallState.CALL_INITIALIZATION.equals(ev.getOldValue())
                && CallState.CALL_IN_PROGRESS.equals(call.getCallState())
                && ProtocolProviderActivator
                    .getConfigurationService()
                        .getBoolean(
                                PNAME_SINGLE_CALL_IN_PROGRESS_POLICY_ENABLED,
                                true))
        {
            CallConference conference = call.getConference();

            synchronized (calls)
            {
                for (Call otherCall : calls)
                {
                    if (!call.equals(otherCall)
                            && CallState.CALL_IN_PROGRESS.equals(
                                    otherCall.getCallState()))
                    {
                        /*
                         * Only put on hold calls which are visually distinctive
                         * from the specified call i.e. do not put on hold calls
                         * which participate in the same telephony conference as
                         * the specified call.
                         */
                        boolean putOnHold;
                        CallConference otherConference
                            = otherCall.getConference();

                        if (conference == null)
                            putOnHold = (otherConference == null);
                        else
                            putOnHold = (conference != otherConference);
                        if (putOnHold)
                            putOnHold(otherCall);
                    }
                }
            }
        }

        /*
         * Forward to onThePhoneStatusPolicy for which we are proxying the
         * Call-related events.
         */
        onThePhoneStatusPolicy.callStateChanged(ev);
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
     * Handles the start and end of the <tt>Call</tt>s this policy applies to in
     * order to have them or stop having them put the other existing calls on
     * hold when the former change their states to
     * <tt>CallState.CALL_IN_PROGRESS</tt>.
     * <p>
     * Also handles call rejection via "busy here" according to the call policy.
     * </p>
     *
     * @param type one of {@link CallEvent#CALL_ENDED},
     * {@link CallEvent#CALL_INITIATED} and {@link CallEvent#CALL_RECEIVED}
     * which describes the type of the event to be handled
     * @param ev a <tt>CallEvent</tt> value which describes the change and the
     * <tt>Call</tt> associated with it
     */
    private void handleCallEvent(int type, CallEvent ev)
    {
        Call call = ev.getSourceCall();

        if(logger.isTraceEnabled())
        {
            logger.trace("Call event fired.");
        }
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

        /*
         * Forward to onThePhoneStatusPolicy for which we are proxying the
         * Call-related events.
         */
        onThePhoneStatusPolicy.handleCallEvent(type, ev);
    }

    /**
     * Notifies this instance that an incoming <tt>Call</tt> has been received.
     *
     * @param ev a <tt>CallEvent</tt> which describes the received incoming
     * <tt>Call</tt>
     */
    private void incomingCallReceived(CallEvent ev)
    {
        Call call = ev.getSourceCall();

        // check whether we should hangup this call saying we are busy
        // already on call
        if (CallState.CALL_INITIALIZATION.equals(call.getCallState()))
        {
            ConfigurationService config
                = ProtocolProviderActivator.getConfigurationService();

            if (config.getBoolean(PNAME_CALL_WAITING_DISABLED, false))
            {
                boolean rejectCallWithBusyHere = false;

                synchronized (calls)
                {
                    for (Call otherCall : calls)
                    {
                        if (!call.equals(otherCall)
                                && CallState.CALL_IN_PROGRESS.equals(
                                        otherCall.getCallState()))
                        {
                            rejectCallWithBusyHere = true;
                            break;
                        }
                    }
                }
                if (rejectCallWithBusyHere)
                {
                    rejectCallWithBusyHere(call);
                    return;
                }
            }

            ProtocolProviderService provider = call.getProtocolProvider();

            if (config.getBoolean(PNAME_REJECT_IN_CALL_ON_DND, false)
                    || provider.getAccountID().getAccountPropertyBoolean(
                            ACCOUNT_PROPERTY_REJECT_IN_CALL_ON_DND,
                            false))
            {
                OperationSetPresence presence
                    = provider.getOperationSet(OperationSetPresence.class);

                // if our provider has no presence op set, lets search for
                // connected provider which will have
                if(presence == null)
                {
                    // There is no presence OpSet. Let's check the connected
                    // CUSAX provider if available
                    OperationSetCusaxUtils cusaxOpSet
                        = provider.getOperationSet(
                                OperationSetCusaxUtils.class);

                    if(cusaxOpSet != null)
                    {
                        ProtocolProviderService linkedCusaxProvider
                            = cusaxOpSet.getLinkedCusaxProvider();

                        if(linkedCusaxProvider != null)
                        {
                            // we found the provider, let's take its presence
                            // opset
                            presence
                                = linkedCusaxProvider.getOperationSet(
                                        OperationSetPresence.class);
                        }

                    }
                }

                if(presence != null)
                {
                    int presenceStatus
                        = (presence == null)
                            ? PresenceStatus.AVAILABLE_THRESHOLD
                            : presence.getPresenceStatus().getStatus();

                    // between AVAILABLE and EXTENDED AWAY (>20, <= 31) are
                    // the busy statuses as DND and On the phone
                    if (presenceStatus > PresenceStatus.ONLINE_THRESHOLD
                        && presenceStatus <=
                                PresenceStatus.EXTENDED_AWAY_THRESHOLD)
                    {
                        rejectCallWithBusyHere(call);
                        return;
                    }
                }
            }
        }

        handleCallEvent(CallEvent.CALL_RECEIVED, ev);
    }

    /**
     * Puts the <tt>CallPeer</tt>s of a specific <tt>Call</tt> on hold.
     *
     * @param call the <tt>Call</tt> the <tt>CallPeer</tt>s of which are to be
     * put on hold
     */
    private void putOnHold(Call call)
    {
        OperationSetBasicTelephony<?> telephony
            = call.getProtocolProvider().getOperationSet(
                    OperationSetBasicTelephony.class);

        if (telephony != null)
        {
            for (Iterator<? extends CallPeer> peerIter = call.getCallPeers();
                    peerIter.hasNext();)
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
                        logger.error("Failed to put " + peer + " on hold.", ex);
                    }
                }
            }
        }
    }

    /**
     * Rejects a <tt>call</tt> with busy here code.
     *
     * @param call the call to reject.
     */
    private void rejectCallWithBusyHere(Call call)
    {
        // We're interested in one-to-one incoming calls.
        if(call.getCallPeerCount() == 1)
        {
            CallPeer peer = call.getCallPeers().next();

            OperationSetBasicTelephony<?> telephony
                = call.getProtocolProvider().getOperationSet(
                        OperationSetBasicTelephony.class);

            if (telephony != null)
            {
                try
                {
                    telephony.hangupCallPeer(
                            peer,
                            HANGUP_REASON_BUSY_HERE,
                            null);
                }
                catch (OperationFailedException ex)
                {
                    logger.error("Failed to reject " + peer, ex);
                }
            }
        }
    }

    /**
     * Unregisters a specific <tt>Call</tt> from this policy in order to have
     * the rules of the latter no longer applied to the former.
     *
     * @param call the <tt>Call</tt> to unregister from this policy in order to
     * have the rules of the latter no longer apply to the former
     */
    private void removeCallListener(Call call)
    {
        if(logger.isTraceEnabled())
        {
            logger.trace("Remove call change listener.");
        }

        call.removeCallChangeListener(listener);

        synchronized (calls)
        {
            calls.remove(call);
        }
    }

    /**
     * Unregisters a specific <tt>OperationSetBasicTelephony</tt> from this
     * policy in order to have the rules of the latter no longer apply to the
     * <tt>Call</tt>s created by the former.
     *
     * @param telephony the <tt>OperationSetBasicTelephony</tt> to unregister
     * from this policy in order to have the rules of the latter apply to the
     * <tt>Call</tt>s created by the former
     */
    private void removeOperationSetBasicTelephonyListener(
            OperationSetBasicTelephony<? extends ProtocolProviderService>
                telephony)
    {
        telephony.removeCallListener(listener);
    }

    /**
     * Handles the registering and unregistering of
     * <tt>OperationSetBasicTelephony</tt> instances in order to apply or
     * unapply the rules of this policy to the <tt>Call</tt>s originating from
     * them.
     *
     * @param ev a <tt>ServiceEvent</tt> value which described a change in an
     * OSGi service and which is to be examined for the registering or
     * unregistering of a <tt>ProtocolProviderService</tt> and thus a
     * <tt>OperationSetBasicTelephony</tt>
     */
    private void serviceChanged(ServiceEvent ev)
    {
        Object service = bundleContext.getService(ev.getServiceReference());

        if (service instanceof ProtocolProviderService)
        {
            if(logger.isTraceEnabled())
            {
                logger.trace("Protocol provider service changed.");
            }

            OperationSetBasicTelephony<?> telephony
                = ((ProtocolProviderService) service).getOperationSet(
                        OperationSetBasicTelephony.class);

            if (telephony != null)
            {
                switch (ev.getType())
                {
                case ServiceEvent.REGISTERED:
                    addOperationSetBasicTelephonyListener(telephony);
                    break;
                case ServiceEvent.UNREGISTERING:
                    removeOperationSetBasicTelephonyListener(telephony);
                    break;
                }
            }
            else
            {
                if(logger.isTraceEnabled())
                {
                    logger.trace("The protocol provider service doesn't support "
                        + "telephony.");
                }

            }
        }
    }

    /**
     * Implements the policy to have the presence statuses of online accounts
     * (i.e. registered <tt>ProtocolProviderService</tt>s) set to
     * &quot;On the phone&quot; when at least one <tt>Call</tt> is in progress.
     *
     * @author Lyubomir Marinov
     */
    private class OnThePhoneStatusPolicy
    {
        /**
         * The regular expression which removes whitespace from the
         * <tt>statusName</tt> property value of <tt>PresenceStatus</tt>
         * instances in order to recognize the <tt>PresenceStatus</tt> which
         * represents &quot;On the phone&quot;.
         */
        private final Pattern presenceStatusNameWhitespace
            = Pattern.compile("\\p{Space}");

        /**
         * The <tt>PresenceStatus</tt>es of <tt>ProtocolProviderService</tt>s
         * before they were changed to &quot;On the phone&quot; remembered so
         * that they can be restored after the last <tt>Call</tt> in progress
         * ends.
         */
        private final Map<ProtocolProviderService,PresenceStatus>
            presenceStatuses
                = Collections.synchronizedMap(
                        new WeakHashMap<ProtocolProviderService,PresenceStatus>());

        /**
         * Notifies this instance that the <tt>callState</tt> of a specific
         * <tt>Call</tt> has changed.
         *
         * @param ev a <tt>CallChangeEvent</tt> which represents the details of
         * the notification such as the affected <tt>Call</tt> and its old and
         * new <tt>CallState</tt>s
         */
        public void callStateChanged(CallChangeEvent ev)
        {
            if(logger.isTraceEnabled())
            {
                logger.trace("Call state changed.[2]");
            }

            Call call = ev.getSourceCall();
            Object oldCallState = ev.getOldValue();
            Object newCallState = call.getCallState();

            if ((CallState.CALL_INITIALIZATION.equals(oldCallState)
                        && CallState.CALL_IN_PROGRESS.equals(newCallState))
                    || (CallState.CALL_IN_PROGRESS.equals(oldCallState)
                            && CallState.CALL_ENDED.equals(newCallState)))
            {
                run();
            }
            else
            {
                if(logger.isTraceEnabled())
                {
                    logger.trace("Not applicable call state.");
                }

            }
        }

        /**
         * Finds the first <tt>PresenceStatus</tt> among the set of
         * <tt>PresenceStatus</tt>es supported by a specific
         * <tt>OperationSetPresence</tt> which represents
         * &quot;On the phone&quot;.
         *
         * @param presence the <tt>OperationSetPresence</tt> which represents
         * the set of supported <tt>PresenceStatus</tt>es
         * @return the first <tt>PresenceStatus</tt> among the set of
         * <tt>PresenceStatus</tt>es supported by <tt>presence</tt> which
         * represents &quot;On the phone&quot; if such a <tt>PresenceStatus</tt>
         * was found; otherwise, <tt>null</tt>
         */
        private PresenceStatus findOnThePhonePresenceStatus(
                OperationSetPresence presence)
        {
            for (Iterator<PresenceStatus> i = presence.getSupportedStatusSet();
                    i.hasNext();)
            {
                PresenceStatus presenceStatus = i.next();

                if (presenceStatusNameWhitespace
                        .matcher(presenceStatus.getStatusName())
                            .replaceAll("")
                                .equalsIgnoreCase("OnThePhone"))
                {
                    return presenceStatus;
                }
            }
            return null;
        }

        private PresenceStatus forgetPresenceStatus(ProtocolProviderService pps)
        {
            return presenceStatuses.remove(pps);
        }

        private void forgetPresenceStatuses()
        {
            presenceStatuses.clear();
        }

        /**
         * Notifies this instance that a new outgoing <tt>Call</tt> was
         * initiated, an incoming <tt>Call</tt> was received or an existing
         * <tt>Call</tt> ended.
         *
         * @param type one of {@link CallEvent#CALL_ENDED},
         * {@link CallEvent#CALL_INITIATED} and {@link CallEvent#CALL_RECEIVED}
         * which describes the type of the event to be handled
         * @param ev a <tt>CallEvent</tt> value which describes the change and
         * the <tt>Call</tt> associated with it
         */
        public void handleCallEvent(int type, CallEvent ev)
        {
            run();
        }

        /**
         * Determines whether there is at least one existing <tt>Call</tt> which
         * is currently in progress i.e. determines whether the local user is
         * currently on the phone.
         *
         * @return <tt>true</tt> if there is at least one existing <tt>Call</tt>
         * which is currently in progress i.e. if the local user is currently on
         * the phone; otherwise, <tt>false</tt>
         */
        private boolean isOnThePhone()
        {
            synchronized (calls)
            {
                for (Call call : calls)
                {
                    if (CallState.CALL_IN_PROGRESS.equals(call.getCallState()))
                        return true;
                }
            }
            return false;
        }

        /**
         * Invokes
         * {@link OperationSetPresence#publishPresenceStatus(PresenceStatus,
         * String)} on a specific <tt>OperationSetPresence</tt> with a specific
         * <tt>PresenceStatus</tt> and catches any exceptions.
         *
         * @param presence the <tt>OperationSetPresence</tt> on which the method
         * is to be invoked
         * @param presenceStatus the <tt>PresenceStatus</tt> to provide as the
         * respective method argument value
         */
        private void publishPresenceStatus(
                OperationSetPresence presence,
                PresenceStatus presenceStatus)
        {
            try
            {
                presence.publishPresenceStatus(presenceStatus, null);
            }
            catch (Throwable t)
            {
                if (t instanceof InterruptedException)
                    Thread.currentThread().interrupt();
                else if (t instanceof ThreadDeath)
                    throw (ThreadDeath) t;
            }
        }

        private PresenceStatus rememberPresenceStatus(
                ProtocolProviderService pps,
                PresenceStatus presenceStatus)
        {
            return presenceStatuses.put(pps, presenceStatus);
        }

        /**
         * Finds the first <tt>PresenceStatus</tt> among the set of
         * <tt>PresenceStatus</tt>es supported by a specific
         * <tt>OperationSetPresence</tt> which represents
         * &quot;In meeting&quot;.
         *
         * @param presence the <tt>OperationSetPresence</tt> which represents
         * the set of supported <tt>PresenceStatus</tt>es
         * @return the first <tt>PresenceStatus</tt> among the set of
         * <tt>PresenceStatus</tt>es supported by <tt>presence</tt> which
         * represents &quot;In meeting&quot; if such a <tt>PresenceStatus</tt>
         * was found; otherwise, <tt>null</tt>
         */
        private PresenceStatus findInMeetingPresenceStatus(
                OperationSetPresence presence)
        {
            for (Iterator<PresenceStatus> i = presence.getSupportedStatusSet();
                    i.hasNext();)
            {
                PresenceStatus presenceStatus = i.next();

                if (presenceStatusNameWhitespace
                        .matcher(presenceStatus.getStatusName())
                            .replaceAll("")
                                .equalsIgnoreCase("InAMeeting"))
                {
                    return presenceStatus;
                }
            }
            return null;
        }

        /**
         * Applies this policy to the current state of the application.
         */
        private void run()
        {
            if(logger.isTraceEnabled())
            {
                logger.trace("On the phone status policy run.");
            }

            if (!ProtocolProviderActivator.getConfigurationService().getBoolean(
                    PNAME_ON_THE_PHONE_STATUS_ENABLED,
                    false))
            {
                if(logger.isTraceEnabled())
                {
                    logger.trace("On the phone status is not enabled.");
                }
                forgetPresenceStatuses();
                return;
            }

            ServiceReference[] ppsRefs;

            try
            {
                ppsRefs
                    = bundleContext.getServiceReferences(
                            ProtocolProviderService.class.getName(),
                            null);
            }
            catch (InvalidSyntaxException ise)
            {
                if(logger.isTraceEnabled())
                {
                    logger.trace("Can't access protocol providers refences.");
                }
                ppsRefs = null;
            }
            if ((ppsRefs == null) || (ppsRefs.length == 0))
            {
                forgetPresenceStatuses();
            }
            else
            {
                boolean isOnThePhone = isOnThePhone();

                CalendarService calendar
                    = ProtocolProviderActivator.getCalendarService();

                if(!isOnThePhone && calendar != null &&
                    calendar.onThePhoneStatusChanged(presenceStatuses))
                {
                    if(logger.isTraceEnabled())
                    {
                        logger.trace("We are not on the phone.");
                    }
                    forgetPresenceStatuses();
                    return;
                }

                for (ServiceReference ppsRef : ppsRefs)
                {
                    ProtocolProviderService pps
                        = (ProtocolProviderService)
                            bundleContext.getService(ppsRef);

                    if (pps == null)
                    {
                        if(logger.isTraceEnabled())
                        {
                            logger.trace("Provider is null.");
                        }
                        continue;
                    }

                    OperationSetPresence presence
                        = pps.getOperationSet(OperationSetPresence.class);

                    if (presence == null)
                    {
                        if(logger.isTraceEnabled())
                        {
                            logger.trace("Presence is null.");
                        }
                        /*
                         * "On the phone" is a PresenceStatus so it is available
                         * only to accounts which support presence in the first
                         * place.
                         */
                        forgetPresenceStatus(pps);
                    }
                    else if (pps.isRegistered())
                    {
                        if(logger.isTraceEnabled())
                        {
                            logger.trace("Provider is registered.");
                        }
                        PresenceStatus onThePhonePresenceStatus
                            = findOnThePhonePresenceStatus(presence);

                        if (onThePhonePresenceStatus == null)
                        {
                            if(logger.isTraceEnabled())
                            {
                                logger.trace("Can't find on the phone status.");
                            }
                            /*
                             * If do not know how to define "On the phone" for
                             * an OperationSetPresence, then we'd better not
                             * mess with it in the first place.
                             */
                            forgetPresenceStatus(pps);
                        }
                        else if (isOnThePhone)
                        {
                            if(logger.isTraceEnabled())
                            {
                                logger.trace(
                                    "Setting the status to on the phone.");
                            }
                            PresenceStatus presenceStatus
                                = presence.getPresenceStatus();

                            if (presenceStatus == null)
                            {
                                if(logger.isTraceEnabled())
                                {
                                    logger.trace("Presence status is null.");
                                }
                                /*
                                 * It is strange that an OperationSetPresence
                                 * does not have a PresenceStatus so it may be
                                 * safer to not mess with it.
                                 */
                                forgetPresenceStatus(pps);
                            }
                            else if (!onThePhonePresenceStatus.equals(
                                    presenceStatus))
                            {
                                if(logger.isTraceEnabled())
                                {
                                    logger.trace(
                                        "On the phone status is published.");
                                }
                                publishPresenceStatus(
                                        presence,
                                        onThePhonePresenceStatus);

                                if(presenceStatus.equals(
                                    findInMeetingPresenceStatus(presence))
                                    && calendar != null)
                                {
                                    Map<ProtocolProviderService,PresenceStatus>
                                        statuses
                                            = calendar.getRememberedStatuses();
                                    for(ProtocolProviderService provider
                                        : statuses.keySet())
                                        rememberPresenceStatus(provider,
                                            statuses.get(provider));
                                }
                                else if (onThePhonePresenceStatus.equals(
                                        presence.getPresenceStatus()))
                                {
                                    rememberPresenceStatus(pps, presenceStatus);
                                }
                                else
                                {
                                    forgetPresenceStatus(pps);
                                }
                            }
                            else
                            {
                                if(logger.isTraceEnabled())
                                {
                                    logger.trace(
                                        "Currently the status is on the phone.");
                                }
                            }
                        }
                        else
                        {
                            if(logger.isTraceEnabled())
                            {
                                logger.trace("Unset on the phone status.");
                            }
                            PresenceStatus presenceStatus
                                = forgetPresenceStatus(pps);

                            if ((presenceStatus != null)
                                    && onThePhonePresenceStatus.equals(
                                            presence.getPresenceStatus()))
                            {
                                if(logger.isTraceEnabled())
                                {
                                    logger.trace("Unset on the phone status.[2]");
                                }

                                publishPresenceStatus(presence, presenceStatus);
                            }
                        }
                    }
                    else
                    {
                        if(logger.isTraceEnabled())
                        {
                            logger.trace("Protocol provider is not registered");
                        }

                        /*
                         * Offline accounts do not get their PresenceStatus
                         * modified for the purposes of "On the phone".
                         */
                        forgetPresenceStatus(pps);
                    }
                }
            }
        }
    }

    /**
     * Implements the listeners interfaces used by this policy.
     *
     * @author Lyubomir Marinov
     */
    private class SingleCallInProgressPolicyListener
        implements CallChangeListener,
                   CallListener,
                   ServiceListener
    {
        /**
         * Stops tracking the state of a specific <tt>Call</tt> and no longer
         * tries to put it on hold when it ends.
         *
         * @see CallListener#callEnded(CallEvent)
         */
        public void callEnded(CallEvent ev)
        {
            SingleCallInProgressPolicy.this.handleCallEvent(
                    CallEvent.CALL_ENDED,
                    ev);
        }

        /**
         * Does nothing because adding <tt>CallPeer<tt>s to <tt>Call</tt>s isn't
         * related to the policy to put existing calls on hold when a new call
         * becomes in-progress and just implements <tt>CallChangeListener</tt>.
         *
         * @see CallChangeListener#callPeerAdded(CallPeerEvent)
         */
        public void callPeerAdded(CallPeerEvent ev)
        {
            /*
             * Not of interest, just implementing CallChangeListener in which
             * only #callStateChanged(CallChangeEvent) is of interest.
             */
        }

        /**
         * Does nothing because removing <tt>CallPeer<tt>s to <tt>Call</tt>s
         * isn't related to the policy to put existing calls on hold when a new
         * call becomes in-progress and just implements
         * <tt>CallChangeListener</tt>.
         *
         * @see CallChangeListener#callPeerRemoved(CallPeerEvent)
         */
        public void callPeerRemoved(CallPeerEvent ev)
        {
            /*
             * Not of interest, just implementing CallChangeListener in which
             * only #callStateChanged(CallChangeEvent) is of interest.
             */
        }

        /**
         * Upon a <tt>Call</tt> changing its state to
         * <tt>CallState.CALL_IN_PROGRESS</tt>, puts the other existing
         * <tt>Call</tt>s on hold.
         *
         * @param ev the <tt>CallChangeEvent</tt> that we are to deliver.
         *
         * @see CallChangeListener#callStateChanged(CallChangeEvent)
         */
        public void callStateChanged(CallChangeEvent ev)
        {
            // we are interested only in CALL_STATE_CHANGEs
            if (ev.getEventType().equals(CallChangeEvent.CALL_STATE_CHANGE))
                SingleCallInProgressPolicy.this.callStateChanged(ev);
        }

        /**
         * Remembers an incoming <tt>Call</tt> so that it can put the other
         * existing <tt>Call</tt>s on hold when it changes its state to
         * <tt>CallState.CALL_IN_PROGRESS</tt>.
         *
         * @see CallListener#incomingCallReceived(CallEvent)
         */
        public void incomingCallReceived(CallEvent ev)
        {
            SingleCallInProgressPolicy.this.incomingCallReceived(ev);
        }

        /**
         * Remembers an outgoing <tt>Call</tt> so that it can put the other
         * existing <tt>Call</tt>s on hold when it changes its state to
         * <tt>CallState.CALL_IN_PROGRESS</tt>.
         *
         * @see CallListener#outgoingCallCreated(CallEvent)
         */
        public void outgoingCallCreated(CallEvent ev)
        {
            SingleCallInProgressPolicy.this.handleCallEvent(
                    CallEvent.CALL_INITIATED,
                    ev);
        }

        /**
         * Starts/stops tracking the new <tt>Call</tt>s originating from a
         * specific <tt>ProtocolProviderService</tt> when it
         * registers/unregisters in order to take them into account when putting
         * existing calls on hold upon a new call entering its in-progress
         * state.
         *
         * @param ev the <tt>ServiceEvent</tt> event describing a change in the
         * state of a service registration which may be a
         * <tt>ProtocolProviderService</tt> supporting
         * <tt>OperationSetBasicTelephony</tt> and thus being able to create new
         * <tt>Call</tt>s
         */
        public void serviceChanged(ServiceEvent ev)
        {
            if(logger.isTraceEnabled())
            {
                logger.trace("Service changed.");
            }
            SingleCallInProgressPolicy.this.serviceChanged(ev);
        }
    }
}
