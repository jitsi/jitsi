/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.util.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.neomedia.device.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * The button responsible to start(the <tt>Call</tt> of) an associated
 * <tt>CallPariticant</tt>.
 *
 * @author Yana Stamcheva
 */
public class DesktopSharingButton
    extends AbstractCallToggleButton
{
    /**
     * Initializes a new <tt>DesktopSharingButton</tt> instance which is meant
     * to be used to initiate a desktop sharing during a call.
     *
     * @param call the <tt>Call</tt> to be associated with the desktop sharing
     * button instance
     */
    public DesktopSharingButton(Call call)
    {
        this(call, false, false);
    }

    /**
     * Initializes a new <tt>HoldButton</tt> instance which is to put a specific
     * <tt>CallPeer</tt> on/off hold.
     *
     * @param call  the <tt>Call</tt> to be associated with the new instance and
     * to be put on/off hold upon performing its action
     * @param fullScreen <tt>true</tt> if the new instance is to be used in
     * full-screen UI; otherwise, <tt>false</tt>
     * @param selected <tt>true</tt> if the new toggle button is to be initially
     * selected; otherwise, <tt>false</tt>
     */
    public DesktopSharingButton(Call call, boolean fullScreen, boolean selected)
    {
        super(  call,
                fullScreen,
                selected,
                ImageLoader.CALL_DESKTOP_BUTTON,
                "service.gui.SHARE_DESKTOP_WITH_CONTACT");
    }

    /**
     * Shares the desktop with the peers in the current call.
     */
    public void buttonPressed()
    {
        if (call != null)
        {
            OperationSetDesktopStreaming desktopOpSet
                = call.getProtocolProvider().getOperationSet(
                        OperationSetDesktopStreaming.class);

            // This shouldn't happen at this stage, because we disable the button
            // if the operation set isn't available.
            if (desktopOpSet == null)
                return;

            boolean isDesktopSharing = desktopOpSet.isLocalVideoAllowed(call);

            if (isDesktopSharing) // If it's already enabled, we disable it.
                CallManager.enableDesktopSharing(call, false);
            else
            {
                MediaService mediaService = GuiActivator.getMediaService();

                List<MediaDevice> desktopDevices = mediaService.getDevices(
                    MediaType.VIDEO, MediaUseCase.DESKTOP);

                int deviceNumber = desktopDevices.size();

                if (deviceNumber == 1)
                    CallManager.enableDesktopSharing(call, true);
                else if (deviceNumber > 1)
                {
                    SelectScreenDialog selectDialog
                        = new SelectScreenDialog(call, desktopDevices);

                    selectDialog.setVisible(true);
                }
            }
        }
    }
}