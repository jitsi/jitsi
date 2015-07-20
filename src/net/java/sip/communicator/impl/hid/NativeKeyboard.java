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
package net.java.sip.communicator.impl.hid;

/**
 * Class used to interact with native keyboard.
 *
 * @author Sebastien Vincent
 */
public class NativeKeyboard
{
    static
    {
        System.loadLibrary("hid");
    }

    /**
     * Simulate a key press.
     *
     * @param key ascii representation of the key
     */
    public void keyPress(char key)
    {
        doKeyAction(key, true);
    }

    /**
     * Simulate a key release.
     *
     * @param key ascii representation of the key
     */
    public void keyRelease(char key)
    {
        doKeyAction(key, false);
    }

    /**
     * Simulate a symbol key press.
     *
     * @param symbol symbol name
     */
    public void symbolPress(String symbol)
    {
        doSymbolAction(symbol, true);
    }

    /**
     * Simulate a symbol key release.
     *
     * @param symbol symbol name
     */
    public  void symbolRelease(String symbol)
    {
        doSymbolAction(symbol, false);
    }

    /**
     * Native method to press or release a key.
     *
     * @param key ascii representation of the key
     * @param pressed if the key is pressed or not (i.e. released)
     */
    private static native void doKeyAction(char key, boolean pressed);

    /**
     * Native method to press or release a key.
     *
     * @param symbol symbol name
     * @param pressed if the key is pressed or not (i.e. released)
     */
    private static native void doSymbolAction(String symbol, boolean pressed);
}
