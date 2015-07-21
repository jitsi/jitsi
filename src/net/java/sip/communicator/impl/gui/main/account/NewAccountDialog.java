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
package net.java.sip.communicator.impl.gui.main.account;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.wizard.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.resources.*;
import org.osgi.framework.*;

/**
 * The <tt>NewAccountDialog</tt> is the dialog containing the form used to
 * create a new account.
 *
 * @author Yana Stamcheva
 * @author Lyubomir Marinov
 */
public class NewAccountDialog
    extends SIPCommDialog
    implements CreateAccountWindow,
               ActionListener,
               PropertyChangeListener
{
    /**
     * The <tt>Logger</tt> used by the <tt>NewAccountDialog</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(NewAccountDialog.class);

    private final TransparentPanel accountPanel
        = new TransparentPanel(new BorderLayout());

    private final JComboBox networkComboBox = new JComboBox();

    private final JButton advancedButton = new JButton(
        GuiActivator.getResources().getI18NString("service.gui.ADVANCED"));

    private final JButton addAccountButton = new JButton(
        GuiActivator.getResources().getI18NString("service.gui.ADD"));

    private final JButton cancelButton = new JButton(
        GuiActivator.getResources().getI18NString("service.gui.CANCEL"));

    private final EmptyAccountRegistrationWizard emptyWizard
            = new EmptyAccountRegistrationWizard();

    private static NewAccountDialog newAccountDialog;

    private JTextArea errorMessagePane;

    /**
     * If account is signing-in ignore close.
     */
    private boolean isCurrentlySigningIn = false;

    /**
     * Status label, show when connecting.
     */
    private final JLabel statusLabel = new JLabel();

    /**
     * The container of the wizard.
     */
    private final AccountRegWizardContainerImpl wizardContainer;

    /**
     * Creates the dialog and initializes the UI.
     */
    public NewAccountDialog()
    {
        super(GuiActivator.getUIService().getMainFrame(), false);

        ResourceManagementService resources = GuiActivator.getResources();

        TransparentPanel mainPanel
            = new TransparentPanel(new BorderLayout(5, 5));
        TransparentPanel networkPanel
            = new TransparentPanel(new BorderLayout());
        JLabel networkLabel
            = new JLabel(resources.getI18NString("service.gui.NETWORK"));
        TransparentPanel rightButtonPanel
            = new TransparentPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        TransparentPanel buttonPanel
            = new TransparentPanel(new BorderLayout());

        String title = resources.getI18NString("service.gui.NEW_ACCOUNT");
        if ((title != null) && title.endsWith("..."))
            title = title.substring(0, title.length() - 3);
        this.setTitle(title);

        this.getContentPane().add(mainPanel);

        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        networkPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JPanel statusPanel = new TransparentPanel(
                new FlowLayout(FlowLayout.CENTER));
        statusPanel.add(statusLabel);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        if (!ConfigurationUtils.isAdvancedAccountConfigDisabled())
        {
            buttonPanel.add(advancedButton, BorderLayout.WEST);
            this.advancedButton.addActionListener(this);
        }

        buttonPanel.add(rightButtonPanel, BorderLayout.EAST);
        buttonPanel.add(statusPanel, BorderLayout.CENTER);

        rightButtonPanel.add(addAccountButton);
        rightButtonPanel.add(cancelButton);
        this.addAccountButton.addActionListener(this);
        this.cancelButton.addActionListener(this);

        mainPanel.add(networkPanel, BorderLayout.NORTH);
        networkPanel.add(networkLabel, BorderLayout.WEST);
        networkPanel.add(networkComboBox, BorderLayout.CENTER);

        this.networkComboBox.setRenderer(new NetworkListCellRenderer());
        this.networkComboBox.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                AccountRegistrationWizard wizard
                    = (AccountRegistrationWizard)
                        networkComboBox.getSelectedItem();

                loadSelectedWizard(wizard);
            }
        });

        mainPanel.add(accountPanel, BorderLayout.CENTER);

        this.initNetworkList();

        wizardContainer
            = (AccountRegWizardContainerImpl)
                GuiActivator.getUIService().getAccountRegWizardContainer();
        /*
         * XXX The wizardContainer will outlive this instance so it is essential
         * to call removePropertyChangeListener on its model in order to prevent
         * a leak of this instance.
         */
        wizardContainer.getModel().addPropertyChangeListener(this);
    }

    /**
     * Detects all currently registered protocol wizards so that we could fill
     * the protocol/network combo with their graphical representation.
     */
    private void initNetworkList()
    {
        // check for preferred wizard
        String prefWName
            = GuiActivator.getResources().getSettingsString(
                    "impl.gui.PREFERRED_ACCOUNT_WIZARD");
        String preferredWizardName
            = (prefWName != null && prefWName.length() > 0) ? prefWName : null;

        Collection<ServiceReference<AccountRegistrationWizard>> accountWizardRefs;
        try
        {
            accountWizardRefs
                = GuiActivator.bundleContext.getServiceReferences(
                        AccountRegistrationWizard.class,
                        null);
        }
        catch (InvalidSyntaxException ex)
        {
            // this shouldn't happen since we're providing no parameter string
            // but let's log just in case.
            logger.error("Error while retrieving service refs", ex);
            return;
        }

        // in case we found any, add them in this container.
        if ((accountWizardRefs != null) && !accountWizardRefs.isEmpty())
        {
            if (logger.isDebugEnabled())
            {
                logger.debug(
                        "Found " + accountWizardRefs.size()
                            + " already installed providers.");
            }

            // Create a list to sort the wizards
            ArrayList<AccountRegistrationWizard> networksList
                = new ArrayList<AccountRegistrationWizard>(
                        accountWizardRefs.size());

            AccountRegistrationWizard prefWiz = null;

            for (ServiceReference<AccountRegistrationWizard> serRef
                    : accountWizardRefs)
            {
                AccountRegistrationWizard wizard
                    = GuiActivator.bundleContext.getService(serRef);

                networksList.add(wizard);

                // is it the preferred protocol ?
                if(preferredWizardName != null
                        && wizard.getClass().getName().equals(
                                preferredWizardName))
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
     * This method gets called when a property is changed.
     *
     * @param evt A PropertyChangeEvent object describing the event source
     *            and the property that has changed.
     */
    public void propertyChange(PropertyChangeEvent evt)
    {
        if(evt.getPropertyName().equals(
                WizardModel.CANCEL_BUTTON_ENABLED_PROPERTY))
        {
            if(evt.getNewValue() instanceof Boolean)
                cancelButton.setEnabled(
                        (Boolean)evt.getNewValue());

        }
        else if(evt.getPropertyName().equals(
                WizardModel.NEXT_FINISH_BUTTON_ENABLED_PROPERTY))
        {
            if(evt.getNewValue() instanceof Boolean)
                addAccountButton.setEnabled(
                        (Boolean)evt.getNewValue());

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
            setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
            setOpaque(true);
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

            setText(wizard.getProtocolName());

            byte[] icon = wizard.getIcon();

            setIcon(
                    ((icon != null) && (icon.length > 0))
                        ? new ImageIcon(icon)
                        : null);

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
        Dimension fixedWidthSize = new Dimension(430, 3);

        fixedWidthPanel.setPreferredSize(fixedWidthSize);
        fixedWidthPanel.setMinimumSize(fixedWidthSize);
        fixedWidthPanel.setMaximumSize(fixedWidthSize);
        this.accountPanel.add(fixedWidthPanel, BorderLayout.SOUTH);

        JComponent simpleWizardForm = (JComponent) wizard.getSimpleForm(false);
        simpleWizardForm.setOpaque(false);

        accountPanel.add(simpleWizardForm);

        //enable the add and advanced buttons if this is a real protocol
        boolean isEmptyAccountRegistrationWizard
            = (wizard instanceof EmptyAccountRegistrationWizard);

        addAccountButton.setEnabled(!isEmptyAccountRegistrationWizard);
        advancedButton.setEnabled(!isEmptyAccountRegistrationWizard);
        if (!isEmptyAccountRegistrationWizard)
            getRootPane().setDefaultButton(addAccountButton);

        if(!wizard.isAdvancedConfigurationEnabled())
            advancedButton.setVisible(false);
        else if(!advancedButton.isVisible())
            advancedButton.setVisible(true);

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
        if (errorMessagePane == null)
        {
            errorMessagePane = new JTextArea();
            errorMessagePane.setLineWrap(true);
            errorMessagePane.setWrapStyleWord(true);
            errorMessagePane.setOpaque(false);
            errorMessagePane.setForeground(Color.RED);

            accountPanel.add(errorMessagePane, BorderLayout.NORTH);

            if (isVisible())
                pack();

            //WORKAROUND: there's something wrong happening in this pack and
            //components get cluttered, partially hiding the password text
            // field. I am under the impression that this has something to do
            // with the message pane preferred size being ignored (or being 0)
            // which is why I am adding it's height to the dialog. It's quite
            // ugly so please fix if you have something better in mind.
            this.setSize(getWidth(), getHeight()+errorMessagePane.getHeight());
        }

        errorMessagePane.setText(errorMessage);

        accountPanel.revalidate();
        accountPanel.repaint();
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
            startConnecting(wizardContainer);

            new Thread(new ProtocolSignInThread(wizard)).start();
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
        newAccountDialog.requestFocus();
    }

    /**
     * Remove the newAccountDialog, when the window is closed.
     * @param isEscaped indicates if the dialog has been escaped
     */
    @Override
    protected void close(boolean isEscaped)
    {
        if(isCurrentlySigningIn)
            return;

        dispose();
    }

    /**
     * Remove the newAccountDialog on dispose.
     */
    @Override
    public void dispose()
    {
        if(isCurrentlySigningIn)
            return;

        if (newAccountDialog == this)
            newAccountDialog = null;

        wizardContainer.getModel().removePropertyChangeListener(this);

        super.dispose();
    }

    /**
     * Overrides set visible to disable closing dialog if currently signing-in.
     *
     * @param visible <tt>true</tt> to make this <tt>NewAccountDialog</tt>
     * visible; otherwise, <tt>false</tt>
     */
    @Override
    public void setVisible(boolean visible)
    {
        if(isCurrentlySigningIn)
            return;

        super.setVisible(visible);
    }

    private void startConnecting(AccountRegWizardContainerImpl wizardContainer)
    {
        isCurrentlySigningIn = true;

        advancedButton.setEnabled(false);
        addAccountButton.setEnabled(false);
        cancelButton.setEnabled(false);

        statusLabel.setText(GuiActivator.getResources().getI18NString(
                "service.gui.CONNECTING"));

        setCursor(new Cursor(Cursor.WAIT_CURSOR));
    }

    private void stopConnecting(AccountRegWizardContainerImpl wizardContainer)
    {
        isCurrentlySigningIn = false;

        statusLabel.setText("");

        setCursor(new Cursor(Cursor.DEFAULT_CURSOR));

        advancedButton.setEnabled(true);
        addAccountButton.setEnabled(true);
        cancelButton.setEnabled(true);
    }

    /**
     * Makes protocol operations in different thread.
     */
    private class ProtocolSignInThread
        implements Runnable
    {
        /**
         * The wizard to use.
         */
        private final AccountRegistrationWizard wizard;

        /**
         * Creates <tt>ProtocolSignInThread</tt>.
         * @param wizard the wizard to use.
         */
        ProtocolSignInThread(AccountRegistrationWizard wizard)
        {
            this.wizard = wizard;
        }

        /**
         * When an object implementing interface <code>Runnable</code> is used
         * to create a thread, starting the thread causes the object's
         * <code>run</code> method to be called in that separately executing
         * thread.
         * <p/>
         * The general contract of the method <code>run</code> is that it may
         * take any action whatsoever.
         *
         * @see Thread#run()
         */
        public void run()
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
                {
                    wizardContainer.saveAccountWizard(protocolProvider, wizard);

                    SwingUtilities.invokeLater(new Runnable()
                    {
                        public void run()
                        {
                            stopConnecting(wizardContainer);

                            NewAccountDialog.this.dispose();
                        }
                    });
                }
                else
                {
                    // no provider, maybe an error, stop connecting
                    // so we can proceed with the actions
                    stopConnecting(wizardContainer);
                }
            }
            catch (OperationFailedException e)
            {
                // make sure buttons don't stay disabled
                SwingUtilities.invokeLater(new Runnable()
                {
                    public void run()
                    {
                        stopConnecting(wizardContainer);
                    }
                });

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
                else if (e.getErrorCode()
                        == OperationFailedException.SERVER_NOT_SPECIFIED)
                {
                    loadErrorMessage(GuiActivator.getResources().getI18NString(
                            "service.gui.SPECIFY_SERVER"));
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
                logger.error("Error creating account: "+e.getMessage(), e);
                // make sure buttons don't stay disabled
                SwingUtilities.invokeLater(new Runnable()
                {
                    public void run()
                    {
                        stopConnecting(wizardContainer);
                    }
                });

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
    }

    /**
     * Sets the selected wizard.
     *
     * @param wizard the wizard to select
     * @param isCreatedForm indicates if the selected wizard should be opened
     * in create account mode
     */
    public void setSelectedWizard(  AccountRegistrationWizard wizard,
                                    boolean isCreateAccount)
    {
        networkComboBox.setSelectedItem(wizard);
    }
}
