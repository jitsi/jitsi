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

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.skin.*;

import org.jitsi.util.*;

/**
 * The dialog created when an incoming call is received.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class ReceivedCallDialog
    extends PreCallDialog
    implements ActionListener,
               CallListener,
               Skinnable
{
    /**
     * The incoming call to render.
     */
    private final Call incomingCall;

    /**
     * The list of resolvers.
     */
    private List<DisplayNameAndImageChangeListener> detailsResolvers
        = new ArrayList<DisplayNameAndImageChangeListener>();

    /**
     * Creates a <tt>ReceivedCallDialog</tt> by specifying the associated call.
     *
     * @param call The associated with this dialog incoming call.
     * @param video if the call is a video call
     * @param existingCall true to answer the call in an existing call (thus
     * obtaining a conference call)
     * @param desktopStreaming whether the incoming call is desktop streaming
     */
    public ReceivedCallDialog(
        Call call,
        boolean video,
        boolean existingCall,
        boolean desktopStreaming)
    {
        super(GuiActivator.getResources()
            .getSettingsString("service.gui.APPLICATION_NAME")
            + " "
            + (desktopStreaming ?
                GuiActivator.getResources()
                    .getI18NString("service.gui.INCOMING_SCREEN_SHARE_STATUS")
                    .toLowerCase()
             : GuiActivator.getResources()
                    .getI18NString("service.gui.INCOMING_CALL_STATUS")
                    .toLowerCase())
            , video, existingCall);

        this.incomingCall = call;

        OperationSetBasicTelephony<?> basicTelephony
            = call.getProtocolProvider().getOperationSet(
                    OperationSetBasicTelephony.class);

        basicTelephony.addCallListener(this);

        initCallLabel(getCallLabels());
    }

    /**
     * Initializes the label of the received call.
     *
     * @param callLabel The label to initialize.
     */
    private void initCallLabel(final JLabel callLabel[])
    {
        Iterator<? extends CallPeer> peersIter = incomingCall.getCallPeers();

        String textAddress = "";
        String textAccount = "";

        ImageIcon imageIcon =
            ImageUtils.scaleIconWithinBounds(ImageLoader
                .getImage(ImageLoader.DEFAULT_USER_PHOTO), 40, 45);

        // we use a table to store peers and so far resolved names
        // in order to be able to reconstruct the text to display if we
        // receive the display name later
        Hashtable<CallPeer, String> peerNamesTable
            = new Hashtable<CallPeer, String>();

        while (peersIter.hasNext())
        {
            final CallPeer peer = peersIter.next();

            String peerAddress = getPeerDisplayAddress(peer);

            textAccount = peer.getProtocolProvider().getAccountID()
                .getDisplayName();

            DisplayNameAndImageChangeListener listener
                = new DisplayNameAndImageChangeListener(peer, peerNamesTable);
            detailsResolvers.add(listener);

            String displayName =
                CallManager.getPeerDisplayName(peer, listener);

            if(displayName != null)
                peerNamesTable.put(peer, displayName);

            if(!StringUtils.isNullOrEmpty(peerAddress))
                textAddress = callLabel[2].getText()
                    + trimPeerAddressToUsername(peerAddress);

            // More peers.
            if (peersIter.hasNext())
            {
                textAddress += ", ";
            }
            else
            {
                byte[] image = CallManager.getPeerImage(peer);

                if (image != null && image.length > 0)
                    imageIcon = ImageUtils.getScaledRoundedIcon(image, 50, 50);
            }
        }

        // will update callLabel[1] with the already found names
        updateTextDisplayName(peerNamesTable);

        callLabel[0].setIcon(imageIcon);

        callLabel[2].setText(textAddress);
        callLabel[2].setForeground(Color.GRAY);

        if(textAccount != null)
        {
            callLabel[3].setText(
                GuiActivator.getResources().getI18NString("service.gui.TO")
                + " " + textAccount);
        }
    }

    /**
     * Uses a table mapping peer to name to update call label display name.
     * @param peerNamesTable the peer to name mapping
     */
    private void updateTextDisplayName(
        final Hashtable<CallPeer, String> peerNamesTable)
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                @Override
                public void run()
                {
                    updateTextDisplayName(peerNamesTable);
                }
            });
            return;
        }

        boolean hasMorePeers = false;
        String textDisplayName = "";

        Iterator<? extends CallPeer> peersIter = incomingCall.getCallPeers();

        JLabel label = getCallLabels()[1];

        while (peersIter.hasNext())
        {
            final CallPeer peer = peersIter.next();

            // More peers.
            if (peersIter.hasNext())
            {
                textDisplayName = label.getText()
                    + peerNamesTable.get(peer) + ", ";

                hasMorePeers = true;
            }
            // Only one peer.
            else
            {
                textDisplayName = GuiActivator.getResources()
                    .getI18NString("service.gui.IS_CALLING",
                        new String[]{ peerNamesTable.get(peer) });
            }
        }

        if (hasMorePeers)
            textDisplayName = GuiActivator.getResources()
                .getI18NString("service.gui.ARE_CALLING",
                    new String[]{textDisplayName});

        label.setText(textDisplayName);
    }

    /**
     * {@inheritDoc}
     *
     * When the <tt>Call</tt> depicted by this dialog is (remotely) ended,
     * close/dispose of this dialog.
     *
     * @param event a <tt>CallEvent</tt> which specifies the <tt>Call</tt> that
     * has ended
     */
    public void callEnded(CallEvent event)
    {
        if (event.getSourceCall().equals(incomingCall))
            dispose();
    }

    @Override
    public void dispose()
    {
        // no more queries needed, if they are still running
        for(DisplayNameAndImageChangeListener listener : detailsResolvers)
        {
            listener.setInterested(false);
        }

        try
        {
            OperationSetBasicTelephony<?> basicTelephony
                = incomingCall.getProtocolProvider().getOperationSet(
                        OperationSetBasicTelephony.class);

            basicTelephony.removeCallListener(this);
        }
        finally
        {
            super.dispose();
        }
    }

    /**
     * Indicates that an incoming call has been received.
     */
    public void incomingCallReceived(CallEvent event) {}

    /**
     * Indicates that an outgoing call has been created.
     */
    public void outgoingCallCreated(CallEvent event) {}

    /**
     * Answers the call when the call button has been pressed.
     */
    @Override
    public void callButtonPressed()
    {
        CallManager.answerCall(incomingCall);
    }

    /**
     * Answers the call in an existing call when the existing call
     * button has been pressed.
     */
    @Override
    public void mergeCallButtonPressed()
    {
        CallManager.answerCallInFirstExistingCall(incomingCall);
    }

    /**
     * Answers the call when the call button has been pressed.
     */
    @Override
    public void videoCallButtonPressed()
    {
        CallManager.answerVideoCall(incomingCall);
    }

    /**
     * Hangups the call when the call button has been pressed.
     */
    @Override
    public void hangupButtonPressed()
    {
        CallManager.hangupCall(incomingCall);
    }

    /**
     * A informative text to show for the peer. If display name and
     * address are the same return null.
     * @param peer the peer.
     * @return the text contain address.
     */
    private String getPeerDisplayAddress(CallPeer peer)
    {
        String peerAddress = peer.getAddress();

        if(StringUtils.isNullOrEmpty(peerAddress, true))
            return null;
        else
        {
            return
                peerAddress.equalsIgnoreCase(peer.getDisplayName())
                    ? null
                    : peerAddress;
        }
    }

    /**
     * Removes the domain/server part from the address only if it is enabled.
     * @param peerAddress peer address to change.
     * @return username part of the address.
     */
    private String trimPeerAddressToUsername(String peerAddress)
    {
        if(ConfigurationUtils.isHideDomainInReceivedCallDialogEnabled())
        {
            if(peerAddress != null && !peerAddress.startsWith("@"))
            {
                return peerAddress.split("@")[0];
            }
        }

        return peerAddress;
    }

    /**
     * Listens for display name update and image update, some searches for
     * display name are slow, so we add a listener to update them when
     * result comes in.
     */
    private class DisplayNameAndImageChangeListener
        implements CallManager.DetailsResolveListener
    {
        /**
         * The call peer we are interested in.
         */
        private CallPeer peer;

        /**
         * The table with all discovered peer names.
         */
        private Hashtable<CallPeer, String> peerNamesTable;

        /**
         * By default we are interested in events.
         */
        private boolean interested = true;

        /**
         * Constructs.
         * @param peer
         * @param peerNamesTable
         */
        private DisplayNameAndImageChangeListener(
            CallPeer peer,
            Hashtable<CallPeer, String> peerNamesTable)
        {
            this.peer = peer;
            this.peerNamesTable = peerNamesTable;
        }

        @Override
        public void displayNameUpdated(String displayName)
        {
            if(displayName != null)
            {
                peerNamesTable.put(peer, displayName);

                updateTextDisplayName(peerNamesTable);
            }
        }

        @Override
        public void imageUpdated(byte[] image)
        {
            if(image != null)
                ImageUtils.setScaledLabelImage(
                    getCallLabels()[0], image, 50, 50);
        }

        /**
         * Are we interested.
         * @return
         */
        @Override
        public boolean isInterested()
        {
            return interested;
        }

        /**
         * Changes the interested value.
         * @param value
         */
        public void setInterested(boolean value)
        {
            this.interested = value;
        }
    }
}
