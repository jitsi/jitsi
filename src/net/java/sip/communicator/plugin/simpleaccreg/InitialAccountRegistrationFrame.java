/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.simpleaccreg;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

import javax.imageio.*;
import javax.swing.*;

import net.java.sip.communicator.service.gui.*;
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
    extends JFrame
    implements ServiceListener
{
    private Logger logger
        = Logger.getLogger(InitialAccountRegistrationFrame.class);

    private JTextArea messageArea =
        new JTextArea(Resources.getString("initialAccountRegistration"));

    private MainPanel mainPanel = new MainPanel(new BorderLayout(10, 10));

    private JPanel mainAccountsPanel = new JPanel(new BorderLayout(10, 10));

    private JPanel accountsPanel = new JPanel(new GridLayout(0, 2, 10, 10));

    private JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

    private JButton signinButton
        = new JButton(Resources.getString("signin"));

    private JButton cancelButton
        = new JButton(Resources.getString("cancel"));

    private Vector registrationForms = new Vector();

    /**
     * Creates an instance of <tt>NoAccountFoundPage</tt>.
     */
    public InitialAccountRegistrationFrame()
    {
        this.setTitle(Resources.getString("signin"));

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

        SigninActionListener actionListener = new SigninActionListener();

        this.signinButton.addActionListener(actionListener);
        this.cancelButton.addActionListener(actionListener);

        this.buttonPanel.add(cancelButton);
        this.buttonPanel.add(signinButton);

        this.messageArea.setLineWrap(true);
        this.messageArea.setWrapStyleWord(true);
        this.messageArea.setEditable(false);
        this.messageArea.setOpaque(false);

        this.getRootPane().setDefaultButton(signinButton);

        this.initAccountWizards();
    }

    private void initAccountWizards()
    {
        SimpleAccountRegistrationActivator.bundleContext.addServiceListener(this);

        ServiceReference[] serviceRefs = null;
        try
        {
            serviceRefs = SimpleAccountRegistrationActivator.bundleContext
                .getServiceReferences(
                    AccountRegistrationWizard.class.getName(), null);

            if (serviceRefs == null || serviceRefs.length <= 0)
                return;

            AccountRegistrationWizard wizard;
            for (int i = 0; i < serviceRefs.length; i++)
            {
                wizard = (AccountRegistrationWizard)
                    SimpleAccountRegistrationActivator
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

        private JLabel signupLabel
            = new JLabel("<html><a href=''>"
                + Resources.getString("signup")
                + "</a></html>", JLabel.RIGHT);

        private AccountRegistrationWizard wizard;

        public AccountRegistrationPanel(
            AccountRegistrationWizard accountWizard,
            boolean isPreferredWizard)
        {
            super(new BorderLayout(5, 5));

            this.wizard = accountWizard;

            this.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            this.setPreferredSize(new Dimension(230, 150));

            this.setOpaque(false);

            this.inputPanel.setOpaque(false);

            this.labelsPanel.setOpaque(false);

            this.fieldsPanel.setOpaque(false);

            this.emptyPanel.setOpaque(false);

            this.add(inputRegisterPanel, BorderLayout.CENTER);

            this.inputRegisterPanel.add(inputPanel, BorderLayout.NORTH);

            if (wizard.isWebSignupSupported())
                this.inputRegisterPanel.add(signupLabel, BorderLayout.SOUTH);

            this.inputPanel.add(labelsPanel, BorderLayout.WEST);

            this.inputPanel.add(fieldsPanel, BorderLayout.CENTER);

            this.iconDescriptionPanel.add(
                protocolLabel, BorderLayout.NORTH);

            this.signupLabel.setFont(signupLabel.getFont().deriveFont(10f));
            this.signupLabel.addMouseListener(new MouseAdapter()
                {
                    public void mousePressed(MouseEvent arg0)
                    {
                        try
                        {
                            wizard.webSignup();
                        }
                        catch (UnsupportedOperationException e)
                        {
                            // This should not happen, because we check if the
                            // operation is supported, before adding the sign up.
                            logger.error("The web sign up is not supported.", e);
                        }
                    }
                });

            this.protocolLabel.setFont(
                protocolLabel.getFont().deriveFont(Font.BOLD, 14f));
            this.usernameExampleLabel.setForeground(Color.DARK_GRAY);
            this.usernameExampleLabel.setFont(
                usernameExampleLabel.getFont().deriveFont(8f));

            this.labelsPanel.add(usernameLabel);
            this.labelsPanel.add(emptyPanel);
            this.labelsPanel.add(passwordLabel);

            this.fieldsPanel.add(usernameField);
            this.fieldsPanel.add(usernameExampleLabel);
            this.fieldsPanel.add(passwordField);

            this.usernameExampleLabel.setText(wizard.getUserNameExample());

            this.protocolLabel.setText(wizard.getProtocolName());

            Image image = null;
            try
            {
                image = ImageIO.read(
                    new ByteArrayInputStream(wizard.getPageImage()));
            }
            catch (IOException e)
            {
                logger.error("Unable to load image.", e);
            }

            if (image != null)
            {
                image = image.getScaledInstance(32, 32, Image.SCALE_SMOOTH);

                protocolLabel.setIcon(new ImageIcon(image));
            }

            if (isPreferredWizard)
            {
                descriptionArea.setBorder(BorderFactory
                    .createEmptyBorder(10, 0, 0, 0));

                descriptionArea.setFont(
                    descriptionArea.getFont().deriveFont(10f));
                descriptionArea.setPreferredSize(new Dimension(200, 50));
                descriptionArea.setLineWrap(true);
                descriptionArea.setWrapStyleWord(true);
                descriptionArea.setText(wizard.getProtocolDescription());
//                descriptionArea.setBorder(SIPCommBorders.getRoundBorder());

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
            Graphics2D g2d = (Graphics2D) g;

            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            // do the superclass behavior first
            super.paintComponent(g2d);

            g2d.setColor(new Color(
                Resources.getColor("desktopBackgroundColor")));

            // paint the background with the chosen color
            g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
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
        Object sService = SimpleAccountRegistrationActivator.bundleContext.
            getService(event.getServiceReference());

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
            = Resources.getLoginProperty("preferredAccountWizard");

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
        public void actionPerformed(ActionEvent evt)
        {
            JButton button = (JButton) evt.getSource();

            if (button.equals(signinButton))
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
            else
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
            Graphics2D g2d = (Graphics2D) g;

            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            // do the superclass behavior first
            super.paintComponent(g2d);

            g2d.setColor(new Color(
                Resources.getColor("accountRegistrationBackground")));

            // paint the background with the chosen color
            g2d.fillRoundRect(10, 10, getWidth() - 20, getHeight() - 20, 15, 15);
        }
    }
}
