/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.account;

import java.awt.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.gui.*;

/**
 * The <tt>NoAccountFoundPage</tt> is the page shown in the account
 * registration wizard shown in the beginning of the program, when no registered
 * accounts are found.
 * 
 * @author Yana Stamcheva
 */
public class NoAccountFoundPage
    extends JPanel
    implements WizardPage
{
    private static String NO_ACCOUNT_FOUND_PAGE = "NoAccountFoundPage";

    private JTextArea messageArea =
        new JTextArea(Messages.getI18NString("noAccountFound").getText());

    /**
     * Creates an instance of <tt>NoAccountFoundPage</tt>.
     */
    public NoAccountFoundPage()
    {
        super(new BorderLayout());

        this.setPreferredSize(new Dimension(300, 380));

        this.messageArea.setLineWrap(true);
        this.messageArea.setWrapStyleWord(true);
        this.messageArea.setEditable(false);
        this.messageArea.setFont(Constants.FONT.deriveFont(Font.BOLD, 14f));
        this.messageArea.setOpaque(false);

        this.messageArea.setBorder(BorderFactory.createEmptyBorder(
            25, 10, 10, 10));

        this.add(messageArea, BorderLayout.CENTER);
    }

    /**
     * Implements the <tt>WizardPage.getIdentifier</tt> method. Returns the
     * identifier of this page.
     */
    public Object getIdentifier()
    {
        return NO_ACCOUNT_FOUND_PAGE;
    }

    /**
     * Implements the <tt>WizardPage.getNextPageIdentifier</tt> method.
     * Returns the identifier of the default wizard page.
     */
    public Object getNextPageIdentifier()
    {
        return WizardPage.DEFAULT_PAGE_IDENTIFIER;
    }

    /**
     * Implements the <tt>WizardPage.getBackPageIdentifier</tt> method.
     * Returns null to identify that this is the first wizard page and this way
     * to disable the "Back" wizard button.
     */
    public Object getBackPageIdentifier()
    {
        return null;
    }

    /**
     * Implements the <tt>WizardPage.getWizardForm</tt> method. Returns this
     * panel.
     */
    public Object getWizardForm()
    {
        return this;
    }

    public void pageHiding()
    {
    }

    public void pageShown()
    {
    }

    public void pageShowing()
    {
    }

    public void pageNext()
    {
    }

    public void pageBack()
    {
    }
}
