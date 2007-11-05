/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.customcontrols.wizard;

import java.beans.*;
import java.util.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.swing.*;
import javax.swing.border.*;

import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.service.gui.*;

/**
 * This class implements a basic wizard dialog, where the programmer can insert
 * one or more Components to act as panels. These panels can be navigated
 * through arbitrarily using the 'Next' or 'Back' buttons, or the dialog itself
 * can be closed using the 'Cancel' button. Note that even though the dialog
 * uses a CardLayout manager, the order of the panels is not linear. Each panel
 * determines at runtime what its next and previous panel will be.
 */
public class Wizard
    extends SIPCommDialog
    implements WindowListener, WizardContainer, PropertyChangeListener
{
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
    public static final String NEXT_BUTTON_ACTION_COMMAND =
        "NextButtonActionCommand";

    /**
     * The String-based action command for the 'Back' button.
     */
    public static final String BACK_BUTTON_ACTION_COMMAND =
        "BackButtonActionCommand";

    /**
     * The String-based action command for the 'Cancel' button.
     */
    public static final String CANCEL_BUTTON_ACTION_COMMAND =
        "CancelButtonActionCommand";

    private BufferedImage wizardIcon;

    private JLabel wizardIconLabel;

    private JPanel wizardIconPanel =
        new JPanel(new FlowLayout(FlowLayout.CENTER));

    /**
     * The i18n text used for the buttons. Loaded from a property resource file.
     */
    static String BACK_TEXT;

    static String NEXT_TEXT;

    static String FINISH_TEXT;

    static String CANCEL_TEXT;

    /**
     * The image icons used for the buttons. Filenames are loaded from a
     * property resource file.
     */
    static Icon BACK_ICON;

    static Icon NEXT_ICON;

    static Icon FINISH_ICON;

    static Icon CANCEL_ICON;

    private WizardModel wizardModel;

    private WizardController wizardController;

    private JPanel cardPanel;

    private CardLayout cardLayout;

    private JButton backButton;

    private JButton nextButton;

    private JButton cancelButton;

    private Vector wizardListeners = new Vector();

    /**
     * This method accepts a java.awt.Dialog object as the javax.swing.JDialog's
     * parent.
     * 
     * @param owner The java.awt.Dialog object that is the owner of this dialog.
     */
    public Wizard(Dialog owner)
    {
        super(owner);

        wizardModel = new WizardModel();

        initComponents();
    }

    /**
     * This method accepts a java.awt.Frame object as the javax.swing.JDialog's
     * parent.
     * 
     * @param owner The java.awt.Frame object that is the owner of the
     *            javax.swing.JDialog.
     */
    public Wizard(Frame owner)
    {
        super(owner);

        wizardModel = new WizardModel();

        initComponents();
    }

    /**
     * Returns an instance of the JDialog that this class created.This is useful
     * in the event that you want to change any of the JDialog parameters
     * manually.
     * 
     * @return The JDialog instance that this class created.
     */
    public JDialog getDialog()
    {
        return this;
    }

    /**
     * Convienence method that displays a modal wizard dialog and blocks until
     * the dialog has completed.
     * 
     * @param modal whether to show a modal dialog
     */
    public void showDialog(boolean modal)
    {
        if (modal)
            this.setModal(true);

        this.pack();
        super.setVisible(true);
    }

    /**
     * Returns the current model of the wizard dialog.
     * 
     * @return A WizardModel instance, which serves as the model for the wizard
     *         dialog.
     */
    public WizardModel getModel()
    {
        return wizardModel;
    }

    /**
     * Adds the given WizardPage in this wizard. Each WizardPage is identified
     * by a unique Object-based identifier (often a String), which can be used
     * by the setCurrentPanel() method to display the panel at runtime.
     * 
     * @param id An Object-based identifier used to identify the WizardPage
     *            object
     * @param page The WizardPage object to register in this wizard
     */
    public void registerWizardPage(Object id, WizardPage page)
    {
        // Add the incoming panel to our JPanel display that is managed by
        // the CardLayout layout manager.

        Object wizardForm = page.getWizardForm();

        if (wizardForm instanceof Component)
            cardPanel.add((Component) wizardForm, id);

        // Place a reference to it in the model.
        wizardModel.registerPage(id, page);
    }

    /**
     * Removes from the wizard the <tt>WizardPage</tt> corresponding to the
     * given identifier.
     * 
     * @param id The identifer of the wizard page.
     */
    public void unregisterWizardPage(Object id)
    {
        WizardPage wizardPage = wizardModel.getWizardPage(id);
        if (wizardPage != null)
        {
            cardPanel.remove((Component) wizardModel.getWizardPage(id)
                .getWizardForm());

            wizardModel.unregisterPage(id);
        }
    }

    /**
     * Checks whether a page with the given id exists in the wizard.
     * 
     * @param id the identifier of the searched page
     * @return TRUE if the page with the given id exists in the wizard, FALSE
     *         otherwise.
     */
    public boolean containsPage(Object id)
    {
        if (wizardModel.getWizardPage(id) != null)
            return true;
        else
            return false;
    }

    /**
     * Displays the panel identified by the object passed in. This is the same
     * Object-based identified used when registering the panel.
     * 
     * @param id The Object-based identifier of the panel to be displayed.
     */
    public void setCurrentPage(Object id)
    {

        // Get the hashtable reference to the panel that should
        // be displayed. If the identifier passed in is null, then close
        // the dialog.

        if (id == null)
            close(Wizard.ERROR_RETURN_CODE);

        WizardPage oldPanelDescriptor = wizardModel.getCurrentWizardPage();
        if (oldPanelDescriptor != null)
            oldPanelDescriptor.pageHiding();

        wizardModel.setCurrentPanel(id);
        wizardModel.getCurrentWizardPage().pageShowing();

        // Show the panel in the dialog.

        cardLayout.show(cardPanel, id.toString());
        wizardModel.getCurrentWizardPage().pageShown();

        this.pack();
    }

    /**
     * Method used to listen for property change events from the model and
     * update the dialog's graphical components as necessary.
     * 
     * @param evt PropertyChangeEvent passed from the model to signal that one
     *            of its properties has changed value.
     */
    public void propertyChange(PropertyChangeEvent evt)
    {

        if (evt.getPropertyName().equals(WizardModel.CURRENT_PAGE_PROPERTY))
        {
            wizardController.resetButtonsToPanelRules();
        }
        else if (evt.getPropertyName().equals(
            WizardModel.NEXT_FINISH_BUTTON_TEXT_PROPERTY))
        {
            nextButton.setText(evt.getNewValue().toString());
        }
        else if (evt.getPropertyName().equals(
            WizardModel.BACK_BUTTON_TEXT_PROPERTY))
        {
            backButton.setText(evt.getNewValue().toString());
        }
        else if (evt.getPropertyName().equals(
            WizardModel.CANCEL_BUTTON_TEXT_PROPERTY))
        {
            cancelButton.setText(evt.getNewValue().toString());
        }
        else if (evt.getPropertyName().equals(
            WizardModel.NEXT_FINISH_BUTTON_ENABLED_PROPERTY))
        {
            nextButton.setEnabled(((Boolean) evt.getNewValue()).booleanValue());
        }
        else if (evt.getPropertyName().equals(
            WizardModel.BACK_BUTTON_ENABLED_PROPERTY))
        {
            backButton.setEnabled(((Boolean) evt.getNewValue()).booleanValue());
        }
        else if (evt.getPropertyName().equals(
            WizardModel.CANCEL_BUTTON_ENABLED_PROPERTY))
        {
            cancelButton.setEnabled(((Boolean) evt.getNewValue())
                .booleanValue());
        }
        else if (evt.getPropertyName().equals(
            WizardModel.NEXT_FINISH_BUTTON_ICON_PROPERTY))
        {
            nextButton.setIcon((Icon) evt.getNewValue());
        }
        else if (evt.getPropertyName().equals(
            WizardModel.BACK_BUTTON_ICON_PROPERTY))
        {
            backButton.setIcon((Icon) evt.getNewValue());
        }
        else if (evt.getPropertyName().equals(
            WizardModel.CANCEL_BUTTON_ICON_PROPERTY))
        {
            cancelButton.setIcon((Icon) evt.getNewValue());
        }

    }

    /**
     * Mirrors the WizardModel method of the same name.
     * 
     * @return A boolean indicating if the button is enabled.
     */
    public boolean isBackButtonEnabled()
    {
        return wizardModel.getBackButtonEnabled().booleanValue();
    }

    /**
     * Mirrors the WizardModel method of the same name.
     * 
     * @param newValue The new enabled status of the button.
     */
    public void setBackButtonEnabled(boolean newValue)
    {
        wizardModel.setBackButtonEnabled(new Boolean(newValue));
    }

    /**
     * Mirrors the WizardModel method of the same name.
     * 
     * @return A boolean indicating if the button is enabled.
     */
    public boolean isNextFinishButtonEnabled()
    {
        return wizardModel.getNextFinishButtonEnabled().booleanValue();
    }

    /**
     * Mirrors the WizardModel method of the same name.
     * 
     * @param newValue The new enabled status of the button.
     */
    public void setNextFinishButtonEnabled(boolean newValue)
    {
        wizardModel.setNextFinishButtonEnabled(new Boolean(newValue));
    }

    /**
     * Mirrors the WizardModel method of the same name.
     * 
     * @return A boolean indicating if the button is enabled.
     */
    public boolean isCancelButtonEnabled()
    {
        return wizardModel.getCancelButtonEnabled().booleanValue();
    }

    /**
     * Mirrors the WizardModel method of the same name.
     * 
     * @param newValue The new enabled status of the button.
     */
    public void setCancelButtonEnabled(boolean newValue)
    {
        wizardModel.setCancelButtonEnabled(new Boolean(newValue));
    }

    /**
     * Closes the dialog and sets the return code to the integer parameter.
     * 
     * @param code The return code.
     */
    void close(int code)
    {
        WizardPage oldPanelDescriptor = wizardModel.getCurrentWizardPage();
        if (oldPanelDescriptor != null)
            oldPanelDescriptor.pageHiding();

        if (this.containsPage(WizardPage.DEFAULT_PAGE_IDENTIFIER))
        {
            this.unregisterWizardPage(WizardPage.DEFAULT_PAGE_IDENTIFIER);
        }

        if (this.containsPage(WizardPage.SUMMARY_PAGE_IDENTIFIER))
        {
            this.unregisterWizardPage(WizardPage.SUMMARY_PAGE_IDENTIFIER);
        }
        this.removeWizzardIcon();

        if (code == CANCEL_RETURN_CODE)
            this.fireWizardEvent(WizardEvent.CANCEL);
        else if (code == FINISH_RETURN_CODE)
            this.fireWizardEvent(WizardEvent.SUCCESS);
        else if (code == ERROR_RETURN_CODE)
            this.fireWizardEvent(WizardEvent.ERROR);

        this.dispose();
    }

    /**
     * This method initializes the components for the wizard dialog: it creates
     * a JDialog as a CardLayout panel surrounded by a small amount of space on
     * each side, as well as three buttons at the bottom.
     */

    private void initComponents()
    {

        wizardModel.addPropertyChangeListener(this);
        wizardController = new WizardController(this);

        this.getContentPane().setLayout(new BorderLayout());
        this.addWindowListener(this);

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

        backButton.setMnemonic(Messages.getI18NString("back").getMnemonic());
        nextButton.setMnemonic(Messages.getI18NString("next").getMnemonic());
        cancelButton
            .setMnemonic(Messages.getI18NString("cancel").getMnemonic());

        // Create the buttons with a separator above them, then place them
        // on the east side of the panel with a small amount of space between
        // the back and the next button, and a larger amount of space between
        // the next button and the cancel button.

        buttonPanel.setLayout(new BorderLayout());
        buttonPanel.add(separator, BorderLayout.NORTH);

        buttonBox.setBorder(new EmptyBorder(new Insets(5, 10, 5, 10)));
        buttonBox.add(backButton);
        buttonBox.add(Box.createHorizontalStrut(10));
        buttonBox.add(nextButton);
        buttonBox.add(Box.createHorizontalStrut(30));
        buttonBox.add(cancelButton);

        buttonPanel.add(buttonBox, java.awt.BorderLayout.EAST);

        this.getContentPane().add(buttonPanel, java.awt.BorderLayout.SOUTH);
        this.getContentPane().add(cardPanel, java.awt.BorderLayout.CENTER);
        this.getContentPane().add(wizardIconPanel, java.awt.BorderLayout.WEST);
    }

    /**
     * If the user presses the close box on the dialog's window, treat it as a
     * cancel.
     * 
     * @param e The event passed in from AWT.
     */

    public void windowClosing(WindowEvent e)
    {
        this.close(Wizard.CANCEL_RETURN_CODE);
    }

    static
    {
        BACK_TEXT = Messages.getI18NString("back").getText();
        NEXT_TEXT = Messages.getI18NString("next").getText();
        CANCEL_TEXT = Messages.getI18NString("cancel").getText();
        FINISH_TEXT = Messages.getI18NString("finish").getText();
    }

    public BufferedImage getWizzardIcon()
    {
        return wizardIcon;
    }

    public void setWizzardIcon(BufferedImage wizardIcon)
    {
        wizardIconLabel = new JLabel();
        wizardIconLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(20, 20, 20, 20), BorderFactory
                .createTitledBorder("")));

        this.wizardIconLabel.setIcon(new ImageIcon(wizardIcon));
        this.wizardIconPanel.add(wizardIconLabel);
    }

    public void removeWizzardIcon()
    {
        if (wizardIconLabel != null)
            this.wizardIconPanel.remove(wizardIconLabel);
    }

    public void addWizardListener(WizardListener l)
    {
        synchronized (wizardListeners)
        {
            if (!wizardListeners.contains(l))
                wizardListeners.add(l);
        }
    }

    public void removeWizardListener(WizardListener l)
    {
        synchronized (wizardListeners)
        {
            if (wizardListeners.contains(l))
                wizardListeners.remove(l);
        }
    }

    private void fireWizardEvent(int eventCode)
    {
        WizardEvent wizardEvent = new WizardEvent(this, eventCode);

        Iterator listeners = null;
        synchronized (wizardListeners)
        {
            listeners = new ArrayList(wizardListeners).iterator();
        }

        while (listeners.hasNext())
        {
            WizardListener l = (WizardListener) listeners.next();
            l.wizardFinished(wizardEvent);
        }
    }

    /**
     * Implements the <tt>SIPCommDialog</tt> close method.
     */
    protected void close(boolean isEscaped)
    {
        this.close(Wizard.CANCEL_RETURN_CODE);
    }

    public void windowActivated(WindowEvent e)
    {
    }

    public void windowClosed(WindowEvent e)
    {
    }

    public void windowDeactivated(WindowEvent e)
    {
    }

    public void windowDeiconified(WindowEvent e)
    {
    }

    public void windowIconified(WindowEvent e)
    {
    }

    public void windowOpened(WindowEvent e)
    {
    }

    public JButton getNextButton()
    {
        return this.nextButton;
    }

    public JButton getBackButton()
    {
        return this.backButton;
    }

    public void refresh()
    {
        this.pack();
        this.repaint();
    }
}
