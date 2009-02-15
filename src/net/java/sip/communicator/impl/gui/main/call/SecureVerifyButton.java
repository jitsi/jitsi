/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.event.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * The UI button used to toggle on or off call securing
 *
 * @author Emanuel Onica
 */
@SuppressWarnings("serial")
public class SecureVerifyButton
    extends SIPCommButton
    implements ActionListener
{
    private final CallParticipant callParticipant;

    public SecureVerifyButton(CallParticipant callParticipant)
    {
        super(ImageLoader.getImage(ImageLoader.SECURE_BUTTON_OFF));

        this.callParticipant = callParticipant;
        setToolTipText(
                GuiActivator.getResources().getI18NString(
                "impl.media.security.SECURITY_OFF"));
//        this.addActionListener(this);
    }

    public void actionPerformed(ActionEvent evt)
    {
        Call call = callParticipant.getCall();

        if (call != null)
        {
            OperationSetSecureTelephony telephony
                = (OperationSetSecureTelephony) call.getProtocolProvider()
                    .getOperationSet(OperationSetSecureTelephony.class);

            if (telephony != null )
            {
                if (!telephony.isSecure(callParticipant))
                {
                    telephony.setSecure(callParticipant,
                        true,
                        OperationSetSecureTelephony.
                        SecureStatusChangeSource
                            .SECURE_STATUS_CHANGE_BY_LOCAL);
                }
                else
                {
                    telephony.setSecure(callParticipant,
                        false,
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
            this.setImage(ImageLoader.getImage(ImageLoader.SECURE_BUTTON_ON));
            this.setToolTipText(GuiActivator.getResources().getI18NString(
                    "impl.media.security.SECURITY_ON"));
        }
        else
        {
            this.setImage(ImageLoader.getImage(ImageLoader.SECURE_BUTTON_OFF));
            this.setToolTipText(
                GuiActivator.getResources()
                    .getI18NString("impl.media.security.TOGGLE_ON_SECURITY"));
        }
    }
}
