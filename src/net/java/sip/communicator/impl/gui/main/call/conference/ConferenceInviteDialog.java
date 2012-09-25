/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call.conference;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.call.*;
import net.java.sip.communicator.impl.gui.main.contactlist.contactsource.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.swing.*;
// disambiguation

/**
 * The invite dialog is the one shown when the user clicks on the conference
 * button in the chat toolbar.
 *
 * @author Yana Stamcheva
 */
public class ConferenceInviteDialog
    extends InviteDialog
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The account selector box.
     */
    private final JComboBox accountSelectorBox = new JComboBox();

    /**
     * The last selected account.
     */
    private Object lastSelectedAccount;

    /**
     * The call.
     */
    private final Call call;

    /**
     * The current provider contact source.
     */
    private ContactSourceService currentProviderContactSource;

    /**
     * The current string contact source.
     */
    private ContactSourceService currentStringContactSource;

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

        // init the list, as we check whether features are supported
        // it may take some time if we have too much contacts
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                // Initialize the list of contacts to select from.
                initContactListData(
                    (ProtocolProviderService) accountSelectorBox
                        .getSelectedItem());
            }
        });

        this.getContentPane().add(accountSelectorPanel, BorderLayout.NORTH);

        this.accountSelectorBox.setRenderer(new DefaultListCellRenderer()
        {
            private static final long serialVersionUID = 0L;

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
                Collection<UIContact> selectedContacts
                    = destContactList.getContacts(null);

                if (selectedContacts != null && selectedContacts.size() > 0)
                {
                    ProtocolProviderService selectedProvider
                        = (ProtocolProviderService) accountSelectorBox
                            .getSelectedItem();

                    inviteContacts(selectedContacts);

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

        // Obtain the last conference provider used.
        ProtocolProviderService lastConfProvider
            = ConfigurationManager.getLastCallConferenceProvider();

        // Try to select the last used account if it's available.
        if(call != null)
            accountSelectorBox.setSelectedItem(call.getProtocolProvider());
        else if (lastConfProvider != null)
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
        this.setCurrentProvider(protocolProvider);

        srcContactList.removeContactSource(currentProviderContactSource);
        srcContactList.removeContactSource(currentStringContactSource);

        currentProviderContactSource
            = new ProtocolContactSourceServiceImpl(
                                            protocolProvider,
                                            OperationSetBasicTelephony.class);
        currentStringContactSource
            = new StringContactSourceServiceImpl(
                                            protocolProvider,
                                            OperationSetBasicTelephony.class);

        srcContactList.addContactSource(currentProviderContactSource);
        srcContactList.addContactSource(currentStringContactSource);

        srcContactList.applyDefaultFilter();
    }

    /**
     * Invites the contacts to the chat conference.
     *
     * @param contacts the list of contacts to invite
     */
    private void inviteContacts(Collection<UIContact> contacts)
    {
        ProtocolProviderService selectedProvider = null;
        Map<ProtocolProviderService, List<String>> selectedProviderCallees
            = new HashMap<ProtocolProviderService, List<String>>();
        List<String> callees = null;

        Iterator<UIContact> contactsIter = contacts.iterator();

        while (contactsIter.hasNext())
        {
            UIContact uiContact = contactsIter.next();

            Iterator<UIContactDetail> contactDetailsIter = uiContact
                .getContactDetailsForOperationSet(
                    OperationSetBasicTelephony.class).iterator();

            // We invite the first protocol contact that corresponds to the
            // invite provider.
            if (contactDetailsIter.hasNext())
            {
                UIContactDetail inviteDetail = contactDetailsIter.next();
                selectedProvider = inviteDetail
                    .getPreferredProtocolProvider(
                        OperationSetBasicTelephony.class);

                if (selectedProvider == null)
                    selectedProvider
                        = (ProtocolProviderService) accountSelectorBox
                            .getSelectedItem();

                if(selectedProvider != null
                    && selectedProviderCallees.get(selectedProvider) != null)
                {
                    callees = selectedProviderCallees.get(selectedProvider);
                }
                else
                {
                    callees = new ArrayList<String>();
                }

                callees.add(inviteDetail.getAddress());
                selectedProviderCallees.put(selectedProvider, callees);
            }
        }

        if(call != null)
        {
            CallManager.inviteToConferenceCall(selectedProviderCallees, call);
        }
        else
        {
            CallManager.createConferenceCall(selectedProviderCallees);
        }
    }
}
