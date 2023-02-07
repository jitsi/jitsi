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
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * The invite dialog is the one shown when the user clicks on the conference
 * button in the chat toolbar.
 *
 * @author Yana Stamcheva
 * @author Lyubomir Marinov
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
     * The telephony conference into which this instance is to invite
     * participants.
     */
    private final CallConference conference;

    /**
     * The current provider contact source.
     */
    private ContactSourceService currentProviderContactSource;

    /**
     * The current string contact source.
     */
    private ContactSourceService currentStringContactSource;

    /**
     * The previously selected protocol provider, with which this dialog has
     * been instantiated.
     */
    private ProtocolProviderService preselectedProtocolProvider;

    /**
     * Initializes a new <tt>ConferenceInviteDialog</tt> instance which is to
     * invite contacts/participants in a specific telephony conference.
     *
     * @param conference the telephony conference in which the new instance is
     * to invite contacts/participants
     */
    public ConferenceInviteDialog(
                            CallConference conference,
                            ProtocolProviderService preselectedProvider,
                            List<ProtocolProviderService> protocolProviders)
    {
        // Set the correct dialog title depending if we're going to create a
        // video bridge conference call
        super(GuiActivator.getResources().getI18NString("service.gui.INVITE_CONTACT_TO_CALL"), false);

        this.conference = conference;
        this.preselectedProtocolProvider = preselectedProvider;

        if (preselectedProtocolProvider == null)
            initAccountSelectorPanel(protocolProviders);

        // init the list, as we check whether features are supported
        // it may take some time if we have too much contacts
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                initContactSources();

                // Initialize the list of contacts to select from.
                if (preselectedProtocolProvider != null)
                    initContactListData(preselectedProtocolProvider);
                else
                    initContactListData(
                        (ProtocolProviderService) accountSelectorBox
                            .getSelectedItem());
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
                    if (preselectedProtocolProvider == null)
                        preselectedProtocolProvider
                            = (ProtocolProviderService) accountSelectorBox
                                .getSelectedItem();
                    inviteContacts(selectedContacts);

                    // Store the last used account in order to pre-select it
                    // next time.
                    GuiActivator.setLastCallConferenceProvider(
                        preselectedProtocolProvider);

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
        this(null, null, null);
    }

    /**
     * Creates an instance of <tt>ConferenceInviteDialog</tt> by specifying an
     * already created conference. To use when inviting contacts to an existing
     * conference is needed.
     *
     * @param conference the existing <tt>CallConference</tt>
     */
    public ConferenceInviteDialog(CallConference conference)
    {
        this(conference, null, null);
    }

    /**
     * Initializes the account selector panel.
     *
     * @param protocolProviders the list of protocol providers we'd like to
     * show in the account selector box
     */
    private void initAccountSelectorPanel(
                        List<ProtocolProviderService> protocolProviders)
    {
        JLabel accountSelectorLabel = new JLabel(
            GuiActivator.getResources().getI18NString("service.gui.CALL_VIA"));

        TransparentPanel accountSelectorPanel
            = new TransparentPanel(new BorderLayout());

        accountSelectorPanel.setBorder(
            BorderFactory.createEmptyBorder(5, 5, 5, 5));
        accountSelectorPanel.add(accountSelectorLabel, BorderLayout.WEST);
        accountSelectorPanel.add(accountSelectorBox, BorderLayout.CENTER);

        // Initialize the account selector box.
        if (protocolProviders != null && protocolProviders.size() > 0)
            this.initAccountListData(protocolProviders);
        else
            this.initAccountListData();

        this.accountSelectorBox.setRenderer(new DefaultListCellRenderer()
        {
            private static final long serialVersionUID = 0L;

            @Override
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

        this.getContentPane().add(accountSelectorPanel, BorderLayout.NORTH);
    }

    /**
     * Initializes the account selector box with the given list of
     * <tt>ProtocolProviderService</tt>-s.
     *
     * @param protocolProviders the list of <tt>ProtocolProviderService</tt>-s
     * we'd like to show in the account selector box
     */
    private void initAccountListData(
                            List<ProtocolProviderService> protocolProviders)
    {
        Iterator<ProtocolProviderService> providersIter
            = protocolProviders.iterator();

        while (providersIter.hasNext())
        {
            ProtocolProviderService protocolProvider
                = providersIter.next();

            accountSelectorBox.addItem(protocolProvider);
        }

        if (accountSelectorBox.getItemCount() > 0)
            accountSelectorBox.setSelectedIndex(0);
    }

    /**
     * Initializes the account list.
     */
    private void initAccountListData()
    {
        Iterator<ProtocolProviderService> protocolProviders
            = GuiActivator.getUIService().getMainFrame().getProtocolProviders();

        while(protocolProviders.hasNext())
        {
            ProtocolProviderService protocolProvider
                = protocolProviders.next();
            OperationSet opSet
                = protocolProvider.getOperationSet(
                        OperationSetTelephonyConferencing.class);

            if ((opSet != null) && protocolProvider.isRegistered())
                accountSelectorBox.addItem(protocolProvider);
        }

        // Try to select the last used account if available.
        ProtocolProviderService pps
            = GuiActivator.getLastCallConferenceProvider();

        if (pps == null && conference != null)
        {
            /*
             * Pick up the first account from the ones participating in the
             * associated telephony conference which supports
             * OperationSetTelephonyConferencing.
             */
            for (Call call : conference.getCalls())
            {
                ProtocolProviderService callPps = call.getProtocolProvider();

                if (callPps.getOperationSet(
                            OperationSetTelephonyConferencing.class)
                        != null)
                {
                    pps = callPps;
                    break;
                }
            }
        }

        if (pps != null)
            accountSelectorBox.setSelectedItem(pps);
        else if (accountSelectorBox.getItemCount() > 0)
            accountSelectorBox.setSelectedIndex(0);
    }

    /**
     * Initializes contact list sources.
     */
    private void initContactSources()
    {
        DemuxContactSourceService demuxCSService
             = GuiActivator.getDemuxContactSourceService();

        // If the DemuxContactSourceService isn't registered we use the default
        // contact source set.
        if (demuxCSService == null)
            return;

        Iterator<UIContactSource> sourcesIter
            = new ArrayList<UIContactSource>(
                srcContactList.getContactSources()).iterator();

        srcContactList.removeAllContactSources();

        while (sourcesIter.hasNext())
        {
            ContactSourceService contactSource
                = sourcesIter.next().getContactSourceService();

            srcContactList.addContactSource(
                demuxCSService.createDemuxContactSource(contactSource));
        }
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

        Iterator<UIContactSource> sourcesIter
            = new ArrayList<UIContactSource>(
                srcContactList.getContactSources()).iterator();

        while (sourcesIter.hasNext())
        {
            ContactSourceService contactSource
                = sourcesIter.next().getContactSourceService();

            if (contactSource instanceof ProtocolAwareContactSourceService)
            {
                ((ProtocolAwareContactSourceService) contactSource)
                    .setPreferredProtocolProvider(
                        OperationSetBasicTelephony.class, protocolProvider);
            }
        }

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
        ProtocolProviderService selectedProvider;
        Map<ProtocolProviderService, List<String>> selectedProviderCallees
            = new HashMap<ProtocolProviderService, List<String>>();
        List<String> callees;

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
                {
                    selectedProvider
                        = (ProtocolProviderService)
                            accountSelectorBox.getSelectedItem();
                }

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

        if(conference != null)
        {
            CallManager.inviteToConferenceCall(
                    selectedProviderCallees,
                    conference);
        }
        else
        {
            CallManager.createConferenceCall(selectedProviderCallees);
        }
    }
}
