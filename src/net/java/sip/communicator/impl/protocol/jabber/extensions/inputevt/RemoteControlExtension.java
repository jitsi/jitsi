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

/**
 * This class implements input event extension.
 *
 * @author Sebastien Vincent
 */
public class RemoteControlExtension
    implements PacketExtension
{
    /**
     * AWT event that represents our <tt>RemoteControlExtension</tt>.
     */
    private final ComponentEvent event;

    /**
     * Size of the panel that contains video
     */
    private final Dimension videoPanelSize;

    /**
     * Constructor.
     *
     */
    public RemoteControlExtension()
    {
        videoPanelSize = null;
        event = null;
    }

    /**
     * Constructor.
     *
     * @param videoPanelSize size of the panel that contains video
     */
    public RemoteControlExtension(Dimension videoPanelSize)
    {
        this.videoPanelSize = videoPanelSize;
        this.event = null;
    }

    /**
     * Constructor.
     *
     * @param event AWT event
     */
    public RemoteControlExtension(ComponentEvent event)
    {
        this.event = event;
        this.videoPanelSize = null;
    }

    /**
     * Constructor.
     *
     * @param videoPanelSize size of the panel that contains video
     * @param event AWT event
     */
    public RemoteControlExtension(InputEvent event,
            Dimension videoPanelSize)
    {
        this.videoPanelSize = videoPanelSize;
        this.event = event;
    }

    /**
     * Get <tt>ComponentEvent</tt> that represents our
     * <tt>InputExtensionItem</tt>.
     *
     * @return AWT <tt>ComponentEvent</tt>
     */
    public ComponentEvent getEvent()
    {
        return event;
    }

    /**
     * Get the element name of the <tt>PacketExtension</tt>.
     *
     * @return "remote-control"
     */
    public String getElementName()
    {
        return RemoteControlExtensionProvider.ELEMENT_REMOTE_CONTROL;
    }

    /**
     * Returns the XML namespace of the extension sub-packet root element.
     * The namespace is always "http://jitsi.org/protocol/inputevt".
     *
     * @return the XML namespace of the packet extension.
     */
    public String getNamespace()
    {
        return RemoteControlExtensionProvider.NAMESPACE;
    }

    /**
     * Get the XML representation.
     *
     * @return XML representation of the item
     */
    public String toXML()
    {
        String ret = null;

        if(event == null)
        {
            return null;
        }

        if(event instanceof MouseEvent)
        {
            MouseEvent e = (MouseEvent)event;

            switch(e.getID())
            {
            case MouseEvent.MOUSE_DRAGGED:
            case MouseEvent.MOUSE_MOVED:
                if(videoPanelSize != null)
                {
                    Point p = e.getPoint();
                    double x = (p.getX() / videoPanelSize.width);
                    double y = (p.getY() / videoPanelSize.height);
                    ret = RemoteControlExtensionProvider.getMouseMovedXML(x, y);
                }
                break;
            case MouseEvent.MOUSE_WHEEL:
                MouseWheelEvent ew = (MouseWheelEvent)e;
                ret = RemoteControlExtensionProvider.getMouseWheelXML(
                        ew.getWheelRotation());
                break;
            case MouseEvent.MOUSE_PRESSED:
                ret = RemoteControlExtensionProvider.getMousePressedXML(
                        e.getModifiers());
                break;
            case MouseEvent.MOUSE_RELEASED:
                ret = RemoteControlExtensionProvider.getMouseReleasedXML(
                        e.getModifiers());
                break;
            default:
                break;
            }
        }
        else if(event instanceof KeyEvent)
        {
            KeyEvent e = (KeyEvent)event;
            int keycode = e.getKeyCode();
            int key = e.getKeyChar();

            if(key != KeyEvent.CHAR_UNDEFINED)
            {
                keycode = e.getKeyChar();
            }
            else
            {
                keycode = e.getKeyCode();
            }

            if(keycode == 0)
            {
                return null;
            }

            switch(e.getID())
            {
            case KeyEvent.KEY_PRESSED:
                ret = RemoteControlExtensionProvider.getKeyPressedXML(keycode);
                break;
            case KeyEvent.KEY_RELEASED:
                ret = RemoteControlExtensionProvider.getKeyReleasedXML(keycode);
                break;
            case KeyEvent.KEY_TYPED:
                ret = RemoteControlExtensionProvider.getKeyTypedXML(keycode);
                break;
            default:
                break;
            }
        }

        return ret;
    }
}
