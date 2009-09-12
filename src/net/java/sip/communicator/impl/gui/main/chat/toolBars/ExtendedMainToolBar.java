/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat.toolBars;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.main.chat.history.*;
import net.java.sip.communicator.impl.gui.main.contactlist.addcontact.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.swing.*;

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
    implements MouseListener
{
    BufferedImage backgroundImage
        = ImageLoader.getImage(ImageLoader.TOOL_BAR_BACKGROUND);

    Rectangle rectangle
        = new Rectangle(0, 0,
                    backgroundImage.getWidth(null),
                    backgroundImage.getHeight(null));

    TexturePaint texture = new TexturePaint(backgroundImage, rectangle);

    private ToolBarButton copyButton = new ToolBarButton(
        ImageLoader.getImage(ImageLoader.COPY_ICON));

    private ToolBarButton cutButton = new ToolBarButton(
        ImageLoader.getImage(ImageLoader.CUT_ICON));

    private ToolBarButton pasteButton = new ToolBarButton(
        ImageLoader.getImage(ImageLoader.PASTE_ICON));

    private ToolBarButton saveButton = new ToolBarButton(
        ImageLoader.getImage(ImageLoader.SAVE_ICON));

    private ToolBarButton printButton = new ToolBarButton(
        ImageLoader.getImage(ImageLoader.PRINT_ICON));

    private ToolBarButton previousButton = new ToolBarButton(
        ImageLoader.getImage(ImageLoader.PREVIOUS_ICON));

    private ToolBarButton nextButton = new ToolBarButton(
        ImageLoader.getImage(ImageLoader.NEXT_ICON));

    private ToolBarButton historyButton = new ToolBarButton(
        ImageLoader.getImage(ImageLoader.HISTORY_ICON));
    
    private ToolBarButton addButton = new ToolBarButton(
        ImageLoader.getImage(ImageLoader.QUICK_MENU_ADD_ICON));
    
    private ToolBarButton sendFileButton = new ToolBarButton(
        ImageLoader.getImage(ImageLoader.SEND_FILE_ICON));

    private ToolBarButton fontButton = new ToolBarButton(
        ImageLoader.getImage(ImageLoader.FONT_ICON));

    private ToolBarButton settingsButton = new ToolBarButton(
        ImageLoader.getImage(ImageLoader.QUICK_MENU_CONFIGURE_ICON));

    private static int DEFAULT_BUTTON_HEIGHT
        = GuiActivator.getResources()
            .getSettingsInt("impl.gui.MAIN_TOOLBAR_BUTTON_HEIGHT");

    private static int DEFAULT_BUTTON_WIDTH
        = GuiActivator.getResources()
            .getSettingsInt("impl.gui.MAIN_TOOLBAR_BUTTON_WIDTH");

    private Contact currentChatContact = null;

    /**
     * Creates an instance and constructs the <tt>MainToolBar</tt>.
     * 
     * @param messageWindow The parent <tt>ChatWindow</tt>.
     */
    public ExtendedMainToolBar(ChatWindow messageWindow)
    {
        super(messageWindow);
    }        

    protected void init()
    {
        this.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        this.setPreferredSize(new Dimension(300, DEFAULT_BUTTON_HEIGHT + 5));

        this.add(cutButton);
        this.add(copyButton);
        this.add(pasteButton);
        this.add(settingsButton);
        this.add(previousButton);
        this.add(nextButton);
        this.add(historyButton);
        this.add(addButton);

        this.saveButton.setName("save");
        this.saveButton.setToolTipText(
            GuiActivator.getResources().getI18NString("service.gui.SAVE")
                + " Ctrl-S");

        this.printButton.setName("print");
        this.printButton.setToolTipText(
            GuiActivator.getResources().getI18NString("service.gui.PRINT"));

        this.cutButton.setName("cut");
        this.cutButton.setToolTipText(
            GuiActivator.getResources().getI18NString("service.gui.CUT")
                + " Ctrl-X");

        this.copyButton.setName("copy");
        this.copyButton.setToolTipText(
            GuiActivator.getResources().getI18NString("service.gui.COPY")
                + " Ctrl-C");

        this.pasteButton.setName("paste");
        this.pasteButton.setToolTipText(
            GuiActivator.getResources().getI18NString("service.gui.PASTE")
                + " Ctrl-P");

        this.previousButton.setName("previous");
        this.previousButton.setToolTipText(
            GuiActivator.getResources()
                .getI18NString("service.gui.PREVIOUS_TOOLTIP"));

        this.nextButton.setName("next");
        this.nextButton.setToolTipText(
            GuiActivator.getResources()
                .getI18NString("service.gui.NEXT_TOOLTIP"));

        this.sendFileButton.setName("sendFile");
        this.sendFileButton.setToolTipText(
            GuiActivator.getResources().getI18NString("service.gui.SEND_FILE"));

        this.historyButton.setName("history");
        this.historyButton.setToolTipText(
            GuiActivator.getResources().getI18NString("service.gui.HISTORY")
                + " Ctrl-H");

        this.addButton.setName("addContact");
        this.addButton.setToolTipText(
            GuiActivator.getResources().getI18NString("service.gui.ADD_CONTACT"));

        this.fontButton.setName("font");
        this.fontButton.setToolTipText(
            GuiActivator.getResources().getI18NString("service.gui.FONT"));

        this.settingsButton.setName("settings");
        this.settingsButton.setToolTipText(
            GuiActivator.getResources().getI18NString("service.gui.SETTINGS"));

        this.saveButton.setPreferredSize(
            new Dimension(DEFAULT_BUTTON_WIDTH, DEFAULT_BUTTON_HEIGHT));
        this.printButton.setPreferredSize(
            new Dimension(DEFAULT_BUTTON_WIDTH, DEFAULT_BUTTON_HEIGHT));
        this.cutButton.setPreferredSize(
            new Dimension(DEFAULT_BUTTON_WIDTH, DEFAULT_BUTTON_HEIGHT));
        this.copyButton.setPreferredSize(
            new Dimension(DEFAULT_BUTTON_WIDTH, DEFAULT_BUTTON_HEIGHT));
        this.pasteButton.setPreferredSize(
            new Dimension(DEFAULT_BUTTON_WIDTH, DEFAULT_BUTTON_HEIGHT));
        this.previousButton.setPreferredSize(
            new Dimension(DEFAULT_BUTTON_WIDTH, DEFAULT_BUTTON_HEIGHT));
        this.nextButton.setPreferredSize(
            new Dimension(DEFAULT_BUTTON_WIDTH, DEFAULT_BUTTON_HEIGHT));
        this.sendFileButton.setPreferredSize(
            new Dimension(DEFAULT_BUTTON_WIDTH, DEFAULT_BUTTON_HEIGHT));
        this.historyButton.setPreferredSize(
            new Dimension(DEFAULT_BUTTON_WIDTH, DEFAULT_BUTTON_HEIGHT));
        this.fontButton.setPreferredSize(
            new Dimension(DEFAULT_BUTTON_WIDTH, DEFAULT_BUTTON_HEIGHT));
        this.settingsButton.setPreferredSize(
            new Dimension(DEFAULT_BUTTON_WIDTH, DEFAULT_BUTTON_HEIGHT));

        this.saveButton.addMouseListener(this);
        this.printButton.addMouseListener(this);
        this.cutButton.addMouseListener(this);
        this.copyButton.addMouseListener(this);
        this.pasteButton.addMouseListener(this);
        this.previousButton.addMouseListener(this);
        this.nextButton.addMouseListener(this);
        this.sendFileButton.addMouseListener(this);
        this.historyButton.addMouseListener(this);
        this.addButton.addMouseListener(this);
        this.fontButton.addMouseListener(this);
        this.settingsButton.addMouseListener(this);

        // Disable all buttons that do nothing.
        this.saveButton.setEnabled(false);
        this.printButton.setEnabled(false);
        this.sendFileButton.setEnabled(false);
        this.fontButton.setEnabled(false);
    }

    protected void chatChanged(ChatPanel panel) 
    {
        if(panel.getChatSession() instanceof MetaContactChatSession)
        { 
            MetaContact contact = 
                (MetaContact) panel.getChatSession().getDescriptor();

            if(contact == null) return;

            Contact defaultContact = contact.getDefaultContact();
            if(defaultContact == null) return;

            ContactGroup parent = defaultContact.getParentContactGroup();
            boolean isParentPersist = true;
            boolean isParentResolved = true;
            if(parent != null)
            {
                isParentPersist = parent.isPersistent();
                isParentResolved = parent.isResolved();
            }
            
            if(!defaultContact.isPersistent() &&
               !defaultContact.isResolved() &&
               !isParentPersist &&
               !isParentResolved)
            {
               addButton.setVisible(true);
               currentChatContact = defaultContact;
            }
            else
            {
                addButton.setVisible(false);
                currentChatContact = null;
            }  
        }
    }

    /**
     * Handles the <tt>ActionEvent</tt>, when one of the toolbar buttons is
     * clicked.
     */
    public void mousePressed(MouseEvent e)
    {
        ToolBarButton button = (ToolBarButton) e.getSource();
        button.setMousePressed(true);
    }

    private static class ToolBarButton
        extends JLabel
    {
        private boolean isMouseOver = false;

        private boolean isMousePressed = false;

        public ToolBarButton(Image iconImage)
        {
            super(new ImageIcon(iconImage));

            this.setFont(getFont().deriveFont(Font.BOLD, 10f));
            this.setForeground(new Color(
                GuiActivator.getResources()
                    .getColor("service.gui.TOOL_BAR_FOREGROUND")));

            this.setVerticalTextPosition(SwingConstants.BOTTOM);
            this.setHorizontalTextPosition(SwingConstants.CENTER);
        }

        public void setMouseOver(boolean isMouseOver)
        {
            this.isMouseOver = isMouseOver;
            this.repaint();
        }

        public void setMousePressed(boolean isMousePressed)
        {
            this.isMousePressed = isMousePressed;
            this.repaint();
        }

        public void paintComponent(Graphics g)
        {
            Graphics t = g.create();
            try
            {
                internalPaintComponent(t);
            }
            finally
            {
                t.dispose();
            }

            super.paintComponent(g);
        }

        private void internalPaintComponent(Graphics g)
        {
            Graphics2D g2 = (Graphics2D) g;

            AntialiasingManager.activateAntialiasing(g2);

            Color color = null;

            if(isMouseOver)
            {
                color = new Color(
                    GuiActivator.getResources()
                    .getColor("service.gui.TOOL_BAR_ROLLOVER_BACKGROUND"));

                g2.setColor(color);

                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 2, 8, 8);
            }

            if (isMousePressed)
            {
                color = new Color(GuiActivator.getResources()
                        .getColor("service.gui.TOOL_BAR_BACKGROUND"));

                g2.setColor(new Color(   color.getRed(),
                                        color.getGreen(),
                                        color.getBlue(),
                                        100));

                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 2, 8, 8);
            }
        }
    }

    public void mouseClicked(MouseEvent e)
    {
        JLabel button = (JLabel) e.getSource();
        String buttonText = button.getName();

        ChatPanel chatPanel = messageWindow.getCurrentChatPanel();

        if (buttonText.equalsIgnoreCase("cut"))
        {
            chatPanel.cut();
        }
        else if (buttonText.equalsIgnoreCase("copy"))
        {
            chatPanel.copy();
        }
        else if (buttonText.equalsIgnoreCase("paste"))
        {
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
        else if (buttonText.equalsIgnoreCase("history"))
        {
            HistoryWindow history;

            HistoryWindowManager historyWindowManager
                = GuiActivator.getUIService().getHistoryWindowManager();

            Object historyContact = chatPanel.getChatSession().getDescriptor();

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
                history = new HistoryWindow(
                    chatPanel.getChatSession().getDescriptor());

                history.setVisible(true);

                historyWindowManager.addHistoryWindowForContact(historyContact,
                                                                history);
            }
        }
        else if (buttonText.equals("addContact")) 
        {
            if(currentChatContact != null)
            {
                AddContactWizard addCWizz = 
                        new AddContactWizard(
                            GuiActivator.getUIService().getMainFrame(),
                            currentChatContact.getAddress(),
                            currentChatContact.getProtocolProvider()
                        );

                addCWizz.setVisible(true);
            }
        }
        else if (buttonText.equalsIgnoreCase("settings"))
        {
            GuiActivator.getUIService().setConfigurationWindowVisible(true);
        }
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
        ToolBarButton button = (ToolBarButton) e.getSource();
        button.setMousePressed(false);
    }

    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        if (backgroundImage != null)
        {
            Graphics2D g2 = (Graphics2D) g;

            g2.setPaint(texture);

            g2.fillRect(0, 2, this.getWidth(), this.getHeight() - 2);

            g2.setColor(new Color(
                GuiActivator.getResources()
                .getColor("service.gui.MAIN_WINDOW_BACKGROUND")));

            g2.drawRect(0, this.getHeight() - 2, this.getWidth(), 2);
        }
    }
}
