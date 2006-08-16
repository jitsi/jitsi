/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.message.history;

import java.awt.*;
import javax.swing.*;

import net.java.sip.communicator.impl.gui.i18n.*;
/**
 * The <tt>NavigationPanel</tt> is the panel where user could navigate through
 * the message history.
 * 
 * @author Yana Stamcheva
 */
public class NavigationPanel extends JPanel {

    private JButton nextPageButton = new JButton(Messages.getString("next"));

    private JButton previousPageButton = new JButton(Messages
            .getString("previous"));

    private JButton lastPageButton = new JButton(Messages.getString("last"));

    private JButton firstPageButton = new JButton(Messages.getString("first"));

    /**
     * Constructs the <tt>NavigationPanel</tt> by adding all navigation buttons.
     */
    public NavigationPanel() {
        super(new FlowLayout(FlowLayout.CENTER));

        this.add(firstPageButton);
        this.add(previousPageButton);
        this.add(nextPageButton);
        this.add(lastPageButton);
    }
}
