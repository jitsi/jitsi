/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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
// disambiguation

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
     * The <tt>Logger</tt> used by the <tt>CallShortcut</tt> class
     * and its instances for logging output.
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
    private KeybindingsService keybindingsService =
        GlobalShortcutActivator.getKeybindingsService();

    /**
     * List of incoming calls not yet answered.
     */
    private ArrayList<Call> incomingCalls = new ArrayList<Call>();

    /**
     * List of answered calls: active (off hold) or on hold.
     */
    private ArrayList<Call> answeredCalls = new ArrayList<Call>();

    /**
     * Next mute state action.
     */
    private boolean mute = true;

    /**
     * Constructor.
     */
    public CallShortcut()
    {
    }

    /**
     * Callback when an shortcut is typed
     *
     * @param evt <tt>GlobalShortcutEvent</tt>
     */
    public void shortcutReceived(GlobalShortcutEvent evt)
    {
        AWTKeyStroke keystroke = evt.getKeyStroke();
        GlobalKeybindingSet set = keybindingsService.getGlobalBindings();

        for(Map.Entry<String, List<AWTKeyStroke>> entry :
            set.getBindings().entrySet())
        {
            for(AWTKeyStroke ks : entry.getValue())
            {
                if(ks == null)
                    continue;

                if(entry.getKey().equals("answer") &&
                    keystroke.getKeyCode() == ks.getKeyCode() &&
                    keystroke.getModifiers() == ks.getModifiers())
                {
                    // Try to answer the new incoming call, if there is any.
                    manageNextIncomingCall(CallAction.ANSWER);

                }
                else if(entry.getKey().equals("hangup") &&
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
                else if(entry.getKey().equals("answer_hangup") &&
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
                else if(entry.getKey().equals("mute") &&
                    keystroke.getKeyCode() == ks.getKeyCode() &&
                    keystroke.getModifiers() == ks.getModifiers())
                {
                    synchronized(incomingCalls)
                    {
                        for(Call c : incomingCalls)
                        {
                            handleMute(c);
                        }
                    }

                    synchronized(answeredCalls)
                    {
                        for(Call c : answeredCalls)
                        {
                            handleMute(c);
                        }
                    }

                    // next action will revert change done here (mute or unmute)
                    mute = !mute;
                }
            }
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
        CallShortcut.addCall(event.getSourceCall(), this.incomingCalls);
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
        CallShortcut.addCall(event.getSourceCall(), this.answeredCalls);
    }

    /**
     * Adds a created call to the managed call list.
     *
     * @param call The call to add to the managed call list.
     * @param calls The managed call list.
     */
    private static void addCall(Call call, ArrayList<Call> calls)
    {
        synchronized(calls)
        {
            if(call.getCallGroup() == null)
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

        CallShortcut.removeCall(sourceCall, incomingCalls);
        CallShortcut.removeCall(sourceCall, answeredCalls);
    }

    /**
     * Removes an ended call to the managed call list.
     *
     * @param call The call to remove from the managed call list.
     * @param calls The managed call list.
     */
    private static void removeCall(Call call, ArrayList<Call> calls)
    {
        synchronized(calls)
        {
            if(calls.contains(call))
                calls.remove(call);
        }
    }

    /**
     * Handle the mute for a <tt>Call</tt>.
     *
     * @param c the <tt>Call</tt>
     */
    private void handleMute(Call c)
    {
        // handle only established call
        if(c.getCallState() != CallState.CALL_IN_PROGRESS)
            return;

        // handle only connected peer (no on hold peer)
        if(c.getCallPeers().next().getState() != CallPeerState.CONNECTED)
            return;

        MediaAwareCall<?,?,?> cc = (MediaAwareCall<?,?,?>)c;
        if(mute != cc.isMute())
        {
            cc.setMute(mute);
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
            public void run()
            {
                try
                {
                    List<Call> calls;
                    CallGroup group = call.getCallGroup();
                    if(group != null)
                    {
                        calls = group.getCalls();
                    }
                    else
                    {
                        calls = new Vector<Call>();
                        calls.add(call);
                    }
                    Call tmpCall;
                    Iterator<? extends CallPeer> callPeers;
                    CallPeer callPeer;

                    for(int i = 0; i < calls.size();  ++i)
                    {
                        tmpCall = calls.get(i);
                        final OperationSetBasicTelephony<?> opSet =
                            tmpCall.getProtocolProvider()
                            .getOperationSet(OperationSetBasicTelephony.class);
                        callPeers = tmpCall.getCallPeers();
                        while(callPeers.hasNext())
                        {
                            callPeer = callPeers.next();
                            switch(callAction)
                            {
                                case ANSWER:
                                    if(callPeer.getState() ==
                                            CallPeerState.INCOMING_CALL)
                                    {
                                        opSet.answerCallPeer(callPeer);
                                    }
                                    break;
                                case HANGUP:
                                    opSet.hangupCallPeer(callPeer);
                                    break;
                            }
                        }
                    }

                }
                catch(OperationFailedException e)
                {
                    logger.info(
                            "Failed to answer/hangup call via global shortcut",
                            e);
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
            Call call;
            int i = incomingCalls.size();
            while(i != 0)
            {
                --i;
                call = incomingCalls.get(i);

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
        Call call;

        synchronized(answeredCalls)
        {
            int i = answeredCalls.size();
            while(i != 0)
            {
                --i;
                call = answeredCalls.get(i);

                // If we are not limited to active call, then we close all
                // answered calls. Otherwise, we close only active calls (hold
                // off calls).
                if(!closeOnlyActiveCalls
                        || CallShortcut.isActiveCall(call))
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
     * Return true if the call is active: at least one call peer is active (not
     * on hold).
     *
     * @param call The call that this function will determine as active or not.
     *
     * @return True if the call is active. False, otherwise.
     */
    private static boolean isActiveCall(Call call)
    {
        List<Call> calls;
        CallGroup group = call.getCallGroup();
        if(group != null)
        {
            calls = group.getCalls();
        }
        else
        {
            calls = new Vector<Call>();
            calls.add(call);
        }

        for(int i = 0; i < calls.size();  ++i)
        {
            if(isAtLeastOneActiveCallPeer(calls.get(i).getCallPeers()))
            {
                // If there is a single active call peer, then the call is
                // active.
                return true;
            }
        }
        return false;
    }

    /**
     * Return true if at least one call peer is active: not on hold.
     *
     * @param callPeers The call peer list which may contain at least an active
     * call.

     * @return True if at least one call peer is active: not on hold. False,
     * otherwise.
     */
    private static boolean isAtLeastOneActiveCallPeer(
            Iterator<? extends CallPeer> callPeers)
    {
        CallPeer callPeer;
        
        while(callPeers.hasNext())
        {
            callPeer = callPeers.next();
            if(!CallPeerState.isOnHold(callPeer.getState()))
            {
                // If at least one peer is active, then the call is active.
                return true;
            }
        }
        return false;
    }
}
