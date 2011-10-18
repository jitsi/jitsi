/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.util.skin.*;

/**
 * The <tt>SecurityStatusLabel</tt> is meant to be used to visualize the audio
 * and video security details in a call.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class SecurityStatusLabel
    extends JLabel
    implements Skinnable
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

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
     * The default security status icon.
     */
    private final Icon defaultIcon;

    /**
     * Creates an instance of <tt>SecurityStatusLabel</tt> by specifying the
     * <tt>GuiCallPeer</tt>, the icon and the alignment to use for the label.
     */
    public SecurityStatusLabel(Icon securityIcon)
    {
        this.defaultIcon = securityIcon;

        loadSkin();

        this.setHorizontalAlignment(JLabel.CENTER);

        this.setToolTipText("Security status");
    }

    /**
     * Create an extended tooltip showing some more security details.
     * @return the created tooltip
     */
    public JToolTip createToolTip()
    {
        ExtendedTooltip tip = new ExtendedTooltip(true);

        tip.setTitle("Security status");

        ImageIcon audioStatusIcon;
        String audioStatusString;
        if (isAudioSecurityOn)
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
        if (isVideoSecurityOn)
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

        String cipher = "Cipher: " + encryptionCipher;

        tip.addLine(audioStatusIcon,
                    audioStatusString);

        tip.addLine(videoStatusIcon,
                    videoStatusString);

        tip.addLine(null, cipher);

        tip.setComponent(this);

        return tip;
    }

    /**
     * Sets the audio security on or off.
     *
     * @param isAudioSecurityOn indicates if the audio security is turned on or
     * off.
     */
    public void setAudioSecurityOn(boolean isAudioSecurityOn)
    {
        this.isAudioSecurityOn = isAudioSecurityOn;
    }

    /**
     * Sets the video security on or off.
     *
     * @param isVideoSecurityOn indicates if the video security is turned on or
     * off.
     */
    public void setVideoSecurityOn(boolean isVideoSecurityOn)
    {
        this.isVideoSecurityOn = isVideoSecurityOn;
    }

    /**
     * Sets the cipher used for the encryption of the current call.
     *
     * @param encryptionCipher the cipher used for the encryption of the
     * current call.
     */
    public void setEncryptionCipher(String encryptionCipher)
    {
        this.encryptionCipher = encryptionCipher;
    }

    /**
     * Reloads icon.
     */
    public void loadSkin()
    {
        this.setIcon(defaultIcon);
    }
}
