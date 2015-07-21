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

import java.beans.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.service.gui.*;

/**
 * The model for the Wizard component, which tracks the text, icons, and
 * enabled state of each of the buttons, as well as the current panel that
 * is displayed. Note that the model, in its current form, is not intended
 * to be sub-classed.
 *
 * @author Yana Stamcheva
 */
public class WizardModel
{
    /**
     * Identification string for the current panel.
     */
    public static final String CURRENT_PAGE_PROPERTY
        = "currentPageProperty";

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

    private WizardPage currentPanel;

    private final Map<Object, WizardPage> panelHashmap
        = new HashMap<Object, WizardPage>();

    private final Map<String, Object> buttonTextHashmap
        = new HashMap<String, Object>();

    private final Map<String, Icon> buttonIconHashmap
        = new HashMap<String, Icon>();

    private final Map<String, Boolean> buttonEnabledHashmap
        = new HashMap<String, Boolean>();

    private final PropertyChangeSupport propertyChangeSupport;

    /**
     * Default constructor.
     */
    public WizardModel()
    {
        propertyChangeSupport = new PropertyChangeSupport(this);
    }

    /**
     * Returns the currently displayed WizardPage.
     * @return The currently displayed WizardPage
     */
    WizardPage getCurrentWizardPage()
    {
        return currentPanel;
    }

    /**
     * Registers the WizardPage in the model using the
     * Object-identifier specified.
     * @param id Object-based identifier
     * @param page WizardPage that describes the panel
     */
    void registerPage(Object id, WizardPage page)
    {
        panelHashmap.put(id, page);
    }

     /**
      * Unregisters the <tt>WizardPage</tt> corresponding to the given id.
      *
      * @param id The id of the <tt>WizardPage</tt>.
      */
    void unregisterPage(Object id)
    {
        panelHashmap.remove(id);
    }

    /**
     * Returns the <tt>WizardPage</tt> corresponding to the given identifier.
     * @param id The identifier of the page.
     * @return the <tt>WizardPage</tt> corresponding to the given identifier.
     */
    WizardPage getWizardPage(Object id)
    {
        return panelHashmap.get(id);
    }

    Iterator<Map.Entry<Object, WizardPage>> getAllPages()
    {
        return panelHashmap.entrySet().iterator();
    }

    /**
     * Sets the current panel to that identified by the Object passed in.
     * @param id Object-based panel identifier
     * @return boolean indicating success or failure
     */
     boolean setCurrentPanel(Object id)
     {
        //  First, get the hashtable reference to the panel that should
        //  be displayed.
        WizardPage nextPanel = panelHashmap.get(id);

        //  If we couldn't find the panel that should be displayed, return
        //  false.
        if (nextPanel == null)
            throw new WizardPanelNotFoundException();

        WizardPage oldPanel = currentPanel;
        currentPanel = nextPanel;

        if (oldPanel != currentPanel)
        {
            firePropertyChange(CURRENT_PAGE_PROPERTY,
                    oldPanel, currentPanel);
        }

        return true;
    }

    /**
     * Returns the text for the Back button.
     * @return the text for the Back button.
     */
    Object getBackButtonText()
    {
        return buttonTextHashmap.get(BACK_BUTTON_TEXT_PROPERTY);
    }

    /**
     * Sets the text for the back button.
     * @param newText The text to set.
     */
    void setBackButtonText(Object newText)
    {
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
    Object getNextFinishButtonText()
    {
        return buttonTextHashmap.get(NEXT_FINISH_BUTTON_TEXT_PROPERTY);
    }

    void setNextFinishButtonText(Object newText)
    {
        Object oldText = getNextFinishButtonText();
        if (!newText.equals(oldText))
        {
            buttonTextHashmap.put(NEXT_FINISH_BUTTON_TEXT_PROPERTY, newText);
            firePropertyChange(NEXT_FINISH_BUTTON_TEXT_PROPERTY,
                    oldText, newText);
        }
    }

    /**
     * Returns the text for the Cancel button.
     * @return the text for the Cancel button.
     */
    Object getCancelButtonText()
    {
        return buttonTextHashmap.get(CANCEL_BUTTON_TEXT_PROPERTY);
    }

    /**
     * Sets the text for the Cancel button.
     * @param newText The text to set.
     */
    void setCancelButtonText(Object newText)
    {
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
    Icon getBackButtonIcon()
    {
        return buttonIconHashmap.get(BACK_BUTTON_ICON_PROPERTY);
    }

    /**
     * Sets the icon for the Back button.
     * @param newIcon The new icon to set.
     */
    void setBackButtonIcon(Icon newIcon)
    {
        Object oldIcon = getBackButtonIcon();
        if (!newIcon.equals(oldIcon))
        {
            buttonIconHashmap.put(BACK_BUTTON_ICON_PROPERTY, newIcon);
            firePropertyChange(BACK_BUTTON_ICON_PROPERTY, oldIcon, newIcon);
        }
    }

    /**
     * Returns the icon for the Next/Finish button.
     * @return the icon for the Next/Finish button.
     */
    Icon getNextFinishButtonIcon()
    {
        return buttonIconHashmap.get(NEXT_FINISH_BUTTON_ICON_PROPERTY);
    }

    /**
     * Sets the icon for the Next/Finish button.
     * @param newIcon The new icon to set.
     */
    public void setNextFinishButtonIcon(Icon newIcon)
    {
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
    Icon getCancelButtonIcon()
    {
        return buttonIconHashmap.get(CANCEL_BUTTON_ICON_PROPERTY);
    }

    /**
     * Sets the icon for the Cancel button.
     * @param newIcon The new icon to set.
     */
    void setCancelButtonIcon(Icon newIcon)
    {
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
    Boolean getBackButtonEnabled()
    {
        return buttonEnabledHashmap.get(BACK_BUTTON_ENABLED_PROPERTY);
    }

    /**
     * Enables or disables the Back button.
     * @param enabled <code>true</code> to enable the Back button,
     * <code>false</code> to disable it.
     */
    void setBackButtonEnabled(boolean enabled)
    {
        Boolean newValue = enabled;
        Boolean oldValue = getBackButtonEnabled();
        if (!newValue.equals(oldValue))
        {
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
    Boolean getNextFinishButtonEnabled()
    {
        return buttonEnabledHashmap.get(NEXT_FINISH_BUTTON_ENABLED_PROPERTY);
    }

    /**
     * Enables or disables the Next/Finish button.
     * @param enabled <code>true</code> to enable the Next/Finish button,
     * <code>false</code> to disable it.
     */
    void setNextFinishButtonEnabled(boolean enabled)
    {
        Boolean newValue = enabled;
        Boolean oldValue = getNextFinishButtonEnabled();
        if (!newValue.equals(oldValue))
        {
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
    Boolean getCancelButtonEnabled()
    {
        return buttonEnabledHashmap.get(CANCEL_BUTTON_ENABLED_PROPERTY);
    }

    /**
     * Enables or disables the Cancel button.
     * @param enabled <code>true</code> to enable the Cancel button,
     * <code>false</code> to disable it.
     */
    void setCancelButtonEnabled(boolean enabled)
    {
        Boolean newValue = enabled;
        Boolean oldValue = getCancelButtonEnabled();
        if (!newValue.equals(oldValue))
        {
            buttonEnabledHashmap.put(CANCEL_BUTTON_ENABLED_PROPERTY, newValue);
            firePropertyChange(CANCEL_BUTTON_ENABLED_PROPERTY,
                    oldValue, newValue);
        }
    }

    /**
     * Adds a <tt>PropertyChangeListener</tt>
     * @param p The <tt>PropertyChangeListener</tt> to add.
     */
    public void addPropertyChangeListener(PropertyChangeListener p)
    {
        propertyChangeSupport.addPropertyChangeListener(p);
    }

    /**
     * Removes a <tt>PropertyChangeListener</tt>
     * @param p The <tt>PropertyChangeListener</tt> to remove.
     */
    public void removePropertyChangeListener(PropertyChangeListener p)
    {
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
            Object oldValue, Object newValue)
    {
        propertyChangeSupport.firePropertyChange(propertyName,
                oldValue, newValue);
    }
}
