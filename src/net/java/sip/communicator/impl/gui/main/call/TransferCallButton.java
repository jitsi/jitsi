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
import net.java.sip.communicator.service.protocol.event.*;
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
                transferCall();
            }
        });
    }

    /**
     * Transfers the given <tt>callPeer</tt>.
     */
    private void transferCall()
    {
        OperationSetAdvancedTelephony telephony
            = call.getProtocolProvider()
                .getOperationSet(OperationSetAdvancedTelephony.class);

        // If the telephony operation set is null we have nothing more to
        // do here.
        if (telephony == null)
            return;

        Collection<CallPeer> transferCalls = getTransferCallPeers();

        CallPeer initialPeer = call.getCallPeers().next();

        if (transferCalls == null)
        {
            final TransferCallDialog dialog
                = new TransferCallDialog(initialPeer);

            /*
             * Transferring a call works only when the call is in progress
             * so close the dialog (if it's not already closed, of course)
             * once the dialog ends.
             */
            CallChangeListener callChangeListener = new CallChangeAdapter()
            {
                /*
                 * Implements
                 * CallChangeAdapter#callStateChanged(CallChangeEvent).
                 */
                public void callStateChanged(CallChangeEvent evt)
                {
                    // we are interested only in CALL_STATE_CHANGEs
                    if(!evt.getEventType().equals(
                            CallChangeEvent.CALL_STATE_CHANGE))
                        return;

                    if (!CallState.CALL_IN_PROGRESS.equals(call
                        .getCallState()))
                    {
                        dialog.setVisible(false);
                        dialog.dispose();
                    }
                }
            };
            call.addCallChangeListener(callChangeListener);
            try
            {
                dialog.setModal(true);
                dialog.pack();
                dialog.setVisible(true);
            }
            finally
            {
                call.removeCallChangeListener(callChangeListener);
            }
        }
        else
        {
            TransferActiveCallsMenu activeCallsMenu
                = new TransferActiveCallsMenu(
                    TransferCallButton.this, initialPeer, transferCalls);

            activeCallsMenu.showPopupMenu();
        }
    }

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
}
