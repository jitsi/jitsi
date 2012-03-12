/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.menus;

import java.awt.*;
import java.awt.event.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.skin.*;
import net.java.sip.communicator.util.swing.*;
import org.osgi.framework.*;

import javax.swing.*;
import javax.swing.border.*;

/**
 * The auto answer menu dynamically adds/removes menu items for enabled/disabled
 * protocol providers.
 *
 * @author Damian Minkov
 */
public class AutoAnswerMenu
    extends SIPCommMenu
    implements Skinnable,
               ServiceListener
{
    /**
     * Creates the menu and load already registered providers.
     */
    public AutoAnswerMenu()
    {
        super(GuiActivator.getResources()
            .getI18NString("service.gui.AUTO_ANSWER"));

        loadSkin();

        GuiActivator.bundleContext.addServiceListener(this);

        this.registerMenuItems();
    }

    /**
     * Registers all menu items.
     */
    private void registerMenuItems()
    {
        for (ProtocolProviderFactory providerFactory : GuiActivator
                    .getProtocolProviderFactories().values())
        {
            ServiceReference serRef;
            ProtocolProviderService protocolProvider;

            for (AccountID accountID : providerFactory.getRegisteredAccounts())
            {
                serRef = providerFactory.getProviderForAccount(accountID);

                protocolProvider =
                    (ProtocolProviderService) GuiActivator.bundleContext
                        .getService(serRef);

                addAccount(protocolProvider);
            }
        }
    }

    /**
     * Adds a menu item for the account given by <tt>protocolProvider</tt>.
     * @param protocolProvider the <tt>ProtocolProviderService</tt>, for which
     * to add a menu
     */
    public void addAccount(ProtocolProviderService protocolProvider)
    {
        OperationSetBasicAutoAnswer opSet = protocolProvider
            .getOperationSet(OperationSetBasicAutoAnswer.class);

        if(opSet == null)
        {
            return;
        }

        boolean isHidden
            = protocolProvider
                    .getAccountID()
                        .getAccountProperty(
                            ProtocolProviderFactory.IS_PROTOCOL_HIDDEN)
                != null;

        if (isHidden)
            return;

        AutoAnswerMenuItem providerMenu =
            new AutoAnswerMenuItem(protocolProvider);

        boolean isMenuAdded = false;
        AccountID accountId = protocolProvider.getAccountID();
        // If we already have other accounts.
        for(int i = 0; i < getItemCount(); i++)
        {
            JMenuItem c = getItem(i);
            if (!(c instanceof AutoAnswerMenuItem))
                continue;

            AutoAnswerMenuItem menu = (AutoAnswerMenuItem) c;
            int menuIndex = getPopupMenu().getComponentIndex(menu);

            AccountID menuAccountID = menu.getProtocolProvider().getAccountID();

            int protocolCompare = accountId.getProtocolDisplayName().compareTo(
                menuAccountID.getProtocolDisplayName());

            // If the new account protocol name is before the name of the menu
            // we insert the new account before the given menu.
            if (protocolCompare < 0)
            {
                insert(providerMenu, menuIndex);
                isMenuAdded = true;
                break;
            }
            else if (protocolCompare == 0)
            {
                // If we have the same protocol name, we check the account name.
                if (accountId.getDisplayName()
                        .compareTo(menuAccountID.getDisplayName()) < 0)
                {
                    insert( providerMenu, menuIndex);
                    isMenuAdded = true;
                    break;
                }
            }
        }

        if (!isMenuAdded)
            add(providerMenu);
    }

    /**
     * Remove menu item for the account given by <tt>protocolProvider</tt>.
     * @param protocolProvider the <tt>ProtocolProviderService</tt>, for which
     * to remove the menu
     */
    public void removeAccount(ProtocolProviderService protocolProvider)
    {
        for(int i = 0; i < getItemCount(); i++)
        {
            JMenuItem c = getItem(i);
            
            if (!(c instanceof AutoAnswerMenuItem))
                continue;

            AutoAnswerMenuItem menu = (AutoAnswerMenuItem) c;

            AccountID menuAccountID = menu.getProtocolProvider().getAccountID();

            if(menuAccountID.equals(protocolProvider.getAccountID()))
            {
                this.remove(menu);
                return;
            }
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
                this.addAccount((ProtocolProviderService) service);
                break;
            case ServiceEvent.UNREGISTERING:
                this.removeAccount((ProtocolProviderService) service);
                break;
        }
    }

    /**
     * Represent menu item for provider.
     */
    private class AutoAnswerMenuItem
        extends JMenuItem
        implements ActionListener
    {
        /**
         * The provider.
         */
        private ProtocolProviderService providerService;

        /**
         * Init the menu item.
         * @param provider the provider.
         */
        AutoAnswerMenuItem(ProtocolProviderService provider)
        {
            this(provider,
                 provider.getAccountID().getDisplayName(),
                 ImageUtils.getBytesInImage(
                    provider.getProtocolIcon().getIcon(
                        ProtocolIcon.ICON_SIZE_16x16)));
        }

        /**
         * Creates the menu item.
         * @param provider the provider.
         * @param displayName the display name of the item.
         * @param onlineImage the icon to display
         */
        private AutoAnswerMenuItem(ProtocolProviderService provider,
                                   String displayName,
                                   Image onlineImage)
        {
            super(displayName, new ImageIcon(onlineImage));
            this.providerService = provider;

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
            new AutoAnswerOptionsDialog(providerService).setVisible(true);
        }
    }

    /**
     * The dialog to config auto answer functionality for a provider.
     */
    private class AutoAnswerOptionsDialog
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
         * Create dialog.
         * @param providerService provider.
         */
        AutoAnswerOptionsDialog(ProtocolProviderService providerService)
        {
            super(false);

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
                getTitlePanel(R.getI18NString("service.gui.AUTO_ANSWER")), c);

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
            }

            dispose();
        }

        /**
         * Esc pressed.
         * @param isEscaped indicates if this frame has been closed by
         * pressing the escape
         */
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
        }
    }
}
