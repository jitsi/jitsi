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

import java.awt.event.*;
import java.util.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * Represents an UI means to transfer (the <tt>Call</tt> of) an associated
 * <tt>CallPariticant</tt>.
 *
 * @author Lubomir Marinov
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class TransferCallButton
    extends CallToolBarButton
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
        super(  ImageLoader.getImage(ImageLoader.TRANSFER_CALL_BUTTON),
                GuiActivator.getResources().getI18NString(
                    "service.gui.TRANSFER_BUTTON_TOOL_TIP"));

        this.call = c;

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
        Iterator<Call> activeCalls
            = CallManager.getInProgressCalls().iterator();
        while (activeCalls.hasNext())
        {
            Call activeCall = activeCalls.next();
            if (!activeCall.equals(call)
                // We're only interested in one to one calls
                && activeCall.getCallPeerCount() == 1
                // we are interested only in calls from same protocol
                && call.getProtocolProvider().getProtocolName().equals(
                        activeCall.getProtocolProvider().getProtocolName()))
            {
                if (transferCalls == null)
                    transferCalls = new LinkedList<CallPeer>();

                transferCalls.add(activeCall.getCallPeers().next());
            }
        }
        return transferCalls;
    }
}
