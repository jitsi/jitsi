/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.event.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.neomedia.device.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The local video button is the button used to start/stop video in a
 * conversation.
 *
 * @author Lubomir Marinov
 */
public class LocalVideoButton
    extends SIPCommToggleButton
{
    private static final Logger logger
        = Logger.getLogger(LocalVideoButton.class);

    private static final long serialVersionUID = 0L;

    /**
     * Creates a <tt>LocalVideoButton</tt> by specifying the corresponding
     * <tt>call</tt>.
     * @param call the corresponding to this button call
     */
    public LocalVideoButton(Call call)
    {
        setBgImage(ImageLoader.getImage(ImageLoader.CALL_SETTING_BUTTON_BG));
        setBgRolloverImage(
            ImageLoader.getImage(ImageLoader.CALL_SETTING_BUTTON_BG));
        setIconImage(ImageLoader.getImage(ImageLoader.LOCAL_VIDEO_BUTTON));
        setPressedImage(
            ImageLoader.getImage(ImageLoader.CALL_SETTING_BUTTON_PRESSED_BG));

        setModel(new LocalVideoButtonModel(call));
        setToolTipText(GuiActivator.getResources().getI18NString(
            "service.gui.LOCAL_VIDEO_BUTTON_TOOL_TIP"));

        MediaDevice videoDevice
            = GuiActivator.getMediaService().getDefaultDevice(MediaType.VIDEO);
        if (videoDevice == null
            || videoDevice.getDirection().equals(MediaDirection.RECVONLY))
        {
            this.setEnabled(false);
        }
    }

    private static class LocalVideoButtonModel
        extends ToggleButtonModel
        implements ActionListener,
                   Runnable
    {
        private final Call call;

        private Thread runner;

        public LocalVideoButtonModel(Call call)
        {
            this.call = call;

            addActionListener(this);
        }

        public synchronized void actionPerformed(ActionEvent event)
        {
            if (runner == null)
            {
                runner = new Thread(this, LocalVideoButton.class.getName());
                runner.setDaemon(true);

                setEnabled(false);
                runner.start();
            }
        }

        public void run()
        {
            try
            {
                doRun();
            }
            finally
            {
                synchronized (this)
                {
                    if (Thread.currentThread().equals(runner))
                    {
                        runner = null;
                        setEnabled(true);
                    }
                }
            }
        }

        private void doRun()
        {
            OperationSetVideoTelephony telephony =
                call.getProtocolProvider()
                    .getOperationSet(OperationSetVideoTelephony.class);

            if (telephony != null)
            {
                try
                {
                    telephony.setLocalVideoAllowed(
                        call,
                        !telephony.isLocalVideoAllowed(call));
                }
                catch (OperationFailedException ex)
                {
                    logger.error(
                        "Failed to toggle the streaming of local video.",
                        ex);
                }
            }
        }
    }
}
