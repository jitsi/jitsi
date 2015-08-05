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

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import org.w3c.dom.*;
// disambiguation

/**
 * Utility class to provide XML definition for the desktop sharing SIP
 * event package.
 *
 * @author Sebastien Vincent
 */
public class DesktopSharingProtocolSipImpl
{
    /**
     * The name of the event package supported by
     * <tt>OperationSetDesktopSharingServerSipImpl</tt> in SUBSCRIBE and NOTIFY
     * requests.
     */
    public static final String EVENT_PACKAGE = "remote-control";

    /**
     * The time in seconds before the expiration of a <tt>Subscription</tt> at
     * which the <tt>OperationSetDesktopSharingServerSipImpl</tt> instance
     * managing it should refresh it.
     */
    public static final int REFRESH_MARGIN = 60;

    /**
     * The time in seconds after which a <tt>Subscription</tt> should be expired
     * by the <tt>OperationSetDesktopSharingServerSipImpl</tt> instance which
     * manages it.
     */
    public static final int SUBSCRIPTION_DURATION = 3600;

    /**
     * The content sub-type of the content supported in NOTIFY requests handled
     * by <tt>OperationSetDesktopSharingSipImpl</tt>.
     */
    public static final String CONTENT_SUB_TYPE = "remote-control+xml";

    /**
     * The name of the remote-info XML element <tt>remote-control</tt>.
     */
    private static final String ELEMENT_REMOTE_CONTROL = "remote-control";

    /**
     * The name of the remote-info XML element <tt>mouse-move</tt>.
     */
    private static final String ELEMENT_MOUSE_MOVE = "mouse-move";

    /**
     * The name of the remote-info XML element <tt>mouse-wheel</tt>.
     */
    private static final String ELEMENT_MOUSE_WHEEL = "mouse-wheel";

    /**
     * The name of the remote-info XML element <tt>mouse-press</tt>.
     */
    private static final String ELEMENT_MOUSE_PRESS = "mouse-press";

    /**
     *The name of the remote-info XML element <tt>mouse-release</tt>.
     */
    private static final String ELEMENT_MOUSE_RELEASE = "mouse-release";

    /**
     * The name of the remote-info XML element <tt>key-press</tt>.
     */
    private static final String ELEMENT_KEY_PRESS = "key-press";

    /**
     * The name of the remote-info XML element <tt>key-release</tt>.
     */
    private static final String ELEMENT_KEY_RELEASE = "key-release";

    /**
     * The name of the remote-info XML element <tt>key-type</tt>.
     */
    private static final String ELEMENT_KEY_TYPE = "key-type";

    /**
     * Component to be used in custom generated <tt>MouseEvent</tt> and
     * <tt>KeyEvent</tt>.
     */
    private static final Component component = new Canvas();

    /**
     * Appends a specific array of <tt>String</tt>s to a specific
     * <tt>StringBuffer</tt>.
     *
     * @param stringBuffer the <tt>StringBuffer</tt> to append the specified
     * <tt>strings</tt> to
     * @param strings the <tt>String</tt> values to be appended to the specified
     * <tt>stringBuffer</tt>
     */
    private static void append(StringBuffer stringBuffer, String... strings)
    {
        for (String str : strings)
            stringBuffer.append(str);
    }

    /**
     * Build a remote-info key-press SIP NOTIFY message.
     *
     * @param keycode keyboard's code
     * @return raw XML bytes
     */
    public static String getKeyPressedXML(int keycode)
    {
        StringBuffer xml = new StringBuffer();

        xml.append( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n");

        // <remote-control>
        append(xml, "<", ELEMENT_REMOTE_CONTROL, ">");
        // <key-press>
        append(xml, "<", ELEMENT_KEY_PRESS);
        append(xml, " keycode=\"", Integer.toString(keycode), "\" />");
        append(xml, "</", ELEMENT_REMOTE_CONTROL, ">");

        return xml.toString();
    }

    /**
     * Build a remote-info key-release SIP NOTIFY message.
     *
     * @param keycode keyboard's code
     * @return raw XML bytes
     */
    public static String getKeyReleasedXML(int keycode)
    {
        StringBuffer xml = new StringBuffer();

        xml.append( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n");

        // <remote-control>
        append(xml, "<", ELEMENT_REMOTE_CONTROL, ">");
        // <key-release>
        append(xml, "<", ELEMENT_KEY_RELEASE);
        append(xml, " keycode=\"", Integer.toString(keycode), "\" />");
        append(xml, "</", ELEMENT_REMOTE_CONTROL, ">");

        return xml.toString();
    }

    /**
     * Build a remote-info key-typed SIP NOTIFY message.
     *
     * @param keycode keyboard's code
     * @return raw XML bytes
     */
    public static String getKeyTypedXML(int keycode)
    {
        StringBuffer xml = new StringBuffer();

        xml.append( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n");

        // <remote-control>
        append(xml, "<", ELEMENT_REMOTE_CONTROL, ">");
        // <key-typed>
        append(xml, "<", ELEMENT_KEY_TYPE);
        append(xml, " keychar=\"", Integer.toString(keycode), "\" />");
        append(xml, "</", ELEMENT_REMOTE_CONTROL, ">");

        return xml.toString();
    }

    /**
     * Build a remote-info mouse-press SIP NOTIFY message.
     *
     * @param btns button mask
     * @return raw XML bytes
     */
    public static String getMousePressedXML(int btns)
    {
        StringBuffer xml = new StringBuffer();

        xml.append( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n");

        // <remote-control>
        append(xml, "<", ELEMENT_REMOTE_CONTROL, ">");
        // <mouse-press>
        append(xml, "<", ELEMENT_MOUSE_PRESS);
        append(xml, " btns=\"", Integer.toString(btns), "\" />");
        append(xml, "</", ELEMENT_REMOTE_CONTROL, ">");

        return xml.toString();
    }

    /**
     * Build a remote-info mouse-release SIP NOTIFY message.
     *
     * @param btns button mask
     * @return raw XML bytes
     */
    public static String getMouseReleasedXML(int btns)
    {
        StringBuffer xml = new StringBuffer();

        xml.append( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n");

        // <remote-control>
        append(xml, "<", ELEMENT_REMOTE_CONTROL, ">");
        // <mouse-release>
        append(xml, "<", ELEMENT_MOUSE_RELEASE);
        append(xml, " btns=\"", Integer.toString(btns), "\" />");
        append(xml, "</", ELEMENT_REMOTE_CONTROL, ">");

        return xml.toString();
    }

    /**
     * Build a remote-info mouse-move SIP NOTIFY message.
     *
     * @param x x position of the mouse
     * @param y y position of the mouse
     * @return raw XML bytes
     */
    public static String getMouseMovedXML(double x, double y)
    {
        StringBuffer xml = new StringBuffer();

        xml.append( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n");

        // <remote-control>
        append(xml, "<", ELEMENT_REMOTE_CONTROL, ">");
        // <mouse-press>
        append(xml, "<", ELEMENT_MOUSE_MOVE);
        append(xml, " x=\"", Double.toString(x), "\" y=\"", Double.toString(y),
                "\" />");
        append(xml, "</", ELEMENT_REMOTE_CONTROL, ">");

        return xml.toString();
    }

    /**
     * Build a remote-info mouse-wheel SIP NOTIFY message.
     *
     * @param notch wheel notch
     * @return raw XML bytes
     */
    public static String getMouseWheelXML(int notch)
    {
        StringBuffer xml = new StringBuffer();

        xml.append( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n");

        // <remote-control>
        append(xml, "<", ELEMENT_REMOTE_CONTROL, ">");
        // <mouse-wheel>
        append(xml, "<", ELEMENT_MOUSE_WHEEL);
        append(xml, " notch=\"", Integer.toString(notch), "\" />");
        append(xml, "</", ELEMENT_REMOTE_CONTROL, ">");

        return xml.toString();
    }

    /**
     * Parses an XML element and returns a list of all <tt>MouseEvent</tt>
     * and <tt>KeyEvent</tt> found.
     *
     * @param root XML root element
     * @param size size of the video (used to have right (x,y) for MouseMoved
     * and MouseDragged
     * @param origin origin coordinate (upper left corner)
     * @return list of <tt>java.awt.Event</tt>
     */
    public static List<ComponentEvent> parse(Element root, Dimension size,
            Point origin)
    {
        List<ComponentEvent> events = new ArrayList<ComponentEvent>();
        NodeList nl = null;
        int originX = origin != null ? origin.x : 0;
        int originY = origin != null ? origin.y : 0;

        nl = root.getElementsByTagName(ELEMENT_MOUSE_PRESS);
        if(nl != null)
        {
            for(int i = 0 ; i < nl.getLength() ; i++)
            {
                Element el = (Element)nl.item(i);
                if(el.hasAttribute("btns"))
                {
                    MouseEvent me = new MouseEvent(component,
                        MouseEvent.MOUSE_PRESSED,
                        System.currentTimeMillis(),
                        Integer.parseInt(el.getAttribute("btns")),
                        0, 0, 0, false, 0);

                    events.add(me);
                }
            }
        }

        nl = root.getElementsByTagName(ELEMENT_MOUSE_RELEASE);
        if(nl != null)
        {
            for(int i = 0 ; i < nl.getLength() ; i++)
            {
                Element el = (Element)nl.item(i);
                if(el.hasAttribute("btns"))
                {
                    MouseEvent me = new MouseEvent(component,
                        MouseEvent.MOUSE_RELEASED,
                        System.currentTimeMillis(),
                        Integer.parseInt(el.getAttribute("btns")),
                        0, 0, 0, false, 0);
                    events.add(me);
                }
            }
        }

        nl = root.getElementsByTagName(ELEMENT_MOUSE_MOVE);
        if(nl != null)
        {
            int x = -1;
            int y = -1;

            for(int i = 0 ; i < nl.getLength() ; i++)
            {
                Element el = (Element)nl.item(i);

                if(el.hasAttribute("x"))
                {
                    x = (int)(Double.parseDouble(
                            el.getAttribute("x")) * size.width + originX);
                }

                if(el.hasAttribute("y"))
                {
                    y = (int)(Double.parseDouble(
                            el.getAttribute("y")) * size.height + originY);
                }

                //if(x >= 0 && y >= 0)
                {
                    MouseEvent me = new MouseEvent(component,
                            MouseEvent.MOUSE_MOVED,
                            System.currentTimeMillis(),
                            0, x, y, 0, false, 0);

                    events.add(me);
                }
            }
        }

        nl = root.getElementsByTagName(ELEMENT_MOUSE_WHEEL);
        if(nl != null)
        {
            for(int i = 0 ; i < nl.getLength() ; i++)
            {
                Element el = (Element)nl.item(i);
                if(el.hasAttribute("notch"))
                {
                    MouseWheelEvent me = new MouseWheelEvent(
                            component, MouseEvent.MOUSE_WHEEL,
                            System.currentTimeMillis(),
                            0, 0, 0, 0, false, 0, 0,
                            Integer.parseInt(el.getAttribute(
                                    "notch")));
                    events.add(me);
                }
            }
        }

        nl = root.getElementsByTagName(ELEMENT_KEY_PRESS);
        if(nl != null)
        {
            for(int i = 0 ; i < nl.getLength() ; i++)
            {
                Element el = (Element)nl.item(i);
                if(el.hasAttribute("keycode"))
                {
                    KeyEvent ke = new KeyEvent(component,
                       KeyEvent.KEY_PRESSED,
                       System.currentTimeMillis(),
                       0,
                       Integer.parseInt(el.getAttribute("keycode")),
                       (char)0);

                    events.add(ke);
                }
            }
        }

        nl = root.getElementsByTagName(ELEMENT_KEY_RELEASE);
        if(nl != null)
        {
            for(int i = 0 ; i < nl.getLength() ; i++)
            {
                Element el = (Element)nl.item(i);
                if(el.hasAttribute("keycode"))
                {
                    KeyEvent ke = new KeyEvent(component,
                       KeyEvent.KEY_RELEASED,
                       System.currentTimeMillis(),
                       0,
                       Integer.parseInt(el.getAttribute("keycode")),
                       (char)0);

                    events.add(ke);
                }
            }
        }

        nl = root.getElementsByTagName(ELEMENT_KEY_TYPE);
        if(nl != null)
        {
            for(int i = 0 ; i < nl.getLength() ; i++)
            {
                Element el = (Element)nl.item(i);
                if(el.hasAttribute("keychar"))
                {
                    KeyEvent ke = new KeyEvent(component,
                        KeyEvent.KEY_TYPED,
                        System.currentTimeMillis(),
                        0,
                        0,
                        (char)Integer.parseInt(
                                el.getAttribute("keychar")));

                    events.add(ke);
                }
            }
        }
        return events;
    }
}
