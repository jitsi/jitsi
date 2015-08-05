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
package net.java.sip.communicator.plugin.ippiaccregwizz;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.*;

import javax.swing.*;
import javax.swing.text.*;

import net.java.sip.communicator.plugin.sipaccregwizz.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.util.*;

import org.json.simple.*;

/**
 * @author Yana Stamcheva
 * @author Damian Minkov
 */
public class CreateIppiAccountForm
    extends TransparentPanel
    implements SIPAccountCreationFormService
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The logger.
     */
    private static final Logger logger
        = Logger.getLogger(CreateIppiAccountForm.class);

    /**
     * The user name text field.
     */
    private final JTextField usernameField = new TrimTextField();

    /**
     * The password field.
     */
    private final JPasswordField passField = new JPasswordField();

    /**
     * The retype password field.
     */
    private final JPasswordField retypePassField = new JPasswordField();

    /**
     * The email field.
     */
    private final JTextField emailField = new JTextField();

    /**
     * The error text pane.
     */
    private final JTextPane errorPane = new JTextPane();

    /**
     * The register link.
     */
    private final static String registerLink
        = IppiAccRegWizzActivator.getResources().getSettingsString(
            "plugin.ippiaccregwizz.REGISTER_LINK");

    /**
     * Creates an instance of <tt>RegisterIppiAccountForm</tt>.
     */
    public CreateIppiAccountForm()
    {
        super(new BorderLayout());

        this.init();
    }

    /**
     * Initializes this panel.
     */
    private void init()
    {
        JPanel mainPanel = new TransparentPanel(new BorderLayout());

        mainPanel.setBorder(BorderFactory.createTitledBorder(
            IppiAccRegWizzActivator.getResources()
                .getI18NString("plugin.sipaccregwizz.CREATE_ACCOUNT_TITLE")));

        JPanel labelsPanel = new TransparentPanel(new GridLayout(0, 1, 0, 4));

        JPanel valuesPanel = new TransparentPanel(new GridLayout(0, 1, 0, 4));

        JLabel usernameLabel
            = new JLabel(IppiAccRegWizzActivator.getResources()
                    .getI18NString("plugin.sip2sipaccregwizz.USERNAME"));

        JLabel passLabel
            = new JLabel(IppiAccRegWizzActivator.getResources()
                .getI18NString("service.gui.PASSWORD"));

        JLabel retypePasswordLabel
            = new JLabel(IppiAccRegWizzActivator.getResources()
            .getI18NString("plugin.sip2sipaccregwizz.RETYPE_PASSWORD"));

        JLabel emailLabel
            = new JLabel(IppiAccRegWizzActivator.getResources()
                .getI18NString("plugin.sip2sipaccregwizz.EMAIL"));

        labelsPanel.add(usernameLabel);
        labelsPanel.add(passLabel);
        labelsPanel.add(retypePasswordLabel);
        labelsPanel.add(emailLabel);

        valuesPanel.add(usernameField);
        valuesPanel.add(passField);
        valuesPanel.add(retypePassField);
        valuesPanel.add(emailField);

        JLabel emailDescriptionLabel
            = new JLabel(IppiAccRegWizzActivator.getResources()
                .getI18NString("plugin.sip2sipaccregwizz.EMAIL_NOTE"),
                SwingConstants.CENTER);
        emailDescriptionLabel.setForeground(Color.GRAY);
        emailDescriptionLabel.setFont(emailDescriptionLabel.getFont().deriveFont(8));
        emailDescriptionLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 8, 10));

        initErrorArea();

        mainPanel.add(labelsPanel, BorderLayout.WEST);
        mainPanel.add(valuesPanel, BorderLayout.CENTER);
        mainPanel.add(emailDescriptionLabel, BorderLayout.SOUTH);

        this.add(mainPanel, BorderLayout.CENTER);

        JLabel infoLabel
            = new JLabel(IppiAccRegWizzActivator.getResources()
                .getI18NString("plugin.ippiaccregwizz.INFO_NOTE"),
                SwingConstants.RIGHT);
        infoLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        infoLabel.setForeground(Color.GRAY);
        infoLabel.setFont(emailDescriptionLabel.getFont().deriveFont(8));
        infoLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        infoLabel.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e)
            {
                IppiAccRegWizzActivator.getBrowserLauncher()
                    .openURL("http://ippi.fr");
            }
        });

        this.add(infoLabel, BorderLayout.SOUTH);
    }

    /**
     * Creates the error area component.
     */
    private void initErrorArea()
    {
        SimpleAttributeSet attribs = new SimpleAttributeSet();
        StyleConstants.setAlignment(attribs, StyleConstants.ALIGN_RIGHT);
        StyleConstants.setFontFamily(attribs, errorPane.getFont().getFamily());
        StyleConstants.setForeground(attribs, Color.RED);
        errorPane.setParagraphAttributes(attribs, true);
        errorPane.setPreferredSize(new Dimension(100, 50));
        errorPane.setMinimumSize(new Dimension(100, 50));
        errorPane.setOpaque(false);
    }

    /**
     * Creates this account on the server.
     * @return the created account
     */
    public NewAccount createAccount()
    {
        // Check if the two passwords match.
        String pass1 = new String( passField.getPassword());
        String pass2 = new String( retypePassField.getPassword());
        if (!pass1.equals(pass2))
        {
            showErrorMessage(
                IppiAccRegWizzActivator.getResources().getI18NString(
                    "plugin.sipaccregwizz.NOT_SAME_PASSWORD"));

            return null;
        }

        NewAccount newAccount = null;
        try
        {
            String registerLinkFilled = MessageFormat.format(registerLink,
                URLEncoder.encode(usernameField.getText(), "UTF-8"),
                URLEncoder.encode(new String(passField.getPassword()), "UTF-8"),
                URLEncoder.encode(emailField.getText(), "UTF-8"));

            URL url = new URL(registerLinkFilled);
            URLConnection conn = url.openConnection();

            // If this is not an http connection we have nothing to do here.
            if (!(conn instanceof HttpURLConnection))
            {
                return null;
            }

            HttpURLConnection httpConn = (HttpURLConnection) conn;

            int responseCode = httpConn.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK)
            {
                // Read all the text returned by the server
                BufferedReader in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
                String str;

                StringBuffer stringBuffer = new StringBuffer();
                while ((str = in.readLine()) != null)
                {
                    stringBuffer.append(str);
                }

                if (logger.isInfoEnabled())
                    logger.info("JSON response to create account request: "
                        + stringBuffer.toString());

                newAccount = parseHttpResponse(stringBuffer.toString());
            }
        }
        catch (MalformedURLException e1)
        {
            if (logger.isInfoEnabled())
                logger.info("Failed to create URL with string: "
                        + registerLink, e1);
        }
        catch (IOException e1)
        {
            if (logger.isInfoEnabled())
                logger.info("Failed to open connection.", e1);
        }
        return newAccount;
    }

    /**
     * Returns the form, which would be used by the user to create a new
     * account.
     * @return the component of the form
     */
    public Component getForm()
    {
        return this;
    }

    /**
     * Clears all the data previously entered in the form.
     */
    public void clear()
    {
        usernameField.setText("");
        passField.setText("");
        retypePassField.setText("");
        emailField.setText("");
        errorPane.setText("");

        remove(errorPane);
    }

    /**
     * Parses the given http response.
     * @param response the http response to parse
     * @return the new account
     */
    private NewAccount parseHttpResponse(String response)
    {
        NewAccount newAccount = null;
        try
        {
            JSONObject jsonObject = (JSONObject)JSONValue
                .parseWithException(response);
            boolean isSuccess = (Boolean)jsonObject.get("success");

            if (isSuccess)
            {
                newAccount = new NewAccount(
                    usernameField.getText() + "@ippi.fr",
                    passField.getPassword(),
                    null,
                    "ippi.fr");
            }
            else
            {
                showErrorMessage((String)jsonObject.get("error_message"));
            }
        }
        catch (Throwable e1)
        {
            if (logger.isInfoEnabled())
                logger.info("Failed Json parsing.", e1);
        }

        return newAccount;
    }

    /**
     * Shows the given error message.
     *
     * @param text the text of the error
     */
    private void showErrorMessage(String text)
    {
        errorPane.setText(text);

        if (errorPane.getParent() == null)
            add(errorPane, BorderLayout.NORTH);

        SwingUtilities.getWindowAncestor(CreateIppiAccountForm.this).pack();
    }
}
