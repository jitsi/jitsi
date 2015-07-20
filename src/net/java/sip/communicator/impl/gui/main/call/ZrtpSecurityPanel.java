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
import java.beans.*;
import java.util.List;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.media.*;
import net.java.sip.communicator.service.resources.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.neomedia.*;
import org.jitsi.service.protocol.event.*;
import org.jitsi.service.resources.*;
import org.jitsi.util.event.*;

/**
 * The panel containing details about ZRTP call security.
 *
 * @author Werner Dittman
 * @author Lyubomir Marinov
 * @author Yana Stamcheva
 */
public class ZrtpSecurityPanel
    extends SecurityPanel<ZrtpControl>
    implements  VideoListener,
                PropertyChangeListener
{
    /**
     * The icon indicating that the short authentication string has been
     * verified.
     */
    private static Icon encryptionVerifiedIcon;

    /**
     * The icon indicating that the audio is secure.
     */
    private static Icon audioSecuredIcon;

    /**
     * The icon indicating that the audio is not secure.
     */
    private static Icon audioNotSecuredIcon;

    /**
     * The icon indicating that the video is secure.
     */
    private static Icon videoSecuredIcon;

    /**
     * The icon indicating that the video is not secure.
     */
    private static Icon videoNotSecuredIcon;

    /**
     * The label showing the security authentication string.
     */
    private final JLabel securityStringLabel = createSecurityLabel("", null);

    /**
     * The label showing audio security status.
     */
    private final JLabel audioSecurityLabel;

    /**
     * The label showing video security status.
     */
    private final JLabel videoSecurityLabel;

    /**
     * The button, which closes this panel.
     */
    private final JButton closeButton
        = new SIPCommButton(ImageLoader.getImage(ImageLoader.CLOSE_VIDEO));

    /**
     * The label containing information about the security authentication
     * string.
     */
    private final JLabel compareLabel = createSecurityLabel("", null);

    /**
     * The button confirming the security authentication string.
     */
    private final JButton confirmButton = new JButton("");

    /**
     * Indicates if the security authentication string has been verified.
     */
    private boolean sasVerified = false;

    /**
     * Indicates the state of the audio security (on or off).
     */
    private boolean isAudioSecurityOn = false;

    /**
     * Indicates the state of the video security (on or off).
     */
    private boolean isVideoSecurityOn = false;

    /**
     * The corresponding call peer.
     */
    private final CallPeer callPeer;

    /**
     * The renderer of the corresponding call peer.
     */
    private final SwingCallPeerRenderer peerRenderer;

    /**
     * The security status of the peer
     */
    private SecurityStatusLabel securityStatusLabel;

    /**
     * Button to show the ZID name dialog
     */
    private SIPCommButton zidNameButton;

    private ZidToNameThread zidNameDialogThread;

    private String zidString;
    private String zidNameKey;
    private ConfigurationService configService;
    private String zidNameValue;
    private String zidAorKey;
    private String zidAorValue;
    private boolean zidAorMismatch = false;

    /**
     * Creates an instance of <tt>SecurityPanel</tt> by specifying the
     * corresponding <tt>peer</tt>.
     *
     * @param peerRenderer the parent renderer
     * @param callPeer the peer, which security this panel is about
     * @param zrtpControl the ZRTP security controller that provides information
     *            for this panel and receives the user input
     */
    public ZrtpSecurityPanel(   SwingCallPeerRenderer peerRenderer,
                                CallPeer callPeer,
                                ZrtpControl zrtpControl)
    {
        super(zrtpControl);

        this.peerRenderer = peerRenderer;
        this.callPeer = callPeer;

        OperationSetVideoTelephony videoTelephony
            = callPeer.getProtocolProvider().getOperationSet(
                    OperationSetVideoTelephony.class);

        // Throwing a NullPointerException in the constructor of a UI element
        // may cause more problems than it may (potentially) solve.
        if (videoTelephony != null)
        {
            videoTelephony.addVideoListener(callPeer, this);

            Call call = callPeer.getCall();

            // The value of call may indeed be null because the values of the
            // CallPeer properties are asynchronously updated (with respect to
            // the UI).
            if (call != null)
                videoTelephony.addPropertyChangeListener(call, this);
        }

        audioSecurityLabel = createSecurityLabel("", null);
        videoSecurityLabel = createSecurityLabel("", null);

        zidString = zrtpControl.getPeerZidString();
        zidNameKey = "net.java.sip.communicator.zrtp.ZIDNAME" + zidString;
        zidAorKey = "net.java.sip.communicator.zrtp.ZIDAOR" + zidString;

        configService = GuiActivator.getConfigurationService();
        zidNameValue = configService.getString(zidNameKey);
        zidAorValue = configService.getString(zidAorKey);

        if (zidAorValue != null &&
            !zidAorValue.equalsIgnoreCase(callPeer.getAddress()))
        {
            zidAorMismatch = true;
            if (zidNameDialogThread == null)
            {
                zidNameDialogThread = new ZidToNameThread();
                zidNameDialogThread.start();
            }
        }
        loadSkin();
    }

    /**
     * Adds all components.
     */
    private void addComponents()
    {
        setAudioSecurityOn(isAudioSecurityOn);
        setVideoSecurityOn(isVideoSecurityOn);

        closeButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent ev)
            {
                peerRenderer.setSecurityPanelVisible(false);
            }
        });

        setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();

        constraints.anchor = GridBagConstraints.NORTHEAST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridheight = 1;
        constraints.weightx = 0.5f;
        constraints.weighty = 0f;
        this.add(audioSecurityLabel, constraints);

        constraints.anchor = GridBagConstraints.NORTHEAST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.gridheight = 1;
        constraints.weightx = 0.5f;
        constraints.weighty = 0f;
        constraints.insets = new Insets(5, 0, 0, 0);
        this.add(videoSecurityLabel, constraints);

        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.gridheight = 3;
        constraints.weightx = 0.5f;
        constraints.weighty = 0f;
        constraints.insets = new Insets(0, 5, 0, 5);
        this.add(createSasPanel(), constraints);

        constraints.anchor = GridBagConstraints.NORTHEAST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 2;
        constraints.gridy = 0;
        constraints.gridheight = 1;
        constraints.weightx = 0f;
        constraints.weighty = 0f;
        constraints.insets = new Insets(0, 0, 0, 5);
        this.add(closeButton, constraints);
    }

    /**
     * Creates a security panel label.
     *
     * @param text the text of the label
     * @param icon the label icon
     * @return the created JLabel
     */
    private JLabel createSecurityLabel(String text, Icon icon)
    {
        JLabel label = new JLabel(text, icon, JLabel.LEFT);

        label.setForeground(Color.WHITE);
        return label;
    }

    /**
     * Creates the ZRTP sas panel.
     *
     * @return the created ZRTP SAS panel
     */
    private JPanel createSasPanel()
    {
        sasVerified = getSecurityControl().isSecurityVerified();

        TransparentPanel sasPanel
            = new TransparentPanel()
            {
                @Override
                public void paintComponent(Graphics g)
                {
                    g = g.create();
                    try
                    {
                        AntialiasingManager.activateAntialiasing(g);
                        g.setColor(new Color(1f, 1f, 1f, 0.1f));
                        g.fillRoundRect(
                            0, 0, this.getWidth(), this.getHeight(), 10, 10);
                        g.setColor(Color.WHITE);
                        g.drawRoundRect(
                            0, 0,
                            this.getWidth() - 1, this.getHeight() - 1,
                            10, 10);
                    }
                    finally
                    {
                        g.dispose();
                    }
                }
            };

        sasPanel.setLayout(new BoxLayout(sasPanel, BoxLayout.Y_AXIS));

        initSasLabels();

        SIPCommButton infoButton = new SIPCommButton(
            ImageLoader.getImage(ImageLoader.CALL_INFO));
        infoButton.setToolTipText(GuiActivator.getResources()
            .getI18NString("service.gui.security.SAS_INFO_TOOLTIP"));

        compareLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        JPanel stringPanel = new TransparentPanel(
            new FlowLayout(FlowLayout.CENTER, 5, 0));
        stringPanel.add(compareLabel);
        stringPanel.add(infoButton);

        initZidNameButton();
        stringPanel.add(zidNameButton);

        sasPanel.add(stringPanel);

        securityStringLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        sasPanel.add(securityStringLabel);

        confirmButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent ev)
            {
                if (getSecurityControl() != null)
                {
                    getSecurityControl().setSASVerification(!sasVerified);

                    sasVerified = !sasVerified;
                    securityStringLabel
                        .setIcon(sasVerified ? encryptionVerifiedIcon : null);

                    if (sasVerified)
                        peerRenderer.securityOn(null);
                    else
                        peerRenderer.securityPending();

                    initSasLabels();
                }
            }
        });
        confirmButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        sasPanel.add(confirmButton);

        return sasPanel;
    }

    /**
     * Initializes security authentication string labels depending on the
     * security verification status.
     */
    private void initSasLabels()
    {
        String statusLabel = "zrtp";
        String longNamelabel = GuiActivator.getResources().
            getI18NString("service.gui.ZID_NAME_NOT_SET");

        if (!sasVerified)
        {
            compareLabel.setText(GuiActivator.getResources().getI18NString(
                "service.gui.security.COMPARE_WITH_PARTNER_SHORT"));
            confirmButton.setText(GuiActivator.getResources().getI18NString(
                "service.gui.CONFIRM"));

            zidNameValue = null;
            zidAorValue = null;
            configService.setProperty(zidNameKey, zidNameValue);
            configService.setProperty(zidAorKey, zidAorValue);
        }
        else
        {
            compareLabel.setText(GuiActivator.getResources().getI18NString(
                "service.gui.security.STRING_COMPARED"));
            confirmButton.setText(GuiActivator.getResources().getI18NString(
                "service.gui.CLEAR"));

            if (zidNameValue != null)
            {
                /*
                 * Reduce length of ZID name to fit the security status label.
                 * The security status label's tooltip contains the full name
                 * including an explanatory text.
                 */
                String label = zidNameValue;
                if (zidNameValue.length() > 6)
                {
                    label = zidNameValue.substring(0, 6) + "...";
                }
                statusLabel = "zrtp - " + label;

                longNamelabel = GuiActivator.getResources().getI18NString(
                        "service.gui.ZID_NAME_SET") + " '" + zidNameValue + "'";
            }
        }

        if (securityStatusLabel != null)
        {
            securityStatusLabel.setText(statusLabel);
            securityStatusLabel.setToolTipText(longNamelabel);
        }

        if (compareLabel.isVisible())
        {
            compareLabel.revalidate();
            compareLabel.repaint();

            confirmButton.revalidate();
            confirmButton.repaint();
       }
    }

    /**
     * Initializes the button to start the ZID name dialog.
     */
    private void initZidNameButton()
    {
        zidNameButton = new SIPCommButton(
            ImageLoader.getImage(
                new ImageID("service.gui.buttons.ZRTP_ID_BUTTON")));
        zidNameButton.setToolTipText(GuiActivator.getResources().getI18NString(
            "service.gui.ZID_NAME_BUTTON"));

        zidNameButton.setEnabled(true);

        zidNameButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent ev)
            {
                // Set ZID name only for verified peers (SAS compared and
                // confirmed).
                if (!sasVerified)
                    return;

                if (zidNameDialogThread == null)
                {
                    zidNameDialogThread = new ZidToNameThread();
                    zidNameDialogThread.start();
                }
            }
        });
    }

    /**
     * Refreshes the state of the SAS and the SAS verified padlock.
     *
     * @param ev the security event of which we're notified
     */
    @Override
    public void securityOn(CallPeerSecurityOnEvent ev)
    {
        switch (ev.getSessionType())
        {
        case CallPeerSecurityStatusEvent.AUDIO_SESSION:
            setAudioSecurityOn(true);
            break;
        case CallPeerSecurityStatusEvent.VIDEO_SESSION:
            setVideoSecurityOn(true);
            break;
        }

        String securityString = getSecurityControl().getSecurityString();
        if (securityString != null)
        {
            StringBuffer sb = new StringBuffer(10);
            sb.append(securityString.charAt(0));
            sb.append(' ');
            sb.append(securityString.charAt(1));
            sb.append(' ');
            sb.append(securityString.charAt(2));
            sb.append(' ');
            sb.append(securityString.charAt(3));
            securityString = sb.toString();

            securityStringLabel.setText(securityString);
        }
        else
        {
            securityStringLabel.setText(null);
        }

        sasVerified = getSecurityControl().isSecurityVerified();
        securityStringLabel
            .setIcon(sasVerified ? encryptionVerifiedIcon : null);

        revalidate();
        repaint();
    }

    /**
     * Indicates that the security has gone off.
     */
    @Override
    public void securityOff(CallPeerSecurityOffEvent ev)
    {
        switch (ev.getSessionType())
        {
        case CallPeerSecurityStatusEvent.AUDIO_SESSION:
            setAudioSecurityOn(false);
            break;
        case CallPeerSecurityStatusEvent.VIDEO_SESSION:
            setVideoSecurityOn(false);
            break;
        }

        revalidate();
        repaint();
    }

    @Override
    public void securityTimeout(CallPeerSecurityTimeoutEvent ev) {}

    /**
     * Reloads icons and components.
     */
    public void loadSkin()
    {
        removeAll();

        encryptionVerifiedIcon
            = new ImageIcon(ImageLoader.getImage(ImageLoader.ENCR_VERIFIED));
        audioSecuredIcon
            = new ImageIcon(ImageLoader.getImage(ImageLoader.SECURE_AUDIO_ON));
        audioNotSecuredIcon
            = new ImageIcon(ImageLoader.getImage(ImageLoader.SECURE_AUDIO_OFF));
        videoSecuredIcon
            = new ImageIcon(ImageLoader.getImage(ImageLoader.SECURE_VIDEO_ON));
        videoNotSecuredIcon
            = new ImageIcon(ImageLoader.getImage(ImageLoader.SECURE_VIDEO_OFF));

        addComponents();

        if (isVisible())
        {
            revalidate();
            repaint();
        }
    }

    /**
     * Updates audio security related components, depending on the given audio
     * security state.
     *
     * @param isAudioSecurityOn indicates if the audio is secured or not
     */
    private void setAudioSecurityOn(final boolean isAudioSecurityOn)
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    setAudioSecurityOn(isAudioSecurityOn);
                }
            });
            return;
        }

        Icon statusIcon;
        String statusText;

        this.isAudioSecurityOn = isAudioSecurityOn;

        if (isAudioSecurityOn)
        {
            statusIcon = audioSecuredIcon;
            statusText = GuiActivator.getResources()
                .getI18NString("service.gui.security.SECURE_AUDIO");
        }
        else
        {
            statusIcon = audioNotSecuredIcon;
            statusText = GuiActivator.getResources()
                .getI18NString("service.gui.security.AUDIO_NOT_SECURED");
        }

        audioSecurityLabel.setIcon(statusIcon);
        audioSecurityLabel.setText(statusText);
    }

    /**
     * Updates video security related components, depending on the given video
     * security state.
     *
     * @param isVideoSecurityOn indicates if the video is secured or not
     */
    private void setVideoSecurityOn(final boolean isVideoSecurityOn)
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(
                    new Runnable()
                    {
                        public void run()
                        {
                            setVideoSecurityOn(isVideoSecurityOn);
                        }
                    });
            return;
        }

        this.isVideoSecurityOn = isVideoSecurityOn;

        Icon icon = null;
        String text = null;
        boolean visible = false;

        OperationSetVideoTelephony videoTelephony
            = callPeer.getProtocolProvider().getOperationSet(
                    OperationSetVideoTelephony.class);

        if (videoTelephony != null)
        {
            /*
             * The invocation of MediaAwareCallPeer.isLocalVideoStreaming() is
             * cheaper than the invocation of
             * OperationSetVideoTelephony.getVisualComponents(CallPeer).
             */
            visible
                = ((MediaAwareCallPeer<?,?,?>) callPeer)
                    .isLocalVideoStreaming();
            if (!visible)
            {
                List<Component> videos
                    = videoTelephony.getVisualComponents(callPeer);

                visible = ((videos != null) && (videos.size() != 0));
            }

            if (visible)
            {
                ResourceManagementService r = GuiActivator.getResources();

                if (isVideoSecurityOn)
                {
                    icon = videoSecuredIcon;
                    text = r.getI18NString("service.gui.security.SECURE_VIDEO");
                }
                else
                {
                    icon = videoNotSecuredIcon;
                    text
                        = r.getI18NString(
                                "service.gui.security.VIDEO_NOT_SECURED");
                }
            }
        }

        if ((icon != null) && (text != null))
        {
            videoSecurityLabel.setIcon(icon);
            videoSecurityLabel.setText(text);

            if (!videoSecurityLabel.isVisible())
                videoSecurityLabel.setVisible(true);
        }
        else
            videoSecurityLabel.setVisible(visible);

        revalidate();
        repaint();
    }

    /**
     * {@inheritDoc}
     */
    public void propertyChange(PropertyChangeEvent ev)
    {
        if (OperationSetVideoTelephony.LOCAL_VIDEO_STREAMING.equals(
                ev.getPropertyName()))
        {
            setVideoSecurityOn(isVideoSecurityOn);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void videoAdded(VideoEvent ev)
    {
        setVideoSecurityOn(isVideoSecurityOn);
    }

    /**
     * {@inheritDoc}
     */
    public void videoRemoved(VideoEvent ev)
    {
        setVideoSecurityOn(isVideoSecurityOn);
    }

    /**
     * {@inheritDoc}
     */
    public void videoUpdate(VideoEvent ev)
    {
        setVideoSecurityOn(isVideoSecurityOn);
    }

    /**
     * @return the zidPropertyValue
     */
    public void setSecurityStatusLabel(SecurityStatusLabel ssl)
    {
        securityStatusLabel = ssl;
    }

    /**
     * @return the zidAorMismatch
     */
    public boolean isZidAorMismatch()
    {
        return zidAorMismatch;
    }

    /**
     * Handle the ZID name dialog in an own thread
     */
    private class ZidToNameThread
         extends Thread
    {
        public ZidToNameThread()
        {
        }

        @Override
        public void run()
        {
            String message = zidAorMismatch ?
                GuiActivator.getResources()
                    .getI18NString("service.gui.ZID_NAME_UNEXPECTED") :
                GuiActivator.getResources()
                    .getI18NString("service.gui.ZID_NAME_SET");

            if (zidNameValue == null) // No name for ZID found, ask for a name
            {
                message =
                    GuiActivator.getResources().getI18NString(
                        "service.gui.ZID_NAME_NOT_SET");
                zidNameValue = callPeer.getDisplayName();
            }

            PopupDialog dialog = GuiActivator.getUIService().getPopupDialog();
            String name =
                (String) dialog.showInputPopupDialog(message, GuiActivator
                    .getResources()
                    .getI18NString("service.gui.ZID_NAME_DIALOG"),
                    PopupDialog.INFORMATION_MESSAGE, null, zidNameValue);

            if (name != null)
            {
                zidNameValue = name;
                configService.setProperty(zidNameKey, zidNameValue);
                configService.setProperty(zidAorKey, callPeer.getAddress());
                /*
                 * Reduce length of ZID name to fit the security status label.
                 * The security status label's tool tip contains the full name
                 * including an explanatory text.
                 */
                String label = zidNameValue;
                if (zidNameValue.length() > 6)
                {
                    label = zidNameValue.substring(0, 6) + "...";
                }
                securityStatusLabel.setText("zrtp - " + label);
                securityStatusLabel.setToolTipText(GuiActivator.getResources()
                    .getI18NString("service.gui.ZID_NAME_SET")
                    + " '"
                    + zidNameValue + "'");
            }
            else
                securityStatusLabel.setText("zrtp");

            zidNameDialogThread = null;
        }
    }
}
