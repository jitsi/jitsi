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
package net.java.sip.communicator.service.notification;

/**
 * <tt>VibrateNotificationAction</tt> is meant to define haptic feedback
 * notification using device's vibrator.<br/><br/>
 *
 * Given array of <tt>long</tt> are
 * the duration for which to turn on or off the vibrator in miliseconds.
 * The first value indicates the number of miliseconds to wait before turning
 * the vibrator on. The next value indicates the number of miliseconds for which
 * to keep the vibrator on before turning it off and so on.<br/><br/>
 *
 * The <tt>repeat</tt> parameter is an index into the pattern at which it will
 * be looped until the {@link VibrateNotificationHandler#cancel()} method is
 * called.
 *
 * @author Pawel Domas
 */
public class VibrateNotificationAction
    extends NotificationAction
{
    /**
     * The patter of off/on intervals in milis that will be played.
     */
    private final long[] pattern;

    /**
     * Repeat index into the pattern(-1 to disable repeat).
     */
    private final int repeat;

    /**
     * Descriptor that can be used to identify action.
     */
    private final String descriptor;

    /**
     * Vibrate constantly for the specified period of time.
     *
     * @param descriptor string identifier of this action.
     * @param milis the number of miliseconds to vibrate.
     */
    public VibrateNotificationAction(String descriptor, long milis)
    {
        super(NotificationAction.ACTION_VIBRATE);
        this.pattern = new long[2];
        pattern[0] = 0;
        pattern[1] = milis;
        repeat = -1;
        this.descriptor = descriptor;
    }

    /**
     * Vibrate using given <tt>patter</tt> and optionally loop if the
     * <tt>repeat</tt> index is not <tt>-1</tt>.
     *
     * @param descriptor the string identifier of this action.
     * @param patter the array containing vibrate pattern intervals.
     * @param repeat the index into the patter at which it will be looped
     *               (-1 to disable repeat).
     *
     * @see VibrateNotificationAction
     */
    public VibrateNotificationAction( String descriptor,
                                      long[] patter,
                                      int repeat )
    {
        super(NotificationAction.ACTION_VIBRATE);
        this.pattern = patter;
        this.repeat = repeat;
        this.descriptor = descriptor;
    }

    /**
     * The string identifier of this action.
     *
     * @return string identifier of this action which can be used to distinguish
     *         different actions.
     */
    public String getDescriptor()
    {
        return descriptor;
    }

    /**
     * Returns vibrate pattern array.
     * @return vibrate pattern array.
     */
    public long[] getPattern()
    {
        return pattern;
    }

    /**
     * The index at which the pattern shall be looped during playback
     * or <tt>-1</tt> to play it once.
     *
     * @return the index at which the pattern will be looped or <tt>-1</tt> to
     *         play it once.
     */
    public int getRepeat()
    {
        return repeat;
    }
}
