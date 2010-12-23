/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.resources.*;

/**
 * The <tt>VolumeControlButton</tt> is the button shown in the call window,
 * which allows to adjust the playback volume of your call.
 * 
 * @author Yana Stamcheva
 * @author Damian Minkov
 */
public class OutputVolumeControlButton
    extends AbstractVolumeControlButton
{
    /**
     * Creates not full screen button.
     */
    public OutputVolumeControlButton()
    {
        this(false);
    }

    /**
     * Creates volume control button.
     * @param fullScreen is full screen.
     */
    public OutputVolumeControlButton(boolean fullScreen)
    {
        this(ImageLoader.VOLUME_CONTROL_BUTTON, fullScreen, false);
    }

    /**
     * Creates volume control button.
     * @param iconImageID the image.
     * @param fullScreen is full screen.
     */
    public OutputVolumeControlButton(ImageID iconImageID,
                                     boolean fullScreen,
                                     boolean inSettingsPanel)
    {
        super(fullScreen, inSettingsPanel, iconImageID,
                "service.gui.VOLUME_CONTROL_TOOL_TIP");
    }


    /**
     * Volume control used by the button.
     *
     * @return volume control used by the button.
     */
    @Override
    public VolumeControl getVolumeControl()
    {
        return GuiActivator.getMediaService().getOutputVolumeControl();
    }
}
