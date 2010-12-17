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

/**
 * The <tt>VolumeControlButton</tt> is the button shown in the call window,
 * which allows to adjust the playback volume of your call.
 * 
 * @author Yana Stamcheva
 */
public class VolumeControlButton
    extends AbstractVolumeControlButton
{
    public VolumeControlButton()
    {
        this(false);
    }

    public VolumeControlButton(boolean fullScreen)
    {
        super(null, fullScreen, false, ImageLoader.VOLUME_CONTROL_BUTTON,
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
