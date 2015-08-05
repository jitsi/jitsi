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
package net.java.sip.communicator.impl.gui.main.menus;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;

import javax.swing.*;
import javax.swing.border.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.skin.*;

import org.jitsi.service.resources.*;
import org.jitsi.util.*;
import org.osgi.framework.*;

/**
 * The auto answer menu dynamically adds/removes menu items for enabled/disabled
 * protocol providers.
 *
 * @author Damian Minkov
 */
public class AutoAnswerMenu
    extends SIPCommMenu
    implements Skinnable
{
    /**
     * Listens for new protocol providers registered.
     */
    private static ProtocolProviderListener protocolProviderListener = null;

    /**
     * Creates the menu and load already registered providers.
     */
    public AutoAnswerMenu()
    {
        super(GuiActivator.getResources()
            .getI18NString("service.gui.AUTO_ANSWER"));

        loadSkin();

        registerMenuItems(this);
    }

    /**
     * Registers all menu items.
     */
    public static void registerMenuItems(SIPCommMenu parentMenu)
    {
        if(protocolProviderListener == null)
        {
            protocolProviderListener = new ProtocolProviderListener(parentMenu);
            GuiActivator.bundleContext
                .addServiceListener(protocolProviderListener);
        }

        for (ProtocolProviderFactory providerFactory : GuiActivator
                    .getProtocolProviderFactories().values())
        {
            for (AccountID accountID : providerFactory.getRegisteredAccounts())
            {
                ServiceReference<ProtocolProviderService> serRef
                    = providerFactory.getProviderForAccount(accountID);
                ProtocolProviderService protocolProvider
                    = GuiActivator.bundleContext.getService(serRef);

                addAccountInternal(protocolProvider, parentMenu);
            }
        }

        // if we are in disabled menu mode and we have only one item
        // change its name (like global auto answer)
        if( ConfigurationUtils.isAutoAnswerDisableSubmenu()
            && getAutoAnswerItemCount(parentMenu) == 1)
        {
            updateItem(getAutoAnswerItem(parentMenu, 0), true);
        }
    }

    /**
     * Count number of Auto answer menu items.
     * @param menu
     * @return number of Auto answer menu items.
     */
    public static int getAutoAnswerItemCount(SIPCommMenu menu)
    {
        int count = 0;
        for(int i = 0; i < menu.getItemCount(); i++)
        {
            if(menu.getItem(i) instanceof AutoAnswerMenuItem)
                count++;
        }

        return count;
    }

    /**
     * Return auto answer menu item at index.
     * @param menu the menu.
     * @param index the index to found.
     * @return auto answer menu item at index.
     */
    public static AutoAnswerMenuItem getAutoAnswerItem(SIPCommMenu menu,
                                                       int index)
    {
        int currentIx = 0;
        for(int i = 0; i < menu.getItemCount(); i++)
        {
            if(menu.getItem(i) instanceof AutoAnswerMenuItem)
            {
                if(currentIx == index)
                    return (AutoAnswerMenuItem)menu.getItem(i);

                currentIx++;
            }
        }

        return null;
    }

    /**
     * Adds a menu item for the account given by <tt>protocolProvider</tt>.
     * @param protocolProvider the <tt>ProtocolProviderService</tt>, for which
     * to add a menu
     */
    private static void addAccount(
        final ProtocolProviderService protocolProvider,
        final SIPCommMenu parentMenu)
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    addAccount(protocolProvider, parentMenu);
                }
            });
            return;
        }

        // the initial number of autoanswer menu items
        int initialCount = getAutoAnswerItemCount(parentMenu);

        addAccountInternal(protocolProvider, parentMenu);

        // current count
        int itemsCount = getAutoAnswerItemCount(parentMenu);

        // if menu is disabled for autoanswer and we have added an item
        if(ConfigurationUtils.isAutoAnswerDisableSubmenu()
            && itemsCount != initialCount)
        {
            // if initial count was 1, lets change provider name to the
            // protocol one (the first one have like global name)
            if(initialCount == 1)
            {
                for(int i = 0; i < parentMenu.getItemCount(); i++)
                {
                    JMenuItem item = parentMenu.getItem(i);
                    if(item instanceof AutoAnswerMenuItem)
                    {
                        updateItem((AutoAnswerMenuItem)item, false);
                    }
                }
            }
            else if(initialCount == 0)
            {
                // this is the first item set its name like global one
                updateItem(getAutoAnswerItem(parentMenu, 0), true);
            }
        }
    }

    /**
     * Updates item text and icon.
     * @param item the item to update
     * @param isGlobal whether to make a global item.
     */
    private static void updateItem(AutoAnswerMenuItem item, boolean isGlobal)
    {
        if(item == null)
            return;

        if(isGlobal)
        {
            item.setText(GuiActivator.getResources().getI18NString(
                                    "service.gui.AUTO_ANSWER"));
            item.setIcon(new ImageIcon(
                getIconForProvider(item.getProtocolProvider(),
                    ImageLoader.getImage(ImageLoader.CALL_16x16_ICON),
                    item)));
        }
        else
        {
            item.setText(
                AutoAnswerMenuItem.getItemDisplayName(
                    item.getProtocolProvider()));
            item.setIcon(new ImageIcon(
                getIconForProvider(
                    item.getProtocolProvider(),
                    null,
                    item)));
        }
    }

    /**
     * Adds a menu item for the account given by <tt>protocolProvider</tt>.
     * @param protocolProvider the <tt>ProtocolProviderService</tt>, for which
     * to add a menu
     */
    private static void addAccountInternal(
        ProtocolProviderService protocolProvider,
        SIPCommMenu parentMenu)
    {
        OperationSetBasicAutoAnswer opSet = protocolProvider
            .getOperationSet(OperationSetBasicAutoAnswer.class);

        if(opSet == null)
        {
            return;
        }

        if (protocolProvider.getAccountID().isHidden())
            return;

        AutoAnswerMenuItem providerMenu =
            new AutoAnswerMenuItem(protocolProvider, parentMenu);

        int lastAutoAnswerIx = 0;
        boolean isMenuAdded = false;
        AccountID accountId = protocolProvider.getAccountID();
        // If we already have other accounts.
        for(int i = 0; i < parentMenu.getItemCount(); i++)
        {
            JMenuItem c = parentMenu.getItem(i);
            if (!(c instanceof AutoAnswerMenuItem))
                continue;

            AutoAnswerMenuItem menu = (AutoAnswerMenuItem) c;
            int menuIndex = parentMenu.getPopupMenu().getComponentIndex(menu);

            lastAutoAnswerIx = menuIndex;

            AccountID menuAccountID = menu.getProtocolProvider().getAccountID();

            int protocolCompare = accountId.getProtocolDisplayName().compareTo(
                menuAccountID.getProtocolDisplayName());

            // If the new account protocol name is before the name of the menu
            // we insert the new account before the given menu.
            if (protocolCompare < 0)
            {
                parentMenu.insert(providerMenu, menuIndex);
                isMenuAdded = true;
                break;
            }
            else if (protocolCompare == 0)
            {
                // If we have the same protocol name, we check the account name.
                if (accountId.getDisplayName()
                        .compareTo(menuAccountID.getDisplayName()) < 0)
                {
                    parentMenu.insert(providerMenu, menuIndex);
                    isMenuAdded = true;
                    break;
                }
            }
        }

        if (!isMenuAdded)
        {
            // add it after last auto answer menu found
            if(lastAutoAnswerIx != 0)
                parentMenu.insert(providerMenu, lastAutoAnswerIx);
            else
            {
                // as no items
                // lets find the first separator and add it after it
                // the place is between the separators
                int lastMenuItemIx = 0;
                for(int i = 0; i < parentMenu.getMenuComponentCount(); i++)
                {
                    if(parentMenu.getMenuComponent(i) instanceof JSeparator)
                    {
                        parentMenu.insert(providerMenu, lastMenuItemIx + 1);
                        isMenuAdded = true;
                        break;
                    }
                    else
                        lastMenuItemIx++;
                }

                if(!isMenuAdded)
                    parentMenu.add(providerMenu);
            }
        }
    }

    /**
     * Remove menu item for the account given by <tt>protocolProvider</tt>.
     * @param protocolProvider the <tt>ProtocolProviderService</tt>, for which
     * to remove the menu
     */
    public static void removeAccount(
        final ProtocolProviderService protocolProvider,
        final SIPCommMenu parentMenu)
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    removeAccount(protocolProvider, parentMenu);
                }
            });
            return;
        }

        for(int i = 0; i < parentMenu.getItemCount(); i++)
        {
            JMenuItem c = parentMenu.getItem(i);

            if (!(c instanceof AutoAnswerMenuItem))
                continue;

            AutoAnswerMenuItem menu = (AutoAnswerMenuItem) c;

            AccountID menuAccountID = menu.getProtocolProvider().getAccountID();

            if(menuAccountID.equals(protocolProvider.getAccountID()))
            {
                parentMenu.remove(menu);
                menu.clear();
                break;
            }
        }

        // if menu is disabled for auto answer and we have left with one
        // item set its name like a global one
        if(ConfigurationUtils.isAutoAnswerDisableSubmenu()
            && getAutoAnswerItemCount(parentMenu) == 1)
        {
            updateItem(getAutoAnswerItem(parentMenu, 0), true);
        }
    }

    /**
     * Loads menu item icons.
     */
    public void loadSkin()
    {
        this.setIcon(new ImageIcon(
            ImageLoader.getImage(ImageLoader.CALL_16x16_ICON)));
    }

    /**
     * Check whether any auto answer option is enabled for a protocol.
     * @param providerService the provider.
     * @return whether any auto answer option is enabled for a protocol.
     */
    private static boolean isAutoAnswerEnabled(
        ProtocolProviderService providerService)
    {
        OperationSetBasicAutoAnswer opset = providerService
            .getOperationSet(OperationSetBasicAutoAnswer.class);
        OperationSetAdvancedAutoAnswer opSetAdvanced = providerService
            .getOperationSet(OperationSetAdvancedAutoAnswer.class);

        if(opset == null)
            return false;

        if(opSetAdvanced != null)
        {
            if(opSetAdvanced.isAutoAnswerConditionSet())
            {
                return true;
            }

            if(!StringUtils.isNullOrEmpty(opSetAdvanced.getCallForward()))
            {
                return true;
            }
        }

        return opset.isAutoAnswerWithVideoSet()
            || opset.isAutoAnswerUnconditionalSet();
    }

    /**
     * Returns the icon for the provider.
     * @param providerService the provider
     * @param customProviderImage set custom provider image
     * @return the image.
     */
    private static Image getIconForProvider(
            ProtocolProviderService providerService,
            Image customProviderImage,
            ImageObserver imageObserver)
    {
        Image left
            = isAutoAnswerEnabled(providerService)
                ? ImageLoader.getImage(ImageLoader.AUTO_ANSWER_CHECK)
                : null;

        if(customProviderImage == null)
        {
            byte[] bytes
                = providerService.getProtocolIcon().getIcon(
                        ProtocolIcon.ICON_SIZE_16x16);

            if (bytes != null)
                customProviderImage = ImageUtils.getBytesInImage(bytes);
        }

        return
            ImageUtils.getComposedImage(
                    left,
                    customProviderImage,
                    imageObserver);
    }

    /**
     * Listens for new protocol providers.
     */
    private static class ProtocolProviderListener
        implements ServiceListener
    {
        /**
         * The parent window.
         */
        SIPCommMenu parentMenu;

        /**
         * Creates listener.
         * @param parentMenu the parent menu.
         */
        ProtocolProviderListener(SIPCommMenu parentMenu)
        {
            this.parentMenu = parentMenu;
        }

        /**
         * Implements the <tt>ServiceListener</tt> method. Verifies whether the
         * passed event concerns a <tt>ProtocolProviderService</tt> and adds the
         * corresponding UI controls in the menu.
         *
         * @param event The <tt>ServiceEvent</tt> object.
         */
        public void serviceChanged(ServiceEvent event)
        {
            ServiceReference serviceRef = event.getServiceReference();

            // if the event is caused by a bundle being stopped, we don't want to
            // know
            if (serviceRef.getBundle().getState() == Bundle.STOPPING)
            {
                return;
            }

            Object service = GuiActivator.bundleContext.getService(serviceRef);

            // we don't care if the source service is not a protocol provider
            if (!(service instanceof ProtocolProviderService))
            {
                return;
            }

            switch (event.getType())
            {
                case ServiceEvent.REGISTERED:
                    addAccount((ProtocolProviderService) service, parentMenu);
                    break;
                case ServiceEvent.UNREGISTERING:
                    removeAccount((ProtocolProviderService) service, parentMenu);
                    break;
            }
        }
    }

    /**
     * Represent menu item for provider.
     */
    private static class AutoAnswerMenuItem
        extends JMenuItem
        implements ActionListener
    {
        /**
         * The provider.
         */
        private ProtocolProviderService providerService;

        /**
         * The parent menu.
         */
        private final SIPCommMenu parentMenu;

        /**
         * Init the menu item.
         * @param provider the provider.
         * @param parentMenu the parent menu.
         */
        AutoAnswerMenuItem(ProtocolProviderService provider,
                           SIPCommMenu parentMenu)
        {
            this(provider,
                getItemDisplayName(provider),
                getIconForProvider(provider, null, parentMenu),
                parentMenu);
        }

        /**
         * Creates the menu item.
         * @param provider the provider.
         * @param displayName the display name of the item.
         * @param onlineImage the icon to display
         * @param parentMenu the parent menu.
         */
        private AutoAnswerMenuItem(ProtocolProviderService provider,
                                   String displayName,
                                   Image onlineImage,
                                   SIPCommMenu parentMenu)
        {
            super(
                    displayName,
                    (onlineImage == null) ? null : new ImageIcon(onlineImage));

            this.providerService = provider;
            this.parentMenu = parentMenu;

            this.addActionListener(this);
        }

        /**
         * Returns the protocol provider associated with this menu.
         * @return the protocol provider associated with this menu
         */
        public ProtocolProviderService getProtocolProvider()
        {
            return providerService;
        }

        /**
         * When action is performed on the item show a dialog.
         * @param e
         */
        public void actionPerformed(ActionEvent e)
        {
            new AutoAnswerOptionsDialog(providerService, parentMenu)
                .setVisible(true);
        }

        /**
         * Returns the display name to be used for this provider.
         * @param provider the provider.
         * @return the display name to be used for this provider.
         */
        private static String getItemDisplayName(
            ProtocolProviderService provider)
        {
            if(ConfigurationUtils.isAutoAnswerDisableSubmenu())
                return GuiActivator.getResources()
                            .getI18NString("service.gui.AUTO_ANSWER")
                    + " - " + provider.getAccountID().getDisplayName();
            else
                return provider.getAccountID().getDisplayName();
        }

        /**
         * A bug in macosx leaking instances of Menu and MenuItems, we prevent
         * leaking Protocol Providers.
         */
        public void clear()
        {
            providerService = null;
        }
    }

    /**
     * The dialog to config auto answer functionality for a provider.
     */
    private static class AutoAnswerOptionsDialog
        extends SIPCommDialog
        implements ActionListener
    {
        /**
         * Header name.
         */
        private static final String AUTO_ALERT_INFO_NAME = "Alert-Info";

        /**
         * Header name.
         */
        private static final String AUTO_ALERT_INFO_VALUE = "Auto Answer";

        /**
         * The provider.
         */
        private ProtocolProviderService providerService;

        /**
         * The ok button.
         */
        private final JButton okButton = new JButton(
            GuiActivator.getResources().getI18NString("service.gui.OK"));

        /**
         * The cancel button.
         */
        private final JButton cancelButton = new JButton(
            GuiActivator.getResources().getI18NString("service.gui.CANCEL"));

        /**
         * None radio button.
         */
        private JRadioButton noneRadio;

        /**
         * Unconditional radio button.
         */
        private JRadioButton alwaysAnswerRadio;

        /**
         * Alert info radio button.
         */
        private JRadioButton alertInfoValue;

        /**
         * Custom field radio button.
         */
        private JRadioButton customValueRadio;

        /**
         * Check box to active the video answer for wideo calls.
         */
        private SIPCommCheckBox answerWithVideoCheckBox;

        /**
         * Custom field name text field.
         */
        private JTextField headerNameField = new JTextField();

        /**
         * Custom value name text field.
         */
        private JTextField headerValueField = new JTextField();

        /**
         * Call fwd radio button.
         */
        private JRadioButton callFwd;

        /**
         * Call fwd number field.
         */
        private JTextField callFwdNumberField = new JTextField();

        /**
         * The parent menu.
         */
        private final SIPCommMenu parentMenu;

        /**
         * Create dialog.
         * @param providerService provider.
         * @param parentMenu the parent menu.
         */
        AutoAnswerOptionsDialog(ProtocolProviderService providerService,
                                SIPCommMenu parentMenu)
        {
            super(false);

            this.parentMenu = parentMenu;
            this.providerService = providerService;

            this.setTitle(GuiActivator.getResources()
                .getI18NString("service.gui.AUTO_ANSWER"));

            initComponents();

            loadValues();
        }

        /**
         * Creates panel.
         */
        private void initComponents()
        {
            OperationSetAdvancedAutoAnswer opSetAdvanced = providerService
                        .getOperationSet(OperationSetAdvancedAutoAnswer.class);

            ResourceManagementService R = GuiActivator.getResources();
            ButtonGroup group = new ButtonGroup();
            JPanel mainPanel = new TransparentPanel(new GridBagLayout());

            int currentRow = 0;
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = c.gridy = currentRow++;
            c.anchor = GridBagConstraints.LINE_START;
            c.fill = GridBagConstraints.HORIZONTAL;

            c.gridy = currentRow++;
            noneRadio = new SIPCommRadioButton(
                R.getI18NString("service.gui.NONE"));
            noneRadio.setSelected(true);
            group.add(noneRadio);
            mainPanel.add(noneRadio, c);

            c.gridy = currentRow++;
            mainPanel.add(
                getTitlePanel(R.getI18NString("service.gui.AUTO_ANSWER_LABEL")),
                c);

            c.gridy = currentRow++;
            alwaysAnswerRadio = new SIPCommRadioButton(
                R.getI18NString("service.gui.AUTO_ANSWER_ALL_CALLS"));
            group.add(alwaysAnswerRadio);
            mainPanel.add(alwaysAnswerRadio, c);

            if(opSetAdvanced != null)
            {
                c.gridy = currentRow++;
                alertInfoValue = new SIPCommRadioButton(
                    R.getI18NString("service.gui.AUTO_ANSWER_ALERT_INFO_FIELDS"));
                group.add(alertInfoValue);
                mainPanel.add(alertInfoValue, c);

                c.gridy = currentRow++;
                customValueRadio = new SIPCommRadioButton(
                    R.getI18NString("service.gui.AUTO_ANSWER_CUSTOM_FIELDS"));
                group.add(customValueRadio);
                mainPanel.add(customValueRadio, c);

                JPanel customHeaderPanel = new TransparentPanel(
                    new GridLayout(1, 2));
                JPanel namePanel = new TransparentPanel(new BorderLayout());
                namePanel.add(
                    new JLabel(R.getI18NString("service.gui.AUTO_ANSWER_FIELD")),
                    BorderLayout.WEST);
                namePanel.add(headerNameField, BorderLayout.CENTER);
                JPanel valuePanel = new TransparentPanel(new BorderLayout());
                valuePanel.add(
                    new JLabel(R.getI18NString("service.gui.AUTO_ANSWER_VALUE")),
                    BorderLayout.WEST);
                valuePanel.add(headerValueField, BorderLayout.CENTER);
                customHeaderPanel.add(namePanel);
                customHeaderPanel.add(valuePanel);

                c.gridy = currentRow++;
                c.insets = new Insets(0, 28, 0, 0);
                mainPanel.add(customHeaderPanel, c);

                String description =
                    R.getI18NString("service.gui.AUTO_ANSWER_DESCR_VLUE");
                JLabel descriptionLabel = new JLabel(description);
                descriptionLabel.setToolTipText(description);
                descriptionLabel.setForeground(Color.GRAY);
                descriptionLabel.setFont(
                    descriptionLabel.getFont().deriveFont(8));
                descriptionLabel.setBorder(
                    BorderFactory.createEmptyBorder(0, 0, 8, 0));
                descriptionLabel.setHorizontalAlignment(JLabel.RIGHT);

                c.gridy = currentRow++;
                mainPanel.add(descriptionLabel, c);

                c.gridy = currentRow++;
                c.insets = new Insets(0, 0, 0, 0);
                mainPanel.add(getTitlePanel(
                    R.getI18NString("service.gui.AUTO_ANSWER_FWD_CALLS")), c);

                c.gridy = currentRow++;
                callFwd = new SIPCommRadioButton(
                    R.getI18NString("service.gui.AUTO_ANSWER_FWD_CALLS_TO"));
                group.add(callFwd);
                mainPanel.add(callFwd, c);

                c.gridy = currentRow++;
                c.insets = new Insets(0, 28, 0, 0);
                mainPanel.add(callFwdNumberField, c);

                c.insets = new Insets(0, 0, 0, 0);
            }
            else
            {
                c.insets = new Insets(10, 0, 0, 0);
            }

            c.gridy = currentRow++;
            mainPanel.add(
                    getTitlePanel(
                        R.getI18NString("service.gui.AUTO_ANSWER_VIDEO")),
                    c);
            c.gridy = currentRow++;
            answerWithVideoCheckBox = new SIPCommCheckBox(
                R.getI18NString(
                    "service.gui.AUTO_ANSWER_VIDEO_CALLS_WITH_VIDEO"));
            mainPanel.add(answerWithVideoCheckBox, c);

            TransparentPanel buttonsPanel
                = new TransparentPanel(new FlowLayout(FlowLayout.RIGHT));

            this.getRootPane().setDefaultButton(okButton);
            okButton.setMnemonic(
                GuiActivator.getResources().getI18nMnemonic("service.gui.OK"));
            cancelButton.setMnemonic(
                GuiActivator.getResources().getI18nMnemonic("service.gui.CANCEL"));

            okButton.addActionListener(this);
            cancelButton.addActionListener(this);

            buttonsPanel.add(okButton);
            buttonsPanel.add(cancelButton);

            c.gridy = currentRow++;
            mainPanel.add(buttonsPanel, c);

            mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            this.setLayout(new BorderLayout());
            this.add(mainPanel, BorderLayout.CENTER);
        }

        /**
         * Creates separator with text.
         * @param title the title
         * @return the panel separator.
         */
        private JPanel getTitlePanel(String title)
        {
            JLabel label = new JLabel(title);
            label.setBorder(new EmptyBorder(0, 0, 0, 10));
            label.setFont(UIManager.getFont("TitledBorder.font"));
            label.setForeground(UIManager
                .getColor("TitledBorder.titleColor"));

            JPanel pnlSectionName = new TransparentPanel();
            pnlSectionName.setLayout(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = c.gridy = 0;
            c.anchor = GridBagConstraints.LINE_START;
            c.gridwidth = 2;
            pnlSectionName.add(label, c);
            c.gridx = 2;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            pnlSectionName.add(new JSeparator(), c);

            JPanel pnlSection = new TransparentPanel()
            {
                @Override
                public Component add(Component comp)
                {
                    if(comp instanceof JComponent)
                        ((JComponent)comp).setAlignmentX(LEFT_ALIGNMENT);
                    return super.add(comp);
                }
            };
            pnlSection.setLayout(new BoxLayout(pnlSection, BoxLayout.Y_AXIS));
            pnlSection.add(pnlSectionName);

            return pnlSection;
        }

        /**
         * Saves settings.
         * @param e the event on button.
         */
        public void actionPerformed(ActionEvent e)
        {
            if(e.getSource().equals(okButton))
            {
                OperationSetBasicAutoAnswer opset = providerService
                    .getOperationSet(OperationSetBasicAutoAnswer.class);
                OperationSetAdvancedAutoAnswer opSetAdvanced = providerService
                    .getOperationSet(OperationSetAdvancedAutoAnswer.class);

                if(noneRadio.isSelected())
                {
                    opset.clear();
                    if(opSetAdvanced != null)
                        opSetAdvanced.clear();
                }
                else if(alwaysAnswerRadio.isSelected())
                {
                    opset.setAutoAnswerUnconditional();
                }
                else if(alertInfoValue.isSelected())
                {
                    if(opSetAdvanced != null)
                    {
                        opSetAdvanced.setAutoAnswerCondition(
                            AUTO_ALERT_INFO_NAME,
                            AUTO_ALERT_INFO_VALUE);
                    }
                }
                else if(customValueRadio.isSelected())
                {
                    if(opSetAdvanced != null)
                    {
                        opSetAdvanced.setAutoAnswerCondition(
                            headerNameField.getText(),
                            headerValueField.getText());
                    }
                }
                else if(callFwd.isSelected())
                {
                    if(opSetAdvanced != null)
                        opSetAdvanced.setCallForward(
                            callFwdNumberField.getText());
                }

                opset.setAutoAnswerWithVideo(
                        answerWithVideoCheckBox.isSelected());

                // as settings changed lets update items
                if( ConfigurationUtils.isAutoAnswerDisableSubmenu()
                    && getAutoAnswerItemCount(parentMenu) == 1)
                {
                    updateItem(getAutoAnswerItem(parentMenu, 0), true);
                }
                else
                {
                    for(int i = 0; i < parentMenu.getItemCount(); i++)
                    {
                        JMenuItem item = parentMenu.getItem(i);
                        if(item instanceof AutoAnswerMenuItem)
                        {
                            updateItem((AutoAnswerMenuItem)item, false);
                        }
                    }
                }
            }

            dispose();
        }

        /**
         * Esc pressed.
         * @param isEscaped indicates if this frame has been closed by
         * pressing the escape
         */
        @Override
        protected void close(boolean isEscaped)
        {
            dispose();
        }


        /**
         * Populate values from opset to local components.
         */
        private void loadValues()
        {
            OperationSetBasicAutoAnswer opset = providerService
                .getOperationSet(OperationSetBasicAutoAnswer.class);
            OperationSetAdvancedAutoAnswer opSetAdvanced = providerService
                .getOperationSet(OperationSetAdvancedAutoAnswer.class);

            if(opset == null)
                return;

            noneRadio.setSelected(true);
            alwaysAnswerRadio.setSelected(
                opset.isAutoAnswerUnconditionalSet());

            if(opSetAdvanced != null)
            {
                if(opSetAdvanced.isAutoAnswerConditionSet())
                {
                    String fName = opSetAdvanced.getAutoAnswerHeaderName();
                    String fValue = opSetAdvanced.getAutoAnswerHeaderValue();
                    if(AUTO_ALERT_INFO_NAME.equals(fName)
                       && AUTO_ALERT_INFO_VALUE.equals(fValue))
                    {
                        alertInfoValue.setSelected(true);
                    }
                    else
                    {
                        customValueRadio.setSelected(true);
                        headerNameField.setText(fName);

                        if(!StringUtils.isNullOrEmpty(fValue))
                            headerValueField.setText(fValue);
                    }

                }

                if(!StringUtils.isNullOrEmpty(opSetAdvanced.getCallForward()))
                {
                    callFwd.setSelected(true);
                    callFwdNumberField.setText(opSetAdvanced.getCallForward());
                }
            }

            answerWithVideoCheckBox.setSelected(
                    opset.isAutoAnswerWithVideoSet());
        }
    }
}
