/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;
import java.awt.event.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.swing.*;

/**
 * Represents an UI means to transfer (the <tt>Call</tt> of) an associated
 * <tt>CallPariticant</tt>.
 *
 * @author Lubomir Marinov
 */
public class TransferCallButton
    extends SIPCommButton
{
    /**
     * The <tt>CallPeer</tt> (whose <tt>Call</tt> is) to be
     * transfered.
     */
    private final CallPeer callPeer;

    /**
     * Initializes a new <tt>TransferCallButton</tt> instance which is to
     * transfer (the <tt>Call</tt> of) a specific
     * <tt>CallPeer</tt>.
     *
     * @param peer the <tt>CallPeer</tt> to be associated
     *            with the new instance and to be transfered
     */
    public TransferCallButton(CallPeer peer)
    {
        super(ImageLoader.getImage(ImageLoader.TRANSFER_CALL_BUTTON));

        this.callPeer = peer;

        setToolTipText(GuiActivator.getResources().getI18NString(
            "service.gui.TRANSFER_BUTTON_TOOL_TIP"));
        addActionListener(new ActionListener()
        {

            /**
             * Invoked when an action occurs.
             *
             * @param evt the <tt>ActionEvent</tt> instance containing the
             *            data associated with the action and the act of its
             *            performing
             */
            public void actionPerformed(ActionEvent evt)
            {
                CallManager.transferCall(callPeer);
            }
        });
    }

    /**
     * Gets the first <tt>Frame</tt> in the ancestor <tt>Component</tt>
     * hierarchy of a specific <tt>Component</tt>.
     * <p>
     * The located <tt>Frame</tt> (if any) is often used as the owner of
     * <tt>Dialog</tt>s opened by the specified <tt>Component</tt> in
     * order to provide natural <tt>Frame</tt> ownership.
     *
     * @param component the <tt>Component</tt> which is to have its
     * <tt>Component</tt> hierarchy examined for <tt>Frame</tt>
     * @return the first <tt>Frame</tt> in the ancestor
     * <tt>Component</tt> hierarchy of the specified <tt>Component</tt>;
     * <tt>null</tt>, if no such <tt>Frame</tt> was located
     */
    public static Frame getFrame(Component component)
    {
        while (component != null)
        {
            Container container = component.getParent();

            if (container instanceof Frame)
                return (Frame) container;

            component = container;
        }
        return null;
    }
}
