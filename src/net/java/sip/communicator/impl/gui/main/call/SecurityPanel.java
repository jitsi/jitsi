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
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The panel containing details about call security.
 *
 * @author Werner Dittman
 * @author Lubomir Marinov
 * @author Yana Stamcheva
 */
public class SecurityPanel
    extends TransparentPanel
{
    private final CallPeer peer;

    private final Image iconEncr;
    private final Image iconEncrVerified;

    private boolean sasVerified = false;

    private final SIPCommButton sasVerificationButton;

    private final JLabel securityStringLabel = new JLabel();
    
    /**
     * Creates an instance of <tt>SecurityPanel</tt> by specifying the
     * corresponding <tt>peer</tt>.
     * @param peer the <tt>CallPeer</tt>, with which we established an
     * encrypted call
     */
    public SecurityPanel(CallPeer peer)
    {
        this.peer = peer;

        this.setBorder(null);
        this.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 0));

        this.setToolTipText(GuiActivator.getResources().getI18NString(
        "service.gui.COMPARE_WITH_PARTNER"));

        iconEncrVerified = ImageLoader.getImage(ImageLoader.ENCR_VERIFIED);
        iconEncr = ImageLoader.getImage(ImageLoader.ENCR);
        sasVerificationButton = new SIPCommButton(iconEncr);

        this.addComponentsToPane();
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
                Call call = peer.getCall();

                if (call != null)
                {
                    OperationSetSecureTelephony secure
                        = call
                            .getProtocolProvider()
                                .getOperationSet(
                                    OperationSetSecureTelephony.class);

                    if (secure != null)
                    {
                        secure.setSasVerified( peer, !sasVerified);
                    }

                    if (sasVerified)
                    {
                        sasVerified = false;
                        sasVerificationButton.setImage(iconEncr);
                    }
                    else
                    {
                        sasVerified = true;
                        sasVerificationButton.setImage(iconEncrVerified);
                    }
                }
            }
        });
    }

    /**
     * Refreshes the state of the <tt>securityString</tt> and the
     * <tt>isSecurityVerified</tt> corresponding components.
     *
     * @param securityString the security string
     * @param isSecurityVerified indicates if the security string has been
     * already verified
     */
    public void refreshStates(String securityString, boolean isSecurityVerified)
    {
        if (securityString != null)
        {
            securityStringLabel.setText(GuiActivator.getResources().
                    getI18NString("service.gui.COMPARE_WITH_PARTNER_SHORT", 
                            new String[] {securityString}));
        }

        sasVerificationButton
            .setImage(isSecurityVerified ? iconEncrVerified : iconEncr);

        revalidate();
        repaint();
    }
}
