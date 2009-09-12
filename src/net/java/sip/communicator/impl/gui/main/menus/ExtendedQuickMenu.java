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
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.impl.gui.main.contactlist.addcontact.*;
import net.java.sip.communicator.impl.gui.main.contactlist.addgroup.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.Container;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

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
 * @author Lubomir Marinov
 */
public class ExtendedQuickMenu
    extends JPanel
    implements  MouseListener,
                PluginComponentListener,
                ComponentListener,
                ListSelectionListener
{
    private final Logger logger = Logger.getLogger(ExtendedQuickMenu.class);

    private final SIPCommToolBar toolBar = new SIPCommToolBar();

    private final BufferedImage backgroundImage
        = ImageLoader.getImage(ImageLoader.TOOL_BAR_BACKGROUND);

    private final TexturePaint texture
        = new TexturePaint(
            backgroundImage,
            new Rectangle(
                0,
                0,
                backgroundImage.getWidth(null),
                backgroundImage.getHeight(null)));

    private static final Dimension PREFERRED_BUTTON_SIZE = new Dimension(
        GuiActivator.getResources().getSettingsInt(
            "impl.gui.MAIN_TOOLBAR_BUTTON_WIDTH"),
        GuiActivator.getResources().getSettingsInt(
            "impl.gui.MAIN_TOOLBAR_BUTTON_HEIGHT"));

    private final MoreButton moreButton = new MoreButton();

    private final MainFrame mainFrame;

    private final Map<PluginComponent, Component> pluginsTable =
        new Hashtable<PluginComponent, Component>();

    private final java.util.List<Component> components =
        new LinkedList<Component>();

    /**
     * Create an instance of the <tt>QuickMenu</tt>.
     * @param mainFrame The parent <tt>MainFrame</tt> window.
     */
    public ExtendedQuickMenu(MainFrame mainFrame)
    {
        super(new BorderLayout());

        this.mainFrame = mainFrame;

        this.toolBar.setFloatable(true);
        this.toolBar.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));
        this.toolBar.setOpaque(false);
        this.toolBar.setRollover(true);
        this.add(toolBar, BorderLayout.CENTER);

        Dimension size = new Dimension(650, PREFERRED_BUTTON_SIZE.height + 5);
        this.setMinimumSize(size);
        this.setPreferredSize(size);

        addButton(
            ImageLoader.QUICK_MENU_ADD_ICON,
            "service.gui.ADD_CONTACT",
            "add");
        addButton(
            ImageLoader.QUICK_MENU_CREATE_GROUP_ICON,
            "service.gui.CREATE_GROUP",
            "createGroup");
        addButton(
            ImageLoader.QUICK_MENU_CONFIGURE_ICON,
            "service.gui.SETTINGS",
            "config");
        addButton(
            ImageLoader.QUICK_MENU_SHOW_OFFLINE_ICON,
            "service.gui.HIDE_OFFLINE_CONTACTS",
            "search");
        ToolBarButton muteButton = addButton(
            ImageLoader.QUICK_MENU_SOUND_ON_ICON,
            "service.gui.SOUND_ON_OFF",
            "sound");

        updateMuteButton(
            muteButton, GuiActivator.getAudioNotifier().isMute());

        this.initPluginComponents();
    }

    private ToolBarButton addButton(
            ImageID iconImage,
            String toolTipKey,
            String name)
    {
        ToolBarButton button
            = new ToolBarButton(ImageLoader.getImage(iconImage));

        button.setName(name);
        button.setPreferredSize(PREFERRED_BUTTON_SIZE);
        button.setToolTipText(
            GuiActivator.getResources().getI18NString(toolTipKey));

        toolBar.add(button);
        components.add(button);

        button.addMouseListener(this);
        button.addComponentListener(this);
        return button;
    }
    
    private void initPluginComponents()
    {
        // Search for plugin components registered through the OSGI bundle
        // context.
        ServiceReference[] serRefs;

        String osgiFilter
            = "("
                + Container.CONTAINER_ID
                + "="
                + Container.CONTAINER_MAIN_TOOL_BAR.getID()
                + ")";

        try
        {
            serRefs = GuiActivator.bundleContext.getServiceReferences(
                PluginComponent.class.getName(),
                osgiFilter);
        }
        catch (InvalidSyntaxException exc)
        {
            serRefs = null;
            logger.error("Could not obtain plugin reference.", exc);
        }

        if ((serRefs != null) && (serRefs.length > 0))
        {
            boolean added = false;

            try
            {
                Object selectedValue
                    = mainFrame.getContactListPanel().getContactList()
                        .getSelectedValue();

                for (ServiceReference serRef : serRefs)
                {
                    PluginComponent component = (PluginComponent)
                        GuiActivator.bundleContext.getService(serRef);

                    added
                        = addPluginComponent(component, selectedValue)
                            || added;
                }
            }
            finally
            {
                if (added)
                    this.repaint();
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
        PluginComponent component = event.getPluginComponent();

        // If the container id doesn't correspond to the id of the plugin
        // container we're not interested.
        if(!component.getContainer().equals(Container.CONTAINER_MAIN_TOOL_BAR))
            return;

        boolean added = addPluginComponent(
            component,
            mainFrame.getContactListPanel().getContactList().getSelectedValue());

        if (added)
        {
            this.revalidate();
            this.repaint();
        }
    }

    private boolean addPluginComponent(
            PluginComponent component, Object selectedValue)
    {
        Component c = (Component) component.getComponent();

        if (c == null)
            return false;

        int position = component.getPositionIndex();
        if (position > -1)
        {
            toolBar.add(c, position);
            components.add(position, c);
        }
        else
        {
            toolBar.add(c);
            components.add(c);
        }

        pluginsTable.put(component, c);

        c.setPreferredSize(PREFERRED_BUTTON_SIZE);
        c.addComponentListener(this);

        if(selectedValue instanceof MetaContact)
        {
            component.setCurrentContact((MetaContact)selectedValue);
        }
        else if(selectedValue instanceof MetaContactGroup)
        {
            component
                .setCurrentContactGroup((MetaContactGroup)selectedValue);
        }
        return true;
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
        int maxWidth = this.toolBar.getWidth();

        int width = 0;
        for (Component component : components)
        {
            if (!(component instanceof JComponent))
                continue;
            JComponent c = (JComponent) component;

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

        Component moreButtonComponent = (Component) moreButton.getComponent();

        if (moreButton.getItemsCount() > 0)
            this.add(moreButtonComponent, BorderLayout.EAST);
        else
            this.remove(moreButtonComponent);

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
        if((e.getFirstIndex() != -1) || (e.getLastIndex() != -1))
        {
            for (PluginComponent plugin : pluginsTable.keySet())
            {
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

    private void updateMuteButton(ToolBarButton button, boolean isMute)
    {
        button.setIcon(new ImageIcon(ImageLoader.getImage(
            isMute
                ? ImageLoader.QUICK_MENU_SOUND_OFF_ICON
                : ImageLoader.QUICK_MENU_SOUND_ON_ICON)));
    }

    private static class ToolBarButton
        extends JLabel
    {
        private boolean isMouseOver = false;

        private boolean isMousePressed = false;

        public ToolBarButton(Image iconImage)
        {
            super(new ImageIcon(iconImage));

            Font font = getFont();
            this.setFont(font.deriveFont(Font.BOLD, font.getSize() - 2));

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
            AntialiasingManager.activateAntialiasing(g);

            Graphics2D g2 = (Graphics2D) g;

            if(isMouseOver)
            {
                Color color = new Color(
                    GuiActivator.getResources()
                    .getColor("service.gui.TOOL_BAR_ROLLOVER_BACKGROUND"));

                g2.setColor(color);

                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 2, 8, 8);
            }

            if (isMousePressed)
            {
                Color color = new Color(
                    GuiActivator.getResources()
                        .getColor("service.gui.TOOL_BAR_BACKGROUND"));

                g2.setColor(new Color(   color.getRed(),
                                        color.getGreen(),
                                        color.getBlue(),
                                        100));

                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 2, 8, 8);
            }
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
            GuiActivator.getUIService().setConfigurationWindowVisible(true);
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
                button.setIcon(new ImageIcon(ImageLoader.getImage(
                    ImageLoader.QUICK_MENU_SHOW_OFFLINE_ICON)));

                button.setToolTipText(GuiActivator.getResources()
                    .getI18NString("service.gui.SHOW_OFFLINE_CONTACTS"));
            }
            else
            {
                button.setIcon(new ImageIcon(ImageLoader.getImage(
                    ImageLoader.QUICK_MENU_HIDE_OFFLINE_ICON)));

                button.setToolTipText(GuiActivator.getResources()
                    .getI18NString("service.gui.HIDE_OFFLINE_CONTACTS"));
            }

            contactList.setShowOffline(!ConfigurationManager.isShowOffline());

            if (selectedObject != null)
            {
                contactList.setSelectedIndex(
                    listModel.indexOf(selectedObject));
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

                Dimension screenSize
                    = Toolkit.getDefaultToolkit().getScreenSize();
                aboutWindow.setLocation(
                    screenSize.width / 2 - aboutWindow.getWidth() / 2,
                    screenSize.height / 2 - aboutWindow.getHeight() / 2);

                aboutWindow.setVisible(true);
            }
            else
            {
                MetaContact selectedMetaContact =
                    (MetaContact) selectedValue;

                OperationSetWebContactInfo wContactInfo = null;

                Iterator<Contact> protocolContacts = selectedMetaContact.getContacts();

                while (protocolContacts.hasNext())
                {
                    Contact protoContact = protocolContacts.next();
                    
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
                        GuiActivator.getResources().getI18NString(
                            "service.gui.WARNING"),
                        GuiActivator.getResources().getI18NString(
                            "service.gui.SELECT_CONTACT_SUPPORTING_INFO"),
                        ErrorDialog.WARNING).showDialog();
                }
            }
        }
        else if (buttonName.equals("sound"))
        {
            ToolBarButton muteButton = (ToolBarButton) button;
            boolean mute = !GuiActivator.getAudioNotifier().isMute();

            updateMuteButton(muteButton, mute);
            GuiActivator.getAudioNotifier().setMute(mute);
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
            int width = getWidth();
            int height = getHeight();

            g2.setPaint(texture);
            g2.fillRect(0, 2, width, height - 2);

            g2.setColor(new Color(
                GuiActivator.getResources()
                    .getColor("service.gui.DESKTOP_BACKGROUND")));
            g2.drawRect(0, height - 2, width, 2);
        }
    }
}
