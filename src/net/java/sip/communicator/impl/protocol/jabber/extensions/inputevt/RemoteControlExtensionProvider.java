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
package net.java.sip.communicator.impl.protocol.jabber.extensions.inputevt;

import java.awt.*;
import java.awt.event.*;

import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.provider.*;
import org.xmlpull.v1.*;

/**
 * This class parses incoming remote-control XML element and extracts
 * input events such as keyboard and mouse ones.
 *
 * @author Sebastien Vincent
 */
public class RemoteControlExtensionProvider
    implements PacketExtensionProvider
{
    /**
     * The name of the remote-info XML element <tt>remote-control</tt>.
     */
    public static final String ELEMENT_REMOTE_CONTROL = "remote-control";

    /**
     * The name of the remote-info XML element <tt>mouse-move</tt>.
     */
    public static final String ELEMENT_MOUSE_MOVE = "mouse-move";

    /**
     * The name of the remote-info XML element <tt>mouse-wheel</tt>.
     */
    public static final String ELEMENT_MOUSE_WHEEL = "mouse-wheel";

    /**
     * The name of the remote-info XML element <tt>mouse-press</tt>.
     */
    public static final String ELEMENT_MOUSE_PRESS = "mouse-press";

    /**
     *The name of the remote-info XML element <tt>mouse-release</tt>.
     */
    public static final String ELEMENT_MOUSE_RELEASE = "mouse-release";

    /**
     * The name of the remote-info XML element <tt>key-press</tt>.
     */
    public static final String ELEMENT_KEY_PRESS = "key-press";

    /**
     * The name of the remote-info XML element <tt>key-release</tt>.
     */
    public static final String ELEMENT_KEY_RELEASE = "key-release";

    /**
     * The name of the remote-info XML element <tt>key-type</tt>.
     */
    public static final String ELEMENT_KEY_TYPE = "key-type";

    /**
     * Namespace of this extension.
     */
    public static final String NAMESPACE =
        "http://jitsi.org/protocol/inputevt";

    /**
     * Component to be used in custom generated <tt>MouseEvent</tt> and
     * <tt>KeyEvent</tt>.
     */
    private static final Component component = new Canvas();

    /**
     * Constructor.
     */
    public RemoteControlExtensionProvider()
    {
    }

    /**
     * Parses the extension and returns a <tt>PacketExtension</tt>.
     *
     * @param parser XML parser
     * @return a <tt>PacketExtension</tt> that represents a remote-control
     * element.
     * @throws Exception if an error occurs during XML parsing
     */
    public PacketExtension parseExtension(XmlPullParser parser)
        throws Exception
    {
        RemoteControlExtension result = null;
        boolean done = false;

        while (!done)
        {
            try
            {
                int eventType = parser.next();

                if (eventType == XmlPullParser.START_TAG)
                {
                    if(parser.getName().equals(ELEMENT_MOUSE_MOVE))
                    {
                        String attr = parser.getAttributeValue("", "x");
                        String attr2 = parser.getAttributeValue("", "y");
                        if(attr != null && attr2 != null)
                        {
                            int x = (int)(Double.
                                    parseDouble(attr) * 1000);
                            int y = (int)(Double.
                                    parseDouble(attr2) * 1000);

                            MouseEvent me = new MouseEvent(component,
                                MouseEvent.MOUSE_MOVED,
                                System.currentTimeMillis(),
                                0, x, y, 0, false, 0);

                            result = new RemoteControlExtension(me);
                            continue;
                        }
                    }

                    if(parser.getName().equals(ELEMENT_MOUSE_WHEEL))
                    {
                        String attr = parser.getAttributeValue("", "notch");
                        if(attr != null)
                        {
                            MouseWheelEvent me = new MouseWheelEvent(
                                    component, MouseEvent.MOUSE_WHEEL,
                                    System.currentTimeMillis(),
                                    0, 0, 0, 0, false, 0, 0,
                                    Integer.parseInt(attr));


                            result = new RemoteControlExtension(me);
                            continue;
                        }
                    }

                    if(parser.getName().equals(ELEMENT_MOUSE_PRESS))
                    {
                        String attr = parser.getAttributeValue("", "btns");
                        if(attr != null)
                        {
                            MouseEvent me = new MouseEvent(component,
                                MouseEvent.MOUSE_PRESSED,
                                System.currentTimeMillis(),
                                Integer.parseInt(attr),
                                0, 0, 0, false, 0);

                            result = new RemoteControlExtension(me);
                            continue;
                        }
                    }

                    if(parser.getName().equals(ELEMENT_MOUSE_RELEASE))
                    {
                        String attr = parser.getAttributeValue("", "btns");
                        if(attr != null)
                        {
                            MouseEvent me = new MouseEvent(component,
                                MouseEvent.MOUSE_RELEASED,
                                System.currentTimeMillis(),
                                Integer.parseInt(attr),
                                0, 0, 0, false, 0);

                            result = new RemoteControlExtension(me);
                            continue;
                        }
                    }

                    if(parser.getName().equals(ELEMENT_KEY_PRESS))
                    {
                        String attr = parser.getAttributeValue("", "keycode");
                        if(attr != null)
                        {
                            KeyEvent ke = new KeyEvent(component,
                                    KeyEvent.KEY_PRESSED,
                                    System.currentTimeMillis(),
                                    0,
                                    Integer.parseInt(attr),
                                    (char)0);

                            result = new RemoteControlExtension(ke);
                            continue;
                        }
                    }

                    if(parser.getName().equals(ELEMENT_KEY_RELEASE))
                    {
                        String attr = parser.getAttributeValue("", "keycode");
                        if(attr != null)
                        {
                            KeyEvent ke = new KeyEvent(component,
                                    KeyEvent.KEY_RELEASED,
                                    System.currentTimeMillis(),
                                    0,
                                    Integer.parseInt(attr),
                                    (char)0);

                            result = new RemoteControlExtension(ke);
                            continue;
                        }
                    }

                    if(parser.getName().equals(ELEMENT_KEY_TYPE))
                    {
                        String attr = parser.getAttributeValue("", "keychar");
                        if(attr != null)
                        {
                            KeyEvent ke = new KeyEvent(component,
                                    KeyEvent.KEY_TYPED,
                                    System.currentTimeMillis(),
                                    0,
                                    0,
                                    (char)Integer.parseInt(attr));

                            result = new RemoteControlExtension(ke);
                            continue;
                        }
                    }
                }
                else if (eventType == XmlPullParser.END_TAG)
                {
                    if (parser.getName().equals(
                            RemoteControlExtensionProvider.
                                ELEMENT_REMOTE_CONTROL))
                    {
                        done = true;
                    }
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        if(result == null)
        {
            /* we are not allowed to return null otherwise the parser goes
             * crazy
             */
            result = new RemoteControlExtension(new ComponentEvent(component,
                    0));
        }

        return result;
    }

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
     * Build a key-press remote-control XML element.
     *
     * @param keycode keyboard's code
     * @return raw XML bytes
     */
    public static String getKeyPressedXML(int keycode)
    {
        StringBuffer xml = new StringBuffer();

        append(xml, "<" + ELEMENT_REMOTE_CONTROL + " xmlns=\"" + NAMESPACE
                + "\">");
        // <key-press>
        append(xml, "<", RemoteControlExtensionProvider.ELEMENT_KEY_PRESS);
        append(xml, " keycode=\"", Integer.toString(keycode), "\"/>");
        append(xml, "</" + ELEMENT_REMOTE_CONTROL + ">");
        return xml.toString();
    }

    /**
     * Build a key-release remote-control XML element.
     *
     * @param keycode keyboard's code
     * @return raw XML bytes
     */
    public static String getKeyReleasedXML(int keycode)
    {
        StringBuffer xml = new StringBuffer();

        append(xml, "<" + ELEMENT_REMOTE_CONTROL + " xmlns=\"" + NAMESPACE
                + "\">");
        // <key-release>
        append(xml, "<", RemoteControlExtensionProvider.ELEMENT_KEY_RELEASE);
        append(xml, " keycode=\"", Integer.toString(keycode), "\"/>");
        append(xml, "</" + ELEMENT_REMOTE_CONTROL + ">");
        return xml.toString();
    }

    /**
     * Build a key-typed remote-control XML element.
     *
     * @param keycode keyboard's code
     * @return raw XML bytes
     */
    public static String getKeyTypedXML(int keycode)
    {
        StringBuffer xml = new StringBuffer();

        append(xml, "<" + ELEMENT_REMOTE_CONTROL + " xmlns=\"" + NAMESPACE
                + "\">");
        // <key-typed>
        append(xml, "<", RemoteControlExtensionProvider.ELEMENT_KEY_TYPE);
        append(xml, " keychar=\"", Integer.toString(keycode), "\"/>");
        append(xml, "</" + ELEMENT_REMOTE_CONTROL + ">");
        return xml.toString();
    }

    /**
     * Build a mouse-press remote-control XML element.
     *
     * @param btns button mask
     * @return raw XML bytes
     */
    public static String getMousePressedXML(int btns)
    {
        StringBuffer xml = new StringBuffer();

        append(xml, "<" + ELEMENT_REMOTE_CONTROL + " xmlns=\"" + NAMESPACE
                + "\">");
        // <mouse-press>
        append(xml, "<", RemoteControlExtensionProvider.ELEMENT_MOUSE_PRESS);
        append(xml, " btns=\"", Integer.toString(btns), "\"/>");
        append(xml, "</" + ELEMENT_REMOTE_CONTROL + ">");
        return xml.toString();
    }

    /**
     * Build a remote-info mouse-release remote-control XML element.
     *
     * @param btns button mask
     * @return raw XML bytes
     */
    public static String getMouseReleasedXML(int btns)
    {
        StringBuffer xml = new StringBuffer();

        append(xml, "<" + ELEMENT_REMOTE_CONTROL + " xmlns=\"" + NAMESPACE
                + "\">");
        // <mouse-release>
        append(xml, "<", RemoteControlExtensionProvider.ELEMENT_MOUSE_RELEASE);
        append(xml, " btns=\"", Integer.toString(btns), "\"/>");
        append(xml, "</" + ELEMENT_REMOTE_CONTROL + ">");
        return xml.toString();
    }

    /**
     * Build a remote-info mouse-move remote-control XML element.
     *
     * @param x x position of the mouse
     * @param y y position of the mouse
     * @return raw XML bytes
     */
    public static String getMouseMovedXML(double x, double y)
    {
        StringBuffer xml = new StringBuffer();

        append(xml, "<" + ELEMENT_REMOTE_CONTROL + " xmlns=\"" + NAMESPACE
                + "\">");
        // <mouse-press>
        append(xml, "<", RemoteControlExtensionProvider.ELEMENT_MOUSE_MOVE);
        append(xml, " x=\"", Double.toString(x), "\" y=\"", Double.toString(y),
                "\"/>");
        append(xml, "</" + ELEMENT_REMOTE_CONTROL + ">");
        return xml.toString();
    }

    /**
     * Build a remote-info mouse-wheel remote-control XML element.
     *
     * @param notch wheel notch
     * @return raw XML bytes
     */
    public static String getMouseWheelXML(int notch)
    {
        StringBuffer xml = new StringBuffer();

        append(xml, "<" + ELEMENT_REMOTE_CONTROL + " xmlns=\"" + NAMESPACE
                + "\">");
        // <mouse-wheel>
        append(xml, "<", RemoteControlExtensionProvider.ELEMENT_MOUSE_WHEEL);
        append(xml, " notch=\"", Integer.toString(notch), "\"/>");
        append(xml, "</" + ELEMENT_REMOTE_CONTROL + ">");
        return xml.toString();
    }
}
