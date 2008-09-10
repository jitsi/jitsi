/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.menus;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.event.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.impl.gui.main.contactlist.addcontact.*;
import net.java.sip.communicator.impl.gui.main.contactlist.addgroup.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.Container;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

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
    extends JPanel
    implements  MouseListener,
                PluginComponentListener,
                ComponentListener,
                ListSelectionListener
{
    private Logger logger = Logger.getLogger(QuickMenu.class.getName());

    private SIPCommToolBar toolBar = new SIPCommToolBar();

    BufferedImage backgroundImage
        = ImageLoader.getImage(ImageLoader.TOOL_BAR_BACKGROUND);

    Rectangle rectangle
        = new Rectangle(0, 0,
                    backgroundImage.getWidth(null),
                    backgroundImage.getHeight(null));

    TexturePaint texture = new TexturePaint(backgroundImage, rectangle);

    private ToolBarButton infoButton = new ToolBarButton(
        ImageLoader.getImage(ImageLoader.QUICK_MENU_INFO_ICON));

    private ToolBarButton configureButton = new ToolBarButton(
        ImageLoader.getImage(ImageLoader.QUICK_MENU_CONFIGURE_ICON));

    private ToolBarButton hideShowButton = new ToolBarButton(
        ImageLoader.getImage(ImageLoader.QUICK_MENU_SHOW_OFFLINE_ICON));

    private ToolBarButton addButton = new ToolBarButton(
        ImageLoader.getImage(ImageLoader.QUICK_MENU_ADD_ICON));

    private ToolBarButton soundButton = new ToolBarButton(
        ImageLoader.getImage(ImageLoader.QUICK_MENU_SOUND_ON_ICON));

    private ToolBarButton createGroupButton = new ToolBarButton(
        ImageLoader.getImage(ImageLoader.QUICK_MENU_CREATE_GROUP_ICON));

    private static int DEFAULT_BUTTON_HEIGHT
        = GuiActivator.getResources().getSettingsInt("mainToolbarButtonHeight");

    private static int DEFAULT_BUTTON_WIDTH
        = GuiActivator.getResources().getSettingsInt("mainToolbarButtonWidth");

    private MoreButton moreButton = new MoreButton();

    private ExportedWindow configDialog;

    private MainFrame mainFrame;

    private int movedDownButtons = 0;

    private Hashtable pluginsTable = new Hashtable();

    private LinkedList components = new LinkedList();

    /**
     * Create an instance of the <tt>QuickMenu</tt>.
     * @param mainFrame The parent <tt>MainFrame</tt> window.
     */
    public ExtendedQuickMenu(MainFrame mainFrame)
    {
        super(new BorderLayout());

        this.mainFrame = mainFrame;

        this.toolBar.setOpaque(false);
        this.toolBar.setRollover(true);
        this.toolBar.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));

        this.toolBar.setFloatable(true);

        this.setMinimumSize(new Dimension(650, 40));
        this.setPreferredSize(new Dimension(650, 40));

        this.infoButton.setPreferredSize(
            new Dimension(DEFAULT_BUTTON_WIDTH, DEFAULT_BUTTON_HEIGHT));
        this.configureButton.setPreferredSize(
            new Dimension(DEFAULT_BUTTON_WIDTH, DEFAULT_BUTTON_HEIGHT));
        this.hideShowButton.setPreferredSize(
            new Dimension(DEFAULT_BUTTON_WIDTH, DEFAULT_BUTTON_HEIGHT));
        this.addButton.setPreferredSize(
            new Dimension(DEFAULT_BUTTON_WIDTH, DEFAULT_BUTTON_HEIGHT));
        this.soundButton.setPreferredSize(
            new Dimension(DEFAULT_BUTTON_WIDTH, DEFAULT_BUTTON_HEIGHT));
        this.createGroupButton.setPreferredSize(
            new Dimension(DEFAULT_BUTTON_WIDTH, DEFAULT_BUTTON_HEIGHT));

        this.infoButton.setToolTipText(
            Messages.getI18NString("contactInfo").getText());
        this.configureButton.setToolTipText(
            Messages.getI18NString("settings").getText());
        this.hideShowButton.setToolTipText(
            Messages.getI18NString("hideOfflineContacts").getText());
        this.addButton.setToolTipText(
            Messages.getI18NString("addContact").getText());
        this.soundButton.setToolTipText(
            Messages.getI18NString("soundOnOff").getText());
        this.createGroupButton.setToolTipText(
            Messages.getI18NString("createGroup").getText());

        this.updateMuteButton(
                GuiActivator.getAudioNotifier().isMute());

        this.init();

        this.initPluginComponents();
    }

    /**
     * Initialize the <tt>QuickMenu</tt> by adding the buttons.
     */
    private void init()
    {
        this.add(toolBar, BorderLayout.CENTER);

        this.toolBar.add(addButton);
        this.toolBar.add(createGroupButton);
        this.toolBar.add(configureButton);
        this.toolBar.add(hideShowButton);
        this.toolBar.add(soundButton);

        this.components.add(addButton);
        this.components.add(createGroupButton);
        this.components.add(configureButton);
        this.components.add(hideShowButton);
        this.components.add(soundButton);

        this.addButton.setName("add");
        this.configureButton.setName("config");
        this.hideShowButton.setName("search");
        this.soundButton.setName("sound");
        this.createGroupButton.setName("createGroup");

        this.addButton.addMouseListener(this);
        this.configureButton.addMouseListener(this);
        this.hideShowButton.addMouseListener(this);
        this.soundButton.addMouseListener(this);
        this.createGroupButton.addMouseListener(this);

        this.addButton.addComponentListener(this);
        this.configureButton.addComponentListener(this);
        this.hideShowButton.addComponentListener(this);
        this.infoButton.addComponentListener(this);
        this.soundButton.addComponentListener(this);
        this.createGroupButton.addComponentListener(this);
    }
    
    private void initPluginComponents()
    {
        // Search for plugin components registered through the OSGI bundle
        // context.
        ServiceReference[] serRefs = null;

        String osgiFilter = "("
            + Container.CONTAINER_ID
            + "="+Container.CONTAINER_MAIN_TOOL_BAR.getID()+")";

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

                    Object selectedValue = mainFrame.getContactListPanel()
                    .getContactList().getSelectedValue();

                if(component.getComponent() == null)
                    continue;

                if(selectedValue instanceof MetaContact)
                {
                    component.setCurrentContact((MetaContact)selectedValue);
                }
                else if(selectedValue instanceof MetaContactGroup)
                {
                    component
                        .setCurrentContactGroup((MetaContactGroup)selectedValue);
                }

                Component c = (Component) component.getComponent();

                if (c != null)
                {
                    if (component.getPositionIndex() > -1)
                    {
                        int index = component.getPositionIndex();
                        this.toolBar.add(c, index);
                        this.components.add(index, c);
                    }
                    else
                    {
                        this.toolBar.add(c);
                        this.components.add(c);
                    }

                    this.pluginsTable.put(component, c);
                    
                    this.repaint();
                }
            }
        }

        GuiActivator.getUIService().addPluginComponentListener(this);
    }
    
    private class AddContactAction extends AbstractAction
    {
        public void actionPerformed(ActionEvent arg0)
        {
            AddContactWizard wizard = new AddContactWizard(mainFrame);

            wizard.setVisible(true);
        }
    }
    
    private class ConfigAction extends AbstractAction
    {
        public void actionPerformed(ActionEvent arg0)
        {
            configDialog = GuiActivator.getUIService()
                .getExportedWindow(ExportedWindow.CONFIGURATION_WINDOW);

            configDialog.setVisible(true);
        }
    }
    /**
     * Handles the <tt>ActionEvent</tt> triggered when user clicks on one of
     * the buttons in this toolbar.
     */
    public void mousePressed(MouseEvent e)
    {
        ToolBarButton button = (ToolBarButton) e.getSource();
        button.setMousePressed(true);
    }

    /**
     * Implements the <code>PluginComponentListener.pluginComponentAdded</code>
     * method.
     */
    public void pluginComponentAdded(PluginComponentEvent event)
    {
        PluginComponent pluginComponent = event.getPluginComponent();

        // If the container id doesn't correspond to the id of the plugin
        // container we're not interested.
        if(!pluginComponent.getContainer()
                .equals(Container.CONTAINER_MAIN_TOOL_BAR))
            return;

        int position = pluginComponent.getPositionIndex();

        Component c = (Component) pluginComponent.getComponent();

        if (c == null)
            return;

        if (position > -1)
        {
            this.toolBar.add(c, position);
            components.add(position, c);
        }
        else
        {
            this.toolBar.add(c);
            components.add(c);
        }

        pluginsTable.put(pluginComponent, c);

        c.setPreferredSize(
            new Dimension(DEFAULT_BUTTON_WIDTH, DEFAULT_BUTTON_HEIGHT));
        c.addComponentListener(this);

        Object selectedValue = mainFrame.getContactListPanel()
                .getContactList().getSelectedValue();

        if(selectedValue instanceof MetaContact)
        {
            pluginComponent
                .setCurrentContact((MetaContact)selectedValue);
        }
        else if(selectedValue instanceof MetaContactGroup)
        {
            pluginComponent
                .setCurrentContactGroup((MetaContactGroup)selectedValue);
        }

        this.revalidate();
        this.repaint();
    }

    /**
     * Implements the <code>PluginComponentListener.pluginComponentRemoved</code>
     * method.
     */
    public void pluginComponentRemoved(PluginComponentEvent event)
    {
        PluginComponent c = event.getPluginComponent();
        
        // If the container id doesn't correspond to the id of the plugin
        // container we're not interested.
        if(!c.getContainer()
                .equals(Container.CONTAINER_MAIN_TOOL_BAR))
            return;

        this.pluginsTable.remove(c);
        this.components.remove(c);

        this.toolBar.remove((Component) c.getComponent());
    }

    public void componentHidden(ComponentEvent e)
    {}

    /**
     * Implements ComponentListener.componentMoved method in order to resize
     * the toolbar when buttons are aligned on more than one row.
     */
    public void componentMoved(ComponentEvent e)
    {
        int compCount = this.components.size();

        int maxWidth = this.toolBar.getWidth();

        int width = 0;
        for (int i = 0; i < compCount; i ++)
        {
            JComponent c = (JComponent) this.components.get(i);

            width += c.getWidth() + 10;

            if (width < maxWidth)
            {
                moreButton.removeMenuItem(c);
            }
            else
            {
                moreButton.addMenuItem(c);
            }
        }

        if (moreButton.getItemsCount() > 0)
            this.add(moreButton, BorderLayout.EAST);
        else
            this.remove(moreButton);

        this.revalidate();
        this.repaint();
    }

    public void componentResized(ComponentEvent e)
    {
    }

    public void componentShown(ComponentEvent e)
    {}
    
    public void valueChanged(ListSelectionEvent e)
    {
        if((e.getFirstIndex() != -1 || e.getLastIndex() != -1))
        {
            Enumeration plugins = pluginsTable.keys();

            while (plugins.hasMoreElements())
            {
                PluginComponent plugin = (PluginComponent) plugins.nextElement();

                Object selectedValue = mainFrame.getContactListPanel()
                    .getContactList().getSelectedValue();

                if(selectedValue instanceof MetaContact)
                {
                    plugin.setCurrentContact((MetaContact)selectedValue);
                }
                else if(selectedValue instanceof MetaContactGroup)
                {
                    plugin.setCurrentContactGroup(
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

    private class ToolBarButton
        extends JLabel
    {
        private Image iconImage;

        private boolean isMouseOver = false;

        private boolean isMousePressed = false;

        public ToolBarButton(Image iconImage)
        {
            super(new ImageIcon(iconImage));

            this.setFont(getFont().deriveFont(Font.BOLD, 10f));
            this.setForeground(new Color(
                GuiActivator.getResources().getColor("toolBarForeground")));

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
            Graphics2D g2 = (Graphics2D) g;

            AntialiasingManager.activateAntialiasing(g2);

            Color color = null;

            if(isMouseOver)
            {
                color = new Color(
                    GuiActivator.getResources()
                    .getColor("toolbarRolloverBackground"));

                g2.setColor(color);

                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 2, 8, 8);
            }

            if (isMousePressed)
            {
                color = new Color(
                    GuiActivator.getResources().getColor("toolbarBackground"));

                g2.setColor(new Color(   color.getRed(),
                                        color.getGreen(),
                                        color.getBlue(),
                                        100));

                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 2, 8, 8);
            }

            super.paintComponent(g2);
        }
        
        public Action getAction()
        {
            return null;
        }
    }
    
    public void mouseClicked(MouseEvent e)
    {
        JLabel button = (JLabel) e.getSource();
        String buttonName = button.getName();

        if (buttonName.equals("add"))
        {
            Action a = new AddContactAction();
            a.putValue(Action.NAME, button.getToolTipText());
            a.actionPerformed(null);
        }
        else if (buttonName.equals("config"))
        {
            configDialog = GuiActivator.getUIService()
                .getExportedWindow(ExportedWindow.CONFIGURATION_WINDOW);

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

            if(ConfigurationManager.isShowOffline())
            {
                button.setText(
                    "<html><center>"
                    + Messages.getI18NString("showOffline").getText()
                    + "</center></html>");

                button.setIcon(new ImageIcon(ImageLoader.getImage(
                    ImageLoader.QUICK_MENU_SHOW_OFFLINE_ICON)));

                button.setToolTipText(Messages
                    .getI18NString("showOfflineContacts").getText());
            }
            else
            {
                button.setText(
                    "<html><center>"
                    + Messages.getI18NString("hideOffline").getText()
                    + "</center></html>");

                button.setIcon(new ImageIcon(ImageLoader.getImage(
                    ImageLoader.QUICK_MENU_HIDE_OFFLINE_ICON)));

                button.setToolTipText(Messages
                    .getI18NString("hideOfflineContacts").getText());
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
            Object selectedValue = mainFrame.getContactListPanel()
                .getContactList().getSelectedValue();

            if (selectedValue == null
                || !(selectedValue instanceof MetaContact))
            {
                AboutWindow aboutWindow = new AboutWindow();

                aboutWindow.pack();

                aboutWindow.setLocation(
                    Toolkit.getDefaultToolkit().getScreenSize().width / 2
                        - aboutWindow.getWidth() / 2,
                    Toolkit.getDefaultToolkit().getScreenSize().height / 2
                        - aboutWindow.getHeight() / 2);

                aboutWindow.setVisible(true);
            }
            else
            {
                MetaContact selectedMetaContact =
                    (MetaContact) selectedValue;

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
        else if (buttonName.equals("createGroup"))
        {
            CreateGroupDialog dialog = new CreateGroupDialog(mainFrame);

            dialog.setVisible(true);
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
                .getColor("desktopBackgroundColor")));

            g2.drawRect(0, this.getHeight() - 2, this.getWidth(), 2);
        }
    }
}