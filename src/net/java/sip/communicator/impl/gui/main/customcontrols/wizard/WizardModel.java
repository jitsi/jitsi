/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.customcontrols.wizard;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.HashMap;

import javax.swing.Icon;

/**
 * The model for the Wizard component, which tracks the text, icons, and
 * enabled state of each of the buttons, as well as the current panel that
 * is displayed. Note that the model, in its current form, is not intended
 * to be subclassed.
 * 
 * @author Yana Stamcheva
 */


public class WizardModel {

    /**
     * Identification string for the current panel.
     */    
    public static final String CURRENT_PANEL_DESCRIPTOR_PROPERTY
        = "currentPanelDescriptorProperty";
    
    /**
     * Property identification String for the Back button's text
     */    
    public static final String BACK_BUTTON_TEXT_PROPERTY
        = "backButtonTextProperty";
    /**
     * Property identification String for the Back button's icon
     */    
    public static final String BACK_BUTTON_ICON_PROPERTY
        = "backButtonIconProperty";
    /**
     * Property identification String for the Back button's enabled state
     */    
    public static final String BACK_BUTTON_ENABLED_PROPERTY
        = "backButtonEnabledProperty";

    /**
     * Property identification String for the Next button's text
     */    
    public static final String NEXT_FINISH_BUTTON_TEXT_PROPERTY
        = "nextButtonTextProperty";
    /**
     * Property identification String for the Next button's icon
     */    
    public static final String NEXT_FINISH_BUTTON_ICON_PROPERTY
        = "nextButtonIconProperty";
    /**
     * Property identification String for the Next button's enabled state
     */    
    public static final String NEXT_FINISH_BUTTON_ENABLED_PROPERTY
        = "nextButtonEnabledProperty";
    
    /**
     * Property identification String for the Cancel button's text
     */    
    public static final String CANCEL_BUTTON_TEXT_PROPERTY
        = "cancelButtonTextProperty";
    /**
     * Property identification String for the Cancel button's icon
     */    
    public static final String CANCEL_BUTTON_ICON_PROPERTY
        = "cancelButtonIconProperty";
    /**
     * Property identification String for the Cancel button's enabled state
     */    
    public static final String CANCEL_BUTTON_ENABLED_PROPERTY
        = "cancelButtonEnabledProperty";
    
    private WizardPanelDescriptor currentPanel;
    
    private HashMap panelHashmap;
    
    private HashMap buttonTextHashmap;
    private HashMap buttonIconHashmap;
    private HashMap buttonEnabledHashmap;
    
    private PropertyChangeSupport propertyChangeSupport;
    
    
    /**
     * Default constructor.
     */    
    public WizardModel() {
        
        panelHashmap = new HashMap();
        
        buttonTextHashmap = new HashMap();
        buttonIconHashmap = new HashMap();
        buttonEnabledHashmap = new HashMap();
        
        propertyChangeSupport = new PropertyChangeSupport(this);

    }
    
    /**
     * Returns the currently displayed WizardPanelDescriptor.
     * @return The currently displayed WizardPanelDescriptor
     */    
    WizardPanelDescriptor getCurrentPanelDescriptor() {
        return currentPanel;
    }
    
    /**
     * Registers the WizardPanelDescriptor in the model using the
     * Object-identifier specified.
     * @param id Object-based identifier
     * @param descriptor WizardPanelDescriptor that describes the panel
     */    
     void registerPanel(Object id, WizardPanelDescriptor descriptor) {
        
        //  Place a reference to it in a hashtable so we can access it later
        //  when it is about to be displayed.
        
        panelHashmap.put(id, descriptor);
        
    }  
    
    /**
     * Sets the current panel to that identified by the Object passed in.
     * @param id Object-based panel identifier
     * @return boolean indicating success or failure
     */    
     boolean setCurrentPanel(Object id) {

        //  First, get the hashtable reference to the panel that should
        //  be displayed.
        
        WizardPanelDescriptor nextPanel =
            (WizardPanelDescriptor)panelHashmap.get(id);
        
        //  If we couldn't find the panel that should be displayed, return
        //  false.
        
        if (nextPanel == null)
            throw new WizardPanelNotFoundException();   

        WizardPanelDescriptor oldPanel = currentPanel;
        currentPanel = nextPanel;
        
        if (oldPanel != currentPanel) {
            firePropertyChange(CURRENT_PANEL_DESCRIPTOR_PROPERTY,
                    oldPanel, currentPanel);
        }        
        return true;        
    }
    
    /**
     * Returns the text for the Back button.
     * @return the text for the Back button.
     */
    Object getBackButtonText() {
        return buttonTextHashmap.get(BACK_BUTTON_TEXT_PROPERTY);
    }
    
    /**
     * Sets the text for the back button.
     * @param newText The text to set.
     */
    void setBackButtonText(Object newText) {
        
        Object oldText = getBackButtonText();        
        if (!newText.equals(oldText)) {
            buttonTextHashmap.put(BACK_BUTTON_TEXT_PROPERTY, newText);
            firePropertyChange(BACK_BUTTON_TEXT_PROPERTY, oldText, newText);
        }
    }

    /**
     * Returns the text for the Next/Finish button.
     * @return the text for the Next/Finish button.
     */
    Object getNextFinishButtonText() {
        return buttonTextHashmap.get(NEXT_FINISH_BUTTON_TEXT_PROPERTY);
    }
    
    void setNextFinishButtonText(Object newText) {
        
        Object oldText = getNextFinishButtonText();        
        if (!newText.equals(oldText)) {
            buttonTextHashmap.put(NEXT_FINISH_BUTTON_TEXT_PROPERTY, newText);
            firePropertyChange(NEXT_FINISH_BUTTON_TEXT_PROPERTY,
                    oldText, newText);
        }
    }

    /**
     * Returns the text for the Cancel button.
     * @return the text for the Cancel button.
     */
    Object getCancelButtonText() {
        return buttonTextHashmap.get(CANCEL_BUTTON_TEXT_PROPERTY);
    }
    
    /**
     * Sets the text for the Cancel button.
     * @param newText The text to set.
     */
    void setCancelButtonText(Object newText) {
        
        Object oldText = getCancelButtonText();        
        if (!newText.equals(oldText)) {
            buttonTextHashmap.put(CANCEL_BUTTON_TEXT_PROPERTY, newText);
            firePropertyChange(CANCEL_BUTTON_TEXT_PROPERTY, oldText, newText);
        }
    } 
    
    /**
     * Returns the icon for the Back button.
     * @return the icon for the Back button.
     */
    Icon getBackButtonIcon() {
        return (Icon)buttonIconHashmap.get(BACK_BUTTON_ICON_PROPERTY);
    }
    
    /**
     * Sets the icon for the Back button.
     * @param newIcon The new icon to set.
     */
    void setBackButtonIcon(Icon newIcon) {
        
        Object oldIcon = getBackButtonIcon();        
        if (!newIcon.equals(oldIcon)) {
            buttonIconHashmap.put(BACK_BUTTON_ICON_PROPERTY, newIcon);
            firePropertyChange(BACK_BUTTON_ICON_PROPERTY, oldIcon, newIcon);
        }
    }

    /**
     * Returns the icon for the Next/Finish button.
     * @return the icon for the Next/Finish button.
     */
    Icon getNextFinishButtonIcon() {
        return (Icon)buttonIconHashmap.get(NEXT_FINISH_BUTTON_ICON_PROPERTY);
    }
    
    /**
     * Sets the icon for the Next/Finish button.
     * @param newIcon The new icon to set.
     */
    public void setNextFinishButtonIcon(Icon newIcon) {
        
        Object oldIcon = getNextFinishButtonIcon();        
        if (!newIcon.equals(oldIcon)) {
            buttonIconHashmap.put(NEXT_FINISH_BUTTON_ICON_PROPERTY, newIcon);
            firePropertyChange(NEXT_FINISH_BUTTON_ICON_PROPERTY,
                    oldIcon, newIcon);
        }
    }

    /**
     * Returns the icon for the Cancel button.
     * @return the icon for the Cancel button.
     */
    Icon getCancelButtonIcon() {
        return (Icon)buttonIconHashmap.get(CANCEL_BUTTON_ICON_PROPERTY);
    }
    
    /**
     * Sets the icon for the Cancel button.
     * @param newIcon The new icon to set.
     */
    void setCancelButtonIcon(Icon newIcon) {
        Icon oldIcon = getCancelButtonIcon();        
        if (!newIcon.equals(oldIcon)) {
            buttonIconHashmap.put(CANCEL_BUTTON_ICON_PROPERTY, newIcon);
            firePropertyChange(CANCEL_BUTTON_ICON_PROPERTY, oldIcon, newIcon);
        }
    } 
        
    /**
     * Checks if the Back button is enabled.
     * @return <code>true</code> if the Back button is enabled,
     * <code>false</code> otherwise.
     */
    Boolean getBackButtonEnabled() {
        return (Boolean)buttonEnabledHashmap.get(
                    BACK_BUTTON_ENABLED_PROPERTY);
    }
    
    /**
     * Enables or disables the Back button.
     * @param newValue <code>true</code> to enable the Back button,
     * <code>false</code> to disable it.
     */
    void setBackButtonEnabled(Boolean newValue) {
        
        Boolean oldValue = getBackButtonEnabled();        
        if (newValue != oldValue) {
            buttonEnabledHashmap.put(BACK_BUTTON_ENABLED_PROPERTY, newValue);
            firePropertyChange(BACK_BUTTON_ENABLED_PROPERTY,
                    oldValue, newValue);
        }
    }

    /**
     * Checks if the Next/Finish button is enabled.
     * @return <code>true</code> if the Next/Finish button is enabled,
     * <code>false</code> otherwise.
     */
    Boolean getNextFinishButtonEnabled() {
        return (Boolean)buttonEnabledHashmap.get(
                    NEXT_FINISH_BUTTON_ENABLED_PROPERTY);
    }
    
    /**
     * Enables or disables the Next/Finish button.
     * @param newValue <code>true</code> to enable the Next/Finish button,
     * <code>false</code> to disable it.
     */
    void setNextFinishButtonEnabled(Boolean newValue) {
        
        Boolean oldValue = getNextFinishButtonEnabled();        
        if (newValue != oldValue) {
            buttonEnabledHashmap.put(
                    NEXT_FINISH_BUTTON_ENABLED_PROPERTY, newValue);
            firePropertyChange(NEXT_FINISH_BUTTON_ENABLED_PROPERTY,
                    oldValue, newValue);
        }
    }
    
    /**
     * Checks if the Cancel button is enabled.
     * @return <code>true</code> if the Cancel button is enabled,
     * <code>false</code> otherwise.
     */
    Boolean getCancelButtonEnabled() {
        return (Boolean)buttonEnabledHashmap.get(
                    CANCEL_BUTTON_ENABLED_PROPERTY);
    }
    
    /**
     * Enables or disables the Cancel button.
     * @param newValue <code>true</code> to enable the Cancel button,
     * <code>false</code> to disable it.
     */
    void setCancelButtonEnabled(Boolean newValue) {
        
        Boolean oldValue = getCancelButtonEnabled();        
        if (newValue != oldValue) {
            buttonEnabledHashmap.put(CANCEL_BUTTON_ENABLED_PROPERTY, newValue);
            firePropertyChange(CANCEL_BUTTON_ENABLED_PROPERTY,
                    oldValue, newValue);
        }
    }
    
    
    /**
     * Adds a <tt>PropertyChangeListener</tt>
     * @param p The <tt>PropertyChangeListener</tt> to add.
     */
    public void addPropertyChangeListener(PropertyChangeListener p) {
        propertyChangeSupport.addPropertyChangeListener(p);
    }
    
    /**
     * Removes a <tt>PropertyChangeListener</tt>
     * @param p The <tt>PropertyChangeListener</tt> to remove.
     */
    public void removePropertyChangeListener(PropertyChangeListener p) {
        propertyChangeSupport.removePropertyChangeListener(p);
    }
    
    /**
     * Informs all<tt>PropertyChangeListener</tt>s that the a given property
     * has changed.
     * @param propertyName The name of the property.
     * @param oldValue The old property value.
     * @param newValue The new property value.
     */
    protected void firePropertyChange(String propertyName,
            Object oldValue, Object newValue) {
        propertyChangeSupport.firePropertyChange(propertyName,
                oldValue, newValue);
    }
    
}
