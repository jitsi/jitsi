/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The panel containing details about ZRTP call security.
 *
 * @author Werner Dittman
 * @author Lubomir Marinov
 * @author Yana Stamcheva
 */
public class ZrtpSecurityPanel
    extends SecurityPanel
{
    private SIPCommButton sasVerificationButton;
    private Image iconEncr;
    private Image iconEncrVerified;
    private final JLabel securityStringLabel = new JLabel();

    private final ZrtpControl zrtpControl;

    /**
     * Creates an instance of <tt>SecurityPanel</tt> by specifying the
     * corresponding <tt>peer</tt>.
     * 
     * @param zrtpControl the ZRTP security controller that provides information
     *            for this panel and receives the user input
     */
    public ZrtpSecurityPanel(ZrtpControl zrtpControl)
    {
        this.zrtpControl = zrtpControl;

        this.setBorder(null);
        this.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 0));

        this.setToolTipText(GuiActivator.getResources().getI18NString(
        "service.gui.COMPARE_WITH_PARTNER"));

        loadSkin();
    }

    /**
     * Adds security related components to this panel.
     */
    private void addComponentsToPane()
    {
        this.add(sasVerificationButton);
        this.add(securityStringLabel);

        securityStringLabel
            .setFont(securityStringLabel.getFont().deriveFont(14f));

        // Action to trigger SAS verification
        sasVerificationButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                zrtpControl.setSASVerification(!zrtpControl
                    .isSecurityVerified());

                if (zrtpControl.isSecurityVerified())
                    sasVerificationButton.setImage(iconEncr);
                else
                    sasVerificationButton.setImage(iconEncrVerified);
            }
        });
    }

    /**
     * Refreshes the state of the SAS and the SAS verified padlock.
     */
    public void refreshStates()
    {
        String securityString = zrtpControl.getSecurityString();
        boolean isSecurityVerified = zrtpControl.isSecurityVerified();
        if (securityString != null)
        {
            securityStringLabel.setText(
                GuiActivator.getResources().getI18NString(
                    "service.gui.COMPARE_WITH_PARTNER_SHORT", 
                    new String[] {securityString}
                )
            );
        }
        else
        {
            securityStringLabel.setText(null);
        }

        sasVerificationButton
            .setImage(isSecurityVerified ? iconEncrVerified : iconEncr);

        revalidate();
        repaint();
    }

    /**
     * Reloads icons and components.
     */
    public void loadSkin()
    {
        this.removeAll();
        iconEncrVerified = ImageLoader.getImage(ImageLoader.ENCR_VERIFIED);
        iconEncr = ImageLoader.getImage(ImageLoader.ENCR);
        sasVerificationButton = new SIPCommButton(iconEncr);

        this.addComponentsToPane();
    }
}
