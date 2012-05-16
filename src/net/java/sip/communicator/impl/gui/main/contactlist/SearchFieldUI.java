/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.text.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.call.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.skin.*;
import net.java.sip.communicator.util.swing.*;
import net.java.sip.communicator.util.swing.plaf.*;

/**
 * The <tt>SearchTextFieldUI</tt> is the one responsible for the search field
 * look & feel. It draws a search icon inside the field and adjusts the bounds
 * of the editor rectangle according to it.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class SearchFieldUI
    extends SIPCommTextFieldUI
    implements Skinnable,
               MouseMotionListener,
               MouseListener
{
    /**
     * The icon indicating that this is a search field.
     */
    private ImageIcon searchIcon;

    /**
     * The default icon of the call button.
     */
    private Image callIcon;

    /**
     * The roll over icon of the call button.
     */
    private Image callRolloverIcon;

    /**
     * Indicates if the mouse is currently over the call button.
     */
    private boolean isCallMouseOver = false;

    /**
     * The call button tool tip string.
     */
    private final String callString
        = GuiActivator.getResources().getI18NString("service.gui.CALL");

    /**
     * Indicates if the call icon is currently visible.
     */
    private boolean isCallIconVisible = false;

    /**
     * Indicates if the call button is enabled in this search field.
     */
    private boolean isCallButtonEnabled = true;

    /**
     * Creates a <tt>SIPCommTextFieldUI</tt>.
     */
    public SearchFieldUI()
    {
        // Indicates if the big call button outside the search is enabled.
        String callButtonEnabledString = UtilActivator.getResources()
            .getSettingsString("impl.gui.CALL_BUTTON_ENABLED");

        if (callButtonEnabledString != null
                && callButtonEnabledString.length() > 0)
        {
            // If the outside call button is enabled the call button in this
            // search field is disabled.
            isCallButtonEnabled
                = !new Boolean(callButtonEnabledString).booleanValue();
        }

        loadSkin();
    }

    /**
     * Adds the custom mouse listeners defined in this class to the installed
     * listeners.
     */
    protected void installListeners()
    {
        super.installListeners();

        if (isCallButtonEnabled)
        {
            getComponent().addMouseListener(this);

            getComponent().addMouseMotionListener(this);
        }
    }

    /**
     * Uninstalls listeners for the UI.
     */
    protected void uninstallListeners()
    {
        super.uninstallListeners();

        getComponent().removeMouseListener(this);
        getComponent().removeMouseMotionListener(this);
    }

    /**
     * Implements parent paintSafely method and enables antialiasing.
     * @param g the <tt>Graphics</tt> object that notified us
     */
    protected void paintSafely(Graphics g)
    {
        customPaintBackground(g);
        super.paintSafely(g);
    }

    /**
     * Paints the background of the associated component.
     * @param g the <tt>Graphics</tt> object used for painting
     */
    protected void customPaintBackground(Graphics g)
    {
        Graphics2D g2 = (Graphics2D) g.create();

        try
        {
            AntialiasingManager.activateAntialiasing(g2);
            super.customPaintBackground(g2);

            JTextComponent c = this.getComponent();

            int dy = (c.getY() + c.getHeight()) / 2
                - searchIcon.getIconHeight()/2;

            g2.drawImage(searchIcon.getImage(), 5, dy + 1, null);

            if (c.getText() != null
                && c.getText().length() > 0
                && CallManager.getTelephonyProviders().size() > 0
                && isCallButtonEnabled)
            {
                // Paint call button.
                Rectangle callRect = getCallButtonRect();
                int dx = callRect.x;
                dy = callRect.y;

                if (isCallMouseOver)
                    g2.drawImage(callRolloverIcon, dx, dy, null);
                else
                    g2.drawImage(callIcon, dx, dy, null);

                isCallIconVisible = true;
            }
            else
                isCallIconVisible = false;

        }
        finally
        {
            g2.dispose();
        }
    }

    /**
     * If we are in the case of disabled delete button, we simply call the
     * parent implementation of this method, otherwise we recalculate the editor
     * rectangle in order to leave place for the delete button.
     * @return the visible editor rectangle
     */
    protected Rectangle getVisibleEditorRect()
    {
        Rectangle rect = super.getVisibleEditorRect();

        // Fixes NullPointerException if the rectangle is null for some reason.
        if (rect == null)
            return null;

        if ((rect.width > 0) && (rect.height > 0))
        {
            rect.x += searchIcon.getIconWidth() + 8;
            rect.width -= searchIcon.getIconWidth() + 8;

            if (isCallButtonEnabled)
                rect.width -= callRolloverIcon.getWidth(null) + 12;
            else
                rect.width -= 8;

            return rect;
        }
        return null;
    }

    /**
     * Updates the call button when the mouse was clicked.
     * @param e the <tt>MouseEvent</tt> that notified us of the click
     */
    public void mouseClicked(MouseEvent e)
    {
        updateCallIcon(e);
    }

    /**
     * Updates the call button when the mouse is enters the component area.
     * @param e the <tt>MouseEvent</tt> that notified us
     */
    public void mouseEntered(MouseEvent e)
    {
        updateCallIcon(e);
    }

    /**
     * Updates the call button when the mouse exits the component area.
     * @param e the <tt>MouseEvent</tt> that notified us
     */
    public void mouseExited(MouseEvent e)
    {
        updateCallIcon(e);
    }

    public void mousePressed(MouseEvent e) {}

    public void mouseReleased(MouseEvent e) {}

    /**
     * Updates the delete icon when the mouse is dragged over.
     * @param e the <tt>MouseEvent</tt> that notified us
     */
    public void mouseDragged(MouseEvent e)
    {
        updateCallIcon(e);
    }

    /**
     * Updates the delete icon when the mouse is moved over.
     * @param e the <tt>MouseEvent</tt> that notified us
     */
    public void mouseMoved(MouseEvent e)
    {
        updateCallIcon(e);
    }

    /**
     * Updates the delete icon, changes the cursor and deletes the content of
     * the associated text component when the mouse is pressed over the delete
     * icon.
     *
     * @param evt the mouse event that has prompted us to update the delete
     * icon.
     */
    private void updateCallIcon(MouseEvent evt)
    {
        int x = evt.getX();
        int y = evt.getY();

        Rectangle callButtonRect = getCallButtonRect();

        if (isCallIconVisible && callButtonRect.contains(x, y))
        {
            JTextComponent c = getComponent();
            String searchText = c.getText();

            if (searchText == null)
                return;

            // Show a tool tip over the call button.
            getComponent().setToolTipText(callString + " " + searchText);
            ToolTipManager.sharedInstance().mouseEntered(
                new MouseEvent(c, 0, x, y,
                        x, y, // X-Y of the mouse for the tool tip
                        0, false));

            // Update the default cursor.
            isCallMouseOver = true;
            getComponent().setCursor(Cursor.getDefaultCursor());

            // Perform call action when the call button is clicked.
            if (evt.getID() == MouseEvent.MOUSE_CLICKED)
            {
                CallManager.createCall(searchText, c);
            }
        }
        else
        {
            // Remove the call button tool tip when the mouse exits the call
            // button area.
            getComponent().setToolTipText("");
            ToolTipManager.sharedInstance().mouseExited(
                new MouseEvent(getComponent(), 0, x, y,
                        x, y, // X-Y of the mouse for the tool tip
                        0, false));

            isCallMouseOver = false;
        }

        getComponent().repaint();
    }

    /**
     * Calculates the call button rectangle.
     *
     * @return the call button rectangle
     */
    protected Rectangle getCallButtonRect()
    {
        Component c = getComponent();
        Rectangle rect = c.getBounds();

        int dx = getDeleteButtonRect().x - callRolloverIcon.getWidth(null) - 2;
        int dy = (rect.y + rect.height) / 2 - callRolloverIcon.getHeight(null)/2;

        return new Rectangle(   dx,
                                dy,
                                callRolloverIcon.getWidth(null),
                                callRolloverIcon.getHeight(null));
    }

    /**
     * Reloads UI icons.
     */
    public void loadSkin()
    {
        super.loadSkin();

        searchIcon = UtilActivator.getResources()
            .getImage("service.gui.icons.SEARCH_ICON");

        if (isCallButtonEnabled)
        {
            callIcon = UtilActivator.getResources()
                .getImage("service.gui.buttons.SEARCH_CALL_ICON").getImage();

            callRolloverIcon = UtilActivator.getResources()
                .getImage("service.gui.buttons.SEARCH_CALL_ROLLOVER_ICON")
                    .getImage();
        }
    }

    /**
     * Creates a UI for a SearchFieldUI.
     *
     * @param c the text field
     * @return the UI
     */
    public static ComponentUI createUI(JComponent c)
    {
        return new SearchFieldUI();
    }
}