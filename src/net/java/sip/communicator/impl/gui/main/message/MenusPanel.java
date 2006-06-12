/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.message;

import java.awt.GridLayout;

import javax.swing.JPanel;
import javax.swing.JToolBar;

import net.java.sip.communicator.impl.gui.main.message.menu.MessageWindowMenuBar;
import net.java.sip.communicator.impl.gui.main.message.toolBars.EditTextToolBar;
import net.java.sip.communicator.impl.gui.main.message.toolBars.MainToolBar;
/**
 * MenusPanel is the panel, containing all toolbars in the chat window.
 * @author Yana Stamcheva
 */
public class MenusPanel extends JPanel {

    private MessageWindowMenuBar menuBar;

    private EditTextToolBar editTextToolBar = new EditTextToolBar();

    private MainToolBar mainToolBar;

    private ChatWindow parentWindow;

    /**
     * Creates an instance and constructs the MenusPanel.
     * @param parentWindow The parent ChatWindow for this panel.
     */
    public MenusPanel(ChatWindow parentWindow) {

        super();

        this.parentWindow = parentWindow;

        mainToolBar = new MainToolBar(this.parentWindow);
        menuBar = new MessageWindowMenuBar(this.parentWindow);

        this.setLayout(new GridLayout(0, 1));

        this.add(menuBar);
        this.add(mainToolBar);
        // this.add(editTextToolBar);
    }

    /**
     * Adds a new toolbar to this MenusPanel.
     * @param toolBar The toolbar to add.
     */
    public void addToolBar(JToolBar toolBar) {
        this.add(toolBar);
    }

    public MainToolBar getMainToolBar() {
        return mainToolBar;
    }
}
