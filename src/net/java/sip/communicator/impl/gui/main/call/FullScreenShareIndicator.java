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
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.media.*;
import org.jitsi.service.neomedia.*;

import javax.swing.*;
import javax.swing.plaf.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;

/**
 * <tt>FullScreenShareIndicator</tt> take care of always on top dialog
 * shown when we are sharing our fullscreen. This way when users go to other
 * application can still see an indication that their screen is shared, so
 * don't show sensitive information.
 * We use the java setAlwaysOnTop which is reported to be not working when
 * using fullscreen, for example if you enter fullscreen of the call (but
 * as you are sharing your screen it doesn't make sense). It also stops working
 * if other app goes in always on top, like when using windows you open task
 * manager and it is set as always on top, our indicator dialog will stop to
 * be always on top.
 *
 * @author Damian Minkov
 */
public class FullScreenShareIndicator
    extends CallChangeAdapter
    implements PropertyChangeListener
{
    /**
     * The call to take care of.
     */
    private final Call call;

    /**
     * The dialog that is shown, otherwise null.
     */
    private JDialog dialog = null;

    /**
     * Constructs the indicator and adds the appropriate listeners.
     * @param call
     */
    FullScreenShareIndicator(Call call)
    {
        this.call = call;

        if(call instanceof MediaAwareCall)
        {
            ((MediaAwareCall)call).addVideoPropertyChangeListener(this);
        }
        call.addCallChangeListener(this);
    }

    /**
     * Listens for vide change events of local video straming in mesia case
     * desktop, whether we need to start and show the dialog, or if already
     * shown to close it.
     * @param evt the video event
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if(!evt.getPropertyName()
            .equals(OperationSetVideoTelephony.LOCAL_VIDEO_STREAMING)
            || (call instanceof MediaAwareCall
            && ((MediaAwareCall)call).getMediaUseCase()
            != MediaUseCase.DESKTOP))
            return;

        Object newValue = evt.getNewValue();
        Object oldValue = evt.getOldValue();

        // and if there is no frame shown for region desktop sharing
        if((oldValue == null || oldValue == MediaDirection.RECVONLY)
            && newValue == MediaDirection.SENDRECV
            && DesktopSharingFrame.getFrameForCall(call) == null)
        {
            showDialog();
        }
        else if(oldValue == MediaDirection.SENDRECV
            && (newValue == null || newValue == MediaDirection.RECVONLY))
        {
            closeDialog();
        }
    }

    /**
     * Listens whether we need to show or hide the dialog.
     * @param ev
     */
    @Override
    public void callStateChanged(CallChangeEvent ev)
    {
        if(!CallChangeEvent.CALL_STATE_CHANGE
            .equals(ev.getPropertyName()))
            return;

        Object newValue = ev.getNewValue();

        if(CallState.CALL_INITIALIZATION.equals(newValue)
            || CallState.CALL_IN_PROGRESS.equals(newValue))
        {
            showDialog();

        }
        else if(CallState.CALL_ENDED.equals(newValue))
        {
            ev.getSourceCall().removeCallChangeListener(this);

            if(call instanceof MediaAwareCall)
            {
                ((MediaAwareCall)call)
                    .removeVideoPropertyChangeListener(this);
            }

            closeDialog();
        }
    }

    /**
     * Creates and shows the dialog if not already created.
     */
    private void showDialog()
    {
        if(dialog != null)
            return;

        dialog = new JDialog((Window) null)
        {
            @Override
            public void setVisible(boolean b)
            {
                setLocationByPlatform(false);

                Dimension screenSize =
                    Toolkit.getDefaultToolkit().getScreenSize();
                setLocation(screenSize.width/2 - getWidth()/2,
                    getLocation().y);

                super.setVisible(b);
            }
        };
        dialog.setUndecorated(true);
        dialog.getRootPane().setWindowDecorationStyle(JRootPane.NONE);
        dialog.setAlwaysOnTop(true);

        JLabel label = new JLabel(
            GuiActivator.getResources()
                .getI18NString("service.gui.DESKTOP_SHARING_DIALOG_INDICATE"));
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));

        Color backgroundColor =
            new ColorUIResource(GuiActivator.getResources().
                getColor("service.gui.DESKTOP_BACKGROUND"));

        JPanel panel = new JPanel(new FlowLayout());
        panel.setBackground(backgroundColor);

        JPanel linePanel = new TransparentPanel(new BorderLayout());
        linePanel.add(label, BorderLayout.CENTER);
        linePanel.setBorder(
            BorderFactory.createMatteBorder(0, 0, 0, 1, Color.lightGray));
        panel.add(linePanel);

        SIPCommTextButton stopButton = new SIPCommTextButton("Stop");
        stopButton.setBackground(backgroundColor);
        panel.add(stopButton);
        stopButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                if (CallManager.isDesktopSharingEnabled(call))
                {
                    CallManager.enableDesktopSharing(call, false);
                }
            }
        });

        panel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        dialog.getContentPane().add(panel);
        dialog.pack();
        dialog.setVisible(true);
    }

    /**
     * Closes and clears the dialog instance.
     */
    private void closeDialog()
    {
        if(dialog != null)
        {
            dialog.setVisible(false);
            dialog = null;
        }
    }
}
