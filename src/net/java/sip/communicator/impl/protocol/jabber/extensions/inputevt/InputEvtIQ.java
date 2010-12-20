/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.inputevt;

import java.util.*;

import org.jivesoftware.smack.packet.*;

/**
 * Input event IQ. It is used to transfer key and mouse events through XMPP.
 *
 * @author Sebastien Vincent
 */
public class InputEvtIQ extends IQ
{
    /**
     * The namespace that input event belongs to.
     */
    public static final String NAMESPACE =
        "http://sip-communicator.org/protocol/inputevt";

    /**
     * The name of the element that contains the input event data.
     */
    public static final String ELEMENT_NAME = "inputevt";

    /**
     * The name of the argument that contains the input action value.
     */
    public static final String ACTION_ATTR_NAME = "action";

    /**
     * Action of this <tt>InputIQ</tt>.
     */
    private InputEvtAction action = null;

    /**
     * List of remote-control elements.
     */
    private List<RemoteControlExtension> remoteControls =
        new ArrayList<RemoteControlExtension>();

    /**
     * Constructor.
     */
    public InputEvtIQ()
    {
    }

    /**
     * Get the XML representation of the IQ.
     *
     * @return XML representation of the IQ
     */
    @Override
    public String getChildElementXML()
    {
        StringBuilder bldr = new StringBuilder("<" + ELEMENT_NAME);

        bldr.append(" xmlns='" + NAMESPACE + "'");

        bldr.append(" " + ACTION_ATTR_NAME + "='" + getAction() + "'");

        if(remoteControls.size() > 0)
        {
            bldr.append(">");

            for(RemoteControlExtension p : remoteControls)
                bldr.append(p.toXML());

            bldr.append("</" + ELEMENT_NAME + ">");
        }
        else
        {
            bldr.append("/>");
        }

        return bldr.toString();
    }

    /**
     * Sets the value of this element's <tt>action</tt> attribute. The value of
     * the 'action' attribute MUST be one of the values enumerated here. If an
     * entity receives a value not defined here, it MUST ignore the attribute
     * and MUST return a <tt>bad-request</tt> error to the sender. There is no
     * default value for the 'action' attribute.
     *
     * @param action the value of the <tt>action</tt> attribute.
     */
    public void setAction(InputEvtAction action)
    {
        this.action = action;
    }

    /**
     * Returns the value of this element's <tt>action</tt> attribute. The value
     * of the 'action' attribute MUST be one of the values enumerated here. If
     * an entity receives a value not defined here, it MUST ignore the attribute
     * and MUST return a <tt>bad-request</tt> error to the sender. There is no
     * default value for the 'action' attribute.
     *
     * @return the value of the <tt>action</tt> attribute.
     */
    public InputEvtAction getAction()
    {
        return action;
    }

    /**
     * Add a remote-control extension.
     *
     * @param item remote-control extension
     */
    public void addRemoteControl(RemoteControlExtension item)
    {
        remoteControls.add(item);
    }

    /**
     * Remove a remote-control extension.
     *
     * @param item remote-control extension
     */
    public void removeRemoteControl(RemoteControlExtension item)
    {
        remoteControls.remove(item);
    }

    /**
     * Get the <tt>RemoteControlExtension</tt> list of this IQ.
     *
     * @return list of <tt>RemoteControlExtension</tt>
     */
    public List<RemoteControlExtension> getRemoteControls()
    {
        return remoteControls;
    }
}
