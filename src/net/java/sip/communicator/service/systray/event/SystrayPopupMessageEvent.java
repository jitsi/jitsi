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
package net.java.sip.communicator.service.systray.event;

import java.util.*;

/**
 * The <tt>SystrayPopupMessageEvent</tt>s are posted when user clicks on the
 * system tray popup message.
 *
 * @author Yana Stamcheva
 */
public class SystrayPopupMessageEvent
    extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /** an object to distinguish this <tt>SystrayPopupMessageEvent</tt> */
    private Object tag;

    /**
     * Constructs a new <tt>SystrayPopupMessageEvent</tt> object.
     *
     * @param source object on which the Event initially occurred
     */
    public SystrayPopupMessageEvent(Object source)
    {
        this(source, null);
    }

    /**
     * Creates a new <tt>SystrayPopupMessageEvent</tt> with the source of the
     * event and additional info provided by the popup handler.
     * @param source the source of the event
     * @param tag additional info for listeners
     */
    public SystrayPopupMessageEvent(Object source, Object tag)
    {
        super(source);
        this.tag = tag;
    }

    /**
     * @return the tag
     */
    public Object getTag()
    {
        return tag;
    }

    /**
     * @param tag the tag to set
     */
    public void setTag(Object tag)
    {
        this.tag = tag;
    }
}
