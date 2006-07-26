/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.message.toolBars;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;

import net.java.sip.communicator.impl.gui.customcontrols.ChatToolbarButton;
import net.java.sip.communicator.impl.gui.customcontrols.SIPCommButton;
import net.java.sip.communicator.impl.gui.customcontrols.SIPCommToolBar;
import net.java.sip.communicator.impl.gui.i18n.Messages;
import net.java.sip.communicator.impl.gui.main.history.HistoryWindow;
import net.java.sip.communicator.impl.gui.main.message.ChatWindow;
import net.java.sip.communicator.impl.gui.main.message.SmiliesSelectorBox;
import net.java.sip.communicator.impl.gui.utils.ImageLoader;

/**
 * The <tt>MainToolBar</tt> is a <tt>JToolBar</tt> which contains buttons for
 * file operations, like save and print, for copy-paste operations, etc. It's
 * the main toolbar in the <tt>ChatWindow</tt>. It contains only
 * <tt>ChatToolbarButton</tt>s, which have a specific background icon and
 * rollover behaviour to differentiates them from normal buttons.
 * 
 * @see net.java.sip.communicator.impl.gui.main.customcontrols.ChatToolbarButton
 * 
 * @author Yana Stamcheva
 */
public class MainToolBar extends SIPCommToolBar implements ActionListener {

    private ChatToolbarButton copyButton = new ChatToolbarButton(ImageLoader
            .getImage(ImageLoader.COPY_ICON));

    private ChatToolbarButton cutButton = new ChatToolbarButton(ImageLoader
            .getImage(ImageLoader.CUT_ICON));

    private ChatToolbarButton pasteButton = new ChatToolbarButton(ImageLoader
            .getImage(ImageLoader.PASTE_ICON));

    private ChatToolbarButton smileyButton = new ChatToolbarButton(ImageLoader
            .getImage(ImageLoader.SMILIES_ICON));

    private ChatToolbarButton saveButton = new ChatToolbarButton(ImageLoader
            .getImage(ImageLoader.SAVE_ICON));

    private ChatToolbarButton printButton = new ChatToolbarButton(ImageLoader
            .getImage(ImageLoader.PRINT_ICON));

    private ChatToolbarButton previousButton = new ChatToolbarButton(ImageLoader
            .getImage(ImageLoader.PREVIOUS_ICON));

    private ChatToolbarButton nextButton = new ChatToolbarButton(ImageLoader
            .getImage(ImageLoader.NEXT_ICON));

    private ChatToolbarButton historyButton = new ChatToolbarButton(ImageLoader
            .getImage(ImageLoader.HISTORY_ICON));

    private ChatToolbarButton sendFileButton = new ChatToolbarButton(ImageLoader
            .getImage(ImageLoader.SEND_FILE_ICON));

    private ChatToolbarButton fontButton = new ChatToolbarButton(ImageLoader
            .getImage(ImageLoader.FONT_ICON));

    private ChatWindow messageWindow;

    /**
     * Creates an instance and constructs the <tt>MainToolBar</tt>.
     * 
     * @param messageWindow The parent <tt>ChatWindow</tt>.
     */
    public MainToolBar(ChatWindow messageWindow) {

        this.messageWindow = messageWindow;

        this.setRollover(true);
        this.setLayout(new FlowLayout(FlowLayout.LEFT, 3, 0));
        this.setBorder(BorderFactory.createEmptyBorder(2, 2, 5, 2));

        this.add(saveButton);
        this.add(printButton);

        this.addSeparator();

        this.add(cutButton);
        this.add(copyButton);
        this.add(pasteButton);

        this.addSeparator();

        this.add(smileyButton);

        this.addSeparator();

        this.add(previousButton);
        this.add(nextButton);

        this.addSeparator();

        this.add(sendFileButton);
        this.add(historyButton);

        this.addSeparator();

        this.add(fontButton);

        this.saveButton.setName("save");
        this.saveButton.setToolTipText(Messages.getString("save"));

        this.printButton.setName("print");
        this.printButton.setToolTipText(Messages.getString("print"));

        this.cutButton.setName("cut");
        this.cutButton.setToolTipText(Messages.getString("cut"));

        this.copyButton.setName("copy");
        this.copyButton.setToolTipText(Messages.getString("copy"));

        this.pasteButton.setName("paste");
        this.pasteButton.setToolTipText(Messages.getString("paste"));

        this.smileyButton.setName("smiley");
        this.smileyButton.setToolTipText(Messages.getString("insertSmiley"));

        this.previousButton.setName("previous");
        this.previousButton.setToolTipText(Messages.getString("previous"));

        this.nextButton.setName("next");
        this.nextButton.setToolTipText(Messages.getString("next"));

        this.sendFileButton.setName("sendFile");
        this.sendFileButton.setToolTipText(Messages.getString("sendFile"));

        this.historyButton.setName("history");
        this.historyButton.setToolTipText(Messages.getString("history"));

        this.fontButton.setName("font");
        this.fontButton.setToolTipText(Messages.getString("font"));

        this.saveButton.addActionListener(this);
        this.printButton.addActionListener(this);
        this.cutButton.addActionListener(this);
        this.copyButton.addActionListener(this);
        this.pasteButton.addActionListener(this);
        this.smileyButton.addActionListener(this);
        this.previousButton.addActionListener(this);
        this.nextButton.addActionListener(this);
        this.sendFileButton.addActionListener(this);
        this.historyButton.addActionListener(this);
        this.fontButton.addActionListener(this);

        // Disable all buttons that do nothing.
        this.saveButton.setEnabled(false);
        this.printButton.setEnabled(false);
        this.previousButton.setEnabled(false);
        this.nextButton.setEnabled(false);
        this.sendFileButton.setEnabled(false);
        this.historyButton.setEnabled(false);
        this.fontButton.setEnabled(false);
    }


    /**
     * Handles the <tt>ActionEvent</tt>, when one of the toolbar buttons is
     * clicked.
     */
    public void actionPerformed(ActionEvent e) {

        ChatToolbarButton button = (ChatToolbarButton) e.getSource();
        String buttonText = button.getName();

        if (buttonText.equalsIgnoreCase("save")) {
            // TODO: Implement the save operation in chat MainToolBar.
        }
        else if (buttonText.equalsIgnoreCase("print")) {
            // TODO: Implement the print operation in chat MainToolBar.
        }
        else if (buttonText.equalsIgnoreCase("cut")) {
            
            this.messageWindow.getCurrentChatPanel().cut();
        }
        else if (buttonText.equalsIgnoreCase("copy")) {
            
            this.messageWindow.getCurrentChatPanel().copy();
        } 
        else if (buttonText.equalsIgnoreCase("paste")) {
            
           this.messageWindow.getCurrentChatPanel().paste();
        } 
        else if (buttonText.equalsIgnoreCase("smiley")) {

            if (e.getSource() instanceof SIPCommButton) {

                SmiliesSelectorBox smiliesBox = new SmiliesSelectorBox(
                        ImageLoader.getDefaultSmiliesPack(), messageWindow);

                if (!smiliesBox.isVisible()) {

                    smiliesBox.setInvoker((Component) e.getSource());

                    smiliesBox.setLocation(smiliesBox.getPopupLocation());

                    smiliesBox.setVisible(true);
                }
            }

        } else if (buttonText.equalsIgnoreCase("previous")) {

        } else if (buttonText.equalsIgnoreCase("next")) {

        } else if (buttonText.equalsIgnoreCase("sendFile")) {

        } else if (buttonText.equalsIgnoreCase("history")) {

            HistoryWindow history = new HistoryWindow();

            history.setContacts(messageWindow.getCurrentChatPanel()
                    .getChatContacts());
            history.setVisible(true);

        } else if (buttonText.equalsIgnoreCase("font")) {

        }
    }

    /**
     * Returns the button used to show the list of smilies.
     * @return the button used to show the list of smilies.
     */
    public ChatToolbarButton getSmileyButton() {
        return smileyButton;
    }
}
