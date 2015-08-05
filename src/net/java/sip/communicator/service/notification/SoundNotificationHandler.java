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
 * The <tt>SoundNotificationHandler</tt> interface is meant to be
 * implemented by the notification bundle in order to provide handling of
 * sound actions.
 *
 * @author Yana Stamcheva
 */
public interface SoundNotificationHandler
    extends NotificationHandler
{
    /**
     * Start playing the sound pointed by <tt>getDescriotor</tt>. This
     * method should check the loopInterval value to distinguish whether to play
     * a simple sound or to play it in loop.
     * @param action the action to act upon
     * @param data Additional data for the event.
     */
    public void start(SoundNotificationAction action, NotificationData data);

    /**
     * Stops playing the sound pointing by <tt>getDescriptor</tt>. This method
     * is meant to be used to stop sounds that are played in loop.
     * @param data Additional data for the event.
     */
    public void stop(NotificationData data);

    /**
     * Stops/Restores all currently playing sounds.
     *
     * @param isMute mute or not currently playing sounds
     */
    public void setMute(boolean isMute);

    /**
     * Specifies if currently the sound is off.
     *
     * @return TRUE if currently the sound is off, FALSE otherwise
     */
    public boolean isMute();

    /**
     * Tells if the given notification sound is currently played.
     *
     * @param data Additional data for the event.
     */
    public boolean isPlaying(NotificationData data);
}
