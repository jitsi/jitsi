/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.account;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.lookandfeel.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.impl.gui.utils.Constants;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * The <tt>NoAccountFoundPage</tt> is the page shown in the account
 * registration wizard shown in the beginning of the program, when no registered
 * accounts are found.
 * 
 * @author Yana Stamcheva
 */
public class InitialAccountRegistrationFrame
    extends SIPCommFrame
    implements ServiceListener
{
    private Logger logger
        = Logger.getLogger(InitialAccountRegistrationFrame.class);

    private JTextArea messageArea =
        new JTextArea(Messages.getI18NString("initialAccountRegistration")
            .getText());

    private MainPanel mainPanel = new MainPanel(new BorderLayout(10, 10));

    private JPanel mainAccountsPanel = new JPanel(new BorderLayout(10, 10));

    private JPanel accountsPanel = new JPanel(new GridLayout(0, 2, 10, 10));

    private JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

    private JButton signinButton
        = new JButton(Messages.getI18NString("signin").getText());

    private Vector registrationForms = new Vector();

    /**
     * Creates an instance of <tt>NoAccountFoundPage</tt>.
     */
    public InitialAccountRegistrationFrame()
    {
        this.setTitle(Messages.getI18NString("signin").getText());

        this.mainPanel.setBorder(
            BorderFactory.createEmptyBorder(20, 20, 20, 20));

        this.getContentPane().add(mainPanel);

        this.mainPanel.add(messageArea, BorderLayout.NORTH);
        this.mainPanel.add(mainAccountsPanel, BorderLayout.CENTER);
        this.mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        this.mainAccountsPanel.add(accountsPanel, BorderLayout.CENTER);

        this.mainAccountsPanel.setOpaque(false);
        this.accountsPanel.setOpaque(false);
        this.buttonPanel.setOpaque(false);

        this.signinButton.addActionListener(new SigninActionListener());
        this.buttonPanel.add(signinButton);

        this.messageArea.setLineWrap(true);
        this.messageArea.setWrapStyleWord(true);
        this.messageArea.setEditable(false);
        this.messageArea.setOpaque(false);

        this.initAccountWizards();
    }

    private void initAccountWizards()
    {
        GuiActivator.bundleContext.addServiceListener(this);

        ServiceReference[] serviceRefs = null;
        try
        {
            serviceRefs = GuiActivator.bundleContext.getServiceReferences(
                AccountRegistrationWizard.class.getName(), null);

            if (serviceRefs == null || serviceRefs.length <= 0)
                return;

            AccountRegistrationWizard wizard;
            for (int i = 0; i < serviceRefs.length; i++)
            {
                wizard = (AccountRegistrationWizard) GuiActivator
                    .bundleContext.getService(serviceRefs[i]);

                this.addAccountRegistrationForm(wizard);
            }
        }
        catch (InvalidSyntaxException ex)
        {
            logger.error("GuiActivator : ", ex);
        }
    }

    /**
     * 
     */
    private class AccountRegistrationPanel extends JPanel
    {
        private JLabel protocolLabel = new JLabel();

        private JLabel usernameLabel = new JLabel("Login");

        private JLabel passwordLabel = new JLabel("Password");

        private JTextField usernameField = new JTextField();

        private JLabel usernameExampleLabel = new JLabel();

        private JPasswordField passwordField = new JPasswordField();

        private JPanel labelsPanel = new JPanel(new GridLayout(0, 1, 5, 0));

        private JPanel fieldsPanel = new JPanel(new GridLayout(0, 1, 5, 0));

        private JPanel emptyPanel = new JPanel();

        private JPanel inputPanel = new JPanel(new BorderLayout(5, 5));

        private JPanel iconDescriptionPanel = new JPanel(new BorderLayout());

        private JPanel inputRegisterPanel = new JPanel(new BorderLayout());

        private JTextArea descriptionArea = new JTextArea(); 

        private AccountRegistrationWizard wizard;

        public AccountRegistrationPanel(
            AccountRegistrationWizard wizard,
            boolean isPreferredWizard)
        {
            super(new BorderLayout(5, 5));

            this.wizard = wizard;

            this.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            this.setPreferredSize(new Dimension(230, 140));

            this.setOpaque(false);

            this.inputPanel.setOpaque(false);

            this.labelsPanel.setOpaque(false);

            this.fieldsPanel.setOpaque(false);

            this.emptyPanel.setOpaque(false);

            this.add(inputRegisterPanel, BorderLayout.CENTER);

            this.inputRegisterPanel.add(inputPanel, BorderLayout.NORTH);

            this.inputPanel.add(labelsPanel, BorderLayout.WEST);

            this.inputPanel.add(fieldsPanel, BorderLayout.CENTER);

            this.iconDescriptionPanel.add(
                protocolLabel, BorderLayout.NORTH);

            this.protocolLabel.setFont(
                Constants.FONT.deriveFont(Font.BOLD, 14f));
            this.usernameExampleLabel.setForeground(Color.DARK_GRAY);
            this.usernameExampleLabel.setFont(
                Constants.FONT.deriveFont(8f));

            this.labelsPanel.add(usernameLabel);
            this.labelsPanel.add(emptyPanel);
            this.labelsPanel.add(passwordLabel);

            this.fieldsPanel.add(usernameField);
            this.fieldsPanel.add(usernameExampleLabel);
            this.fieldsPanel.add(passwordField);

            this.usernameExampleLabel.setText(wizard.getUserNameExample());

            this.protocolLabel.setText(wizard.getProtocolName());

            Image image = ImageLoader.getBytesInImage(wizard.getPageImage());

            if (image != null)
            {
                image = image.getScaledInstance(32, 32, Image.SCALE_SMOOTH);

                protocolLabel.setIcon(new ImageIcon(image));
            }

            if (isPreferredWizard)
            {
                descriptionArea.setBorder(BorderFactory
                    .createEmptyBorder(10, 0, 0, 0));

                descriptionArea.setFont(Constants.FONT.deriveFont(10f));
                descriptionArea.setPreferredSize(new Dimension(200, 50));
                descriptionArea.setLineWrap(true);
                descriptionArea.setWrapStyleWord(true);
                descriptionArea.setText(wizard.getProtocolDescription());
                descriptionArea.setBorder(SIPCommBorders.getRoundBorder());

                this.iconDescriptionPanel.add(
                    descriptionArea, BorderLayout.CENTER);

                this.add(iconDescriptionPanel, BorderLayout.WEST);
            }
            else
            {
                this.add(iconDescriptionPanel, BorderLayout.NORTH);
            }
        }

        public void paintComponent(Graphics g)
        {
            AntialiasingManager.activateAntialiasing(g);

            // do the superclass behavior first
            super.paintComponent(g);

            g.setColor(new Color(
                ColorProperties.getColor("desktopBackgroundColor")));

            // paint the background with the chosen color
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
        }

        public boolean isFilled()
        {
            if(usernameField.getText() != null
                && usernameField.getText().length() > 0)
                return true;

            return false;
        }

        public void signin()
        {
            wizard.signin(  usernameField.getText(),
                            new String(passwordField.getPassword()));
        }
    }

    /**
     * 
     */
    protected void close(boolean isEscaped)
    {
    }

    /**
     * Handles registration of a new account wizard.
     */
    public void serviceChanged(ServiceEvent event)
    {
        Object sService = GuiActivator.bundleContext.getService(
            event.getServiceReference());

        // we don't care if the source service is not a plugin component
        if (! (sService instanceof AccountRegistrationWizard))
            return;

        AccountRegistrationWizard wizard
            = (AccountRegistrationWizard) sService;

        if (event.getType() == ServiceEvent.REGISTERED)
        {
                this.addAccountRegistrationForm(wizard);
        }
    }

    /**
     * Adds a simple account registration form corresponding to the given
     * <tt>AccountRegistrationWizard</tt>.
     * 
     * @param wizard the <tt>AccountRegistrationWizard</tt>, which gives us
     * information to fill our simple form.
     */
    private void addAccountRegistrationForm(AccountRegistrationWizard wizard)
    {
     // We don't need to add wizards that are not interested in a
        // simple sign in form.
        if (!wizard.isSimpleFormEnabled())
            return;

        String preferredWizardName
            = LoginProperties.getProperty("preferredAccountWizard");

        AccountRegistrationPanel accountPanel;

        if (preferredWizardName != null
            && preferredWizardName.equals(wizard.getClass().getName()))
        {
            accountPanel = new AccountRegistrationPanel(wizard, true);

            mainAccountsPanel.add(
                accountPanel,
                BorderLayout.NORTH);
        }
        else
        {
            accountPanel = new AccountRegistrationPanel(wizard, false);

            this.accountsPanel.add(accountPanel);
        }

        this.registrationForms.add(accountPanel);

        this.pack();
    }

    /**
     * Handles the event triggered by the "Signin" button.
     */
    private class SigninActionListener implements ActionListener
    {
        public void actionPerformed(ActionEvent arg0)
        {
            Iterator regIterator = registrationForms.iterator();

            if (regIterator.hasNext())
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            while(regIterator.hasNext())
            {
                AccountRegistrationPanel regForm
                    = (AccountRegistrationPanel) regIterator.next();

                if (regForm.isFilled())
                    regForm.signin();
            }

            InitialAccountRegistrationFrame.this.dispose();
        }
    }

    private class MainPanel extends JPanel
    {
        public MainPanel(LayoutManager layoutManager)
        {
            super(layoutManager);
        }

        public void paintComponent(Graphics g)
        {
            AntialiasingManager.activateAntialiasing(g);

            // do the superclass behavior first
            super.paintComponent(g);

            g.setColor(new Color(
                ColorProperties.getColor("accountRegistrationBackground")));

            // paint the background with the chosen color
            g.fillRoundRect(10, 10, getWidth() - 20, getHeight() - 20, 15, 15);
        }
    }
}
