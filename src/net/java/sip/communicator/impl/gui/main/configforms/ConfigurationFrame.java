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
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.gui.*;

/**
 * The implementation of the <tt>ConfigurationManager</tt> interface.
 * 
 * @author Yana Stamcheva
 */
public class ConfigurationFrame
    extends SIPCommDialog
    implements  ConfigurationManager, 
                MouseListener
{
    private Vector configContainer = new Vector();

    private JScrollPane formScrollPane = new JScrollPane();

    private SIPCommList configList = new SIPCommList();

    private TitlePanel titlePanel = new TitlePanel();

    private JPanel centerPanel = new JPanel(new BorderLayout());
    
    private JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    
    private JButton cancelButton = new JButton(Messages.getString("close"));

    private MainFrame mainFrame;

    /**
     * Creates an instance of <tt>ConfigurationManagerImpl</tt>.
     * 
     * @param mainFrame The main application window.
     */
    public ConfigurationFrame(MainFrame mainFrame) {
        
        super(mainFrame);
       
        this.mainFrame = mainFrame;

        this.setTitle(Messages.getString("configuration"));
        
        this.getContentPane().setLayout(new BorderLayout());

        this.addDefaultForms();

        this.centerPanel.add(formScrollPane, BorderLayout.CENTER);

        this.getContentPane().add(centerPanel, BorderLayout.CENTER);

        this.getContentPane().add(configList, BorderLayout.WEST);
        
        this.buttonsPanel.add(cancelButton);
        
        this.cancelButton.addActionListener(new ActionListener(){

            public void actionPerformed(ActionEvent e)
            {
                dispose();
            }
        });
        
        this.getContentPane().add(buttonsPanel, BorderLayout.SOUTH);         
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
     * @see ConfigurationManager#addConfigurationForm(ConfigurationForm)
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
     * @see ConfigurationManager#removeConfigurationForm(ConfigurationForm)
     */
    public void removeConfigurationForm(ConfigurationForm configForm) {

        this.configContainer.remove(configForm);
    }

    /**
     * Calculates the size of the frame depending on the size of the largest
     * contained form.
     */
    public void recalculateSize() {

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

        if (width > Constants.CONFIG_FRAME_MAX_WIDTH)
            width = Constants.CONFIG_FRAME_MAX_WIDTH;

        if (height > Constants.CONFIG_FRAME_MAX_HEIGHT)
            height = Constants.CONFIG_FRAME_MAX_HEIGHT;

        width = width + configList.getPreferredSize().getWidth();

        height = height + titlePanel.getPreferredSize().getHeight();

        this.setSize((int) width + 120, (int) height + 50);
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
     * Implements <code>ExportedDialog.isDialogVisible</code> method.
     * @see net.java.sip.communicator.service.gui.ExportedDialog#isDialogVisible()
     */
    public boolean isDialogVisible() {
        return this.isVisible();
    }

    /**
     * Implements <code>ExportedDialog.showDialog</code> method.
     * @see net.java.sip.communicator.service.gui.ExportedDialog#showDialog()
     */
    public void showDialog() {
        
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
        
        this.cancelButton.requestFocus();
    }

    /**
     * Implements <code>ExportedDialog.hideDialog</code> method.
     * @see net.java.sip.communicator.service.gui.ExportedDialog#hideDialog()
     */
    public void hideDialog() {
        this.setVisible(false);
    }

    /**
     * Implements <code>ExportedDialog.resizeDialog</code> method.
     * @see net.java.sip.communicator.service.gui.ExportedDialog#resizeDialog(int, int)
     */
    public void resizeDialog(int width, int height) {
        this.setSize(width, height);
    }

    /**
     * Implements <code>ExportedDialog.moveDialog</code> method.
     * @see net.java.sip.communicator.service.gui.ExportedDialog#moveDialog(int, int)
     */
    public void moveDialog(int x, int y) {
        this.setLocation(x, y);
    }

    /**
     * Implements <tt>SIPCommFrame</tt> close method.
     */
    protected void close(boolean isEscaped)
    {
        this.cancelButton.doClick();
    }    
}
