/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util.swing;

import java.awt.*;
import java.awt.event.*;

/**
 * The <tt>LowPriorityEventQueue</tt> schedules low priority events to be
 * dispatched through the system event queue.
 *
 * @author Yana Stamcheva
 */
public class LowPriorityEventQueue
{
    /**
     * Causes <code>runnable</code> to have its <code>run</code>
     * method called in the event dispatch thread with low priority.
     *
     * @param runnable  the <code>Runnable</code> whose <code>run</code>
     * method should be executed synchronously on the <code>EventQueue</code>
     */
    public static void invokeLater(Runnable runnable)
    {
        Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(
            new LowPriorityInvocationEvent(
                Toolkit.getDefaultToolkit(), runnable));
    }

    /**
     * The <tt>LowPriorityInvocationEvent</tt> is an <tt>InvocationEvent</tt>
     * that replaces the default event id with the <tt>PaintEvent.UPDATE</tt>
     * in order to indicate that this event should be dispatched with the same
     * priority as an update paint event, which is normally with lower priority
     * than other events.
     */
    private static class LowPriorityInvocationEvent extends InvocationEvent
    {
        public LowPriorityInvocationEvent(Object source, Runnable runnable)
        {
            super(source, PaintEvent.UPDATE, runnable, null, false);
        }
    }
}
