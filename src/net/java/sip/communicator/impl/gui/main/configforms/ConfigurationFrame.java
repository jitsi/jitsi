/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.configforms;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

import javax.imageio.*;
import javax.swing.*;

import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.service.gui.*;

/**
 * The implementation of the <tt>ConfigurationManager</tt> interface.
 * 
 * @author Yana Stamcheva
 */
public class ConfigurationFrame
    extends SIPCommDialog
    implements  ConfigurationWindow, 
                MouseListener
{
    private Vector configContainer = new Vector();

    private JScrollPane formScrollPane = new JScrollPane();

    private SIPCommList configList = new SIPCommList();

    private TitlePanel titlePanel = new TitlePanel();

    private JPanel mainPanel = new JPanel(new BorderLayout());
    
    private JPanel centerPanel = new JPanel(new BorderLayout());
    
    private JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    
    private I18NString closeString = Messages.getI18NString("close");
    
    private JButton closeButton = new JButton(closeString.getText());

    private MainFrame mainFrame;

    /**
     * Creates an instance of <tt>ConfigurationManagerImpl</tt>.
     * 
     * @param mainFrame The main application window.
     */
    public ConfigurationFrame(MainFrame mainFrame) {
        
        super(mainFrame);
       
        this.mainFrame = mainFrame;

        this.setTitle(Messages.getI18NString("configuration").getText());
        
        this.getContentPane().setLayout(new BorderLayout());

        this.addDefaultForms();

        this.centerPanel.add(formScrollPane, BorderLayout.CENTER);

        this.mainPanel.add(centerPanel, BorderLayout.CENTER);

        this.mainPanel.add(configList, BorderLayout.WEST);
        
        this.buttonsPanel.add(closeButton);
        
        this.closeButton.setMnemonic(closeString.getMnemonic());
        
        this.closeButton.addActionListener(new ActionListener(){

            public void actionPerformed(ActionEvent e)
            {
                dispose();
            }
        });
        
        this.mainPanel.add(buttonsPanel, BorderLayout.SOUTH);
        
        this.getContentPane().add(mainPanel);
    }

    /**
     * Some configuration forms constructed from the ui implementation itself
     * are added here in the configuration dialog.
     */
    public void addDefaultForms() {
        //this.addConfigurationForm(new GeneralConfigurationForm());
        //this.addConfigurationForm(new AppearanceConfigurationForm());
        this.addConfigurationForm(
                new AccountsConfigurationForm(mainFrame));
    }

    /**
     * Implements the <code>ConfigurationManager.addConfigurationForm</code>
     * method. Checks if the form contained in the <tt>ConfigurationForm</tt>
     * is an instance of java.awt.Component and if so adds the form in this
     * dialog, otherwise throws a ClassCastException.
     * @see ConfigurationWindow#addConfigurationForm(ConfigurationForm)
     */
    public void addConfigurationForm(ConfigurationForm configForm) {

        if(configForm.getForm() instanceof Component) {
            this.configContainer.add(configForm);
    
            this.recalculateSize();
            
            Icon image = null;
            try {
                image = new ImageIcon(ImageIO.read(
                        new ByteArrayInputStream(configForm.getIcon())));
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            
            ConfigMenuItemPanel configItem = new ConfigMenuItemPanel(configForm
                    .getTitle(), image);
    
            configItem.addMouseListener(this);
    
            this.configList.addCell(configItem);
        }
        else {
            throw new ClassCastException("ConfigurationFrame :"
            + configForm.getForm().getClass()
            + " is not a class supported by this ui implementation");
        }
    }

    /**
     * Implements <code>ConfigurationManager.removeConfigurationForm</code>
     * method. Removes the given <tt>ConfigurationForm</tt> from this
     * dialog.
     * @see ConfigurationWindow#removeConfigurationForm(ConfigurationForm)
     */
    public void removeConfigurationForm(ConfigurationForm configForm) {

        this.configContainer.remove(configForm);
    }

    /**
     * Calculates the size of the frame depending on the size of the largest
     * contained form.
     */
    public void recalculateSize()
    {
        double width = 0;

        double height = 0;

        for (int i = 0; i < configContainer.size(); i++) {

            ConfigurationForm configForm = (ConfigurationForm) configContainer
                    .get(i);
            
            Component form = (Component)configForm.getForm();
            if (width < form.getPreferredSize().getWidth())
                width = form.getPreferredSize().getWidth();

            if (height < form.getPreferredSize().getHeight())
                height = form.getPreferredSize().getHeight();
        }
     
        this.mainPanel.setPreferredSize(new Dimension(
            (int) width + 150, (int) height + 100));
    }

    /**
     * Handles the <tt>MouseEvent</tt> triggered when user clicks on the left
     * configuration dialog menu. Here we display the corresponding
     * configuration form.
     */
    public void mouseClicked(MouseEvent e) {

        ConfigMenuItemPanel configItemPanel = (ConfigMenuItemPanel) e
                .getSource();

        this.configList.refreshCellStatus(configItemPanel);

        if ((e.getModifiers() & InputEvent.BUTTON1_MASK) 
                == InputEvent.BUTTON1_MASK) {

            for (int i = 0; i < this.configContainer.size(); i++) {

                ConfigurationForm configForm 
                    = (ConfigurationForm) this.configContainer.get(i);

                if (configItemPanel.getText().equals(configForm.getTitle())) {

                    this.formScrollPane.getViewport().removeAll();

                    this.formScrollPane.getViewport()
                        .add((Component)configForm.getForm());

                    this.titlePanel.removeAll();

                    this.titlePanel.setTitleText(configForm.getTitle());

                    this.centerPanel.remove(titlePanel);

                    this.centerPanel.add(titlePanel, BorderLayout.NORTH);

                    this.validate();
                }
            }
        }
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    /**
     * Implements <code>ApplicationWindow.isWindowVisible</code> method.
     * @see net.java.sip.communicator.service.gui.ApplicationWindow#isWindowVisible()
     */
    public boolean isWindowVisible() {
        return this.isVisible();
    }

    /**
     * Implements <code>ApplicationWindow.showWindow</code> method.
     * @see net.java.sip.communicator.service.gui.ApplicationWindow#showWindow()
     */
    public void showWindow() {
        
        ConfigurationForm configForm 
            = (ConfigurationForm) this.configContainer.get(0);
        
        this.formScrollPane.getViewport().removeAll();

        this.formScrollPane.getViewport()
            .add((Component)configForm.getForm());

        this.titlePanel.removeAll();
        
        this.titlePanel.setTitleText(configForm.getTitle());

        this.centerPanel.remove(titlePanel);

        this.centerPanel.add(titlePanel, BorderLayout.NORTH);
        
        this.setVisible(true);
        
        this.closeButton.requestFocus();
    }

    /**
     * Implements <code>ApplicationWindow.hideWindow</code> method.
     * @see net.java.sip.communicator.service.gui.ApplicationWindow#hideWindow()
     */
    public void hideWindow() {
        this.setVisible(false);
    }

    /**
     * Implements <code>ApplicationWindow.resizeWindow</code> method.
     * @see net.java.sip.communicator.service.gui.ApplicationWindow#resizeWindow(int, int)
     */
    public void resizeWindow(int width, int height) {
        this.setSize(width, height);
    }

    /**
     * Implements <code>ApplicationWindow.moveWindow</code> method.
     * @see net.java.sip.communicator.service.gui.ApplicationWindow#moveWindow(int, int)
     */
    public void moveWindow(int x, int y) {
        this.setLocation(x, y);
    }

    /**
     * Implements <code>ApplicationWindow.minimizeWindow</code> method.
     * @see net.java.sip.communicator.service.gui.ApplicationWindow#minimizeWindow()
     */
    public void minimizeWindow()
    {}

    /**
     * Implements <code>ApplicationWindow.maximizeWindow</code> method.
     * @see net.java.sip.communicator.service.gui.ApplicationWindow#maximizeWindow()
     */
    public void maximizeWindow()
    {}
    
    /**
     * Implements <tt>SIPCommFrame</tt> close method.
     */
    protected void close(boolean isEscaped)
    {
        this.closeButton.doClick();
    }

    public WindowID getWindowID()
    {
        return ApplicationWindow.CONFIGURATION_WINDOW;
    }
    
}
