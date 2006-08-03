/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.account;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import net.java.sip.communicator.impl.gui.i18n.Messages;
import net.java.sip.communicator.impl.gui.utils.AntialiasingManager;
import net.java.sip.communicator.impl.gui.utils.Constants;
import net.java.sip.communicator.impl.gui.utils.ImageLoader;
import net.java.sip.communicator.service.gui.WizardPage;

/**
 * The <tt>NoAccountFoundPage</tt> is the page shown in the account registration
 * wizard shown in the beginning of the program, when no registered accounts are
 * found.
 * @author Yana Stamcheva
 */
public class NoAccountFoundPage extends JPanel
    implements WizardPage {

    private static String NO_ACCOUNT_FOUND_PAGE = "NoAccountFoundPage";
    
    private JTextArea messageArea
        = new JTextArea(Messages.getString("noAccountFound"));
        
    public NoAccountFoundPage() {
        super(new BorderLayout());
        
        this.messageArea.setLineWrap(true);
        this.messageArea.setWrapStyleWord(true);
        this.messageArea.setEditable(false);
        this.messageArea.setFont(Constants.FONT.deriveFont(Font.BOLD, 14f));
                
        this.add(messageArea, BorderLayout.CENTER);
        
        this.setBorder(BorderFactory.createEmptyBorder(55, 10, 10, 10));
    }
    
    public Object getIdentifier() {
        return NO_ACCOUNT_FOUND_PAGE;
    }

    public Object getNextPageIdentifier() {
        return WizardPage.DEFAULT_PAGE_IDENTIFIER;
    }

    public Object getBackPageIdentifier() {
        return null;
    }

    public Object getWizardForm() {
        return this;
    }

    public void pageHiding() {
    }

    public void pageShown() {
    }

    public void pageShowing() {
    }

    public void pageNext() {
    }

    public void pageBack() {
    }
    
    protected void paintComponent(Graphics g) {

        super.paintComponent(g);

        AntialiasingManager.activateAntialiasing(g);

        Graphics2D g2 = (Graphics2D) g;
       
        g2.drawImage(ImageLoader.getImage(
                ImageLoader.AUTH_WINDOW_BACKGROUND),
                0, 0, this.getWidth(), this.getHeight(), null);
    }
}
