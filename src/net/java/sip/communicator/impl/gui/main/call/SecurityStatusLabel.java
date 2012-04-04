/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.util.skin.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>SecurityStatusLabel</tt> is meant to be used to visualize the audio
 * and video security details in a call.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class SecurityStatusLabel
    extends JLabel
    implements  Skinnable
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The icon used for the not secured state.
     */
    private static Icon securityOffIcon;

    /**
     * The icon used for the secured state.
     */
    private static Icon securityOnIcon;

    /**
     * The background used to indicate that the call is secured.
     */
    private final static Color securityOnBackground
        = new Color(GuiActivator.getResources()
            .getColor("service.gui.SECURITY_ON"));

    /**
     * The background used to indicate that the call is not secured.
     */
    private final static Color securityOffBackground
        = new Color(GuiActivator.getResources()
            .getColor("service.gui.SECURITY_OFF"));

    /**
     * The background used to indicate that the call is not secured.
     */
    private final static Color goingSecureBackground
        = new Color(GuiActivator.getResources()
            .getColor("service.gui.GOING_SECURE"));

    /**
     * Indicates security status.
     */
    private boolean isSecure = false;

    /**
     * Creates an instance of <tt>SecurityStatusLabel</tt> by specifying the
     * <tt>GuiCallPeer</tt>, the icon and the alignment to use for the label.
     */
    public SecurityStatusLabel()
    {
        loadSkin();

        setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 3));
        setForeground(Color.WHITE);

        setHorizontalAlignment(JLabel.CENTER);
        setHorizontalTextPosition(JLabel.LEFT);
    }

    /**
     * Paints a custom background to better indicate security state.
     *
     * @param g the <tt>Graphics</tt> object
     */
    public void paintComponent(Graphics g)
    {
        g = g.create();

        try
        {
            AntialiasingManager.activateAntialiasing(g);
            g.setColor(getBackground());

            if (getIcon() != null)
            {
                if (isSecure)
                    g.fillRoundRect(
                        0, 0, this.getWidth(), this.getHeight(), 20, 20);
                else
                    g.fillRoundRect(
                        0, 0, this.getWidth(), this.getHeight(), 20, 20);
            }

            g.setColor(getForeground());

            super.paintComponent(g);
        }
        finally
        {
            g.dispose();
        }
    }

    /**
     * Sets the audio security on or off.
     */
    public void setSecurityOn()
    {
        isSecure = true;
        setIcon(securityOnIcon);
        setBackground(securityOnBackground);
        setToolTipText(GuiActivator.getResources().getI18NString(
            "service.gui.security.CALL_SECURED_TOOLTIP"));
    }

    /**
     * Sets the audio security on or off.
     */
    public void setSecurityOff()
    {
        isSecure = false;
        setIcon(securityOffIcon);
        setBackground(securityOffBackground);
        this.setToolTipText(GuiActivator.getResources().getI18NString(
            "service.gui.security.CALL_NOT_SECURED_TOOLTIP"));
    }

    /**
     * Sets the audio security on or off.
     */
    public void setSecurityPending()
    {
        isSecure = false;
        setIcon(securityOnIcon);
        setBackground(goingSecureBackground);
        this.setToolTipText(GuiActivator.getResources().getI18NString(
            "service.gui.security.CALL_SECURED_COMPARE_TOOLTIP"));
    }

    /**
     * Reloads icons.
     */
    public void loadSkin()
    {
        securityOffIcon = new ImageIcon(ImageLoader
            .getImage(ImageLoader.SECURE_BUTTON_OFF));

        securityOnIcon = new ImageIcon(ImageLoader
            .getImage(ImageLoader.SECURE_BUTTON_ON));
    }
}
