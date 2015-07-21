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
package net.java.sip.communicator.service.systray;

import java.util.*;

import net.java.sip.communicator.service.systray.event.*;

/**
 * Abstract base implementation of <tt>PopupMessageHandler</tt> which
 * facilitates the full implementation of the interface.
 *
 * @author Lubomir Marinov
 */
public abstract class AbstractPopupMessageHandler
    implements PopupMessageHandler
{

    /**
     * The list of <tt>SystrayPopupMessageListener</tt>s registered with this
     * instance.
     */
    private final List<SystrayPopupMessageListener> popupMessageListeners
        = new Vector<SystrayPopupMessageListener>();

    /**
     * Adds a <tt>SystrayPopupMessageListener</tt> to this instance so that it
     * receives <tt>SystrayPopupMessageEvent</tt>s.
     *
     * @param listener the <tt>SystrayPopupMessageListener</tt> to be added to
     * this instance
     * @see PopupMessageHandler#addPopupMessageListener(
     * SystrayPopupMessageListener)
     */
    public void addPopupMessageListener(SystrayPopupMessageListener listener)
    {
        synchronized (popupMessageListeners)
        {
            if (!popupMessageListeners.contains(listener))
                popupMessageListeners.add(listener);
        }
    }

    /**
     * Notifies the <tt>SystrayPopupMessageListener</tt>s registered with this
     * instance that a <tt>SystrayPopupMessageEvent</tt> has occurred.
     *
     * @param evt the <tt>SystrayPopupMessageEvent</tt> to be fired to the
     * <tt>SystrayPopupMessageListener</tt>s registered with this instance
     */
    protected void firePopupMessageClicked(SystrayPopupMessageEvent evt)
    {
        List<SystrayPopupMessageListener> listeners;

        synchronized (popupMessageListeners)
        {
            listeners
                = new ArrayList<SystrayPopupMessageListener>(
                        popupMessageListeners);
        }

        for (SystrayPopupMessageListener listener : listeners)
            listener.popupMessageClicked(evt);
    }

    /**
     * Removes a <tt>SystrayPopupMessageListener</tt> from this instance so that
     * it no longer receives <tt>SystrayPopupMessageEvent</tt>s.
     *
     * @param listener the <tt>SystrayPopupMessageListener</tt> to be removed
     * from this instance
     * @see PopupMessageHandler#removePopupMessageListener(
     * SystrayPopupMessageListener)
     */
    public void removePopupMessageListener(SystrayPopupMessageListener listener)
    {
        synchronized (popupMessageListeners)
        {
            popupMessageListeners.remove(listener);
        }
    }
}
