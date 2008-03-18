/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.chat.toolBars;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.event.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.main.chat.history.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.Container;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * The <tt>MainToolBar</tt> is a <tt>JToolBar</tt> which contains buttons
 * for file operations, like save and print, for copy-paste operations, etc.
 * It's the main toolbar in the <tt>ChatWindow</tt>. It contains only
 * <tt>ChatToolbarButton</tt>s, which have a specific background icon and
 * rollover behaviour to differentiates them from normal buttons.
 * 
 * @author Yana Stamcheva
 */
public class ExtendedMainToolBar
    extends MainToolBar
    implements  MouseListener,
                PluginComponentListener
{
    private Logger logger = Logger.getLogger(ExtendedMainToolBar.class);

    private ToolBarButton copyButton = new ToolBarButton(
        Messages.getI18NString("copy").getText(),
        ImageLoader.getImage(ImageLoader.COPY_ICON));

    private ToolBarButton cutButton = new ToolBarButton(
        Messages.getI18NString("cut").getText(),
        ImageLoader.getImage(ImageLoader.CUT_ICON));

    private ToolBarButton pasteButton = new ToolBarButton(
        Messages.getI18NString("paste").getText(),
        ImageLoader.getImage(ImageLoader.PASTE_ICON));

    private ToolBarButton saveButton = new ToolBarButton(
        Messages.getI18NString("save").getText(),
        ImageLoader.getImage(ImageLoader.SAVE_ICON));

    private ToolBarButton printButton = new ToolBarButton(
        Messages.getI18NString("print").getText(),
        ImageLoader.getImage(ImageLoader.PRINT_ICON));

    private ToolBarButton previousButton = new ToolBarButton(
        Messages.getI18NString("back").getText(),
        ImageLoader.getImage(ImageLoader.PREVIOUS_ICON));

    private ToolBarButton nextButton = new ToolBarButton(
        Messages.getI18NString("next").getText(),
        ImageLoader.getImage(ImageLoader.NEXT_ICON));

    private ToolBarButton historyButton = new ToolBarButton(
        Messages.getI18NString("history").getText(),
        ImageLoader.getImage(ImageLoader.HISTORY_ICON));

    private ToolBarButton sendFileButton = new ToolBarButton(
        Messages.getI18NString("sendFile").getText(),
        ImageLoader.getImage(ImageLoader.SEND_FILE_ICON));

    private ToolBarButton fontButton = new ToolBarButton(
        Messages.getI18NString("font").getText(),
        ImageLoader.getImage(ImageLoader.FONT_ICON));

    private static int DEFAULT_BUTTON_HEIGHT
        = SizeProperties.getSize("mainToolbarButtonHeight");

    private static int DEFAULT_BUTTON_WIDTH
        = SizeProperties.getSize("mainToolbarButtonWidth");

    private SmiliesSelectorBox smiliesBox;

    private ChatWindow messageWindow;

    /**
     * Creates an instance and constructs the <tt>MainToolBar</tt>.
     * 
     * @param messageWindow The parent <tt>ChatWindow</tt>.
     */
    public ExtendedMainToolBar(ChatWindow messageWindow)
    {
        this.messageWindow = messageWindow;

        this.smiliesBox = new SmiliesSelectorBox(
            ImageLoader.getDefaultSmiliesPack(), messageWindow);

        this.smiliesBox.setText(Messages.getI18NString("smiley").getText());
        this.smiliesBox.setRollover(true);

        this.setRollover(true);
        this.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        this.setPreferredSize(new Dimension(300, DEFAULT_BUTTON_HEIGHT));

//        this.setBorder(BorderFactory.createEmptyBorder(2, 2, 5, 2));

//        this.add(saveButton);
//        this.add(printButton);

//        this.addSeparator();

        this.add(cutButton);
        this.add(copyButton);
        this.add(pasteButton);

        this.addSeparator();

        this.add(smiliesBox);

        this.addSeparator();

        this.add(previousButton);
        this.add(nextButton);

        this.addSeparator();

//        this.add(sendFileButton);
        this.add(historyButton);

//        this.addSeparator();
//
//        this.add(fontButton);

        this.saveButton.setName("save");
        this.saveButton.setToolTipText(
            Messages.getI18NString("save").getText() + " Ctrl-S");

        this.printButton.setName("print");
        this.printButton.setToolTipText(
            Messages.getI18NString("print").getText());

        this.cutButton.setName("cut");
        this.cutButton.setToolTipText(
            Messages.getI18NString("cut").getText() + " Ctrl-X");

        this.copyButton.setName("copy");
        this.copyButton.setToolTipText(
            Messages.getI18NString("copy").getText() + " Ctrl-C");

        this.pasteButton.setName("paste");
        this.pasteButton.setToolTipText(
            Messages.getI18NString("paste").getText() + " Ctrl-P");

        this.smiliesBox.setName("smiley");
        this.smiliesBox.setToolTipText(
            Messages.getI18NString("insertSmiley").getText() + " Ctrl-M");

        this.previousButton.setName("previous");
        this.previousButton.setToolTipText(
            Messages.getI18NString("previous").getText());

        this.nextButton.setName("next");
        this.nextButton.setToolTipText(
            Messages.getI18NString("next").getText());

        this.sendFileButton.setName("sendFile");
        this.sendFileButton.setToolTipText(
            Messages.getI18NString("sendFile").getText());

        this.historyButton.setName("history");
        this.historyButton.setToolTipText(
            Messages.getI18NString("history").getText() + " Ctrl-H");

        this.fontButton.setName("font");
        this.fontButton.setToolTipText(
            Messages.getI18NString("font").getText());

        int buttonWidth = calculateButtonWidth();

        this.saveButton.setPreferredSize(
            new Dimension(buttonWidth, DEFAULT_BUTTON_HEIGHT));
        this.printButton.setPreferredSize(
            new Dimension(buttonWidth, DEFAULT_BUTTON_HEIGHT));
        this.cutButton.setPreferredSize(
            new Dimension(buttonWidth, DEFAULT_BUTTON_HEIGHT));
        this.copyButton.setPreferredSize(
            new Dimension(buttonWidth, DEFAULT_BUTTON_HEIGHT));
        this.pasteButton.setPreferredSize(
            new Dimension(buttonWidth, DEFAULT_BUTTON_HEIGHT));
        this.previousButton.setPreferredSize(
            new Dimension(buttonWidth, DEFAULT_BUTTON_HEIGHT));
        this.nextButton.setPreferredSize(
            new Dimension(buttonWidth, DEFAULT_BUTTON_HEIGHT));
        this.sendFileButton.setPreferredSize(
            new Dimension(buttonWidth, DEFAULT_BUTTON_HEIGHT));
        this.historyButton.setPreferredSize(
            new Dimension(buttonWidth, DEFAULT_BUTTON_HEIGHT));
        this.fontButton.setPreferredSize(
            new Dimension(buttonWidth, DEFAULT_BUTTON_HEIGHT));

        this.saveButton.addMouseListener(this);
        this.printButton.addMouseListener(this);
        this.cutButton.addMouseListener(this);
        this.copyButton.addMouseListener(this);
        this.pasteButton.addMouseListener(this);
        this.previousButton.addMouseListener(this);
        this.nextButton.addMouseListener(this);
        this.sendFileButton.addMouseListener(this);
        this.historyButton.addMouseListener(this);
        this.fontButton.addMouseListener(this);

        // Disable all buttons that do nothing.
        this.saveButton.setEnabled(false);
        this.printButton.setEnabled(false);
        this.sendFileButton.setEnabled(false);
        this.fontButton.setEnabled(false);
        
        this.initPluginComponents();
    }

    /**
     * Handles the <tt>ActionEvent</tt>, when one of the toolbar buttons is
     * clicked.
     */
    public void mousePressed(MouseEvent e)
    {
        JLabel button = (JLabel) e.getSource();
        String buttonText = button.getName();

        ChatPanel chatPanel = messageWindow.getCurrentChatPanel();
        
        if (buttonText.equalsIgnoreCase("save")) {
            // TODO: Implement the save operation in chat MainToolBar.
        }
        else if (buttonText.equalsIgnoreCase("print")) {
            // TODO: Implement the print operation in chat MainToolBar.
        }
        else if (buttonText.equalsIgnoreCase("cut")) {

            chatPanel.cut();
        }
        else if (buttonText.equalsIgnoreCase("copy")) {

            chatPanel.copy();
        }
        else if (buttonText.equalsIgnoreCase("paste")) {

            chatPanel.paste();
        }
        else if (buttonText.equalsIgnoreCase("previous"))
        {   
            chatPanel.loadPreviousPageFromHistory();
        }
        else if (buttonText.equalsIgnoreCase("next"))
        {   
            chatPanel.loadNextPageFromHistory();
        }
        else if (buttonText.equalsIgnoreCase("sendFile")) {

        }
        else if (buttonText.equalsIgnoreCase("history"))
        {
            HistoryWindow history;

            HistoryWindowManager historyWindowManager
                = messageWindow.getMainFrame().getHistoryWindowManager();

            Object historyContact = chatPanel.getChatIdentifier();

            if(historyWindowManager
                .containsHistoryWindowForContact(historyContact))
            {
                history = historyWindowManager
                    .getHistoryWindowForContact(historyContact);

                if(history.getState() == JFrame.ICONIFIED)
                    history.setState(JFrame.NORMAL);
                
                history.toFront();
            }
            else
            {
                history = new HistoryWindow(messageWindow
                    .getMainFrame(), chatPanel.getChatIdentifier());

                history.setVisible(true);

                historyWindowManager.addHistoryWindowForContact(historyContact,
                                                                history);
            }
        }
        else if (buttonText.equalsIgnoreCase("font")) {

        }
    }

    /**
     * Returns the button used to show the list of smilies.
     * 
     * @return the button used to show the list of smilies.
     */
    public SmiliesSelectorBox getSmiliesSelectorBox()
    {
        return smiliesBox;
    }

    /**
     * Returns TRUE if there are selected menus in this toolbar, otherwise
     * returns FALSE.
     * @return TRUE if there are selected menus in this toolbar, otherwise
     * returns FALSE
     */
    public boolean hasSelectedMenus()
    {
        if(smiliesBox.isMenuSelected())
            return true;
        
        return false;
    }

    /**
     * Disables/Enables history arrow buttons depending on whether the
     * current page is the first, the last page or a middle page.
     */
    public void changeHistoryButtonsState(ChatPanel chatPanel)
    {
        ChatConversationPanel convPanel = chatPanel.getChatConversationPanel();
        
        Date firstMsgInHistory = chatPanel.getFirstHistoryMsgTimestamp();
        Date lastMsgInHistory = chatPanel.getLastHistoryMsgTimestamp();
        Date firstMsgInPage = convPanel.getPageFirstMsgTimestamp();
        Date lastMsgInPage = convPanel.getPageLastMsgTimestamp();
        
        if(firstMsgInHistory == null || lastMsgInHistory == null)
        {
            previousButton.setEnabled(false);
            nextButton.setEnabled(false);
            return;
        }
        
        if(firstMsgInHistory.compareTo(firstMsgInPage) < 0)
            previousButton.setEnabled(true);
        else
            previousButton.setEnabled(false);
        
        if(lastMsgInPage.getTime() > 0
                && (lastMsgInHistory.compareTo(lastMsgInPage) > 0))
        {
            nextButton.setEnabled(true);
        }
        else
        {
            nextButton.setEnabled(false);
        }
    }
    
    private void initPluginComponents()
    {
        Iterator pluginComponents = GuiActivator.getUIService()
            .getComponentsForContainer(
                Container.CONTAINER_CHAT_TOOL_BAR);

        if(pluginComponents.hasNext())
            this.addSeparator();

        while (pluginComponents.hasNext())
        {
            Component c = (Component)pluginComponents.next();

            this.add(c);

            this.revalidate();
            this.repaint();
        }

        // Search for plugin components registered through the OSGI bundle
        // context.
        ServiceReference[] serRefs = null;

        String osgiFilter = "("
            + Container.CONTAINER_ID
            + "="+Container.CONTAINER_CHAT_TOOL_BAR.getID()+")";

        try
        {
            serRefs = GuiActivator.bundleContext.getServiceReferences(
                PluginComponent.class.getName(),
                osgiFilter);
        }
        catch (InvalidSyntaxException exc)
        {
            logger.error("Could not obtain plugin reference.", exc);
        }

        if (serRefs != null)
        {
            for (int i = 0; i < serRefs.length; i ++)
            {
                PluginComponent component = (PluginComponent) GuiActivator
                    .bundleContext.getService(serRefs[i]);;

                this.add((Component)component.getComponent());

                this.revalidate();
                this.repaint();
            }
        }

        GuiActivator.getUIService().addPluginComponentListener(this);
    }


    /**
     * Implements the <code>PluginComponentListener.pluginComponentAdded</code>
     * method.
     */
    public void pluginComponentAdded(PluginComponentEvent event)
    {
        PluginComponent c = event.getPluginComponent();
        
        if(c.getContainer().equals(Container.CONTAINER_CHAT_TOOL_BAR))
        {
            this.addSeparator();
            this.add((Component) c.getComponent());

            this.revalidate();
            this.repaint();
        }
    }

    /**
     * Implements the <code>PluginComponentListener.pluginComponentRemoved</code>
     * method.
     */
    public void pluginComponentRemoved(PluginComponentEvent event)
    {
        PluginComponent c = event.getPluginComponent();

        if(c.getContainer().equals(Container.CONTAINER_CHAT_TOOL_BAR))
        {
            this.remove((Component) c.getComponent());
        }
    }

    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        Image backgroundImage
            = ImageLoader.getImage(ImageLoader.TOOL_BAR_BACKGROUND);

        g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), null);
    }

    private class ToolBarButton
        extends JLabel
    {
        private Image iconImage;

        private boolean isMouseOver = false;

        public ToolBarButton(String text, Image iconImage)
        {
            super(text, new ImageIcon(iconImage), JLabel.CENTER);

            this.setFont(getFont().deriveFont(Font.BOLD, 10f));
            this.setForeground(new Color(
                ColorProperties.getColor("toolBarForeground")));

            this.setVerticalTextPosition(SwingConstants.BOTTOM);
            this.setHorizontalTextPosition(SwingConstants.CENTER);
        }

        public void setMouseOver(boolean isMouseOver)
        {
            this.isMouseOver = isMouseOver;
            this.repaint();
        }

        public void paintComponent(Graphics g)
        {
            Graphics2D g2 = (Graphics2D) g;

            AntialiasingManager.activateAntialiasing(g2);

            super.paintComponent(g2);

            g2.setStroke(new BasicStroke(1.5f));

            g2.setColor(new Color(0x646464));

            if (isMouseOver)
                g.drawRoundRect(0, 0, this.getWidth() - 1,
                                this.getHeight() - 3, 5, 5);
        }
    }

    public void mouseClicked(MouseEvent e)
    {
    }

    public void mouseEntered(MouseEvent e)
    {
        ToolBarButton button = (ToolBarButton) e.getSource();
        button.setMouseOver(true);
    }

    public void mouseExited(MouseEvent e)
    {
        ToolBarButton button = (ToolBarButton) e.getSource();
        button.setMouseOver(false);
    }

    public void mouseReleased(MouseEvent e)
    {
    }
    
    private int calculateButtonWidth()
    {
        int width = DEFAULT_BUTTON_WIDTH;

        FontMetrics fontMetrics
            = copyButton.getFontMetrics(copyButton.getFont());

        int textWidth = fontMetrics.stringWidth(
            Messages.getI18NString("copy").getText());

        if (textWidth > width)
            width = textWidth;

        textWidth = fontMetrics.stringWidth(
            Messages.getI18NString("cut").getText());

        if (textWidth > width)
            width = textWidth;

        textWidth = fontMetrics.stringWidth(
            Messages.getI18NString("paste").getText());

        if (textWidth > width)
            width = textWidth;

        textWidth = fontMetrics.stringWidth(
            Messages.getI18NString("back").getText());

        if (textWidth > width)
            width = textWidth;

        textWidth = fontMetrics.stringWidth(
            Messages.getI18NString("next").getText());

        if (textWidth > width)
            width = textWidth;

        textWidth = fontMetrics.stringWidth(
            Messages.getI18NString("history").getText());

        if (textWidth > width)
            width = textWidth;

        textWidth = fontMetrics.stringWidth(
            Messages.getI18NString("smiley").getText());

        if (textWidth > width)
            width = textWidth;

        // Return the width by 
        return width + 5;
    }
}
