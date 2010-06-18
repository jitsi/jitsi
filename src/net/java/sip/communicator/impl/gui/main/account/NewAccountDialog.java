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
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

import org.osgi.framework.*;

/**
 * The <tt>NewAccountDialog</tt> is the dialog containing the form used to
 * create a new account.
 * 
 * @author Yana Stamcheva
 */
public class NewAccountDialog
    extends SIPCommDialog
    implements  ActionListener
{
    private final Logger logger = Logger.getLogger(NewAccountDialog.class);

    private TransparentPanel mainPanel
        = new TransparentPanel(new BorderLayout(5, 5));

    private TransparentPanel accountPanel
        = new TransparentPanel(new BorderLayout());

    private TransparentPanel networkPanel
        = new TransparentPanel(new BorderLayout());

    private JLabel networkLabel = new JLabel(
        GuiActivator.getResources().getI18NString("service.gui.NETWORK"));

    private JComboBox networkComboBox = new JComboBox();

    private JButton advancedButton = new JButton(
        GuiActivator.getResources().getI18NString("service.gui.ADVANCED"));

    private JButton addAccountButton = new JButton(
        GuiActivator.getResources().getI18NString("service.gui.ADD"));

    private JButton cancelButton = new JButton(
        GuiActivator.getResources().getI18NString("service.gui.CANCEL"));

    private TransparentPanel rightButtonPanel
        = new TransparentPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));

    private TransparentPanel buttonPanel
        = new TransparentPanel(new BorderLayout());

    private EmptyAccountRegistrationWizard emptyWizard
            = new EmptyAccountRegistrationWizard();

    private String preferredWizardName;

    private static NewAccountDialog newAccountDialog;

    /**
     * Creates the dialog and initializes the UI.
     */
    public NewAccountDialog()
    {
        super(GuiActivator.getUIService().getMainFrame());

        this.setTitle(GuiActivator.getResources()
            .getI18NString("service.gui.NEW_ACCOUNT"));

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

    /**
     * Detects all currently registered protocol wizards so that we could fill
     * the protocol/network combo with their graphical representation.
     */
    private void initNetworkList()
    {
        // check for preferred wizard
        String prefWName = GuiActivator.getResources().
            getSettingsString("impl.gui.PREFERRED_ACCOUNT_WIZARD");
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
            if (logger.isDebugEnabled())
                logger.debug("Found "
                         + accountWizardRefs.length
                         + " already installed providers.");

            // Create a list to sort the wizards
            ArrayList<AccountRegistrationWizard> networksList =
                new ArrayList<AccountRegistrationWizard>();
            networksList.ensureCapacity(accountWizardRefs.length);

            AccountRegistrationWizard prefWiz = null;

            for (int i = 0; i < accountWizardRefs.length; i++)
            {
                AccountRegistrationWizard wizard
                    = (AccountRegistrationWizard) GuiActivator.bundleContext
                        .getService(accountWizardRefs[i]);

                networksList.add(wizard);

                // is it the preferred protocol ?
                if(preferredWizardName != null
                    && wizard.getClass().getName().equals(preferredWizardName))
                {
                    prefWiz = wizard;
                }
            }

            // Sort the list
            Collections.sort(networksList,
                            new Comparator<AccountRegistrationWizard>()
            {
                public int compare(AccountRegistrationWizard arg0,
                                   AccountRegistrationWizard arg1)
                {
                    return arg0.getProtocolName().compareToIgnoreCase(
                                    arg1.getProtocolName());
                }
            });

            // Add the items in the combobox
            for (int i=0; i<networksList.size(); i++)
            {
                networkComboBox.addItem(networksList.get(i));
            }

            //if we have a prefered wizard auto select it
            if (prefWiz != null)
            {
                networkComboBox.setSelectedItem(prefWiz);
            }
            else//if we don't we send our empty page and let the wizard choose.
            {
                networkComboBox.insertItemAt(emptyWizard, 0);
                networkComboBox.setSelectedItem(emptyWizard);
                
                //disable the advanced and add buttons so that it would be 
                //clear for the user that they need to choose a network first
                advancedButton.setEnabled(false);
                addAccountButton.setEnabled(false);
            }
        }
    }

    /**
     * A custom cell renderer for the network combobox.
     */
    private static class NetworkListCellRenderer
        extends JLabel
        implements ListCellRenderer
    {
        private static final long serialVersionUID = 0L;

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
            byte[] icon = wizard.getIcon();

            if( icon != null && icon.length > 0)
            {
                this.setIcon(new ImageIcon(icon));
            }
            else
            {
                this.setIcon(null);
            }

            return this;
        }
    }

    /**
     * Loads the given wizard in the user interface.
     *
     * @param wizard the wizard to load
     */
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

        accountPanel.add(simpleWizardForm);

        //enable the add and advanced buttons if this is a real protocol
        addAccountButton.setEnabled(
                        !(wizard instanceof EmptyAccountRegistrationWizard));
        advancedButton.setEnabled(
                        !(wizard instanceof EmptyAccountRegistrationWizard));

        accountPanel.revalidate();
        accountPanel.repaint();

        this.pack();
    }

    /**
     * Loads the given error message in the current dialog, by re-validating the
     * content.
     *
     * @param errorMessage The error message to load.
     */
    private void loadErrorMessage(String errorMessage)
    {
        JEditorPane errorMessagePane = new JEditorPane();
        errorMessagePane.setOpaque(false);
        errorMessagePane.setText(errorMessage);

        errorMessagePane.setForeground(Color.RED);

        accountPanel.add(errorMessagePane, BorderLayout.NORTH);
        accountPanel.revalidate();
        accountPanel.repaint();

        this.pack();

        //WORKAROUND: there's something wrong happening in this pack and
        //components get cluttered, partially hiding the password text field.
        //I am under the impression that this has something to do with the
        //message pane preferred size being ignored (or being 0) which is why
        //I am adding it's height to the dialog. It's quite ugly so please fix
        //if you have something better in mind.
        this.setSize(getWidth(), getHeight()+errorMessagePane.getHeight());
    }

    /**
     * Handles button actions.
     * @param event the <tt>ActionEvent</tt> that notified us
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

            wizardContainer.setTitle(
                GuiActivator.getResources().getI18NString(
                "service.gui.ACCOUNT_REGISTRATION_WIZARD"));

            wizardContainer.setCurrentWizard(wizard);

            wizardContainer.showDialog(false);

            this.dispose();
        }
        else if (sourceButton.equals(addAccountButton))
        {
            ProtocolProviderService protocolProvider;
            try
            {
                if(wizard == emptyWizard)
                {
                    loadErrorMessage(GuiActivator.getResources().getI18NString(
                        "service.gui.CHOOSE_NETWORK"));

                }
                protocolProvider = wizard.signin();

                if (protocolProvider != null)
                    wizardContainer.saveAccountWizard(protocolProvider, wizard);

                this.dispose();
            }
            catch (OperationFailedException e)
            {
                // If the sign in operation has failed we don't want to close
                // the dialog in order to give the user the possibility to
                // retry.
                if (logger.isDebugEnabled())
                    logger.debug("The sign in operation has failed.");

                if (e.getErrorCode()
                        == OperationFailedException.ILLEGAL_ARGUMENT)
                {
                    loadErrorMessage(GuiActivator.getResources().getI18NString(
                        "service.gui.USERNAME_NULL"));
                }
                else if (e.getErrorCode()
                        == OperationFailedException.IDENTIFICATION_CONFLICT)
                {
                    loadErrorMessage(GuiActivator.getResources().getI18NString(
                        "service.gui.USER_EXISTS_ERROR"));
                }
                else
                {
                    loadErrorMessage(GuiActivator.getResources().getI18NString(
                                "service.gui.ACCOUNT_CREATION_FAILED",
                                new String[]{e.getMessage()}));
                }
            }
            catch (Exception e)
            {
                // If the sign in operation has failed we don't want to close
                // the dialog in order to give the user the possibility to
                // retry.
                if (logger.isDebugEnabled())
                    logger.debug("The sign in operation has failed.");

                loadErrorMessage(GuiActivator.getResources().getI18NString(
                                "service.gui.ACCOUNT_CREATION_FAILED",
                                new String[]{e.getMessage()}));
            }
        }
        else if (sourceButton.equals(cancelButton))
        {
            this.dispose();
        }
    }

    /**
     * Shows the new account dialog.
     */
    public static void showNewAccountDialog()
    {
        if (newAccountDialog == null)
            newAccountDialog = new NewAccountDialog();

        newAccountDialog.pack();
        newAccountDialog.setVisible(true);
    }

    /**
     * Remove the newAccountDialog, when the window is closed.
     * @param isEscaped indicates if the dialog has been escaped
     */
    protected void close(boolean isEscaped)
    {
        newAccountDialog = null;
    }

    /**
     * Remove the newAccountDialog on dispose.
     */
    public void dispose()
    {
        newAccountDialog = null;

        super.dispose();
    }
}
