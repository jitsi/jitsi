/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.utils.*;

/**
 * @author Yana Stamcheva
 */
public class SecurityStatusLabel
    extends JLabel
{
    private final CallPeerPanel callParticipantPanel;

    public SecurityStatusLabel( CallPeerPanel callParticipantPanel,
                                Icon icon,
                                int alignment)
    {
        super(icon, alignment);

        this.callParticipantPanel = callParticipantPanel;

        this.setToolTipText("Security status");
    }

    /**
     * Create tooltip.
     */
    public JToolTip createToolTip()
    {
        ExtendedTooltip tip = new ExtendedTooltip(true);

        tip.setTitle("Security status");

        ImageIcon audioStatusIcon;
        String audioStatusString;
        if (callParticipantPanel.isAudioSecurityOn())
        {
            audioStatusIcon = new ImageIcon(
                ImageLoader.getImage(ImageLoader.SECURE_AUDIO_ON));
            audioStatusString = "Audio security on.";
        }
        else
        {
            audioStatusIcon = new ImageIcon(
                ImageLoader.getImage(ImageLoader.SECURE_AUDIO_OFF));
            audioStatusString = "Audio security off.";
        }

        ImageIcon videoStatusIcon;
        String videoStatusString;
        if (callParticipantPanel.isVideoSecurityOn())
        {
            videoStatusIcon = new ImageIcon(
                ImageLoader.getImage(ImageLoader.SECURE_VIDEO_ON));
            videoStatusString = "Video security on.";
        }
        else
        {
            videoStatusIcon = new ImageIcon(
                ImageLoader.getImage(ImageLoader.SECURE_VIDEO_OFF));
            videoStatusString = "Video security off.";
        }

        String cipher = "Cipher: " + callParticipantPanel.getEncryptionCipher();

        tip.addLine(audioStatusIcon,
                    audioStatusString);

        tip.addLine(videoStatusIcon,
                    videoStatusString);

        tip.addLine(null, cipher);

        tip.setComponent(this);

        return tip;
    }
}
