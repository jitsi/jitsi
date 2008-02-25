/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.menus;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.impl.gui.main.contactlist.addcontact.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.event.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>QuickMenu</tt> is the toolbar on the top of the main
 * application window. It provides quick access to the "User info" window, the
 * "Configuration" window, the "Add contact" window and the "Hide/Open offline
 * contacts" window.
 * <p>
 * Note that this class implements the <tt>PluginComponentListener</tt>. This
 * means that this toolbar is a plugable container and could contain plugin
 * components.
 *
 * @author Yana Stamcheva
 */
public class ExtendedQuickMenu
    extends SIPCommToolBar 
    implements  MouseListener,
                PluginComponentListener,
                ComponentListener,
                ListSelectionListener
{
    private Logger logger = Logger.getLogger(QuickMenu.class.getName());

    private ToolBarButton infoButton = new ToolBarButton(
        Messages.getI18NString("info").getText(),
        ImageLoader.getImage(ImageLoader.QUICK_MENU_INFO_ICON));

    private ToolBarButton configureButton = new ToolBarButton(
        Messages.getI18NString("settings").getText(),
        ImageLoader.getImage(ImageLoader.QUICK_MENU_CONFIGURE_ICON));

    private ToolBarButton searchButton = new ToolBarButton(
        Messages.getI18NString("showOffline").getText(),
        ImageLoader.getImage(ImageLoader.QUICK_MENU_SHOW_OFFLINE_ICON));

    private ToolBarButton addButton = new ToolBarButton(
        Messages.getI18NString("add").getText(),
        ImageLoader.getImage(ImageLoader.QUICK_MENU_ADD_ICON));

    private ToolBarButton soundButton = new ToolBarButton(
        Messages.getI18NString("sound").getText(),
        ImageLoader.getImage(ImageLoader.QUICK_MENU_SOUND_ON_ICON));

    private static int BUTTON_HEIGHT
        = SizeProperties.getSize("mainToolbarButtonHeight");

    private static int BUTTON_WIDTH
        = SizeProperties.getSize("mainToolbarButtonWidth");

    private ConfigurationWindow configDialog;

    private MainFrame mainFrame;

    private int movedDownButtons = 0;

    /**
     * Create an instance of the <tt>QuickMenu</tt>.
     * @param mainFrame The parent <tt>MainFrame</tt> window.
     */
    public ExtendedQuickMenu(MainFrame mainFrame)
    {
        this.mainFrame = mainFrame;

        this.setRollover(true);
        this.setLayout(new FlowLayout(FlowLayout.LEFT, 2, 0));

        this.setFloatable(true);

        this.infoButton.setPreferredSize(
            new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT));
        this.configureButton.setPreferredSize(
            new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT));
        this.searchButton.setPreferredSize(
            new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT));
        this.addButton.setPreferredSize(
            new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT));
        this.soundButton.setPreferredSize(
            new Dimension(BUTTON_HEIGHT, BUTTON_HEIGHT));

        this.infoButton.setToolTipText(
            Messages.getI18NString("userInfo").getText());
        this.configureButton.setToolTipText(
            Messages.getI18NString("configure").getText());
        this.searchButton.setToolTipText(
            Messages.getI18NString("showOfflineUsers").getText());
        this.addButton.setToolTipText(
            Messages.getI18NString("addContact").getText());
        this.soundButton.setToolTipText(
            Messages.getI18NString("soundOnOff").getText());

        this.updateMuteButton(
                GuiActivator.getAudioNotifier().isMute());

        this.init();
    }

    /**
     * Initialize the <tt>QuickMenu</tt> by adding the buttons.
     */
    private void init()
    {
        this.add(addButton);
        this.add(configureButton);
        this.add(infoButton);
        this.add(searchButton);
        this.add(soundButton);

        this.addButton.setName("add");
        this.configureButton.setName("config");
        this.searchButton.setName("search");
        this.infoButton.setName("info");
        this.soundButton.setName("sound");

        this.addButton.addMouseListener(this);
        this.configureButton.addMouseListener(this);
        this.searchButton.addMouseListener(this);
        this.infoButton.addMouseListener(this);
        this.soundButton.addMouseListener(this);

        this.addButton.addComponentListener(this);
        this.configureButton.addComponentListener(this);
        this.searchButton.addComponentListener(this);
        this.infoButton.addComponentListener(this);
        this.soundButton.addComponentListener(this);
    }
    
    private void initPluginComponents()
    {
        Iterator pluginComponents = GuiActivator.getUIService()
            .getComponentsForContainer(
                UIService.CONTAINER_MAIN_TOOL_BAR);

        if(pluginComponents.hasNext())
            this.addSeparator();

        while (pluginComponents.hasNext())
        {
            Component c = (Component)pluginComponents.next();

            this.add(c);

            if (c instanceof ContactAwareComponent)
            {
                Object selectedValue = mainFrame.getContactListPanel()
                    .getContactList().getSelectedValue();

                if(selectedValue instanceof MetaContact)
                {
                    ((ContactAwareComponent)c)
                        .setCurrentContact((MetaContact)selectedValue);
                }
                else if(selectedValue instanceof MetaContactGroup)
                {
                    ((ContactAwareComponent)c)
                        .setCurrentContactGroup((MetaContactGroup)selectedValue);
                }
            }
            
            this.revalidate();
            this.repaint();
        }
    }

    /**
     * Handles the <tt>ActionEvent</tt> triggered when user clicks on one of
     * the buttons in this toolbar.
     */
    public void mousePressed(MouseEvent e)
    {
        JLabel button = (JLabel) e.getSource();
        String buttonName = button.getName();

        if (buttonName.equals("add"))
        {
            AddContactWizard wizard = new AddContactWizard(mainFrame);
            
            wizard.setVisible(true);
        }
        else if (buttonName.equals("config"))
        {

            configDialog = GuiActivator.getUIService().getConfigurationWindow();

            configDialog.setVisible(true);
        }
        else if (buttonName.equals("search"))
        {
            ContactList contactList = mainFrame.getContactListPanel()
                .getContactList();

            ContactListModel listModel
                = (ContactListModel) contactList.getModel();
            
            Object selectedObject = null;
            int currentlySelectedIndex = contactList.getSelectedIndex();
            if(currentlySelectedIndex != -1)
            {
                selectedObject
                    = listModel.getElementAt(currentlySelectedIndex);
            }
            
            contactList.setShowOffline(!ConfigurationManager.isShowOffline());
            
            if (selectedObject != null)
            {
                if (selectedObject instanceof MetaContact)
                {
                    contactList.setSelectedIndex(
                        listModel.indexOf((MetaContact) selectedObject));
                }
                else
                {
                    contactList.setSelectedIndex(
                        listModel.indexOf(
                                (MetaContactGroup) selectedObject));
                }
            }           
        }
        else if (buttonName.equals("info"))
        {
            MetaContact selectedMetaContact =
                (MetaContact) mainFrame.getContactListPanel()
                    .getContactList().getSelectedValue();

            if(selectedMetaContact != null)
            {
                OperationSetWebContactInfo wContactInfo = null;
                
                Iterator protocolContacts = selectedMetaContact.getContacts();
                
                while(protocolContacts.hasNext())
                {
                    Contact protoContact = (Contact) protocolContacts.next();
                    
                    wContactInfo = mainFrame.getWebContactInfoOpSet(
                        protoContact.getProtocolProvider());
                    
                    if(wContactInfo != null)
                        break;
                }
                
                if(wContactInfo != null)
                {
                    Contact defaultContact = selectedMetaContact
                        .getDefaultContact();

                    GuiActivator.getBrowserLauncher().openURL(
                            wContactInfo.getWebContactInfo(defaultContact)
                                .toString());
                }
                else
                {
                    new ErrorDialog(mainFrame,
                        Messages.getI18NString("warning").getText(),
                        Messages.getI18NString("selectContactSupportingInfo")
                            .getText(),
                        ErrorDialog.WARNING).showDialog();
                }
            }            
        }
        else if (buttonName.equals("sound"))
        {
            if(GuiActivator.getAudioNotifier().isMute())
            {
                updateMuteButton(false);
                GuiActivator.getAudioNotifier().setMute(false);
            }
            else
            {
                updateMuteButton(true);
                GuiActivator.getAudioNotifier().setMute(true);
            }
        }
    }

    /**
     * Implements the <code>PluginComponentListener.pluginComponentAdded</code>
     * method.
     */
    public void pluginComponentAdded(PluginComponentEvent event)
    {
        Component c = (Component) event.getSource();
        
        // If the container id doesn't correspond to the id of the plugin
        // container we're not interested.
        if(!event.getContainerID()
                .equals(UIService.CONTAINER_MAIN_TOOL_BAR))
            return;
        
        this.add(c);
        
        if (c instanceof ContactAwareComponent)
        {
            Object selectedValue = mainFrame.getContactListPanel()
                    .getContactList().getSelectedValue();
            
            if(selectedValue instanceof MetaContact)
            {
                ((ContactAwareComponent)c)
                    .setCurrentContact((MetaContact)selectedValue);
            }
            else if(selectedValue instanceof MetaContactGroup)
            {
                ((ContactAwareComponent)c)
                    .setCurrentContactGroup((MetaContactGroup)selectedValue);
            }
        }
        
        this.revalidate();
        this.repaint();
    }

    /**
     * Implements the <code>PluginComponentListener.pluginComponentRemoved</code>
     * method.
     */
    public void pluginComponentRemoved(PluginComponentEvent event) {
        Component c = (Component) event.getSource();
        
        // If the container id doesn't correspond to the id of the plugin
        // container we're not interested.
        if(!event.getContainerID()
                .equals(UIService.CONTAINER_MAIN_TOOL_BAR))
            return;
        
        this.remove(c);
    }

    public void componentHidden(ComponentEvent e)
    {}

    /**
     * Implements ComponentListener.componentMoved method in order to resize
     * the toolbar when buttons are aligned on more than one row.
     */
    public void componentMoved(ComponentEvent e)
    {
        int compCount = this.getComponentCount();

        int biggestY = 0;
        for (int i = 0; i < compCount; i ++)
        {
            Component c = this.getComponent(i);
            
            if(c instanceof JButton)
            {
                if(c.getY() > biggestY)
                    biggestY = c.getY();
            }
        }
        
        this.setPreferredSize(
            new Dimension(this.getWidth(), biggestY + BUTTON_HEIGHT));
        
        ((JPanel)this.getParent()).revalidate();
        ((JPanel)this.getParent()).repaint();
    }

    public void componentResized(ComponentEvent e)
    {}

    public void componentShown(ComponentEvent e)
    {}
    
    public void valueChanged(ListSelectionEvent e)
    {   
        if((e.getFirstIndex() != -1 || e.getLastIndex() != -1))
        {
            Iterator pluginComponents = GuiActivator.getUIService()
                .getComponentsForContainer(
                    UIService.CONTAINER_MAIN_TOOL_BAR);
            
            while (pluginComponents.hasNext())
            {
                Component c = (Component)pluginComponents.next();
                    
                if(!(c instanceof ContactAwareComponent))
                    continue;
                
                Object selectedValue = mainFrame.getContactListPanel()
                    .getContactList().getSelectedValue();
                            
                if(selectedValue instanceof MetaContact)
                {
                    ((ContactAwareComponent)c)
                        .setCurrentContact((MetaContact)selectedValue);
                }
                else if(selectedValue instanceof MetaContactGroup)
                {
                    ((ContactAwareComponent)c)
                        .setCurrentContactGroup(
                            (MetaContactGroup)selectedValue);
                }
            }
        }
    }

    public void updateMuteButton(boolean isMute)
    {
        if(!isMute)
            this.soundButton.setIcon(
                new ImageIcon(ImageLoader.getImage(
                    ImageLoader.QUICK_MENU_SOUND_ON_ICON)));
        else
            this.soundButton.setIcon(
                new ImageIcon(ImageLoader.getImage(
                    ImageLoader.QUICK_MENU_SOUND_OFF_ICON)));
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
}