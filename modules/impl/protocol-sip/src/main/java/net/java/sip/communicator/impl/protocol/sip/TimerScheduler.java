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
package net.java.sip.communicator.impl.protocol.sip;

import java.util.*;

/**
 * Represents an analogy of <code>Timer</code> which does not have the
 * disadvantage of <code>Timer</code> to always create its thread at
 * construction time. It also allows the currently scheduled
 * <code>TimerTask</code>s to be canceled while still being able to schedule new
 * <code>TimerTask</code>s later on.
 *
 * @author Lubomir Marinov
 */
public class TimerScheduler
{

    /**
     * The timer which will handle all scheduled tasks.
     */
    private Timer timer;

    /**
     * Discarding any currently scheduled <code>TimerTask</code>s.
     */
    public synchronized void cancel()
    {
       if (timer != null)
       {
           timer.cancel();
           timer = null;
       }
    }

    /**
     * Gets the timer which handles all scheduled tasks. If it still doesn't
     * exists, a new <tt>Timer</tt> is created.
     *
     * @return the <tt>Timer</tt> which handles all scheduled tasks
     */
    private synchronized Timer getTimer()
    {
       if (timer == null)
           timer = new Timer(true);
       return timer;
    }

    /**
     * Schedules the specified <code>TimerTask</code> for execution after the
     * specified delay.
     *
     * @param task
     *            the <code>TimerTask</code> to be executed after the specified
     *            delay
     * @param delay
     *            the delay in milliseconds before the specified
     *            <code>TimerTask</code> is executed
     */
    public synchronized void schedule(TimerTask task, long delay)
    {
        getTimer().schedule(task, delay);
    }

    /**
     * Schedules the specified <code>TimerTask</code> for repeated fixed-delay
     * execution, beginning after the specified delay. Subsequent executions
     * take place at approximately regular intervals separated by the specified
     * period.
     *
     * @param task
     *            the <code>TimerTask</code> to be scheduled
     * @param delay
     *            the delay in milliseconds before the specified
     *            <code>TimerTask</code> is executed
     * @param period
     *            the time in milliseconds between successive executions of the
     *            specified <code>TimerTask</code>
     */
    public synchronized void schedule(TimerTask task, long delay, long period)
    {
        getTimer().schedule(task, delay, period);
    }
}
