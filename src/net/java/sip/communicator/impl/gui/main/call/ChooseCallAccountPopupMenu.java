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
import java.awt.event.*;
import java.awt.image.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.account.*;
import net.java.sip.communicator.util.skin.*;

/**
 * The <tt>ChooseCallAccountDialog</tt> is the dialog shown when calling a
 * contact in order to let the user choose the account he'd prefer to use in
 * order to call this contact.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class ChooseCallAccountPopupMenu
    extends SIPCommPopupMenu
    implements Skinnable
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The invoker component.
     */
    protected final JComponent invoker;

    /**
     * The call interface listener, which would be notified once the call
     * interface is created.
     */
    private CallInterfaceListener callInterfaceListener;

    /**
     * The <tt>MetaContact</tt> we're calling.
     */
    private UIContactImpl uiContact;

    /**
     * Creates this dialog.
     *
     * @param invoker the invoker of this pop up menu
     * @param contactToCall the contact to call
     * @param telephonyProviders a list of all possible telephony providers
     */
    public ChooseCallAccountPopupMenu(
        JComponent invoker,
        final String contactToCall,
        List<ProtocolProviderService> telephonyProviders)
    {
        this(invoker, contactToCall, telephonyProviders,
            OperationSetBasicTelephony.class);
    }

    /**
     * Creates this dialog.
     *
     * @param invoker the invoker of this pop up menu
     * @param contactToCall the contact to call
     * @param telephonyProviders a list of all possible telephony providers
     * @param l <tt>CallInterfaceListener</tt> instance
     */
    public ChooseCallAccountPopupMenu(
        JComponent invoker,
        final String contactToCall,
        List<ProtocolProviderService> telephonyProviders,
        CallInterfaceListener l)
    {
        this(invoker, contactToCall, telephonyProviders,
            OperationSetBasicTelephony.class);

        callInterfaceListener = l;
    }

    /**
     * Creates this dialog.
     *
     * @param invoker the invoker of this pop up menu
     * @param contactToCall the contact to call
     * @param telephonyProviders a list of all possible telephony providers
     * @param opSetClass the operation set class indicating what operation
     * would be performed when a given item is selected from the menu
     */
    public ChooseCallAccountPopupMenu(
        JComponent invoker,
        final String contactToCall,
        List<ProtocolProviderService> telephonyProviders,
        Class<? extends OperationSet> opSetClass)
    {
        this.invoker = invoker;
        this.init(GuiActivator.getResources()
                    .getI18NString(getI18NKeyCallVia()));

        for (ProtocolProviderService provider : telephonyProviders)
        {
            this.addTelephonyProviderItem(provider, contactToCall, opSetClass);
        }
    }

    /**
     * Creates this dialog by specifying a list of telephony contacts to choose
     * from.
     *
     * @param invoker the invoker of this pop up
     * @param telephonyObjects the list of telephony contacts to select through
     */
    public ChooseCallAccountPopupMenu(  JComponent invoker,
                                        List<?> telephonyObjects)
    {
        this(   invoker,
                telephonyObjects,
                OperationSetBasicTelephony.class);
    }

    /**
     * Creates this dialog by specifying a list of telephony contacts to choose
     * from.
     *
     * @param invoker the invoker of this pop up
     * @param telephonyObjects the list of telephony contacts to select through
     * @param opSetClass the operation class, which indicates what action would
     * be performed if an item is selected from the list
     */
    public ChooseCallAccountPopupMenu(JComponent invoker,
                                      List<?> telephonyObjects,
                                      Class<? extends OperationSet> opSetClass)
    {
        this.invoker = invoker;
        this.init(GuiActivator.getResources()
                    .getI18NString(getI18NKeyChooseContact()));

        for (Object o : telephonyObjects)
        {
            if (o instanceof UIContactDetailImpl)
                this.addTelephonyContactItem(
                    (UIContactDetailImpl) o, opSetClass);
            else if (o instanceof ChatTransport)
                this.addTelephonyChatTransportItem((ChatTransport) o,
                        opSetClass);
        }
    }

    /**
     * Returns the key to use for choose contact string. Can be overridden
     * by extenders.
     * @return the key to use for choose contact string.
     */
    protected String getI18NKeyChooseContact()
    {
        return "service.gui.CHOOSE_CONTACT";
    }

    /**
     * Returns the key to use for choose contact string. Can be overridden
     * by extenders.
     * @return the key to use for choose contact string.
     */
    protected String getI18NKeyCallVia()
    {
        return "service.gui.CALL_VIA";
    }

    /**
     * Initializes and add some common components.
     *
     * @param infoString the string we'd like to show on the top of this
     * popup menu
     */
    private void init(String infoString)
    {
        setInvoker(invoker);

        this.add(createInfoLabel(infoString));

        this.addSeparator();

        this.setFocusable(true);
    }

    /**
     * Adds the given <tt>telephonyProvider</tt> to the list of available
     * telephony providers.
     *
     * @param telephonyProvider the provider to add.
     * @param contactString the contact to call when the provider is selected
     * @param opSetClass the operation set class indicating what action would
     * be performed when an item is selected
     */
    private void addTelephonyProviderItem(
        final ProtocolProviderService telephonyProvider,
        final String contactString,
        final Class<? extends OperationSet> opSetClass)
    {
        final ProviderMenuItem providerItem
            = new ProviderMenuItem(telephonyProvider);

        providerItem.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                if (uiContact != null)
                    itemSelected(
                        opSetClass,
                        providerItem.getProtocolProvider(),
                        contactString,
                        uiContact);
                else
                    itemSelected(
                        opSetClass,
                        providerItem.getProtocolProvider(),
                        contactString);

                if (callInterfaceListener != null)
                    callInterfaceListener.callInterfaceStarted();

                ChooseCallAccountPopupMenu.this.setVisible(false);
            }
        });

        this.add(providerItem);
    }

    /**
     * Adds the given <tt>telephonyContact</tt> to the list of available
     * telephony contact.
     *
     * @param telephonyContact the telephony contact to add
     * @param opSetClass the operation set class, that indicates the action that
     * would be performed when an item is selected
     */
    private void addTelephonyContactItem(
        final UIContactDetailImpl telephonyContact,
        final Class<? extends OperationSet> opSetClass)
    {
        final ContactMenuItem contactItem
            = new ContactMenuItem(telephonyContact);

        contactItem.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                List<ProtocolProviderService> providers
                    = AccountUtils.getOpSetRegisteredProviders(
                        opSetClass,
                        telephonyContact.getPreferredProtocolProvider(opSetClass),
                        telephonyContact.getPreferredProtocol(opSetClass));

                if (providers == null || providers.size() <= 0)
                {
                    new ErrorDialog(null,
                        GuiActivator.getResources().getI18NString(
                            "service.gui.CALL_FAILED"),
                        GuiActivator.getResources().getI18NString(
                            "service.gui.NO_ONLINE_TELEPHONY_ACCOUNT"))
                        .showDialog();
                    return;
                }
                else if (providers.size() > 1)
                {
                    itemSelected(
                        opSetClass, providers, telephonyContact.getAddress());
                }
                else // providersCount == 1
                {
                    ProtocolProviderService provider = providers.get(0);
                    String contactAddress = telephonyContact.getAddress();

                    if (uiContact != null)
                        itemSelected(
                            opSetClass,
                            provider,
                            contactAddress,
                            uiContact);
                    else
                        itemSelected(
                            opSetClass,
                            provider,
                            contactAddress);
                }

                ChooseCallAccountPopupMenu.this.setVisible(false);
            }
        });

        String category = telephonyContact.getCategory();

        if (category != null && category.equals(ContactDetail.Category.Phone))
        {
            int index = findPhoneItemIndex();
            if (index < 0)
                add(contactItem);
            else
                insert(contactItem, findPhoneItemIndex());
        }
        else
        {
            Component lastComp = getComponent(getComponentCount() - 1);
            if (lastComp instanceof ContactMenuItem)
                category = ((ContactMenuItem) lastComp).getCategory();

            if (category != null
                && category.equals(ContactDetail.Category.Phone))
                addSeparator();

            add(contactItem);
        }
    }

    /**
     * Returns the index of a phone menu item.
     *
     * @return the index of a phone menu item
     */
    private int findPhoneItemIndex()
    {
        int index = -1;
        for (int i = getComponentCount() - 1; i > 1; i--)
        {
            Component c = getComponent(i);

            if (c instanceof ContactMenuItem)
            {
                String category = ((ContactMenuItem) c).getCategory();
                if (category == null
                    || !category.equals(ContactDetail.Category.Phone))
                continue;
            }
            else if (c instanceof JSeparator)
                index = i - 1;
            else
                return index;
        }

        return index;
    }

    /**
     * Adds the given <tt>ChatTransport</tt> to the list of available
     * telephony chat transports.
     *
     * @param telTransport the telephony chat transport to add
     * @param opSetClass the class of the operation set indicating the operation
     * to be executed in the item is selected
     */
    private void addTelephonyChatTransportItem(
        final ChatTransport telTransport,
        final Class<? extends OperationSet> opSetClass)
    {
        final ChatTransportMenuItem transportItem
            = new ChatTransportMenuItem(telTransport);

        transportItem.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                ProtocolProviderService provider
                    = telTransport.getProtocolProvider();
                String contactAddress = telTransport.getName();

                if (uiContact != null)
                    CallManager.createCall(
                        opSetClass, provider, contactAddress, uiContact);
                else
                    CallManager.createCall(
                        opSetClass, provider, contactAddress);

                ChooseCallAccountPopupMenu.this.setVisible(false);
            }
        });

        this.add(transportItem);
    }

    /**
     * Shows the dialog at the given location.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     */
    public void showPopupMenu(int x, int y)
    {
        setLocation(x, y);
        setVisible(true);
    }

    /**
     * Shows this popup menu regarding to its invoker location.
     */
    public void showPopupMenu()
    {
        Point location = new Point(invoker.getX(),
            invoker.getY() + invoker.getHeight());

        SwingUtilities
            .convertPointToScreen(location, invoker.getParent());
        setLocation(location);
        setVisible(true);
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

    /**
     * Creates the info label.
     *
     * @param infoString the string we'd like to show on the top of this
     * popup menu
     * @return the created info label
     */
    private Component createInfoLabel(String infoString)
    {
        JMenuItem infoLabel = new JMenuItem();

        infoLabel.setEnabled(false);
        infoLabel.setFocusable(false);

        infoLabel.setText("<html><b>" + infoString + "</b></html>");

        return infoLabel;
    }

    /**
     * Item was selected, give a chance for extenders to override.
     *
     * @param opSetClass the operation set to use.
     * @param protocolProviderService the protocol provider
     * @param contact the contact address
     *  @param uiContact the <tt>MetaContact</tt> selected
     */
    protected void itemSelected(
                    Class<? extends OperationSet> opSetClass,
                    ProtocolProviderService protocolProviderService,
                    String contact,
                    UIContactImpl uiContact)
    {
        CallManager.createCall(
            opSetClass,
            protocolProviderService,
            contact,
            uiContact);
    }

    /**
     * Item was selected, give a chance for extenders to override.
     *
     * @param opSetClass the operation set to use.
     * @param protocolProviderService the protocol provider
     * @param contact the contact address selected
     */
    protected void itemSelected(Class<? extends OperationSet> opSetClass,
                    ProtocolProviderService protocolProviderService,
                    String contact)
    {
        CallManager.createCall(
            opSetClass,
            protocolProviderService,
            contact);
    }

    /**
     * Item was selected, give a chance for extenders to override.
     *
     * @param opSetClass the operation set to use.
     * @param providers list of available protocol providers
     * @param contact the contact address selected
     */
    protected void itemSelected(Class<? extends OperationSet> opSetClass,
                                List<ProtocolProviderService> providers,
                                String contact)
    {
        ChooseCallAccountDialog callAccountDialog
            = new ChooseCallAccountDialog(contact, opSetClass, providers);

        if (uiContact != null)
            callAccountDialog.setUIContact(uiContact);
        callAccountDialog.setVisible(true);
    }

    /**
     * A custom menu item corresponding to a specific
     * <tt>ProtocolProviderService</tt>.
     */
    private class ProviderMenuItem
        extends JMenuItem
        implements Skinnable
    {
        /**
         * Serial version UID.
         */
        private static final long serialVersionUID = 0L;

        private final ProtocolProviderService protocolProvider;

        public ProviderMenuItem(ProtocolProviderService protocolProvider)
        {
            this.protocolProvider = protocolProvider;
            this.setText(protocolProvider.getAccountID().getDisplayName());

            loadSkin();
        }

        public ProtocolProviderService getProtocolProvider()
        {
            return protocolProvider;
        }

        /**
         * Reloads protocol icon.
         */
        public void loadSkin()
        {
            byte[] protocolIcon
                = protocolProvider.getProtocolIcon()
                    .getIcon(ProtocolIcon.ICON_SIZE_16x16);

            if (protocolIcon != null)
                this.setIcon(ImageLoader.getIndexedProtocolIcon(
                                ImageUtils.getBytesInImage(protocolIcon),
                                protocolProvider));
        }
    }

    /**
     * A custom menu item corresponding to a specific protocol <tt>Contact</tt>.
     */
    private class ContactMenuItem
        extends JMenuItem
        implements Skinnable
    {
        /**
         * Serial version UID.
         */
        private static final long serialVersionUID = 0L;

        private final UIContactDetailImpl contact;

        public ContactMenuItem(UIContactDetailImpl contact)
        {
            this.contact = contact;

            String itemName = "<html>";
            Iterator<String> labels = contact.getLabels();

            if (labels != null && labels.hasNext())
                while (labels.hasNext())
                    itemName += "<b style=\"color: gray\">"
                                + labels.next().toLowerCase() + "</b> ";

            itemName += contact.getAddress() + "</html>";

            this.setText(itemName);
            loadSkin();
        }

        /**
         * Returns the category of the underlying contact detail.
         *
         * @return the category of the underlying contact detail
         */
        public String getCategory()
        {
            return contact.getCategory();
        }

        /**
         * Reloads contact icon.
         */
        public void loadSkin()
        {
            ImageIcon contactIcon = contact.getStatusIcon();

            if (contactIcon == null)
            {
                PresenceStatus status = contact.getPresenceStatus();

                BufferedImage statusIcon = null;
                if (status != null)
                    statusIcon = Constants.getStatusIcon(status);

                if (statusIcon != null)
                    contactIcon = ImageLoader.getIndexedProtocolIcon(
                        statusIcon,
                        contact.getPreferredProtocolProvider(null));
            }

            if (contactIcon != null)
                this.setIcon(ImageLoader.getIndexedProtocolIcon(
                    contactIcon.getImage(),
                    contact.getPreferredProtocolProvider(null)));
        }
    }

    /**
     * A custom menu item corresponding to a specific <tt>ChatTransport</tt>.
     */
    private class ChatTransportMenuItem
        extends JMenuItem
        implements Skinnable
    {
        /**
         * Serial version UID.
         */
        private static final long serialVersionUID = 0L;

        private final ChatTransport chatTransport;

        public ChatTransportMenuItem(ChatTransport chatTransport)
        {
            this.chatTransport = chatTransport;
            this.setText(chatTransport.getName());

            loadSkin();
        }

        /**
         * Reloads transport icon.
         */
        public void loadSkin()
        {
            PresenceStatus status = chatTransport.getStatus();
            byte[] statusIconBytes = status.getStatusIcon();

            Icon statusIcon = null;
            if (statusIconBytes != null && statusIconBytes.length > 0)
            {
                statusIcon = ImageLoader.getIndexedProtocolIcon(
                    ImageUtils.getBytesInImage(statusIconBytes),
                    chatTransport.getProtocolProvider());
            }

            if (statusIcon != null)
                this.setIcon(statusIcon);
        }
    }

    /**
     * Reloads all menu items.
     */
    public void loadSkin()
    {
        Component[] components = getComponents();
        for(Component component : components)
        {
            if(component instanceof Skinnable)
            {
                Skinnable skinnableComponent = (Skinnable) component;
                skinnableComponent.loadSkin();
            }
        }
    }
}
