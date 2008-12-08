/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * The UI button used to toggle on or off call securing
 *
 * @author Emanuel Onica
 */
public class SecureButton
    extends JButton
    implements ActionListener
{
    private final CallParticipant callParticipant;

    public SecureButton(CallParticipant callParticipant)
    {
        super(new ImageIcon(ImageLoader.getImage(ImageLoader.SECURE_BUTTON_OFF)));
        this.callParticipant = callParticipant;
//        this.addActionListener(this);
    }

    public void actionPerformed(ActionEvent evt)
    {
        Call call = callParticipant.getCall();

        if (call != null)
        {
            String command = evt.getActionCommand();
            if (command.equals("startSecureMode"))
            {
                OperationSetSecureTelephony telephony =
                    (OperationSetSecureTelephony) call.getProtocolProvider()
                    .getOperationSet(OperationSetSecureTelephony.class);

                if (telephony != null && telephony.isSecure(callParticipant))
                {
                    updateSecureButton(false);
                    telephony.setSecure(callParticipant,
                                        false,
                                        OperationSetSecureTelephony.
                                        SecureStatusChangeSource
                                            .SECURE_STATUS_CHANGE_BY_LOCAL);
                }
                else if (telephony != null)
                {
                    updateSecureButton(true);
                    telephony.setSecure(callParticipant,
                                        true,
                                        OperationSetSecureTelephony.
                                        SecureStatusChangeSource
                                            .SECURE_STATUS_CHANGE_BY_LOCAL);
                }
            }
        }
    }


    /**
     * The method used to update the secure button state (pressed or not pressed)
     *
     * @param isSecure parameter reflecting the current button state
     */
    public void updateSecureButton(boolean isSecure)
    {
        if(isSecure)
        {
            this.setIcon(
                new ImageIcon(
                    ImageLoader.getImage(ImageLoader.SECURE_BUTTON_ON)));
            // TODO GoClear
            // We deactivate the tooltip at the moment, because the 
            // secure mode cannot be toggled off
            //this.setToolTipText(
            //        Messages.getI18NString("toggleOffSecurity").getText());
            this.setToolTipText(null);
        }
        else
        {
            this.setIcon(
                new ImageIcon(
                    ImageLoader.getImage(ImageLoader.SECURE_BUTTON_OFF)));
            this.setToolTipText(
                    Messages.getI18NString("toggleOnSecurity").getText());
        }
    }
}
