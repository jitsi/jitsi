/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.customcontrols.wizard;

import java.beans.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.swing.*;
import javax.swing.border.*;

import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.service.gui.*;

/**
 * This class implements a basic wizard dialog, where the programmer can
 * insert one or more Components to act as panels. These panels can be navigated
 * through arbitrarily using the 'Next' or 'Back' buttons, or the dialog itself
 * can be closed using the 'Cancel' button. Note that even though the dialog
 * uses a CardLayout manager, the order of the panels is not linear. Each panel
 * determines at runtime what its next and previous panel will be.
 */
public class Wizard extends WindowAdapter
    implements WizardContainer, PropertyChangeListener {

    /**
     * The identifier of the summary wizard page.
     */
    String SUMMARY_PAGE_IDENTIFIER = "SUMMARY";
    
    /**
     * The identifier of the default wizard page.
     */
    String DEFAULT_PAGE_IDENTIFIER = "DEFAULT";
    
    
    /**
     * Indicates that the 'Finish' button was pressed to close the dialog.
     */    
    public static final int FINISH_RETURN_CODE = 0;
    /**
     * Indicates that the 'Cancel' button was pressed to close the dialog, or
     * the user pressed the close box in the corner of the window.
     */    
    public static final int CANCEL_RETURN_CODE = 1;
    /**
     * Indicates that the dialog closed due to an internal error.
     */    
    public static final int ERROR_RETURN_CODE = 2;
        
    /**
     * The String-based action command for the 'Next' button.
     */    
    public static final String NEXT_BUTTON_ACTION_COMMAND
        = "NextButtonActionCommand";
    /**
     * The String-based action command for the 'Back' button.
     */    
    public static final String BACK_BUTTON_ACTION_COMMAND
        = "BackButtonActionCommand";
    /**
     * The String-based action command for the 'Cancel' button.
     */    
    public static final String CANCEL_BUTTON_ACTION_COMMAND
        = "CancelButtonActionCommand";
        
    private BufferedImage wizardIcon;
    
    private JLabel wizardIconLabel;
    
    private JPanel wizardIconPanel
        = new JPanel(new FlowLayout(FlowLayout.CENTER));
    
    /**
     *  The i18n text used for the buttons. Loaded from a property resource
     *  file.
     */
    static String BACK_TEXT;
    static String NEXT_TEXT;
    static String FINISH_TEXT;
    static String CANCEL_TEXT;

    /**
     *  The image icons used for the buttons. Filenames are loaded from a
     *  property resource file.
     */
    static Icon BACK_ICON;
    static Icon NEXT_ICON;
    static Icon FINISH_ICON;
    static Icon CANCEL_ICON;
    
    
    private WizardModel wizardModel;
    private WizardController wizardController;
    private JDialog wizardDialog;
        
    private JPanel cardPanel;
    private CardLayout cardLayout;            
    private JButton backButton;
    private JButton nextButton;
    private JButton cancelButton;
    
    private int returnCode;

    
    
    /**
     * Default constructor. This method creates a new WizardModel object and
     * passes it into the overloaded constructor.
     */    
    public Wizard() {
        this((Frame)null);
    }
    
    /**
     * This method accepts a java.awt.Dialog object as the javax.swing.JDialog's
     * parent.
     * @param owner The java.awt.Dialog object that is the owner of this dialog.
     */    
    public Wizard(Dialog owner) {
        wizardModel = new WizardModel();
        wizardDialog = new JDialog(owner);         
        initComponents();
    }
 
    /**
     * This method accepts a java.awt.Frame object as the javax.swing.JDialog's
     * parent.
     * @param owner The java.awt.Frame object that is the owner of the
     * javax.swing.JDialog.
     */    
    public Wizard(Frame owner) {
        wizardModel = new WizardModel();
        wizardDialog = new JDialog(owner);         
        initComponents();
    }
    
    /**
     * Returns an instance of the JDialog that this class created.This is useful
     * in the event that you want to change any of the JDialog parameters
     * manually. 
     * @return The JDialog instance that this class created.
     */    
    public JDialog getDialog() {
        return wizardDialog;
    }
    
    /**
     * Returns the owner of the generated javax.swing.JDialog.
     * @return The owner (java.awt.Frame or java.awt.Dialog) of the
     * javax.swing.JDialog generated
     * by this class.
     */    
    public Component getOwner() {
        return wizardDialog.getOwner();
    }
    
    /**
     * Sets the title of the generated javax.swing.JDialog.
     * @param s The title of the dialog.
     */    
    public void setTitle(String s) {
        wizardDialog.setTitle(s);
    }
    
    /**
     * Returns the current title of the generated dialog.
     * @return The String-based title of the generated dialog.
     */    
    public String getTitle() {
        return wizardDialog.getTitle();
    }
    
    /**
     * Sets the modality of the generated javax.swing.JDialog.
     * @param b the modality of the dialog
     */    
    public void setModal(boolean b) {
        wizardDialog.setModal(b);
    }
    
    /**
     * Returns the modality of the dialog.
     * @return A boolean indicating whether or not the generated
     * javax.swing.JDialog is modal.
     */    
    public boolean isModal() {
        return wizardDialog.isModal();
    }
    
    /**
     * Convienence method that displays a modal wizard dialog and blocks until
     * the dialog has completed.
     * @return Indicates how the dialog was closed. Compare this value against
     * the RETURN_CODE constants at the beginning of the class.
     */    
    public int showModalDialog() {
        
        wizardDialog.setModal(true);
        wizardDialog.pack();
        wizardDialog.setVisible(true);
        
        return returnCode;
    }
    
    /**
     * Returns the current model of the wizard dialog.
     * @return A WizardModel instance, which serves as the model for the
     * wizard dialog.
     */    
    public WizardModel getModel() {
        return wizardModel;
    }
    
    /**
     * Adds the given WizardPage in this wizard. Each WizardPage is identified
     * by a unique Object-based identifier (often a String), which can be used
     * by the setCurrentPanel() method to display the panel at runtime.
     * @param id An Object-based identifier used to identify the
     * WizardPage object
     * @param page The WizardPage object to register in this wizard
     */    
    public void registerWizardPage(Object id, WizardPage page) {
        
        //  Add the incoming panel to our JPanel display that is managed by
        //  the CardLayout layout manager.
        
        Object wizardForm = page.getWizardForm();
        
        if(wizardForm instanceof Component)
            cardPanel.add((Component)wizardForm, id);
                
        //  Place a reference to it in the model. 
        wizardModel.registerPage(id, page);
    }  
    
    /**
     * Removes from the wizard the <tt>WizardPage</tt> corresponding to the
     * given identifier.
     * 
     * @param id The identifer of the wizard page.
     */    
    public void unregisterWizardPage(Object id) {
        
        cardPanel.remove(
                (Component)wizardModel.getWizardPage(id).getWizardForm());
        
        wizardModel.unregisterPage(id);
    }
    
    /**
     * Checks whether a page with the given id exists in the wizard.
     * @param id the identifier of the searched page
     * @return TRUE if the page with the given id exists in the wizard,
     * FALSE otherwise. 
     */
    public boolean containsPage(Object id) {
        if(wizardModel.getWizardPage(id) != null)
            return true;
        else
            return false;
    }
    /**
     * Displays the panel identified by the object passed in. This is the same
     * Object-based identified used when registering the panel.
     * @param id The Object-based identifier of the panel to be displayed.
     */    
    public void setCurrentPage(Object id) {

        //  Get the hashtable reference to the panel that should
        //  be displayed. If the identifier passed in is null, then close
        //  the dialog.
        
        if (id == null)
            close(ERROR_RETURN_CODE);
        
        WizardPage oldPanelDescriptor
            = wizardModel.getCurrentWizardPage();
        if (oldPanelDescriptor != null)
            oldPanelDescriptor.pageHiding();
        
        wizardModel.setCurrentPanel(id);
        wizardModel.getCurrentWizardPage().pageShowing();
        
        //  Show the panel in the dialog.
        
        cardLayout.show(cardPanel, id.toString());
        wizardModel.getCurrentWizardPage().pageShown();        
        
        
    }
    
    /**
     * Method used to listen for property change events from the model and
     * update the dialog's graphical components as necessary.
     * @param evt PropertyChangeEvent passed from the model to signal that one
     * of its properties has changed value.
     */    
    public void propertyChange(PropertyChangeEvent evt) {
        
        if (evt.getPropertyName().equals(
                WizardModel.CURRENT_PAGE_PROPERTY)) {
            wizardController.resetButtonsToPanelRules();
        } else if (evt.getPropertyName().equals(
                WizardModel.NEXT_FINISH_BUTTON_TEXT_PROPERTY)) {
            nextButton.setText(evt.getNewValue().toString());
        } else if (evt.getPropertyName().equals(
                WizardModel.BACK_BUTTON_TEXT_PROPERTY)) {
            backButton.setText(evt.getNewValue().toString());
        } else if (evt.getPropertyName().equals(
                WizardModel.CANCEL_BUTTON_TEXT_PROPERTY)) {
            cancelButton.setText(evt.getNewValue().toString());
        } else if (evt.getPropertyName().equals(
                WizardModel.NEXT_FINISH_BUTTON_ENABLED_PROPERTY)) {
            nextButton.setEnabled(((Boolean)evt.getNewValue()).booleanValue());
        } else if (evt.getPropertyName().equals(
                WizardModel.BACK_BUTTON_ENABLED_PROPERTY)) {
            backButton.setEnabled(((Boolean)evt.getNewValue()).booleanValue());
        } else if (evt.getPropertyName().equals(
                WizardModel.CANCEL_BUTTON_ENABLED_PROPERTY)) {
            cancelButton.setEnabled(((Boolean)evt.getNewValue()).booleanValue());
        } else if (evt.getPropertyName().equals(
                WizardModel.NEXT_FINISH_BUTTON_ICON_PROPERTY)) {
            nextButton.setIcon((Icon)evt.getNewValue());
        } else if (evt.getPropertyName().equals(
                WizardModel.BACK_BUTTON_ICON_PROPERTY)) {
            backButton.setIcon((Icon)evt.getNewValue());
        } else if (evt.getPropertyName().equals(
                WizardModel.CANCEL_BUTTON_ICON_PROPERTY)) {
            cancelButton.setIcon((Icon)evt.getNewValue());
        }
        
    }
    
    /**
     * Retrieves the last return code set by the dialog.
     * @return An integer that identifies how the dialog was closed. See the
     * RETURN_CODE constants of this class for possible values.
     */    
    public int getReturnCode() {
        return returnCode;
    }
    
   /**
     * Mirrors the WizardModel method of the same name.
     * @return A boolean indicating if the button is enabled.
     */  
    public boolean isBackButtonEnabled() {
        return wizardModel.getBackButtonEnabled().booleanValue();
    }

   /**
     * Mirrors the WizardModel method of the same name.
     * @param newValue The new enabled status of the button.
     */ 
    public void setBackButtonEnabled(boolean newValue) {
        wizardModel.setBackButtonEnabled(new Boolean(newValue));
    }

   /**
     * Mirrors the WizardModel method of the same name.
     * @return A boolean indicating if the button is enabled.
     */ 
    public boolean isNextFinishButtonEnabled() {
        return wizardModel.getNextFinishButtonEnabled().booleanValue();
    }

   /**
     * Mirrors the WizardModel method of the same name.
     * @param newValue The new enabled status of the button.
     */ 
    public void setNextFinishButtonEnabled(boolean newValue) {
        wizardModel.setNextFinishButtonEnabled(new Boolean(newValue));
    }
 
   /**
     * Mirrors the WizardModel method of the same name.
     * @return A boolean indicating if the button is enabled.
     */ 
    public boolean isCancelButtonEnabled() {
        return wizardModel.getCancelButtonEnabled().booleanValue();
    }

    /**
     * Mirrors the WizardModel method of the same name.
     * @param newValue The new enabled status of the button.
     */ 
    public void setCancelButtonEnabled(boolean newValue) {
        wizardModel.setCancelButtonEnabled(new Boolean(newValue));
    }
    
    /**
     * Closes the dialog and sets the return code to the integer parameter.
     * @param code The return code.
     */    
    void close(int code) {
        WizardPage oldPanelDescriptor 
            = wizardModel.getCurrentWizardPage();
        if (oldPanelDescriptor != null)
            oldPanelDescriptor.pageHiding();
        
        if(this.containsPage(WizardPage.DEFAULT_PAGE_IDENTIFIER)) {
            this.unregisterWizardPage(
                    WizardPage.DEFAULT_PAGE_IDENTIFIER);
        }
        
        if(this.containsPage(WizardPage.SUMMARY_PAGE_IDENTIFIER)) {
            this.unregisterWizardPage(
                    WizardPage.SUMMARY_PAGE_IDENTIFIER);
        }
        this.removeWizzardIcon();
        
        returnCode = code;
        wizardDialog.dispose();
    }
    
    /**
     * This method initializes the components for the wizard dialog: it creates
     * a JDialog as a CardLayout panel surrounded by a small amount of space on
     * each side, as well as three buttons at the bottom.
     */
    
    private void initComponents() {

        wizardModel.addPropertyChangeListener(this);       
        wizardController = new WizardController(this);       

        wizardDialog.getContentPane().setLayout(new BorderLayout());
        wizardDialog.addWindowListener(this);
                
        /*
         * Create the outer wizard panel, which is responsible for three 
         * buttons: Next, Back, and Cancel. It is also responsible a JPanel
         * above them that uses a CardLayout layout manager to display multiple
         * panels in the same spot.
         */
        
        JPanel buttonPanel = new JPanel();
        JSeparator separator = new JSeparator();
        Box buttonBox = new Box(BoxLayout.X_AXIS);

        cardPanel = new JPanel();

        cardLayout = new CardLayout(); 
        cardPanel.setLayout(cardLayout);
        
        backButton = new JButton();
        nextButton = new JButton();
        cancelButton = new JButton();
        
        this.getDialog().getRootPane().setDefaultButton(nextButton);
        
        backButton.setActionCommand(BACK_BUTTON_ACTION_COMMAND);
        nextButton.setActionCommand(NEXT_BUTTON_ACTION_COMMAND);
        cancelButton.setActionCommand(CANCEL_BUTTON_ACTION_COMMAND);

        backButton.addActionListener(wizardController);
        nextButton.addActionListener(wizardController);
        cancelButton.addActionListener(wizardController);
        
        //  Create the buttons with a separator above them, then place them
        //  on the east side of the panel with a small amount of space between
        //  the back and the next button, and a larger amount of space between
        //  the next button and the cancel button.
        
        buttonPanel.setLayout(new BorderLayout());
        buttonPanel.add(separator, BorderLayout.NORTH);

        buttonBox.setBorder(new EmptyBorder(new Insets(5, 10, 5, 10)));       
        buttonBox.add(backButton);
        buttonBox.add(Box.createHorizontalStrut(10));
        buttonBox.add(nextButton);
        buttonBox.add(Box.createHorizontalStrut(30));
        buttonBox.add(cancelButton);
        
        buttonPanel.add(buttonBox, java.awt.BorderLayout.EAST);
        
        wizardDialog.getContentPane().add(
                buttonPanel, java.awt.BorderLayout.SOUTH);
        wizardDialog.getContentPane().add(
                cardPanel, java.awt.BorderLayout.CENTER);
        wizardDialog.getContentPane().add(
                wizardIconPanel, 
                java.awt.BorderLayout.WEST);
    }
    
   /**
     * If the user presses the close box on the dialog's window, treat it
     * as a cancel.
     * @param e The event passed in from AWT.
     */ 
    
    public void windowClosing(WindowEvent e) {
        returnCode = CANCEL_RETURN_CODE;
    }
     
    
    static {        
        BACK_TEXT = Messages.getString("back");
        NEXT_TEXT = Messages.getString("next");
        CANCEL_TEXT = Messages.getString("cancel");
        FINISH_TEXT = Messages.getString("finish");
    }


    public void setLocation(int x, int y) {
        this.wizardDialog.setLocation(x, y);
    }

    public BufferedImage getWizzardIcon() {
        return wizardIcon;
    }

    public void setWizzardIcon(BufferedImage wizardIcon) {
        wizardIconLabel = new JLabel();
        wizardIconLabel.setBorder(BorderFactory
                .createCompoundBorder(
                        BorderFactory.createEmptyBorder(20, 20, 20, 20),
                        BorderFactory.createTitledBorder("")));
        
        this.wizardIconLabel.setIcon(new ImageIcon(wizardIcon));
        this.wizardIconPanel.add(wizardIconLabel);
    }

    public void removeWizzardIcon() {
        if(wizardIconLabel != null)
            this.wizardIconPanel.remove(wizardIconLabel);
    }
    
    public void setReturnCode(int returnCode) {
        this.returnCode = returnCode;
    }   
}
