/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.List;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
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
    extends JPopupMenu
    implements Skinnable
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The invoker component.
     */
    private final JComponent invoker;

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
                    .getI18NString("service.gui.CALL_VIA"));

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
                    .getI18NString("service.gui.CHOOSE_CONTACT"));

        for (Object o : telephonyObjects)
        {
            if (o instanceof UIContactDetail)
                this.addTelephonyContactItem((UIContactDetail) o, opSetClass);
            else if (o instanceof ChatTransport)
                this.addTelephonyChatTransportItem((ChatTransport) o,
                        opSetClass);
        }
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
                if (opSetClass.equals(OperationSetBasicTelephony.class))
                    CallManager.createCall( providerItem.getProtocolProvider(),
                                            contactString);
                else if (opSetClass.equals(OperationSetVideoTelephony.class))
                    CallManager.createVideoCall(
                        providerItem.getProtocolProvider(),
                        contactString);
                else if (opSetClass.equals(
                    OperationSetDesktopSharingServer.class))
                    CallManager.createDesktopSharing(
                        providerItem.getProtocolProvider(),
                        contactString);

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
        final UIContactDetail telephonyContact,
        final Class<? extends OperationSet> opSetClass)
    {
        final ContactMenuItem contactItem
            = new ContactMenuItem(telephonyContact);

        contactItem.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                List<ProtocolProviderService> providers
                    = GuiActivator.getOpSetRegisteredProviders(
                        opSetClass,
                        telephonyContact.getPreferredProtocolProvider(opSetClass),
                        telephonyContact.getPreferredProtocol(opSetClass));

                int providersCount = providers.size();

                if (providers == null || providersCount <= 0)
                {
                    new ErrorDialog(null,
                        GuiActivator.getResources().getI18NString(
                            "service.gui.CALL_FAILED"),
                        GuiActivator.getResources().getI18NString(
                            "service.gui.NO_ONLINE_TELEPHONY_ACCOUNT"))
                        .showDialog();
                    return;
                }
                else if (providersCount > 1)
                {
                    new ChooseCallAccountDialog(
                        telephonyContact.getAddress(), opSetClass, providers)
                    .setVisible(true);
                }
                else // providersCount == 1
                {
                    if (opSetClass.equals(OperationSetBasicTelephony.class))
                    {
                        CallManager.createCall(
                            providers.get(0), telephonyContact.getAddress());
                    }
                    else if (opSetClass.equals(OperationSetVideoTelephony.class))
                    {
                        CallManager.createVideoCall(
                            providers.get(0), telephonyContact.getAddress());
                    }
                    else if (opSetClass.equals(
                        OperationSetDesktopSharingServer.class))
                    {
                        CallManager.createDesktopSharing(
                            providers.get(0), telephonyContact.getAddress());
                    }
                }

                ChooseCallAccountPopupMenu.this.setVisible(false);
            }
        });

        this.add(contactItem);
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
                if (opSetClass.equals(OperationSetBasicTelephony.class))
                    CallManager.createCall( telTransport.getProtocolProvider(),
                                            telTransport.getName());
                else if (opSetClass.equals(OperationSetVideoTelephony.class))
                    CallManager.createVideoCall(
                        telTransport.getProtocolProvider(),
                        telTransport.getName());
                else if (opSetClass.equals(
                    OperationSetDesktopSharingServer.class))
                    CallManager.createDesktopSharing(
                        telTransport.getProtocolProvider(),
                        telTransport.getName());

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
     * Creates the info label.
     *
     * @param infoString the string we'd like to show on the top of this
     * popup menu
     * @return the created info label
     */
    private Component createInfoLabel(String infoString)
    {
        JLabel infoLabel = new JLabel();

        infoLabel.setText("<html><b>" + infoString + "</b></html>");

        return infoLabel;
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
            this.setText(protocolProvider.getAccountID().getAccountAddress());

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
                this.setIcon(new ImageIcon(protocolIcon));
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

        private final UIContactDetail contact;

        public ContactMenuItem(UIContactDetail contact)
        {
            this.contact = contact;
            this.setText(contact.getDisplayName());
            loadSkin();
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
                else
                    statusIcon
                        = Constants.getStatusIcon(Constants.OFFLINE_STATUS);

                if (statusIcon != null)
                    contactIcon = new ImageIcon(statusIcon);
            }

            if (contactIcon != null)
                this.setIcon(contactIcon);
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
            this.setText(chatTransport.getDisplayName());

            loadSkin();
        }

        /**
         * Reloads transport icon.
         */
        public void loadSkin()
        {
            PresenceStatus status = chatTransport.getStatus();
            Image statusIcon = null;

            statusIcon = ImageLoader.badgeImageWithProtocolIndex(
                    ImageUtils.getBytesInImage(
                            status.getStatusIcon()),
                            chatTransport.getProtocolProvider());

            if (statusIcon != null)
                this.setIcon(new ImageIcon(statusIcon));
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
