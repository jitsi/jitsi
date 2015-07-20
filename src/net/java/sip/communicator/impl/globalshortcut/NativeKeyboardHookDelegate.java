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

/**
 * NativeKeyboardHookDelegate interface.
 *
 * @author Sebastien Vincent
 */
public interface NativeKeyboardHookDelegate
{
    /**
     * CTRL modifier.
     */
    public static final int MODIFIERS_CTRL = 1;

    /**
     * ALT modifier.
     */
    public static final int MODIFIERS_ALT = 2;

    /**
     * SHIFT modifier.
     */
    public static final int MODIFIERS_SHIFT = 4;

    /**
     * Logo modifier (i.e. CMD/Apple key on Mac OS X, Windows key on
     * MS Windows).
     */
    public static final int MODIFIERS_LOGO = 8;

   /**
     * Receive a key press event.
     *
     * @param keycode keycode received
     * @param modifiers modifiers received (ALT or CTRL + letter, ...)
     * @param onRelease this parameter is true if the shortcut is released
     */
    public void receiveKey(int keycode, int modifiers, boolean onRelease);
}
