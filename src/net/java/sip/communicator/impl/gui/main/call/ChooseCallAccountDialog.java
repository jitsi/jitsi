/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>ChooseCallAccountDialog</tt> is the dialog shown when calling a
 * contact in order to let the user choose the account he'd prefer to use in
 * order to call this contact.
 *
 * @author Yana Stamcheva
 */
public class ChooseCallAccountDialog
    extends SIPCommDialog
{
    private final ButtonGroup accountGroup = new ButtonGroup();

    private ProtocolProviderService selectedProvider = null;

    /**
     * Creates this dialog.
     * @param telephonyProviders a list of all possible telephony providers
     */
    public ChooseCallAccountDialog(
        Vector<ProtocolProviderService> telephonyProviders)
    {
        TransparentPanel choicePanel
            = new TransparentPanel(new GridLayout(0, 1));

        for (ProtocolProviderService provider : telephonyProviders)
        {
            ProviderRadioButton providerButton
                = new ProviderRadioButton(provider);

            accountGroup.add(providerButton);
            choicePanel.add(providerButton);

            // Select the first button.
            if (accountGroup.getButtonCount() == 1)
                providerButton.setSelected(true);
        }

        TransparentPanel contentPane = new TransparentPanel();
        contentPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        contentPane.setLayout(new BorderLayout(10, 10));
        contentPane.add(createInfoArea(), BorderLayout.NORTH);
        contentPane.add(choicePanel, BorderLayout.CENTER);
        contentPane.add(createButtonPanel(), BorderLayout.SOUTH);

        this.getContentPane().add(contentPane);
    }

    /**
     * Shows the dialog.
     * @return the selected protocol provider
     */
    public ProtocolProviderService showDialog()
    {
        pack();

        setModal(true);
        setVisible(true);

        return selectedProvider;
    }

    private Component createInfoArea()
    {
        JTextArea infoArea = new JTextArea();

        infoArea.setOpaque(false);
        infoArea.setWrapStyleWord(true);
        infoArea.setLineWrap(true);
        infoArea.setEditable(false);

        infoArea.setText(GuiActivator.getResources()
            .getI18NString("service.gui.SELECT_ACCOUNT_FOR_CALL"));

        return infoArea;
    }

    /**
     * Creates the button panel.
     * @return the button panel
     */
    private Component createButtonPanel()
    {
        TransparentPanel buttonPanel
            = new TransparentPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton callButton = new JButton(
            GuiActivator.getResources()
                .getI18NString("service.gui.CALL_CONTACT"),
            GuiActivator.getResources()
                .getImage("service.gui.icons.CALL_16x16_ICON"));

        callButton.setMnemonic(GuiActivator.getResources()
                .getI18nMnemonic("service.gui.CALL_CONTACT"));

        callButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                Enumeration<AbstractButton> buttons = accountGroup.getElements();
                while (buttons.hasMoreElements())
                {
                    ProviderRadioButton b
                        = (ProviderRadioButton) buttons.nextElement();

                    if (b.getModel() == accountGroup.getSelection())
                    {
                        selectedProvider = b.getProtocolProvider();
                    }
                }
                dispose();
            }
        });

        JButton cancelButton = new JButton(
            GuiActivator.getResources().getI18NString("service.gui.CANCEL"));

        cancelButton.setMnemonic(GuiActivator.getResources()
            .getI18nMnemonic("service.gui.CANCEL"));

        cancelButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                dispose();
            }
        });

        buttonPanel.add(callButton);
        buttonPanel.add(cancelButton);

        this.getRootPane().setDefaultButton(callButton);

        return buttonPanel;
    }

    /**
     * A custom radio button corresponding to a <tt>ProtocolProviderService</tt>.
     */
    private class ProviderRadioButton extends JRadioButton
    {
        private final ProtocolProviderService protocolProvider;

        public ProviderRadioButton(ProtocolProviderService protocolProvider)
        {
            this.protocolProvider = protocolProvider;
            this.setText(protocolProvider.getAccountID().getAccountAddress());

//            byte[] protocolIcon
//                = protocolProvider.getProtocolIcon()
//                    .getIcon(ProtocolIcon.ICON_SIZE_16x16);
//
//            if (protocolIcon != null)
//                this.setIcon(new ImageIcon(protocolIcon));
        }

        public ProtocolProviderService getProtocolProvider()
        {
            return protocolProvider;
        }
    }

    @Override
    protected void close(boolean isEscaped) {}
}
