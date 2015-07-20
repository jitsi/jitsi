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
package net.java.sip.communicator.plugin.desktoputil;

import java.awt.*;
import java.awt.event.*;

/**
 * The <tt>LowPriorityEventQueue</tt> schedules low priority events to be
 * dispatched through the system event queue.
 *
 * @author Yana Stamcheva
 * @author Lyubomir Marinov
 */
public class LowPriorityEventQueue
{
    /**
     * Initializes a new <tt>Runnable</tt> which allows the repetitive execution
     * the specific <tt>runnable</tt> on the application's <tt>EventQueue</tt>
     * instance.
     *
     * @param runnable the <tt>Runnable</tt> which is to be repetitively
     * executed on the application's <tt>EventQueue</tt> instance
     * @return a new <tt>Runnable</tt> which allows the repetitive execution of
     * the specified <tt>runnable</tT> on the application's <tt>EventQueue</tt>
     * instance
     */
    public static Runnable createRepetitiveInvokeLater(final Runnable runnable)
    {
        return
            new Runnable()
            {
                /**
                 * The <tt>AWTEvent</tt> instance which is to execute the
                 * specified <tt>runnable</tt> on {@link #eventQueue} i.e. the
                 * application's <tt>EventQueue</tt> instance.
                 */
                private AWTEvent event;

                /**
                 * The <tt>EventQueue</tt> instance on which {@link #event} is
                 * to execute the specified <tt>runnable</tt> i.e. the
                 * application's <tt>EventQueue</tt> instance. 
                 */
                private EventQueue eventQueue;

                /**
                 * The indicator which determines whether the execution of the
                 * specified <tt>runnable</tt> has already been scheduled and
                 * has not been performed yet. If <tt>true</tt>, invocations to
                 * {@link #run()} will do nothing.
                 */
                private boolean posted = false;

                /**
                 * Schedules the specified <tt>runnable</tt> to be executed
                 * (unless the execution has already been scheduled and has not
                 * been performed yet).
                 */
                public synchronized void run()
                {
                    if (posted)
                        return;

                    if (event == null)
                    {
                        Toolkit defaultToolkit = Toolkit.getDefaultToolkit();

                        eventQueue = defaultToolkit.getSystemEventQueue();
                        event
                            = new LowPriorityInvocationEvent(
                                    defaultToolkit,
                                    new Runnable()
                                    {
                                        public void run()
                                        {
                                            runInEvent();
                                        }
                                    });
                    }

                    eventQueue.postEvent(event);
                    posted = true;
                }

                /**
                 * Runs during the dispatch of {@link #event} and executed the
                 * specified <tt>runnable</tt>.
                 */
                private void runInEvent()
                {
                    synchronized (this)
                    {
                        posted = false;
                    }

                    runnable.run();
                }
            };
    }

    /**
     * Causes <tt>runnable</tt> to have its <tt>run</tt> method called in the
     * event dispatch thread with low priority.
     *
     * @param runnable the <tt>Runnable</tt> whose <tt>run</tt> method should be
     * executed synchronously on the <tt>EventQueue</tt>
     */
    public static void invokeLater(Runnable runnable)
    {
        Toolkit defaultToolkit = Toolkit.getDefaultToolkit();

        defaultToolkit.getSystemEventQueue().postEvent(
                new LowPriorityInvocationEvent(defaultToolkit, runnable));
    }

    /**
     * The <tt>LowPriorityInvocationEvent</tt> is an <tt>InvocationEvent</tt>
     * that replaces the default event id with the <tt>PaintEvent.UPDATE</tt>
     * in order to indicate that this event should be dispatched with the same
     * priority as an update paint event, which is normally with lower priority
     * than other events.
     */
    private static class LowPriorityInvocationEvent
        extends InvocationEvent
    {
        /**
         * Serial version UID.
         */
        private static final long serialVersionUID = 0L;

        public LowPriorityInvocationEvent(Object source, Runnable runnable)
        {
            super(source, PaintEvent.UPDATE, runnable, null, false);
        }
    }
}
