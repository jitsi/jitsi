/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.account;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

public class NewAccountDialog
    extends SIPCommDialog
    implements ActionListener
{
    private Logger logger = Logger.getLogger(NewAccountDialog.class);

    private TransparentPanel mainPanel
        = new TransparentPanel(new BorderLayout(5, 5));

    private TransparentPanel accountPanel
        = new TransparentPanel(new BorderLayout());

    private TransparentPanel networkPanel
        = new TransparentPanel(new BorderLayout());

    private JLabel networkLabel = new JLabel(
        Messages.getI18NString("network").getText());

    private JComboBox networkComboBox = new JComboBox();

    private JButton advancedButton = new JButton(
        Messages.getI18NString("advanced").getText());

    private JButton addAccountButton = new JButton(
        Messages.getI18NString("add").getText());

    private JButton cancelButton = new JButton(
        Messages.getI18NString("cancel").getText());

    private TransparentPanel rightButtonPanel
        = new TransparentPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));

    private TransparentPanel buttonPanel
        = new TransparentPanel(new BorderLayout());

    private String preferredWizardName;

    public NewAccountDialog()
    {
        super(GuiActivator.getUIService().getMainFrame());

        this.setTitle(Messages.getI18NString("newAccount").getText());

        this.getContentPane().add(mainPanel);

        this.mainPanel.setBorder(
            BorderFactory.createEmptyBorder(15, 15, 15, 15));

        this.networkPanel.setBorder(
            BorderFactory.createEmptyBorder(5, 5, 5, 5));

        this.mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        this.buttonPanel.add(advancedButton, BorderLayout.WEST);
        this.buttonPanel.add(rightButtonPanel, BorderLayout.EAST);
        this.advancedButton.addActionListener(this);

        this.rightButtonPanel.add(addAccountButton);
        this.rightButtonPanel.add(cancelButton);
        this.addAccountButton.addActionListener(this);
        this.cancelButton.addActionListener(this);

        this.mainPanel.add(networkPanel, BorderLayout.NORTH);
        this.networkPanel.add(networkLabel, BorderLayout.WEST);
        this.networkPanel.add(networkComboBox, BorderLayout.CENTER);

        this.getRootPane().setDefaultButton(addAccountButton);

        this.networkComboBox.setRenderer(new NetworkListCellRenderer());
        this.networkComboBox.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                AccountRegistrationWizard wizard
                    = (AccountRegistrationWizard) networkComboBox
                        .getSelectedItem();

                loadSelectedWizard(wizard);
            }
        });

        this.mainPanel.add(accountPanel, BorderLayout.CENTER);

        this.initNetworkList();
    }

    private void initNetworkList()
    {
        // check for preferred wizard
        String prefWName = GuiActivator.getResources().
            getSettingsString("preferredAccountWizard");
        if(prefWName != null && prefWName.length() > 0)
            preferredWizardName = prefWName;

        ServiceReference[] accountWizardRefs = null;
        try
        {
            accountWizardRefs = GuiActivator.bundleContext
                .getServiceReferences(
                    AccountRegistrationWizard.class.getName(),
                    null);
        }
        catch (InvalidSyntaxException ex)
        {
            // this shouldn't happen since we're providing no parameter string
            // but let's log just in case.
            logger.error(
                "Error while retrieving service refs", ex);
            return;
        }

        // in case we found any, add them in this container.
        if (accountWizardRefs != null)
        {
            logger.debug("Found "
                         + accountWizardRefs.length
                         + " already installed providers.");
            
            // Create a list to sort the wizards
            ArrayList<AccountRegistrationWizard> list =
                new ArrayList<AccountRegistrationWizard>();
            list.ensureCapacity(accountWizardRefs.length);
            
            AccountRegistrationWizard prefWiz = null; 
            
            for (int i = 0; i < accountWizardRefs.length; i++)
            {
                AccountRegistrationWizard wizard
                    = (AccountRegistrationWizard) GuiActivator.bundleContext
                        .getService(accountWizardRefs[i]);

                list.add(wizard);

                // is it the prefered protocol ?
                if(preferredWizardName != null
                    && wizard.getClass().getName().equals(preferredWizardName))
                {
                    prefWiz = wizard;
                }
            }
            
            // Sort the list
            Collections.sort(list, new Comparator<AccountRegistrationWizard>() {
                public int compare(AccountRegistrationWizard arg0,
                        AccountRegistrationWizard arg1)
                {
                    return arg0.getProtocolName().compareTo(arg1.getProtocolName());
                }
            });
            
            // Add the item in the combobox and if
            // there is a prefered wizard auto select it
            for (int i=0; i<list.size(); i++)
            {
                networkComboBox.addItem(list.get(i));
            }
            if (prefWiz != null)
            {
                networkComboBox.setSelectedItem(prefWiz);
            }
        }
    }

    private class NetworkListCellRenderer
        extends JLabel
        implements ListCellRenderer
    {
        public NetworkListCellRenderer()
        {
            this.setOpaque(true);

            this.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        }

        public Component getListCellRendererComponent(JList list, Object value,
            int index, boolean isSelected, boolean cellHasFocus)
        {
            AccountRegistrationWizard wizard
                = (AccountRegistrationWizard) value;

            if (isSelected)
            {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            }
            else
            {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }

            this.setText(wizard.getProtocolName());
            this.setIcon(new ImageIcon(
                ImageLoader.getBytesInImage(wizard.getIcon())));

            return this;
        }
    }

    private void loadSelectedWizard(AccountRegistrationWizard wizard)
    {
        accountPanel.removeAll();

        TransparentPanel fixedWidthPanel = new TransparentPanel();

        this.accountPanel.add(fixedWidthPanel, BorderLayout.SOUTH);
        fixedWidthPanel.setPreferredSize(new Dimension(430, 3));
        fixedWidthPanel.setMinimumSize(new Dimension(430, 3));
        fixedWidthPanel.setMaximumSize(new Dimension(430, 3));

        JComponent simpleWizardForm = (JComponent) wizard.getSimpleForm();
        simpleWizardForm.setOpaque(false);

        accountPanel.add(simpleWizardForm, BorderLayout.NORTH);
        accountPanel.revalidate();
        accountPanel.repaint();
        this.pack();
    }

    @Override
    protected void close(boolean isEscaped)
    {
    }

    /**
     * 
     */
    public void actionPerformed(ActionEvent event)
    {
        JButton sourceButton = (JButton) event.getSource();

        AccountRegistrationWizard wizard
            = (AccountRegistrationWizard) networkComboBox.getSelectedItem();

        AccountRegWizardContainerImpl wizardContainer
            = ((AccountRegWizardContainerImpl) GuiActivator.getUIService()
                .getAccountRegWizardContainer());

        if (sourceButton.equals(advancedButton))
        {
            wizard.setModification(false);

            wizardContainer.setTitle(Messages.getI18NString(
                "accountRegistrationWizard").getText());

            wizardContainer.setCurrentWizard(wizard);

            wizardContainer.showDialog(false);

            this.dispose();
        }
        else if (sourceButton.equals(addAccountButton))
        {
            ProtocolProviderService protocolProvider = wizard.signin();

            if (protocolProvider != null)
                wizardContainer.saveAccountWizard(protocolProvider, wizard);

            this.dispose();
        }
        else if (sourceButton.equals(cancelButton))
        {
            this.dispose();
        }
    }
}
