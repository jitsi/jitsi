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
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.main.call.CallParticipantPanel.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.notification.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.swing.*;

public class SecurityPanel
    extends TransparentPanel
{
    private CallParticipant participant;

    private Image iconEncr;
    private Image iconEncrVerified;

    private boolean sasVerified = false;

    private NotificationService notificationService = null;

    private static final String ZRTP_SECURE_NOTIFICATION
        = "ZrtpSecureNotification";

    private static final String ZRTP_ALERT_NOTIFICATION
        = "ZrtpAlertNotification";

    private SIPCommButton sasVerificationButton
        = new SIPCommButton(iconEncr);

    private JLabel securityStringLabel = new JLabel();

    public SecurityPanel(CallParticipant participant)
    {
        this.participant = participant;

        this.setLayout(new GridLayout(1, 0, 5, 5));

        this.setBorder(
            BorderFactory.createTitledBorder("Compare with partner"));

        iconEncrVerified =
                ImageLoader.getImage(ImageLoader.ENCR_VERIFIED);
        iconEncr = ImageLoader.getImage(ImageLoader.ENCR);

        notificationService = GuiActivator.getNotificationService();

        if(notificationService != null)
        {
            notificationService.registerDefaultNotificationForEvent(
                    ZRTP_SECURE_NOTIFICATION,
                    NotificationService.ACTION_SOUND,
                    SoundProperties.ZRTP_SECURE,
                    null);

            notificationService.registerDefaultNotificationForEvent(
                    ZRTP_ALERT_NOTIFICATION,
                    NotificationService.ACTION_SOUND,
                    SoundProperties.ZRTP_ALERT,
                    null);
        }

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
                Call call = participant.getCall();

                if (call != null)
                {
                    OperationSetSecureTelephony secure
                        = (OperationSetSecureTelephony) call
                            .getProtocolProvider().getOperationSet(
                                    OperationSetSecureTelephony.class);

                    if (secure != null)
                    {
                        sucess = secure.setSasVerified( participant,
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

    public void refreshStates(CallParticipantSecurityOnEvent event)
    {
        String securityString = event.getSecurityString();

        if (securityString != null)
        {
            securityStringLabel.setText(securityString);
        }

        if (event.isSecurityVerified())
        {
            sasVerificationButton.setImage(iconEncrVerified);
        }
        else {
            sasVerificationButton.setImage(iconEncr);
        }
        notificationService.fireNotification(ZRTP_SECURE_NOTIFICATION);

        revalidate();
        repaint();
    }
}
