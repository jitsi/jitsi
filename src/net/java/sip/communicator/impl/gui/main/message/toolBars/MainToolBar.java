/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.message.toolBars;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.message.*;
import net.java.sip.communicator.impl.gui.main.message.history.*;
import net.java.sip.communicator.impl.gui.utils.*;

/**
 * The <tt>MainToolBar</tt> is a <tt>JToolBar</tt> which contains buttons
 * for file operations, like save and print, for copy-paste operations, etc.
 * It's the main toolbar in the <tt>ChatWindow</tt>. It contains only
 * <tt>ChatToolbarButton</tt>s, which have a specific background icon and
 * rollover behaviour to differentiates them from normal buttons.
 * 
 * @author Yana Stamcheva
 */
public class MainToolBar
    extends SIPCommToolBar
    implements ActionListener
{

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

    private ChatToolbarButton previousButton = new ChatToolbarButton(
        ImageLoader.getImage(ImageLoader.PREVIOUS_ICON));

    private ChatToolbarButton nextButton = new ChatToolbarButton(ImageLoader
        .getImage(ImageLoader.NEXT_ICON));

    private ChatToolbarButton historyButton = new ChatToolbarButton(ImageLoader
        .getImage(ImageLoader.HISTORY_ICON));

    private ChatToolbarButton sendFileButton = new ChatToolbarButton(
        ImageLoader.getImage(ImageLoader.SEND_FILE_ICON));

    private ChatToolbarButton fontButton = new ChatToolbarButton(ImageLoader
        .getImage(ImageLoader.FONT_ICON));

    SmiliesSelectorBox smiliesBox;
    
    private ChatWindow messageWindow;

    /**
     * Creates an instance and constructs the <tt>MainToolBar</tt>.
     * 
     * @param messageWindow The parent <tt>ChatWindow</tt>.
     */
    public MainToolBar(ChatWindow messageWindow) {

        this.messageWindow = messageWindow;

        this.smiliesBox = new SmiliesSelectorBox(
            ImageLoader.getDefaultSmiliesPack(), messageWindow);
        
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
        this.saveButton.setToolTipText(Messages.getString("save") + " Ctrl-S");

        this.printButton.setName("print");
        this.printButton.setToolTipText(Messages.getString("print"));

        this.cutButton.setName("cut");
        this.cutButton.setToolTipText(Messages.getString("cut") + " Ctrl-X");

        this.copyButton.setName("copy");
        this.copyButton.setToolTipText(Messages.getString("copy") + " Ctrl-C");

        this.pasteButton.setName("paste");
        this.pasteButton
            .setToolTipText(Messages.getString("paste") + " Ctrl-P");

        this.smileyButton.setName("smiley");
        this.smileyButton.setToolTipText(Messages.getString("insertSmiley")
            + " Ctrl-M");

        this.previousButton.setName("previous");
        this.previousButton.setToolTipText(Messages.getString("previous"));

        this.nextButton.setName("next");
        this.nextButton.setToolTipText(Messages.getString("next"));

        this.sendFileButton.setName("sendFile");
        this.sendFileButton.setToolTipText(Messages.getString("sendFile"));

        this.historyButton.setName("history");
        this.historyButton.setToolTipText(Messages.getString("history")
            + " Ctrl-H");

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
        this.fontButton.setEnabled(false);
    }

    /**
     * Handles the <tt>ActionEvent</tt>, when one of the toolbar buttons is
     * clicked.
     */
    public void actionPerformed(ActionEvent e)
    {

        AbstractButton button = (AbstractButton) e.getSource();
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
                
                smiliesBox.setInvoker((Component) e.getSource());

                smiliesBox.setLocation(smiliesBox.getPopupLocation());

                smiliesBox.setVisible(true);                
            }
        }
        else if (buttonText.equalsIgnoreCase("previous")) {

        }
        else if (buttonText.equalsIgnoreCase("next")) {

        }
        else if (buttonText.equalsIgnoreCase("sendFile")) {

        }
        else if (buttonText.equalsIgnoreCase("history")) {

            HistoryWindow history = new HistoryWindow(messageWindow
                .getMainFrame(), messageWindow.getCurrentChatPanel()
                .getMetaContact());

            history.setVisible(true);

        }
        else if (buttonText.equalsIgnoreCase("font")) {

        }
    }

    /**
     * Returns the button used to show the list of smilies.
     * 
     * @return the button used to show the list of smilies.
     */
    public AbstractButton getSmileyButton()
    {
        return smileyButton;
    }

    /**
     * Returns the button used to show the history window.
     * 
     * @return the button used to show the history window.
     */
    public ChatToolbarButton getHistoryButton()
    {
        return historyButton;
    }
    
    /**
     * Returns TRUE if there are selected menus in this toolbar, otherwise
     * returns FALSE.
     * @return TRUE if there are selected menus in this toolbar, otherwise
     * returns FALSE
     */
    public boolean hasSelectedMenus()
    {        
        if(smiliesBox.isVisible())
            return true;
        
        return false;
    }
    
    public void closeAllMenus()
    {
        smiliesBox.setVisible(false);
    }
}
