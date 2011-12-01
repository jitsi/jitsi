/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.globalshortcut;

import java.awt.*;
import java.util.*;
import java.util.List; // disambiguation

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
     * Keybindings service.
     */
    private KeybindingsService keybindingsService =
        GlobalShortcutActivator.getKeybindingsService();

    /**
     * List of incoming calls.
     */
    private ArrayList<Call> incomingCalls = new ArrayList<Call>();

    /**
     * List of outgoing calls.
     */
    private ArrayList<Call> outgoingCalls = new ArrayList<Call>();

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
        Call choosenCall = null;

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
                    synchronized(incomingCalls)
                    {
                        int size = incomingCalls.size();

                        for(int i = 0 ; i < size ; i++)
                        {
                            Call c = incomingCalls.get(i);

                            if(c.getCallPeers().next().getState() ==
                                CallPeerState.INCOMING_CALL)
                            {
                                choosenCall = c;
                                break;
                            }
                        }
                    }

                    if(choosenCall == null)
                        return;

                    final OperationSetBasicTelephony<?> opSet =
                        choosenCall.getProtocolProvider().getOperationSet(
                            OperationSetBasicTelephony.class);
                    final Call cCall = choosenCall;

                    new Thread()
                    {
                        public void run()
                        {
                            try
                            {
                                opSet.answerCallPeer(
                                    cCall.getCallPeers().next());
                            }
                            catch(OperationFailedException e)
                            {
                                logger.info(
                                    "Failed to answer call via global shortcut",
                                    e);
                            }
                        }
                    }.start();
                }
                else if(entry.getKey().equals("hangup") &&
                    keystroke.getKeyCode() == ks.getKeyCode() &&
                    keystroke.getModifiers() == ks.getModifiers())
                {
                    Call incomingCall = null;
                    Call outgoingCall = null;

                    synchronized(incomingCalls)
                    {
                        int size = incomingCalls.size();

                        for(int i = 0 ; i < size ; i++)
                        {
                            Call c = incomingCalls.get(i);

                            if(c.getCallPeers().next().getState() ==
                                CallPeerState.INCOMING_CALL &&
                                incomingCall == null)
                            {
                                incomingCall = c;
                                break;
                            }
                            else if(c.getCallPeers().next().getState() ==
                                CallPeerState.CONNECTED)
                            {
                                choosenCall = c;
                                break;
                            }
                        }
                    }

                    synchronized(outgoingCalls)
                    {
                        int size = outgoingCalls.size();

                        for(int i = 0 ; i < size ; i++)
                        {
                            Call c = outgoingCalls.get(i);

                            if((c.getCallPeers().next().getState() ==
                                CallPeerState.CONNECTING ||
                                c.getCallPeers().next().getState() ==
                                    CallPeerState.ALERTING_REMOTE_SIDE) &&
                                    outgoingCall == null)
                            {
                                outgoingCall = c;
                                break;
                            }
                            else if(c.getCallPeers().next().getState() ==
                                CallPeerState.CONNECTED)
                            {
                                choosenCall = c;
                                break;
                            }

                        }
                    }

                    if(choosenCall == null && incomingCall != null)
                    {
                        // maybe we just want to hangup (refuse) incoming call
                        choosenCall = incomingCall;
                    }
                    if(choosenCall == null && outgoingCall != null)
                    {
                        // maybe we just want to hangup (cancel) outgoing call
                        choosenCall = outgoingCall;
                    }

                    if(choosenCall == null)
                        return;

                    final OperationSetBasicTelephony<?> opSet =
                        choosenCall.getProtocolProvider().getOperationSet(
                            OperationSetBasicTelephony.class);

                    final Call cCall = choosenCall;
                    new Thread()
                    {
                        public void run()
                        {
                            try
                            {
                                opSet.hangupCallPeer(
                                    cCall.getCallPeers().next());
                            }
                            catch(OperationFailedException e)
                            {
                                logger.info(
                                    "Failed to answer call via global shortcut",
                                    e);
                            }
                        }
                    }.start();
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

                    synchronized(outgoingCalls)
                    {
                        for(Call c : outgoingCalls)
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

    public void incomingCallReceived(CallEvent event)
    {
        Call sourceCall = event.getSourceCall();

        synchronized(incomingCalls)
        {
            incomingCalls.add(sourceCall);
        }
    }

    public void outgoingCallCreated(CallEvent event)
    {
        synchronized(outgoingCalls)
        {
            outgoingCalls.add(event.getSourceCall());
        }
    }

    public void callEnded(CallEvent event)
    {
        Call sourceCall = event.getSourceCall();

        if(incomingCalls.contains(sourceCall))
            incomingCalls.remove(sourceCall);
        else if(outgoingCalls.contains(sourceCall))
            outgoingCalls.remove(sourceCall);
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
        if(c.getCallPeers().next().getState() !=
            CallPeerState.CONNECTED)
            return;

        MediaAwareCall<?,?,?> cc = (MediaAwareCall<?,?,?>)c;
        if(mute && !cc.isMute())
            cc.setMute(true);
        else if(!mute && cc.isMute())
            cc.setMute(false);
    }
}
