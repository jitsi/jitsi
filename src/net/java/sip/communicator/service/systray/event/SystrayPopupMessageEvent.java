/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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
    public SystrayPopupMessageEvent(Object source)
    {
        super(source);
    }
}
