/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.sip2sipaccregwizz;

import java.awt.*;
import java.io.*;
import java.net.*;

import javax.swing.*;
import javax.swing.text.*;

import org.json.*;

import net.java.sip.communicator.plugin.sipaccregwizz.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

/**
 * @author Yana Stamcheva
 */
public class CreateSip2SipAccountForm
    extends TransparentPanel
    implements CreateAccountService
{
    /**
     * The logger.
     */
    private static final Logger logger
        = Logger.getLogger(CreateSip2SipAccountForm.class);

    /**
     * The user name text field.
     */
    private final JTextField usernameField = new JTextField();

    /**
     * The display name text field.
     */
    private final JTextField displayNameField = new JTextField();

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
    private static String registerLink
        = "https://enrollment.sipthor.net/enrollment.phtml?";

    /**
     * Creates an instance of <tt>RegisterSip2SipAccountForm</tt>.
     */
    public CreateSip2SipAccountForm()
    {
        super(new BorderLayout());

        this.setBorder(BorderFactory.createTitledBorder(
            Sip2SipAccRegWizzActivator.getResources()
                .getI18NString("plugin.sipaccregwizz.CREATE_ACCOUNT_TITLE")));

        this.init();
    }

    /**
     * Initializes this panel.
     */
    private void init()
    {
        JPanel labelsPanel = new TransparentPanel(new GridLayout(0, 1));

        JPanel valuesPanel = new TransparentPanel(new GridLayout(0, 1));

        JLabel usernameLabel
            = new JLabel(Sip2SipAccRegWizzActivator.getResources()
                    .getI18NString("plugin.sipaccregwizz.USERNAME"));

        JLabel displayNameLabel
            = new JLabel(Sip2SipAccRegWizzActivator.getResources()
                    .getI18NString("plugin.sipaccregwizz.DISPLAY_NAME"));

        JLabel passLabel
            = new JLabel(Sip2SipAccRegWizzActivator.getResources()
                .getI18NString("service.gui.PASSWORD"));

        JLabel retypePasswordLabel
            = new JLabel(Sip2SipAccRegWizzActivator.getResources()
            .getI18NString("plugin.sip2sipaccregwizz.RETYPE_PASSWORD"));

        JLabel emailLabel
            = new JLabel(Sip2SipAccRegWizzActivator.getResources()
                .getI18NString("plugin.sip2sipaccregwizz.EMAIL"));

        labelsPanel.add(usernameLabel);
        labelsPanel.add(displayNameLabel);
        labelsPanel.add(passLabel);
        labelsPanel.add(retypePasswordLabel);
        labelsPanel.add(emailLabel);

        valuesPanel.add(usernameField);
        valuesPanel.add(displayNameField);
        valuesPanel.add(passField);
        valuesPanel.add(retypePassField);
        valuesPanel.add(emailField);

        initErrorArea();

        add(labelsPanel, BorderLayout.WEST);
        add(valuesPanel, BorderLayout.CENTER);
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
        NewAccount newAccount = null;
        try
        {
            registerLink += "email=" + emailField.getText()
                    + "&password=" + new String(passField.getPassword())
                    + "&display_name=" + displayNameField.getText()
                    + "&username=" + usernameField.getText()
                    + "&user_agent=sip-communicator.org";

            URL url = new URL(registerLink);
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
        displayNameField.setText("");
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
            JSONObject jsonObject = new JSONObject(response);
            boolean isSuccess = jsonObject.getBoolean("success");

            if (isSuccess)
            {
                newAccount = new NewAccount(
                    jsonObject.getString("sip_address"),
                    passField.getPassword(),
                    null,
                    jsonObject.getString("outbound_proxy"));
            }
            else
            {
                String errorMessage
                    = jsonObject.getString("error_message");

                errorPane.setText(errorMessage);
                add(errorPane, BorderLayout.NORTH);

                SwingUtilities.getWindowAncestor(
                    CreateSip2SipAccountForm.this).pack();
            }
        }
        catch (JSONException e1)
        {
            if (logger.isInfoEnabled())
                logger.info("Failed Json parsing.", e1);
        }

        return newAccount;
    }
}
