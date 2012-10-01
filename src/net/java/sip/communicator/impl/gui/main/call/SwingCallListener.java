/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import javax.swing.*;

import net.java.sip.communicator.service.protocol.event.*;

/**
 * A utility implementation of the {@link CallListener} interface which delivers
 * the <tt>CallEvent</tt>s to the AWT event dispatching thread.
 *
 * @author Lyubomir Marinov
 */
public class SwingCallListener
    implements CallListener
{
    /**
     * {@inheritDoc}
     *
     * Delivers the <tt>CallEvent</tt> to the AWT event dispatching thread.
     */
    public void callEnded(CallEvent ev)
    {
        onCallEvent(ev);
    }

    /**
     * Notifies this <tt>CallListener</tt> in the AWT event dispatching thread
     * about a <tt>CallEvent</tt> with <tt>eventID</tt>
     * {@link CallEvent#CALL_ENDED}. The <tt>SwingCallListener</tt>
     * implementation does nothing.
     *
     * @param ev the <tt>CallEvent</tt> this <tt>CallListener</tt> is being
     * notified about
     */
    protected void callEndedInEventDispatchThread(CallEvent ev)
    {
    }

    /**
     * {@inheritDoc}
     *
     * Delivers the <tt>CallEvent</tt> to the AWT event dispatching thread.
     */
    public void incomingCallReceived(CallEvent ev)
    {
        onCallEvent(ev);
    }

    /**
     * Notifies this <tt>CallListener</tt> in the AWT event dispatching thread
     * about a <tt>CallEvent</tt> with <tt>eventID</tt>
     * {@link CallEvent#CALL_RECEIVED}. The <tt>SwingCallListener</tt>
     * implementation does nothing.
     *
     * @param ev the <tt>CallEvent</tt> this <tt>CallListener</tt> is being
     * notified about
     */
    protected void incomingCallReceivedInEventDispatchThread(CallEvent ev)
    {
    }

    /**
     * Notifies this <tt>CallListener</tt> about a specific <tt>CallEvent</tt>.
     * Executes in whichever thread brought the event to this listener. Delivers
     * the event to the AWT event dispatching thread.
     *
     * @param ev the <tt>CallEvent</tt> this <tt>CallListener</tt> is being
     * notified about
     */
    protected void onCallEvent(final CallEvent ev)
    {
        if (SwingUtilities.isEventDispatchThread())
            onCallEventInEventDispatchThread(ev);
        else
        {
            SwingUtilities.invokeLater(
                    new Runnable()
                    {
                        public void run()
                        {
                            onCallEventInEventDispatchThread(ev);
                        }
                    });
        }
    }

    /**
     * Notifies this <tt>CallListener</tt> about a specific <tt>CallEvent</tt>.
     * <tt>SwingCallListener</tt> invokes the method in the AWT event
     * dispatching thread.
     *
     * @param ev the <tt>CallEvent</tt> this <tt>CallListener</tt> is being
     * notified about
     */
    protected void onCallEventInEventDispatchThread(CallEvent ev)
    {
        switch (ev.getEventID())
        {
        case CallEvent.CALL_ENDED:
            callEndedInEventDispatchThread(ev);
            break;
        case CallEvent.CALL_INITIATED:
            outgoingCallCreatedInEventDispatchThread(ev);
            break;
        case CallEvent.CALL_RECEIVED:
            incomingCallReceivedInEventDispatchThread(ev);
            break;
        }
    }

    /**
     * {@inheritDoc}
     *
     * Delivers the <tt>CallEvent</tt> to the AWT event dispatching thread.
     */
    public void outgoingCallCreated(CallEvent ev)
    {
        onCallEvent(ev);
    }

    /**
     * Notifies this <tt>CallListener</tt> in the AWT event dispatching thread
     * about a <tt>CallEvent</tt> with <tt>eventID</tt>
     * {@link CallEvent#CALL_INITIATED}. The <tt>SwingCallListener</tt>
     * implementation does nothing.
     *
     * @param ev the <tt>CallEvent</tt> this <tt>CallListener</tt> is being
     * notified about
     */
    protected void outgoingCallCreatedInEventDispatchThread(CallEvent ev)
    {
    }
}
