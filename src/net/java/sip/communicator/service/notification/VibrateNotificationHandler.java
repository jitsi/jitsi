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
 * The <tt>VibrateNotificationHandler</tt> interface is meant to be
 * implemented by the notification bundle in order to provide handling of
 * vibrate actions.
 *
 * @author Pawel Domas
 */
public interface VibrateNotificationHandler
    extends NotificationHandler
{

    /**
     * Perform vibration patter defined in given <tt>vibrateAction</tt>.
     *
     * @param vibrateAction the <tt>VibrateNotificationAction</tt> containing
     *                      vibration pattern details.
     */
    public void vibrate(VibrateNotificationAction vibrateAction);

    /**
     * Turn the vibrator off.
     */
    public void cancel();

}
