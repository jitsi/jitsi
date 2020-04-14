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
package net.java.sip.communicator.impl.globalshortcut;

import java.awt.*;
import java.util.*;
import java.util.List;

import net.java.sip.communicator.service.globalshortcut.*;
import net.java.sip.communicator.service.keybindings.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.media.*;
import net.java.sip.communicator.util.*;

/**
 * Shortcut for call (take the call, hang up, ...).
 *
 * @author Sebastien Vincent
 * @author Vincent Lucas
 */
public class CallShortcut
    implements GlobalShortcutListener,
               CallListener
{
    /**
     * The <tt>Logger</tt> used by the <tt>CallShortcut</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger = Logger.getLogger(CallShortcut.class);

    /**
     * Lists the call actions available: ANSWER or HANGUP.
     */
    private enum CallAction
    {
        // Answers an incoming call.
        ANSWER,
        // Hangs up a call.
        HANGUP
    }

    /**
     * Keybindings service.
     */
    private final KeybindingsService keybindingsService
        = GlobalShortcutActivator.getKeybindingsService();

    /**
     * List of incoming calls not yet answered.
     */
    private final List<Call> incomingCalls = new ArrayList<Call>();

    /**
     * List of answered calls: active (off hold) or on hold.
     */
    private final List<Call> answeredCalls = new ArrayList<Call>();

    /**
     * Next mute state action.
     */
    private boolean mute = true;

    /**
     * Push to talk state action.
     */
    private boolean ptt_pressed = false;

    /**
     * Initializes a new <tt>CallShortcut</tt> instance.
     */
    public CallShortcut()
    {
    }

    /**
     * Notifies this <tt>GlobalShortcutListener</tt> that a shortcut was
     * triggered.
     *
     * @param evt a <tt>GlobalShortcutEvent</tt> which describes the specifics
     * of the triggering of the shortcut
     */
    public void shortcutReceived(GlobalShortcutEvent evt)
    {
        AWTKeyStroke keystroke = evt.getKeyStroke();
        GlobalKeybindingSet set = keybindingsService.getGlobalBindings();
        for(Map.Entry<String, List<AWTKeyStroke>> entry
                : set.getBindings().entrySet())
        {
            for(AWTKeyStroke ks : entry.getValue())
            {
                if(ks == null)
                    continue;

                String entryKey = entry.getKey();
                if(entryKey.equals("answer") &&
                    keystroke.getKeyCode() == ks.getKeyCode() &&
                    keystroke.getModifiers() == ks.getModifiers())
                {
                    // Try to answer the new incoming call, if there is any.
                    manageNextIncomingCall(CallAction.ANSWER);

                }
                else if(entryKey.equals("hangup") &&
                    keystroke.getKeyCode() == ks.getKeyCode() &&
                    keystroke.getModifiers() == ks.getModifiers())
                {
                    // Try to hang up the new incoming call.
                    if(!manageNextIncomingCall(CallAction.HANGUP))
                    {
                        // There was no new incoming call.
                        // Thus, we try to close all active calls.
                        if(!closeAnsweredCalls(true))
                        {
                            // There was no active call.
                            // Thus, we close all answered (inactive, hold on)
                            // calls.
                            closeAnsweredCalls(false);
                        }
                    }

                }
                else if(entryKey.equals("answer_hangup") &&
                    keystroke.getKeyCode() == ks.getKeyCode() &&
                    keystroke.getModifiers() == ks.getModifiers())
                {
                    // Try to answer the new incoming call.
                    if(!manageNextIncomingCall(CallAction.ANSWER))
                    {
                        // There was no new incoming call.
                        // Thus, we try to close all active calls.
                        if(!closeAnsweredCalls(true))
                        {
                            // There was no active call.
                            // Thus, we close all answered (inactive, hold on)
                            // calls.
                            closeAnsweredCalls(false);
                        }
                    }

                }
                else if(entryKey.equals("mute") &&
                    keystroke.getKeyCode() == ks.getKeyCode() &&
                    keystroke.getModifiers() == ks.getModifiers())
                {
                    try
                    {
                        handleAllCallsMute(mute);
                    }
                    finally
                    {
                        // next action will revert change done here (mute or
                        // unmute)
                        mute = !mute;
                    }
                }
                else if(entryKey.equals("push_to_talk") &&
                    keystroke.getKeyCode() == ks.getKeyCode() &&
                    keystroke.getModifiers() == ks.getModifiers() &&
                    evt.isReleased() == ptt_pressed
                    )
                {
                    try
                    {
                        handleAllCallsMute(ptt_pressed);
                    }
                    finally
                    {
                        ptt_pressed = !ptt_pressed;
                    }
                }
            }
        }
    }

    /**
     * Sets the mute state for all calls.
     *
     * @param mute the state to be set
     */
    private void handleAllCallsMute(boolean mute)
    {
            synchronized(incomingCalls)
            {
                for(Call c : incomingCalls)
                    handleMute(c, mute);
            }
            synchronized(answeredCalls)
            {
                for(Call c : answeredCalls)
                    handleMute(c, mute);
            }
    }

    /**
     * This method is called by a protocol provider whenever an incoming call is
     * received.
     *
     * @param event a CallEvent instance describing the new incoming call
     */
    public void incomingCallReceived(CallEvent event)
    {
        addCall(event.getSourceCall(), this.incomingCalls);
    }

    /**
     * This method is called by a protocol provider upon initiation of an
     * outgoing call.
     * <p>
     *
     * @param event a CalldEvent instance describing the new incoming call.
     */
    public void outgoingCallCreated(CallEvent event)
    {
        addCall(event.getSourceCall(), this.answeredCalls);
    }

    /**
     * Adds a created call to the managed call list.
     *
     * @param call The call to add to the managed call list.
     * @param calls The managed call list.
     */
    private static void addCall(Call call, List<Call> calls)
    {
        synchronized(calls)
        {
            if(!calls.contains(call))
                calls.add(call);
        }
    }

    /**
     * Indicates that all peers have left the source call and that it has
     * been ended. The event may be considered redundant since there are already
     * events issued upon termination of a single call peer but we've
     * decided to keep it for listeners that are only interested in call
     * duration and don't want to follow other call details.
     *
     * @param event the <tt>CallEvent</tt> containing the source call.
     */
    public void callEnded(CallEvent event)
    {
        Call sourceCall = event.getSourceCall();

        removeCall(sourceCall, incomingCalls);
        removeCall(sourceCall, answeredCalls);
    }

    /**
     * Removes an ended call to the managed call list.
     *
     * @param call The call to remove from the managed call list.
     * @param calls The managed call list.
     */
    private static void removeCall(Call call, List<Call> calls)
    {
        synchronized(calls)
        {
            if(calls.contains(call))
                calls.remove(call);
        }
    }

    /**
     * Sets the mute state of a specific <tt>Call</tt> in accord with
     * {@link #mute}.
     *
     * @param call the <tt>Call</tt> to set the mute state of
     * @param mute indicates if the current state is mute or unmute
     */
    private void handleMute(Call call, boolean mute)
    {
        // handle only established call
        if(call.getCallState() != CallState.CALL_IN_PROGRESS)
            return;
        // handle only connected peer (no on hold peer)
        if(call.getCallPeers().next().getState() != CallPeerState.CONNECTED)
            return;

        OperationSetBasicTelephony<?> basicTelephony
            = call.getProtocolProvider().getOperationSet(
                    OperationSetBasicTelephony.class);

        if ((basicTelephony != null)
                && (mute != ((MediaAwareCall<?,?,?>) call).isMute()))
        {
            basicTelephony.setMute(call, mute);
        }
    }


    /**
     * Answers or puts on/off hold the given call.
     *
     * @param call  The call to answer, to put on hold, or to put off hold.
     * @param callAction The action (ANSWER or HANGUP) to do.
     */
    private static void doCallAction(
            final Call call,
            final CallAction callAction)
    {
        new Thread()
        {
            @Override
            public void run()
            {
                try
                {
                    for (Call aCall : CallConference.getCalls(call))
                    {
                        Iterator<? extends CallPeer> callPeers
                            = aCall.getCallPeers();
                        OperationSetBasicTelephony<?> basicTelephony
                            = aCall.getProtocolProvider().getOperationSet(
                                    OperationSetBasicTelephony.class);

                        while(callPeers.hasNext())
                        {
                            CallPeer callPeer = callPeers.next();

                            switch(callAction)
                            {
                            case ANSWER:
                                if(callPeer.getState()
                                        == CallPeerState.INCOMING_CALL)
                                {
                                    basicTelephony.answerCallPeer(callPeer);
                                }
                                break;
                            case HANGUP:
                                basicTelephony.hangupCallPeer(callPeer);
                                break;
                            }
                        }
                    }

                }
                catch(OperationFailedException ofe)
                {
                    logger.error(
                            "Failed to answer/hangup call via global shortcut",
                            ofe);
                }
            }
        }.start();
    }

    /**
     * Answers or hangs up the next incoming call if any.
     *
     * @param callAction The action (ANSWER or HANGUP) to do.
     *
     * @return True if the next incoming call has been answered/hanged up. False
     * if there is no incoming call remaining.
     */
    private boolean manageNextIncomingCall(CallAction callAction)
    {
        synchronized(incomingCalls)
        {
            int i = incomingCalls.size();

            while(i != 0)
            {
                --i;

                Call call = incomingCalls.get(i);

                // Either this incoming call is already answered, or we will
                // answered it. Thus, we switch it to the answered list.
                answeredCalls.add(call);
                incomingCalls.remove(i);

                // We find a call not answered yet.
                if(call.getCallState() == CallState.CALL_INITIALIZATION)
                {
                    // Answer or hang up the ringing call.
                    CallShortcut.doCallAction(call, callAction);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Closes only active calls, or all answered calls depending on the
     * closeOnlyActiveCalls parameter.
     *
     * @param closeOnlyActiveCalls Boolean which must be set to true to only
     * removes the active calls. Otherwise, the whole answered calls will be
     * closed.
     *
     * @return True if there was at least one call closed. False otherwise.
     */
    private boolean closeAnsweredCalls(boolean closeOnlyActiveCalls)
    {
        boolean isAtLeastOneCallClosed = false;

        synchronized(answeredCalls)
        {
            int i = answeredCalls.size();

            while(i != 0)
            {
                --i;

                Call call = answeredCalls.get(i);

                // If we are not limited to active call, then we close all
                // answered calls. Otherwise, we close only active calls (hold
                // off calls).
                if(!closeOnlyActiveCalls || CallShortcut.isActiveCall(call))
                {
                    CallShortcut.doCallAction(call, CallAction.HANGUP);
                    answeredCalls.remove(i);
                    isAtLeastOneCallClosed = true;
                }
            }
        }

        return isAtLeastOneCallClosed;
    }

    /**
     * Returns <tt>true</tt> if a specific <tt>Call</tt> is active - at least
     * one <tt>CallPeer</tt> is active (i.e. not on hold).
     *
     * @param call the <tt>Call</tt> to be determined whether it is active
     * @return <tt>true</tt> if the specified <tt>call</tt> is active;
     * <tt>false</tt>, otherwise.
     */
    private static boolean isActiveCall(Call call)
    {
        for (Call conferenceCall : CallConference.getCalls(call))
        {
            // If at least one CallPeer is active, the whole call is active.
            if (isAtLeastOneActiveCallPeer(conferenceCall.getCallPeers()))
                return true;
        }
        return false;
    }

    /**
     * Returns <tt>true</tt> if at least one <tt>CallPeer</tt> in a list of
     * <tt>CallPeer</tt>s is active i.e. is not on hold; <tt>false</tt>,
     * otherwise.
     *
     * @param callPeers the list of <tt>CallPeer</tt>s to check for at least one
     * active <tt>CallPeer</tt>
     * @return <tt>true</tt> if at least one <tt>CallPeer</tt> in
     * <tt>callPeers</tt> is active i.e. is not on hold; <tt>false</tt>,
     * otherwise
     */
    private static boolean isAtLeastOneActiveCallPeer(
            Iterator<? extends CallPeer> callPeers)
    {
        while (callPeers.hasNext())
        {
            CallPeer callPeer = callPeers.next();

            if (!CallPeerState.isOnHold(callPeer.getState()))
            {
                // If at least one peer is active, then the call is active.
                return true;
            }
        }
        return false;
    }
}
