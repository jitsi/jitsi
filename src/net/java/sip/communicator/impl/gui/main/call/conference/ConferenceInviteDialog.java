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
import java.util.List; //disambiguation

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
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The account selector box.
     */
    private ProtocolProviderSelectorBox accountBox;

    /**
     * The call.
     */
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

        TransparentPanel accountSelectorPanel
            = new TransparentPanel(new BorderLayout());

        accountSelectorPanel.setBorder(
            BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // init the list, as we check whether features are supported
        // it may take some time if we have too much contacts
        SwingUtilities.invokeLater(
            new Runnable()
            {
                public void run()
                {
                    // Initialize the list of contacts to select from.
                    initContactListData();
                }
            });

        this.getContentPane().add(accountSelectorPanel, BorderLayout.NORTH);

        this.addInviteButtonListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                if (getSelectedMetaContacts() != null
                    || getSelectedStrings() != null)
                {
                    inviteContacts();

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

        List<ProtocolProviderService> provs =
            new ArrayList<ProtocolProviderService>();
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
                provs.add(protocolProvider);
            }
        }

        if(accountBox == null)
            accountBox = new ProtocolProviderSelectorBox(provs.iterator());
    }

    /**
     * Initializes the left contact list with the contacts that could be added
     * to the current chat session.
     * @param protocolProvider the protocol provider from which to initialize
     * the contact list data
     */
    private void initContactListData()
    {
        // re-init list.
        this.removeAllMetaContacts();

        MetaContactListService metaContactListService
            = GuiActivator.getContactListService();

        Iterator<ProtocolProviderService> protocolProviders
            = GuiActivator.getUIService()
                .getMainFrame().getProtocolProviders();

        while(protocolProviders.hasNext())
        {
            ProtocolProviderService protocolProvider = protocolProviders.next();

            Iterator<MetaContact> contactListIter = metaContactListService
                .findAllMetaContactsForProvider(protocolProvider);

            while (contactListIter.hasNext())
            {
                MetaContact metaContact = contactListIter.next();

                if (!containsContact(metaContact))
                {
                    if (metaContact.getDefaultContact(
                        OperationSetBasicTelephony.class) != null)
                        addMetaContact(metaContact);
                }
            }
        }
    }

    /**
     * Invites the contacts to the chat conference.
     */
    private void inviteContacts()
    {
        ProtocolProviderService selectedProvider = null;
        Map<ProtocolProviderService, List<String>> selectedProviderCallees =
            new HashMap<ProtocolProviderService, List<String>>();
        List<String> callees = null;

        // Obtain selected contacts.
        Enumeration<MetaContact> selectedContacts = getSelectedMetaContacts();

        if (selectedContacts != null)
        {
            while (selectedContacts.hasMoreElements())
            {
                MetaContact metaContact
                    = selectedContacts.nextElement();

                Iterator<Contact> contactsIter = metaContact.getContacts();

                // We invite the first protocol contact that corresponds to the
                // invite provider.
                if (contactsIter.hasNext())
                {
                    Contact inviteContact = contactsIter.next();
                    selectedProvider = inviteContact.getProtocolProvider();

                    if(selectedProviderCallees.get(selectedProvider) != null)
                    {
                        callees = selectedProviderCallees.get(selectedProvider);
                    }
                    else
                    {
                         callees = new ArrayList<String>();
                    }

                    callees.add(inviteContact.getAddress());
                    selectedProviderCallees.put(selectedProvider, callees);
                }
            }
        }

        // Obtain selected strings.
        Enumeration<ContactWithProvider> selectedContactWithProvider =
            getSelectedContactsWithProvider();

        if (selectedContactWithProvider != null)
        {
            while (selectedContactWithProvider.hasMoreElements())
            {
                ContactWithProvider c =
                    selectedContactWithProvider.nextElement();
                selectedProvider = c.getProvider();

                if(selectedProviderCallees.get(selectedProvider) != null)
                {
                    callees = selectedProviderCallees.get(selectedProvider);
                }
                else
                {
                     callees = new ArrayList<String>();
                }

                callees.add(c.getAddress());
                selectedProviderCallees.put(selectedProvider, callees);
            }
        }

        if(call != null)
        {
            CallManager.inviteToCrossProtocolConferenceCall(
                selectedProviderCallees, call);
        }
        else
        {
            CallManager.createCrossProtocolConferenceCall(
                selectedProviderCallees);
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

    /**
     * Moves a string from left to right.
     */
    @Override
    protected void moveStringFromLeftToRight()
    {
        String newContactText = newContactField.getText();

        ContactWithProvider c = new ContactWithProvider(
            newContactText, accountBox.getSelectedProvider());
        if (newContactText != null && newContactText.length() > 0)
            selectedContactListModel.addElement(c);

        newContactField.setText("");
    }

    /**
     * Returns an enumeration of the list of selected Strings.
     * @return an enumeration of the list of selected Strings
     */
    public Enumeration<ContactWithProvider> getSelectedContactsWithProvider()
    {
        if (selectedContactListModel.getSize() == 0)
            return null;

        Vector<ContactWithProvider> selectedStrings =
            new Vector<ContactWithProvider>();
        Enumeration<?> selectedContacts = selectedContactListModel.elements();
        while(selectedContacts.hasMoreElements())
        {
            Object contact = selectedContacts.nextElement();
            if (contact instanceof ContactWithProvider)
                selectedStrings.add((ContactWithProvider)contact);
        }

        return selectedStrings.elements();
    }

    /**
     * Get the <tt>newContact</tt> component.
     *
     * @return the <tt>newContact</tt> component.
     */
    @Override
    public JComponent getNewContactPanel()
    {
        initAccountListData();

        JPanel panel = new TransparentPanel(new BorderLayout());
        panel.add(super.getNewContactPanel(), BorderLayout.CENTER);
        panel.add(accountBox, BorderLayout.WEST);
        return panel;
    }

    /**
     * Contact with the provider to call him.
     *
     * @author Sebastien Vincent
     */
    private class ContactWithProvider
    {
        /**
         * The provider.
         */
        private final ProtocolProviderService provider;

        /**
         * The contact.
         */
        private final String contact;

        /**
         * Constructor.
         *
         * @param contact the contact
         * @param provider the provider
         */
        public ContactWithProvider(String contact,
            ProtocolProviderService provider)
        {
            this.contact = contact;
            this.provider = provider;
        }

        /**
         * Returns the contact
         *
         * @return the contact
         */
        public String getAddress()
        {
            return contact;
        }

        /**
         * Returns the provider.
         *
         * @return the provider
         */
        public ProtocolProviderService getProvider()
        {
            return provider;
        }

        /**
         * Returns <tt>String</tt> representation.
         *
         * @return <tt>String</tt> representation
         */
        @Override
        public String toString()
        {
            return contact;
        }
    }
}
