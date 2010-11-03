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
import net.java.sip.communicator.util.skin.*;
import net.java.sip.communicator.util.swing.*;

/**
 * Represents an UI means to transfer (the <tt>Call</tt> of) an associated
 * <tt>CallPariticant</tt>.
 *
 * @author Lubomir Marinov
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class TransferCallButton
    extends SIPCommButton
    implements Skinnable
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

        OperationSetAdvancedTelephony<?> telephony =
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
                transferCall();
            }
        });
    }

    /**
     * Transfers the given <tt>callPeer</tt>.
     */
    private void transferCall()
    {
        OperationSetAdvancedTelephony<?> telephony
            = call.getProtocolProvider()
                .getOperationSet(OperationSetAdvancedTelephony.class);

        // If the telephony operation set is null we have nothing more to
        // do here.
        if (telephony == null)
            return;

        Collection<CallPeer> transferCalls = getTransferCallPeers();

        // We support transfer for one-to-one calls only.
        CallPeer initialPeer = call.getCallPeers().next();

        if (transferCalls == null)
            CallManager.openCallTransferDialog(initialPeer);
        else
        {
            TransferActiveCallsMenu activeCallsMenu
                = new TransferActiveCallsMenu(
                    TransferCallButton.this, initialPeer, transferCalls);

            activeCallsMenu.showPopupMenu();
        }
    }

    /**
     * Returns the list of transfer call peers.
     *
     * @return the list of transfer call peers
     */
    private Collection<CallPeer> getTransferCallPeers()
    {
        Collection<CallPeer> transferCalls = null;
        Iterator<Call> activeCalls = CallManager.getActiveCalls();
        while (activeCalls.hasNext())
        {
            Call activeCall = activeCalls.next();
            if (!activeCall.equals(call)
                // We're only interested in one to one calls
                && activeCall.getCallPeerCount() == 1)
            {
                if (transferCalls == null)
                    transferCalls = new LinkedList<CallPeer>();

                transferCalls.add(activeCall.getCallPeers().next());
            }
        }
        return transferCalls;
    }

    /**
     * Reloads icons.
     */
    public void loadSkin()
    {
        this.setBackgroundImage(ImageLoader.getImage(
                ImageLoader.CALL_SETTING_BUTTON_BG));

        this.setIconImage(ImageLoader.getImage(
                ImageLoader.TRANSFER_CALL_BUTTON));
    }
}
