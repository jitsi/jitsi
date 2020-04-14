/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;
import java.util.List;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.account.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
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
     * The <tt>UIContactImpl</tt> we're calling.
     */
    private UIContactImpl uiContact;

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
                GuiActivator.getAccounts(providers));

        this.contactAddress = contactAddress;
        this.opSetClass = opSetClass;

        getAccountsCombo().setRenderer(new DefaultListCellRenderer()
        {
            private static final long serialVersionUID = 0L;

            @Override
            public Component getListCellRendererComponent(
                JList jlist, Object obj, int i,
                boolean flag, boolean flag1)
            {
                super.getListCellRendererComponent(
                    jlist, obj, i, flag, flag1);

                Account account = (Account) obj;

                this.setText(account.getAccountID()
                    .getDisplayName());

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
     * Returns the currently selected provider.
     * @return
     */
    protected ProtocolProviderService getSelectedProvider()
    {
        return ((Account) getAccountsCombo().getSelectedItem())
                        .getProtocolProvider();
    }

    /**
     * Calls through the selected account when the call button is pressed.
     */
    @Override
    public void callButtonPressed()
    {
        if (uiContact != null)
            CallManager.createCall(
                opSetClass, getSelectedProvider(), contactAddress, uiContact);
        else
            CallManager.createCall(
                opSetClass, getSelectedProvider(), contactAddress);
    }

    /**
     * Indicates that the conference call button has been pressed.
     */
    @Override
    public void mergeCallButtonPressed()
    {
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
        callButtonPressed();
    }

    /**
     * Sets the <tt>UIContactImpl</tt> we're currently calling.
     *
     * @param uiContact the <tt>UIContactImpl</tt> we're currently calling
     */
    public void setUIContact(UIContactImpl uiContact)
    {
        this.uiContact = uiContact;
    }
}
