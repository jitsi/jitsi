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
package net.java.sip.communicator.impl.gui.main.call;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Represents an UI means to park (the <tt>Call</tt> of) an associated
 * <tt>CallParticipant</tt>.
 *
 * @author Damian Minkov
 */
public class ParkCallButton
    extends CallToolBarButton
{
    /**
     * Our class logger.
     */
    private static final Logger logger = Logger.getLogger(ParkCallButton.class);

    /**
     * The <tt>Call</tt> to be parked.
     */
    private final Call call;

    /**
     * Initializes a new <tt>ParkCallButton</tt> instance which is to
     * park (the <tt>Call</tt> of) a specific
     * <tt>CallPeer</tt>.
     *
     * @param c the <tt>Call</tt> to be associated with the new instance and
     * to be parked
     */
    public ParkCallButton(Call c)
    {
        super(  ImageLoader.getImage(ImageLoader.PARK_CALL_BUTTON),
                GuiActivator.getResources().getI18NString(
                    "service.gui.PARK_BUTTON_TOOL_TIP"));

        this.call = c;

        addActionListener(new ActionListener()
        {
            /**
             * Invoked when an action occurs.
             *
             * @param evt the <tt>ActionEvent</tt> instance containing the
             * data associated with the action and the act of its performing
             */
            public void actionPerformed(ActionEvent evt)
            {
                parkCall();
            }
        });
    }

    /**
     * Parks the given <tt>callPeer</tt>.
     */
    private void parkCall()
    {
        OperationSetTelephonyPark parkOpSet
            = call.getProtocolProvider()
                .getOperationSet(OperationSetTelephonyPark.class);

        // If the park operation set is null we have nothing more to
        // do here.
        if (parkOpSet == null)
            return;

        // We support park for one-to-one calls only.
        CallPeer peer = call.getCallPeers().next();

        JPopupMenu parkMenu = createParkMenu(peer);

        Point location = new Point(getX(), getY() + getHeight());

        SwingUtilities.convertPointToScreen(location, getParent());

        parkMenu.setLocation(location);
        parkMenu.setVisible(true);
    }

    /**
     * Creates the menu responsible for parking slot input.
     *
     * @return the created popup menu
     */
    private JPopupMenu createParkMenu(final CallPeer peer)
    {
        final SIPCommTextField textField = new SIPCommTextField("");
        textField.setColumns(10);

        final JPopupMenu popupMenu = new SIPCommPopupMenu()
        {
            @Override
            public void setVisible(boolean b)
            {
                super.setVisible(b);
                if(b)
                    textField.requestFocus();
            }
        };

        popupMenu.setInvoker(this);
        popupMenu.setFocusable(true);

        TransparentPanel content = new TransparentPanel();

        JLabel parkingSlot = new JLabel(GuiActivator.getResources()
            .getI18NString("service.gui.PARKING_SLOT"));
        content.add(parkingSlot);

        content.add(textField);
        textField.requestFocus();
        JButton button = new JButton(GuiActivator.getResources()
            .getI18NString("service.gui.PARK"));
        content.add(button);

        popupMenu.add(content);

        button.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                popupMenu.setVisible(false);

                OperationSetTelephonyPark parkOpSet
                    = call.getProtocolProvider()
                        .getOperationSet(OperationSetTelephonyPark.class);
                try
                {
                    parkOpSet.parkCall(textField.getText(), peer);
                }
                catch (OperationFailedException ex)
                {
                    logger.error("Failed to park " + peer.getAddress()
                        + " to " + textField.getText(), ex);
                }
            }
        });

        return popupMenu;
    }
}
