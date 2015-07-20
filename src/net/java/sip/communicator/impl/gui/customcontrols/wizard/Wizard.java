/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.impl.gui.customcontrols.wizard;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.beans.*;
import java.util.*;

import javax.swing.*;
import javax.swing.border.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.gui.*;

import org.jitsi.service.resources.*;

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
    implements  WindowListener,
                WizardContainer,
                PropertyChangeListener
{

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

    private JLabel wizardIconLabel;

    private TransparentPanel wizardIconPanel =
        new TransparentPanel(new FlowLayout(FlowLayout.CENTER));

    /**
     * The i18n text used for the buttons. Loaded from a property resource file.
     */
    ResourceManagementService resources = GuiActivator.getResources();

    private String backButtonDefaultText
        = resources.getI18NString("service.gui.PREVIOUS");

    private String nextButtonDefaultText
        = resources.getI18NString("service.gui.NEXT");

    private String finishButtonDefaultText
        = resources.getI18NString("service.gui.FINISH");

    private String cancelButtonDefaultText
        = resources.getI18NString("service.gui.CANCEL");

    private final WizardModel wizardModel = new WizardModel();

    private WizardController wizardController;

    protected TransparentPanel cardPanel;

    private CardLayout cardLayout;

    private JButton backButton;

    private JButton nextButton;

    private JButton cancelButton;

    private final java.util.List<WizardListener> wizardListeners
        = new Vector<WizardListener>();

    /**
     * Status label, show when connecting.
     */
    private JLabel statusLabel = new JLabel();

    /**
     * If account is signing-in ignore close.
     */
    private boolean isCurrentlySigningIn = false;

    /**
     * This method accepts a java.awt.Dialog object as the javax.swing.JDialog's
     * parent.
     *
     * @param owner The java.awt.Dialog object that is the owner of this dialog.
     */
    public Wizard(Dialog owner)
    {
        super(owner, false);

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
        super(owner, false);

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
     * Convenience method that displays a modal wizard dialog and blocks until
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
     * @param id The identifier of the wizard page.
     */
    public void unregisterWizardPage(final Object id)
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    unregisterWizardPage(id);
                }
            });
            return;
        }

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
        return (wizardModel.getWizardPage(id) != null);
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
        {
            close(Wizard.ERROR_RETURN_CODE);
            return;
        }

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
    public void propertyChange(final PropertyChangeEvent evt)
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    propertyChange(evt);
                }
            });
            return;
        }

        String name = evt.getPropertyName();

        if (WizardModel.CURRENT_PAGE_PROPERTY.equals(name))
        {
            wizardController.resetButtonsToPanelRules();
        }
        else if (WizardModel.NEXT_FINISH_BUTTON_TEXT_PROPERTY.equals(name))
        {
            nextButton.setText(evt.getNewValue().toString());
        }
        else if (WizardModel.BACK_BUTTON_TEXT_PROPERTY.equals(name))
        {
            backButton.setText(evt.getNewValue().toString());
        }
        else if (WizardModel.CANCEL_BUTTON_TEXT_PROPERTY.equals(name))
        {
            cancelButton.setText(evt.getNewValue().toString());
        }
        else if (WizardModel.NEXT_FINISH_BUTTON_ENABLED_PROPERTY.equals(name))
        {
            nextButton.setEnabled((Boolean) evt.getNewValue());
        }
        else if (WizardModel.BACK_BUTTON_ENABLED_PROPERTY.equals(name))
        {
            backButton.setEnabled((Boolean) evt.getNewValue());
        }
        else if (WizardModel.CANCEL_BUTTON_ENABLED_PROPERTY.equals(name))
        {
            cancelButton.setEnabled((Boolean) evt.getNewValue());
        }
        else if (WizardModel.NEXT_FINISH_BUTTON_ICON_PROPERTY.equals(name))
        {
            nextButton.setIcon((Icon) evt.getNewValue());
        }
        else if (WizardModel.BACK_BUTTON_ICON_PROPERTY.equals(name))
        {
            backButton.setIcon((Icon) evt.getNewValue());
        }
        else if (WizardModel.CANCEL_BUTTON_ICON_PROPERTY.equals(name))
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
        return wizardModel.getBackButtonEnabled();
    }

    /**
     * Mirrors the WizardModel method of the same name.
     *
     * @param newValue The new enabled status of the button.
     */
    public void setBackButtonEnabled(boolean newValue)
    {
        if(!(isCurrentlySigningIn && newValue))
            wizardModel.setBackButtonEnabled(newValue);
    }

    /**
     * Mirrors the WizardModel method of the same name.
     *
     * @return A boolean indicating if the button is enabled.
     */
    public boolean isNextFinishButtonEnabled()
    {
        return wizardModel.getNextFinishButtonEnabled();
    }

    /**
     * Mirrors the WizardModel method of the same name.
     *
     * @param newValue The new enabled status of the button.
     */
    public void setNextFinishButtonEnabled(boolean newValue)
    {
        wizardModel.setNextFinishButtonEnabled(newValue);
    }

    /**
     * Mirrors the WizardModel method of the same name.
     *
     * @return A boolean indicating if the button is enabled.
     */
    public boolean isCancelButtonEnabled()
    {
        return wizardModel.getCancelButtonEnabled();
    }

    /**
     * Mirrors the WizardModel method of the same name.
     *
     * @param newValue The new enabled status of the button.
     */
    public void setCancelButtonEnabled(boolean newValue)
    {
        wizardModel.setCancelButtonEnabled(newValue);
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

        TransparentPanel buttonPanel = new TransparentPanel();
        JSeparator separator = new JSeparator();
        Box buttonBox = new Box(BoxLayout.X_AXIS);

        cardPanel = new TransparentPanel();

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

        backButton.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.PREVIOUS"));
        nextButton.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.NEXT"));
        cancelButton.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.CANCEL"));

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

        JPanel statusPanel = new TransparentPanel(
                new FlowLayout(FlowLayout.CENTER));
        statusPanel.setBorder(new EmptyBorder(new Insets(5, 10, 5, 10)));
        statusPanel.add(statusLabel);
        buttonPanel.add(statusPanel, java.awt.BorderLayout.CENTER);

        java.awt.Container contentPane = getContentPane();
        contentPane.add(buttonPanel, java.awt.BorderLayout.SOUTH);
        contentPane.add(cardPanel, java.awt.BorderLayout.CENTER);
        contentPane.add(wizardIconPanel, java.awt.BorderLayout.WEST);
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

    public void setWizzardIcon(BufferedImage wizardIcon)
    {
        wizardIconLabel = new JLabel();
        wizardIconLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(20, 20, 20, 20), BorderFactory
                .createTitledBorder("")));

        this.wizardIconLabel.setIcon(new ImageIcon(wizardIcon));
        this.wizardIconPanel.removeAll();
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
            wizardListeners.remove(l);
        }
    }

    private void fireWizardEvent(int eventCode)
    {
        Iterable<WizardListener> listeners;
        synchronized (wizardListeners)
        {
            listeners = new ArrayList<WizardListener>(wizardListeners);
        }

        WizardEvent wizardEvent = new WizardEvent(this, eventCode);

        for (WizardListener l : listeners)
            l.wizardFinished(wizardEvent);
    }

    /**
     * Implements the <tt>SIPCommDialog</tt> close method.
     */
    @Override
    protected void close(boolean isEscaped)
    {
        if(isCurrentlySigningIn)
            return;

        this.close(Wizard.CANCEL_RETURN_CODE);
    }

    public void windowActivated(WindowEvent e) {}

    public void windowClosed(WindowEvent e) {}

    public void windowDeactivated(WindowEvent e) {}

    public void windowDeiconified(WindowEvent e) {}

    public void windowIconified(WindowEvent e) {}

    public void windowOpened(WindowEvent e) {}

    /**
     * Returns the next wizard button.
     *
     * @return the next wizard button
     */
    public JButton getNextButton()
    {
        return this.nextButton;
    }

    /**
     * Returns the back wizard button.
     *
     * @return the back wizard button
     */
    public JButton getBackButton()
    {
        return this.backButton;
    }

    /**
     * Refreshes this wizard dialog.
     */
    public void refresh()
    {
        this.pack();
        this.repaint();
    }

    /**
     * Returns the default text of the back wizard button.
     *
     * @return the default text of the back wizard button
     */
    public String getBackButtonDefaultText()
    {
        return backButtonDefaultText;
    }

    /**
     * Sets the back button default text.
     *
     * @param backButtonDefaultText the text to set
     */
    void setBackButtonDefaultText(String backButtonDefaultText)
    {
        this.backButtonDefaultText = backButtonDefaultText;
    }

    /**
     * Returns the default text of the next wizard button.
     *
     * @return the default text of the next wizard button.
     */
    public String getNextButtonDefaultText()
    {
        return nextButtonDefaultText;
    }

    /**
     * Sets the next button default text.
     *
     * @param nextButtonDefaultText the text to set
     */
    void setNextButtonDefaultText(String nextButtonDefaultText)
    {
        this.nextButtonDefaultText = nextButtonDefaultText;
    }

    /**
     * Returns the default text of the finish wizard button.
     *
     * @return the default text of the finish wizard button.
     */
    public String getFinishButtonDefaultText()
    {
        return finishButtonDefaultText;
    }

    /**
     * Sets the finish button default text.
     *
     * @param finishButtonDefaultText the text to set
     */
    void setFinishButtonDefaultText(String finishButtonDefaultText)
    {
        this.finishButtonDefaultText = finishButtonDefaultText;
    }

    /**
     * Returns the default text of the cancel wizard button.
     *
     * @return the default text of the cancel wizard button.
     */
    public String getCancelButtonDefaultText()
    {
        return cancelButtonDefaultText;
    }

    /**
     * Sets the cancel button default text.
     *
     * @param cancelButtonDefaultText the text to set
     */
    void setCancelButtonDefaultText(String cancelButtonDefaultText)
    {
        this.cancelButtonDefaultText = cancelButtonDefaultText;
    }

    /**
     * Sets the text label of the "Finish" wizard button.
     *
     * @param text the new label of the button
     */
    public void setFinishButtonText(String text)
    {
        this.setFinishButtonDefaultText(text);
    }

    /**
     * Changes cursor and status label, informing user we are in process
     * of connecting.
     */
    void startCommittingPage()
    {
        isCurrentlySigningIn = true;

        setBackButtonEnabled(false);
        setCancelButtonEnabled(false);
        setNextFinishButtonEnabled(false);

        statusLabel.setText(GuiActivator.getResources().getI18NString(
                "service.gui.CONNECTING"));

        setCursor(new Cursor(Cursor.WAIT_CURSOR));
    }

    /**
     * Changes cursor and status label, informing user we finished the process
     * of connecting.
     */
    void stopCommittingPage()
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    stopCommittingPage();
                }
            });
            return;
        }

        isCurrentlySigningIn = false;

        statusLabel.setText("");

        setCursor(new Cursor(Cursor.DEFAULT_CURSOR));

        setBackButtonEnabled(true);
        setCancelButtonEnabled(true);
        setNextFinishButtonEnabled(true);
    }
}
