/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.customcontrols.wizard;

import java.awt.*;
import javax.swing.*;


/**
 * A base descriptor class used to reference a Component panel for the Wizard,
 * as well as provide general rules as to how the panel should behave.
 * 
 * @author Yana Stamcheva
 */
public class WizardPanelDescriptor {
    
    private static final String DEFAULT_PANEL_IDENTIFIER
        = "defaultPanelIdentifier";
        
    /**
     * Identifier returned by getNextPanelDescriptor() to indicate that this
     * is the last panel and the text of the 'Next' button should change to
     * 'Finish'.
     */    
    public static final FinishIdentifier FINISH = new FinishIdentifier();
    
    private Wizard wizard;
    private Component targetPanel;
    private Object panelIdentifier;
    
    /**
     * Default constructor. The id and the Component panel must be set
     * separately.
     */    
    public WizardPanelDescriptor() {
        panelIdentifier = DEFAULT_PANEL_IDENTIFIER;
        targetPanel = new JPanel();
    }
    
    /**
     * Constructor which accepts both the Object-based identifier and a
     * reference to the Component class which makes up the panel.
     * @param id Object-based identifier
     * @param panel A class which extends java.awt.Component that will be
     * inserted as a panel into the wizard dialog.
     */    
    public WizardPanelDescriptor(Object id, Component panel) {
        panelIdentifier = id;
        targetPanel = panel;
    }
   
    /**
     * Returns to java.awt.Component that serves as the actual panel.
     * @return A reference to the java.awt.Component that serves as the panel
     */    
    public final Component getPanelComponent() {
        return targetPanel;
    }
    
    /**
     * Sets the panel's component as a class that extends java.awt.Component
     * @param panel java.awt.Component which serves as the wizard panel
     */    
    public final void setPanelComponent(Component panel) {
        targetPanel = panel;
    }
    
    /**
     * Returns the unique Object-based identifier for this panel descriptor.
     * @return The Object-based identifier
     */    
    public final Object getPanelDescriptorIdentifier() {
        return panelIdentifier;
    }

    /**
     * Sets the Object-based identifier for this panel. The identifier must be unique
     * from all the other identifiers in the panel.
     * @param id Object-based identifier for this panel.
     */    
    public final void setPanelDescriptorIdentifier(Object id) {
        panelIdentifier = id;
    }
    
    final void setWizard(Wizard w) {
        wizard = w;
    }
    
    /**
     * Returns a reference to the Wizard component.
     * @return The Wizard class hosting this descriptor.
     */    
    public final Wizard getWizard() {
        return wizard;
    }   

    /**
     * Returns a reference to the current WizardModel for this Wizard component.
     * @return The current WizardModel for this Wizard component.
     */    
    public WizardModel getWizardModel() {
        return wizard.getModel();
    }
    
    //  Override this method to provide an Object-based identifier
    //  for the next panel.
    
    /**
     * Override this class to provide the Object-based identifier of the panel
     * that the user should traverse to when the Next button is pressed. Note
     * that this method is only called when the button is actually pressed, so
     * that the panel can change the next panel's identifier dynamically at
     * runtime if necessary. Return null if the button should be disabled.
     * Returns FinishIdentfier if the button text should change to 'Finish' and
     * the dialog should end.
     * @return Object-based identifier.
     */    
    public Object getNextPanelDescriptor() {
        return null;
    }

    //  Override this method to provide an Object-based identifier
    //  for the previous panel.
    
    /**
     * Override this class to provide the Object-based identifier of the panel
     * that the user should traverse to when the Back button is pressed. Note
     * that this method is only called when the button is actually pressed,
     * so that the panel can change the previous panel's identifier dynamically
     * at runtime if necessary. Return null if the button should be disabled.
     * @return Object-based identifier
     */    
    public Object getBackPanelDescriptor() {
        return null;
    }
        
    /**
     * Override this method to provide functionality that will be performed
     * just before the panel is to be displayed.
     */    
    public void aboutToDisplayPanel() {

    }
     
    /**
     * Override this method to perform functionality when the panel itself is
     * displayed.
     */    
    public void displayingPanel() {

    }
     
    /**
     * Override this method to perform functionality just before the panel is
     * to be hidden.
     */    
    public void aboutToHidePanel() {

    }    
      
    
    static class FinishIdentifier {
        public static final String ID = "FINISH";
    }
}
