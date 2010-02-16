/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.call.*;
import net.java.sip.communicator.impl.gui.main.contactlist.addcontact.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>UnknownContactPanel</tt> replaces the contact list, when a
 * <tt>SearchFilter</tt> founds no matches. It is meant to propose the user
 * some alternatives if she's looking for a contact, which is not contained in
 * the contact list.
 *
 * @author Yana Stamcheva
 */
public class UnknownContactPanel
    extends TransparentPanel
{
    private final JButton addContact = new JButton(
        GuiActivator.getResources().getI18NString("service.gui.ADD_CONTACT"),
        GuiActivator.getResources()
            .getImage("service.gui.icons.ADD_CONTACT_16x16_ICON"));

    private final JButton callContact = new JButton(
        GuiActivator.getResources().getI18NString("service.gui.CALL_CONTACT"),
        GuiActivator.getResources()
            .getImage("service.gui.icons.CALL_16x16_ICON"));

    /**
     * The main application window.
     */
    private final MainFrame parentWindow;

    /**
     * Creates the <tt>UnknownContactPanel</tt> by specifying the parent window.
     * @param window the parent window
     */
    public UnknownContactPanel(MainFrame window)
    {
        this.parentWindow = window;

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        addContact.setAlignmentX(JButton.CENTER_ALIGNMENT);
        callContact.setAlignmentX(JButton.CENTER_ALIGNMENT);

        addContact.setMnemonic(GuiActivator.getResources()
            .getI18nMnemonic("service.gui.ADD_CONTACT"));
        callContact.setMnemonic(GuiActivator.getResources()
            .getI18nMnemonic("service.gui.CALL_CONTACT"));

        this.add(addContact);
        this.add(callContact);

        addContact.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                AddContactWizard wizard
                    = new AddContactWizard(parentWindow,
                            parentWindow.getCurrentSearchText());

                wizard.showDialog(false);
            }
        });

        callContact.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                String searchText = parentWindow.getCurrentSearchText();

                if (searchText == null)
                    return;

                Vector<ProtocolProviderService> telephonyProviders
                    = CallManager.getTelephonyProviders();

                if (telephonyProviders.size() == 1)
                {
                    CallManager.createCall(
                        telephonyProviders.get(0), searchText);
                }
                else if (telephonyProviders.size() > 1)
                {
                    ChooseCallAccountPopupMenu chooseAccountDialog
                        = new ChooseCallAccountPopupMenu(
                            callContact,
                            searchText,
                            telephonyProviders);

                    chooseAccountDialog.showPopupMenu();
                }
            }
        });
    }
}
