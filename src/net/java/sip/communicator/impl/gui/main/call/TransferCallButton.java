/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.event.*;
import java.util.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.swing.*;

/**
 * Represents an UI means to transfer (the <tt>Call</tt> of) an associated
 * <tt>CallPariticant</tt>.
 *
 * @author Lubomir Marinov
 * @author Yana Stamcheva
 */
public class TransferCallButton
    extends SIPCommButton
{
    /**
     * The <tt>Call</tt> to be transfered.
     */
    private final Call call;

    /**
     * Initializes a new <tt>TransferCallButton</tt> instance which is to
     * transfer (the <tt>Call</tt> of) a specific
     * <tt>CallPeer</tt>.
     *
     * @param c the <tt>Call</tt> to be associated with the new instance and
     * to be transfered
     */
    public TransferCallButton(Call c)
    {
        super(  ImageLoader.getImage(ImageLoader.CALL_SETTING_BUTTON_BG),
                ImageLoader.getImage(ImageLoader.TRANSFER_CALL_BUTTON));

        this.call = c;

        setToolTipText(GuiActivator.getResources().getI18NString(
            "service.gui.TRANSFER_BUTTON_TOOL_TIP"));

        OperationSetAdvancedTelephony telephony =
            call.getProtocolProvider()
                .getOperationSet(OperationSetAdvancedTelephony.class);

        if (telephony == null)
            this.setEnabled(false);

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
                Iterator<? extends CallPeer> callPeers = call.getCallPeers();
                while (callPeers.hasNext())
                {
                    CallManager.transferCall(callPeers.next());
                }
            }
        });
    }
}
