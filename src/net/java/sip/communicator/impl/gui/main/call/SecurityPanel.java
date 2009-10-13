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

import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.swing.*;

public class SecurityPanel
    extends TransparentPanel
{
    private final CallPeer peer;

    private final Image iconEncr;
    private final Image iconEncrVerified;

    private boolean sasVerified = false;

    private final SIPCommButton sasVerificationButton;

    private final JLabel securityStringLabel = new JLabel();

    public SecurityPanel(CallPeer peer)
    {
        this.peer = peer;

        this.setLayout(new GridLayout(1, 0, 5, 5));

        this.setPreferredSize(new Dimension(200, 60));

        this.setBorder(
            BorderFactory.createTitledBorder("Compare with partner"));

        iconEncrVerified =
                ImageLoader.getImage(ImageLoader.ENCR_VERIFIED);
        iconEncr = ImageLoader.getImage(ImageLoader.ENCR);
        sasVerificationButton = new SIPCommButton(iconEncr);

        this.addComponentsToPane();
    }

    private void addComponentsToPane()
    {
        this.add(sasVerificationButton);
        this.add(securityStringLabel);

        // Action to trigger SAS verification
        sasVerificationButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                boolean sucess = false;
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
                        sucess = secure.setSasVerified( peer,
                                                        !sasVerified);
                    }

                    if (sucess)
                    {
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

            }
        });
    }

    public void refreshStates(CallPeerSecurityOnEvent event)
    {
        String securityString = event.getSecurityString();

        if (securityString != null)
        {
            securityStringLabel.setText(securityString);
        }

        sasVerificationButton
            .setImage(event.isSecurityVerified() ? iconEncrVerified : iconEncr);

        revalidate();
        repaint();
    }
}
