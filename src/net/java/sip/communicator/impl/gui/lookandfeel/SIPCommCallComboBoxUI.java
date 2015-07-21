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
package net.java.sip.communicator.impl.gui.lookandfeel;

import java.awt.*;

import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.*;

/**
 * A custom implementation of the <tt>SIPCommComboBoxUI</tt> specially designed
 * for the call combo box.
 * @author Yana Stamcheva
 */
public class SIPCommCallComboBoxUI extends SIPCommComboBoxUI
{
    /**
     * Creates an instance of the <tt>SIPCommCallComboBoxUI</tt> for the given
     * component.
     * @param c the component for which we create the UI
     * @return an instance of the <tt>SIPCommCallComboBoxUI</tt>
     */
    public static ComponentUI createUI(JComponent c)
    {
        return new SIPCommCallComboBoxUI();
    }

    /**
     * Creates the popup portion of the combo box.
     *
     * @return an instance of <code>ComboPopup</code>
     * @see ComboPopup
     */
    @Override
    protected ComboPopup createPopup()
    {
        SIPCommComboPopup popup = new SIPCommComboPopup( comboBox );
        popup.getAccessibleContext().setAccessibleParent(comboBox);

        return popup;
    }

    private static class SIPCommComboPopup extends BasicComboPopup
    {
        private static final long serialVersionUID = 0L;

        public SIPCommComboPopup(JComboBox combo)
        {
            super(combo);
        }

        /**
         * Makes the popup visible if it is hidden and makes it hidden if it is
         * visible.
         */
        @Override
        protected void togglePopup()
        {
            if ( isVisible() )
            {
                hide();
            }
            else
            {
                setListSelection(comboBox.getSelectedIndex());

                Point location = getPopupLocation();
                show( comboBox, location.x, location.y );
            }
        }

        /**
         * Configures the popup portion of the combo box. This method is called
         * when the UI class is created.
         */
        @Override
        protected void configurePopup() {
            setLayout( new BoxLayout( this, BoxLayout.Y_AXIS ) );
            setBorderPainted( true );
            setBorder(SIPCommBorders.getTextFieldBorder());
            setOpaque( false );
            add( scroller );
            setDoubleBuffered( true );
            setFocusable( false );
        }

        /**
         * Sets the list selection index to the selectedIndex. This
         * method is used to synchronize the list selection with the
         * combo box selection.
         *
         * @param selectedIndex the index to set the list
         */
        private void setListSelection(int selectedIndex)
        {
            if ( selectedIndex == -1 )
                list.clearSelection();
            else
            {
                list.setSelectedIndex( selectedIndex );
                list.ensureIndexIsVisible( selectedIndex );
            }
        }

        /**
         * Calculates the upper left location of the Popup.
         * @return the <tt>Point</tt> indicating the location of the popup
         */
        private Point getPopupLocation()
        {
            Dimension popupSize = comboBox.getSize();
            Insets insets = getInsets();

            // reduce the width of the scrollpane by the insets so that the
            // popup is the same width as the combo box.
            int popupHeight = getPopupHeightForRowCount(
                    comboBox.getMaximumRowCount());

            popupSize.setSize(popupSize.width - (insets.right + insets.left),
                              popupHeight);
            Rectangle popupBounds = computePopupBounds(
                    0,
                    comboBox.getEditor().getEditorComponent().getBounds().y
                        - popupHeight - 4,
                    popupSize.width, popupSize.height);

            Dimension scrollSize = popupBounds.getSize();
            Point popupLocation = popupBounds.getLocation();

            scroller.setMaximumSize( scrollSize );
            scroller.setPreferredSize( scrollSize );
            scroller.setMinimumSize( scrollSize );

            list.revalidate();

            return popupLocation;
        }
    }

    /**
     * Creates and initializes the components which make up the
     * aggregate combo box. This method is called as part of the UI
     * installation process.
     */
    @Override
    protected void installComponents()
    {
        if (arrowButton != null)
            configureArrowButton();

        if (comboBox.isEditable())
            addEditor();

        comboBox.add(currentValuePane);
    }

    /**
     * Returns the area that is reserved for drawing the currently selected item.
     * @return the rectangle
     */
    @Override
    protected Rectangle rectangleForCurrentValue()
    {
        int width = comboBox.getWidth();
        int height = comboBox.getHeight();

        Insets insets = getInsets();
        int buttonSize = 0;
        if ( arrowButton != null )
            buttonSize = arrowButton.getWidth();

        if(comboBox.getComponentOrientation().isLeftToRight())
            return new Rectangle(insets.left, insets.top,
                 width - (insets.left + insets.right + buttonSize),
                             height - (insets.top + insets.bottom));
        else
            return new Rectangle(insets.left + buttonSize, insets.top,
                 width - (insets.left + insets.right + buttonSize),
                             height - (insets.top + insets.bottom));
    }
}
