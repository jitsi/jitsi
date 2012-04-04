/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.media.*;
import net.java.sip.communicator.util.event.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The panel containing details about ZRTP call security.
 *
 * @author Werner Dittman
 * @author Lubomir Marinov
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
     * The label showing the cipher.
     */
    private JLabel cipherLabel;

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
     * The encryption cipher.
     */
    private String encryptionCipher;

    /**
     * The corresponding call peer.
     */
    private final CallPeer callPeer;

    /**
     * The renderer of the corresponding call peer.
     */
    private final CallPeerRenderer peerRenderer;

    /**
     * Creates an instance of <tt>SecurityPanel</tt> by specifying the
     * corresponding <tt>peer</tt>.
     *
     * @param peerRenderer the parent renderer
     * @param callPeer the peer, which security this panel is about
     * @param zrtpControl the ZRTP security controller that provides information
     *            for this panel and receives the user input
     */
    public ZrtpSecurityPanel(   CallPeerRenderer peerRenderer,
                                CallPeer callPeer,
                                ZrtpControl zrtpControl)
    {
        super(zrtpControl);

        this.peerRenderer = peerRenderer;
        this.callPeer = callPeer;

        OperationSetVideoTelephony telephony
            = callPeer.getProtocolProvider()
                .getOperationSet(OperationSetVideoTelephony.class);

        telephony.addVideoListener(callPeer, this);
        telephony.addPropertyChangeListener(callPeer.getCall(), this);

        audioSecurityLabel = createSecurityLabel("", null);

        videoSecurityLabel = createSecurityLabel("", null);

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
            public void actionPerformed(ActionEvent e)
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

        String cipher = "";
        if (encryptionCipher != null && encryptionCipher.length() > 0)
        {
            cipher = GuiActivator.getResources().getI18NString(
                "service.gui.CIPHER", new String[]{encryptionCipher});
        }

        cipherLabel = createSecurityLabel(cipher, null);

        constraints.anchor = GridBagConstraints.NORTHEAST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridheight = 1;
        constraints.weightx = 0.5f;
        constraints.weighty = 0f;
        constraints.insets = new Insets(5, 0, 0, 0);
        this.add(cipherLabel, constraints);

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

        TransparentPanel sasPanel = new TransparentPanel()
        {
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

        sasPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        sasPanel.setPreferredSize(new Dimension(200, 80));
        sasPanel.setMinimumSize(new Dimension(200, 80));
        sasPanel.setMaximumSize(new Dimension(200, 80));
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

        sasPanel.add(stringPanel);

        securityStringLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        sasPanel.add(securityStringLabel);

        confirmButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
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
        if (!sasVerified)
        {
            compareLabel.setText(GuiActivator.getResources().getI18NString(
                    "service.gui.security.COMPARE_WITH_PARTNER_SHORT"));
            confirmButton.setText(GuiActivator.getResources()
                .getI18NString("servoce.gui.CONFIRM"));
        }
        else
        {
           compareLabel.setText(GuiActivator.getResources().getI18NString(
                   "service.gui.security.STRING_COMPARED"));
           confirmButton.setText(GuiActivator.getResources()
               .getI18NString("servoce.gui.CLEAR"));
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
     * Refreshes the state of the SAS and the SAS verified padlock.
     *
     * @param evt the security event of which we're notified
     */
    public void securityOn(CallPeerSecurityOnEvent evt)
    {
        encryptionCipher = evt.getCipher();

        if (encryptionCipher != null)
            cipherLabel.setText(GuiActivator.getResources().getI18NString(
                "service.gui.security.CIPHER", new String[]{encryptionCipher}));

        switch (evt.getSessionType())
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
    public void securityOff(CallPeerSecurityOffEvent evt)
    {
        switch (evt.getSessionType())
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

    public void securityTimeout(CallPeerSecurityTimeoutEvent evt)
    {

    }

    /**
     * Reloads icons and components.
     */
    public void loadSkin()
    {
        removeAll();

        encryptionVerifiedIcon = new ImageIcon(
            ImageLoader.getImage(ImageLoader.ENCR_VERIFIED));

        audioSecuredIcon = new ImageIcon(
            ImageLoader.getImage(ImageLoader.SECURE_AUDIO_ON));

        audioNotSecuredIcon = new ImageIcon(
            ImageLoader.getImage(ImageLoader.SECURE_AUDIO_OFF));

        videoSecuredIcon = new ImageIcon(
            ImageLoader.getImage(ImageLoader.SECURE_VIDEO_ON));

        videoNotSecuredIcon = new ImageIcon(
            ImageLoader.getImage(ImageLoader.SECURE_VIDEO_OFF));

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
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    setVideoSecurityOn(isVideoSecurityOn);
                }
            });
            return;
        }

        this.isVideoSecurityOn = isVideoSecurityOn;

        Icon statusIcon = null;
        String statusText = null;

        final OperationSetVideoTelephony telephony
            = callPeer.getProtocolProvider()
                .getOperationSet(OperationSetVideoTelephony.class);

        if (telephony != null
            && ((telephony.getVisualComponents(callPeer) != null
                && telephony.getVisualComponents(callPeer).size() > 0)
                || ((MediaAwareCallPeer<?, ?, ?>) callPeer)
                .isLocalVideoStreaming()))
        {
            if (isVideoSecurityOn)
            {
                statusIcon = videoSecuredIcon;
                statusText = GuiActivator.getResources()
                    .getI18NString("service.gui.security.SECURE_VIDEO");
            }
            else
            {
                statusIcon = videoNotSecuredIcon;
                statusText = GuiActivator.getResources()
                    .getI18NString("service.gui.security.VIDEO_NOT_SECURED");
            }
        }
        else
        {
            videoSecurityLabel.setVisible(false);
        }

        if (statusIcon != null && statusText != null)
        {
            videoSecurityLabel.setIcon(statusIcon);
            videoSecurityLabel.setText(statusText);

            if (!videoSecurityLabel.isVisible())
                videoSecurityLabel.setVisible(true);
        }

        revalidate();
        repaint();
    }

    /**
     * {@inheritDoc}
     */
    public void propertyChange(final PropertyChangeEvent event)
    {
        if (OperationSetVideoTelephony.LOCAL_VIDEO_STREAMING
                .equals(event.getPropertyName()))
        {
            setVideoSecurityOn(isAudioSecurityOn);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void videoAdded(VideoEvent event)
    {
        setVideoSecurityOn(isAudioSecurityOn);
    }

    /**
     * {@inheritDoc}
     */
    public void videoRemoved(VideoEvent event)
    {
        setVideoSecurityOn(isAudioSecurityOn);
    }

    /**
     * {@inheritDoc}
     */
    public void videoUpdate(VideoEvent event)
    {
        setVideoSecurityOn(isAudioSecurityOn);
    }
}
