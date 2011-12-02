/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;
import java.util.List;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.account.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * A dialog allowing the user to choose a specific account from a given
 * list.
 *
 * @author Yana Stamcheva
 */
public class ChooseCallAccountDialog
    extends PreCallDialog
{
    /**
     * The contact address to be called after an account has been chosen.
     */
    private final String contactAddress;

    /**
     * The operation set class that specifies the operation we're going to make.
     */
    private final Class<? extends OperationSet> opSetClass;

    /**
     * Creates an instance of <tt>ChooseCallAccountDialog</tt>.
     *
     * @param contactAddress the contact address to be called after an account
     * has been chosen
     * @param opSetClass the operation set class that specifies the operation
     * we're going to make
     * @param providers the list of providers to choose from
     */
    public ChooseCallAccountDialog(
                                final String contactAddress,
                                final Class<? extends OperationSet> opSetClass,
                                List<ProtocolProviderService> providers)
    {
        super(  GuiActivator.getResources()
                    .getI18NString("service.gui.CALL_VIA"),
                GuiActivator.getResources().getI18NString(
                    "service.gui.CHOOSE_ACCOUNT"),
                GuiActivator.getAccounts(providers), false);

        this.contactAddress = contactAddress;
        this.opSetClass = opSetClass;

        getAccountsCombo().setRenderer(new DefaultListCellRenderer()
        {
            public Component getListCellRendererComponent(
                JList jlist, Object obj, int i,
                boolean flag, boolean flag1)
            {
                super.getListCellRendererComponent(
                    jlist, obj, i, flag, flag1);

                Account account = (Account) obj;

                this.setText(account.getAccountID()
                        .getAccountAddress());

                byte[] protocolIcon
                    = account.getProtocolProvider().getProtocolIcon()
                        .getIcon(ProtocolIcon.ICON_SIZE_16x16);

                if (protocolIcon != null)
                    this.setIcon(new ImageIcon(protocolIcon));

                return this;
            }
        });
    }

    /**
     * Calls through the selected account when the call button is pressed.
     */
    @Override
    public void callButtonPressed()
    {
        ProtocolProviderService selectedProvider
            = ((Account) getAccountsCombo().getSelectedItem())
                .getProtocolProvider();

        if (opSetClass.equals(OperationSetBasicTelephony.class))
        {
            CallManager.createCall(
                selectedProvider,
                contactAddress);
        }
        else if (opSetClass.equals(OperationSetVideoTelephony.class))
        {
            CallManager.createVideoCall(
                selectedProvider,
                contactAddress);
        }
        else if (opSetClass.equals(
            OperationSetDesktopSharingServer.class))
        {
            CallManager.createDesktopSharing(
                selectedProvider,
                contactAddress);
        }
    }

    /**
     * Disposes the dialog when the hangup button is pressed.
     */
    @Override
    public void hangupButtonPressed()
    {
        dispose();
    }

    /**
     * Not used.
     */
    @Override
    public void videoCallButtonPressed()
    {
        ProtocolProviderService selectedProvider
            = ((Account) getAccountsCombo().getSelectedItem())
                .getProtocolProvider();

        if (opSetClass.equals(OperationSetVideoTelephony.class))
        {
            CallManager.createVideoCall(
                selectedProvider,
                contactAddress);
        }
        else if (opSetClass.equals(
            OperationSetDesktopSharingServer.class))
        {
            CallManager.createDesktopSharing(
                selectedProvider,
                contactAddress);
        }
    }
}
