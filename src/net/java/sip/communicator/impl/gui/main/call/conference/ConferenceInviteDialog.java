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

        // Initialize the account selector box.
        this.initAccountListData();

        // Initialize the list of contacts to select from.
        this.initContactListData(
            (ProtocolProviderService) accountSelectorBox
                .getSelectedItem());

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

        this.addInviteButtonListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                if (getSelectedMetaContacts() != null
                    || getSelectedStrings() != null)
                {
                    ProtocolProviderService selectedProvider
                        = (ProtocolProviderService) accountSelectorBox
                            .getSelectedItem();

                    if (selectedProvider == null)
                        return;

                    inviteContacts(selectedProvider);

                    // Store the last used account in order to pre-select it
                    // next time.
                    ConfigurationManager.setLastCallConferenceProvider(
                        selectedProvider);

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
                }
            }
        }

        // Obtain the last conference provider used.
        ProtocolProviderService lastConfProvider
            = ConfigurationManager.getLastCallConferenceProvider();

        // Try to select the last used account if it's available.
        if (lastConfProvider != null)
            accountSelectorBox.setSelectedItem(lastConfProvider);
    }

    /**
     * Initializes the left contact list with the contacts that could be added
     * to the current chat session.
     * @param protocolProvider the protocol provider from which to initialize
     * the contact list data
     */
    private void initContactListData(ProtocolProviderService protocolProvider)
    {
        // re-init list.
        this.removeAllMetaContacts();

        MetaContactListService metaContactListService
            = GuiActivator.getContactListService();

        Iterator<MetaContact> contactListIter = metaContactListService
            .findAllMetaContactsForProvider(protocolProvider);

        while (contactListIter.hasNext())
        {
            MetaContact metaContact = contactListIter.next();

            if (!containsContact(metaContact))
                this.addMetaContact(metaContact);
        }
    }

    /**
     * Invites the contacts to the chat conference.
     * @param selectedProvider the selected protocol provider
     */
    private void inviteContacts(ProtocolProviderService selectedProvider)
    {
        java.util.List<String> selectedContactAddresses
            = new ArrayList<String>();

        // Obtain selected contacts.
        Enumeration<MetaContact> selectedContacts = getSelectedMetaContacts();

        if (selectedContacts != null)
        {
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
        }

        // Obtain selected strings.
        Enumeration<String> selectedStrings = getSelectedStrings();

        if (selectedStrings != null)
        {
            while (selectedStrings.hasMoreElements())
            {
                selectedContactAddresses.add(selectedStrings.nextElement());
            }
        }

        // Invite all selected.
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

    /**
     * Check if the given <tt>metaContact</tt> is already contained in the call.
     *
     * @param metaContact the <tt>Contact</tt> to check for
     * @return <tt>true</tt> if the given <tt>metaContact</tt> is already
     * contained in the call, otherwise - returns <tt>false</tt>
     */
    private boolean containsContact(MetaContact metaContact)
    {
        // If the call is not yet created we just return false.
        if (call == null)
            return false;

        Iterator<? extends CallPeer> callPeers = call.getCallPeers();

        while(callPeers.hasNext())
        {
            CallPeer callPeer = callPeers.next();

            if(metaContact.containsContact(callPeer.getContact()))
                return true;
        }

        return false;
    }
}
