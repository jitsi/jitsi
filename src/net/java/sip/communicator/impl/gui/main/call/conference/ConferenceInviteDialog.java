/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call.conference;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.call.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The invite dialog is the one shown when the user clicks on the conference
 * button in the chat toolbar.
 *
 * @author Yana Stamcheva
 */
public class ConferenceInviteDialog
    extends InviteDialog
{
    private final JComboBox accountSelectorBox = new JComboBox();

    private Object lastSelectedAccount;

    private final Call call;

    /**
     * Creates <tt>ConferenceInviteDialog</tt> by specifying the call, to which
     * the contacts are invited.
     *
     * @param call the call to which the contacts are invited
     */
    public ConferenceInviteDialog(Call call)
    {
        super(GuiActivator.getResources()
            .getI18NString("service.gui.INVITE_CONTACT_TO_CALL"));

        this.call = call;

        JLabel accountSelectorLabel = new JLabel(
            GuiActivator.getResources().getI18NString("service.gui.CALL_VIA"));

        TransparentPanel accountSelectorPanel
            = new TransparentPanel(new BorderLayout());

        accountSelectorPanel.setBorder(
            BorderFactory.createEmptyBorder(5, 5, 5, 5));
        accountSelectorPanel.add(accountSelectorLabel, BorderLayout.WEST);
        accountSelectorPanel.add(accountSelectorBox, BorderLayout.CENTER);

        this.getContentPane().add(accountSelectorPanel, BorderLayout.NORTH);

        this.accountSelectorBox.setRenderer(new DefaultListCellRenderer()
        {
            public Component getListCellRendererComponent(JList list,
                Object value, int index, boolean isSelected,
                boolean cellHasFocus)
            {
                ProtocolProviderService protocolProvider
                     = (ProtocolProviderService) value;

                if (protocolProvider != null)
                {
                    this.setText(
                        protocolProvider.getAccountID().getDisplayName());
                    this.setIcon(
                        ImageLoader.getAccountStatusImage(protocolProvider));
                }

                return this;
            }
        });

        this.accountSelectorBox.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                Object accountSelectorBoxSelectedItem
                    = accountSelectorBox.getSelectedItem();

                if (lastSelectedAccount == null
                    || !lastSelectedAccount
                        .equals(accountSelectorBoxSelectedItem))
                {
                    lastSelectedAccount = accountSelectorBoxSelectedItem;

                    initContactListData(
                        (ProtocolProviderService) accountSelectorBox
                            .getSelectedItem());
                }
            }
        });

        // Initialize the account selector box.
        this.initAccountListData();

        // Initialize the list of contacts to select from.
        this.initContactListData(
            (ProtocolProviderService) accountSelectorBox
                .getSelectedItem());

        this.addInviteButtonListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                if (getSelectedMetaContacts().hasMoreElements())
                {
                    inviteContacts();

                    // Store the last used account in order to pre-select it
                    // next time.
                    ConfigurationManager.setLastCallConferenceProvider(
                        (ProtocolProviderService) accountSelectorBox
                            .getSelectedItem());

                    dispose();
                }
                else
                {
                    // TODO: The underlying invite dialog should show a message
                    // to the user that she should select at least two contacts
                    // in order to create a conference.
                }
            }
        });

        this.addCancelButtonListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                dispose();
            }
        });
    }

    /**
     * Constructs the <tt>ConferenceInviteDialog</tt>.
     */
    public ConferenceInviteDialog()
    {
        this(null);
    }

    /**
     * Initializes the account list.
     */
    private void initAccountListData()
    {
        // If we have a specified call, we'll have only one provider in the
        // box.
        if (call != null)
        {
            accountSelectorBox.addItem(call.getProtocolProvider());
            accountSelectorBox.setEnabled(false);
        }
        else
        {
            Iterator<ProtocolProviderService> protocolProviders
                = GuiActivator.getUIService()
                    .getMainFrame().getProtocolProviders();

            // Obtain the last conference provider used.
            ProtocolProviderService lastConfProvider
                = ConfigurationManager.getLastCallConferenceProvider();

            while(protocolProviders.hasNext())
            {
                ProtocolProviderService protocolProvider
                    = protocolProviders.next();
                OperationSet opSet
                    = protocolProvider
                        .getOperationSet(
                            OperationSetTelephonyConferencing.class);

                if (opSet != null && protocolProvider.isRegistered())
                {
                    accountSelectorBox.addItem(protocolProvider);

                    // Try to select the last used account if it's available.
                    if (lastConfProvider != null
                            && lastConfProvider.equals(protocolProvider))
                        accountSelectorBox.setSelectedItem(protocolProvider);
                }
            }
        }
    }

    /**
     * Initializes the left contact list with the contacts that could be added
     * to the current chat session.
     */
    private void initContactListData(ProtocolProviderService protocolProvider)
    {
        // re-init list.
        this.removeAllMetaContacts();

        MetaContactListService metaContactListService
            = GuiActivator.getMetaContactListService();

        Iterator<MetaContact> contactListIter = metaContactListService
            .findAllMetaContactsForProvider(protocolProvider);

        while (contactListIter.hasNext())
        {
            MetaContact metaContact = contactListIter.next();

            this.addMetaContact(metaContact);
        }
    }

    /**
     * Invites the contacts to the chat conference.
     */
    private void inviteContacts()
    {
        ProtocolProviderService selectedProvider
            = (ProtocolProviderService) accountSelectorBox.getSelectedItem();

        java.util.List<String> selectedContactAddresses =
            new ArrayList<String>();

        Enumeration<MetaContact> selectedContacts
            = getSelectedMetaContacts();

        while (selectedContacts.hasMoreElements())
        {
            MetaContact metaContact
                = selectedContacts.nextElement();

            Iterator<Contact> contactsIter = metaContact
                .getContactsForProvider(selectedProvider);

            // We invite the first protocol contact that corresponds to the
            // invite provider.
            if (contactsIter.hasNext())
            {
                Contact inviteContact = contactsIter.next();

                selectedContactAddresses.add(inviteContact.getAddress());
            }
        }

        String[] contactAddressStrings = null;
        if (selectedContactAddresses.size() > 0)
        {
            contactAddressStrings = new String[selectedContactAddresses.size()];
            contactAddressStrings
                = selectedContactAddresses.toArray(contactAddressStrings);
        }

        if (call != null)
        {
            CallManager.inviteToConferenceCall(contactAddressStrings, call);
        }
        else
        {
            CallManager.createConferenceCall(
                contactAddressStrings, selectedProvider);
        }
    }
}
