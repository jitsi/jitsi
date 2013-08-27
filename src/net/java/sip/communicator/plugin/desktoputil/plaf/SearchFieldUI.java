/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.desktoputil.plaf;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.text.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.util.skin.*;

import org.jitsi.service.resources.*;

/**
 * The <tt>SearchTextFieldUI</tt> is the one responsible for the search field
 * look & feel. It draws a search icon inside the field and adjusts the bounds
 * of the editor rectangle according to it.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 * @author Marin Dzhigarov
 */
public class SearchFieldUI
    extends SIPCommTextFieldUI
    implements Skinnable
{
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

    /**
     * The default icon of the call button.
     */
    private Image callIcon;

    /**
     * The pressed icon of the call button.
     */
    private Image callPressedIcon;

    /**
     * The roll over icon of the call button.
     */
    private Image callRolloverIcon;

    /**
     * The call button tool tip string.
     */
    private final String callString = DesktopUtilActivator.getResources()
        .getI18NString("service.gui.CALL");

    /**
     * Indicates if the call button is enabled in this search field.
     */
    protected boolean isCallButtonEnabled = true;

    /**
     * Indicates if the call icon is currently visible.
     */
    protected boolean isCallIconVisible = false;

    /**
     * Indicates if the mouse is currently over the call button.
     */
    private boolean isCallMouseOver = false;

    /**
     * Indicates if the mouse is currently over the call button.
     */
    private boolean isCallMousePressed = false;

    /**
     * The icon indicating that this is a search field.
     */
    private ImageIcon searchIcon;

    /**
     * The separator icon shown between the call icon and the close.
     */
    private Image separatorIcon;

    /**
     * Creates a <tt>GenericSearchFieldUI</tt>.
     */
    public SearchFieldUI()
    {
        isCallButtonEnabled = false;

        loadSkin();
    }

    /**
     * Paints the background of the associated component.
     * @param g the <tt>Graphics</tt> object used for painting
     */
    @Override
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

            g2.drawImage(searchIcon.getImage(), 3, dy, null);

            if (c.getText() != null
                && c.getText().length() > 0
                && isCallButtonEnabled)
            {
                // Paint call button.
                Rectangle callRect = getCallButtonRect();
                int dx = callRect.x;
                dy = callRect.y;

                if (isCallMousePressed)
                    g2.drawImage(callPressedIcon, dx, dy, null);
                else if (isCallMouseOver)
                    g2.drawImage(callRolloverIcon, dx, dy, null);
                else
                    g2.drawImage(callIcon, dx, dy, null);

                g2.drawImage(   separatorIcon,
                                dx + callRect.width + 3,
                                dy + (callRect.height
                                    - separatorIcon.getHeight(null))/2,
                                null);

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
     * Calculates the call button rectangle.
     *
     * @return the call button rectangle
     */
    protected Rectangle getCallButtonRect()
    {
        Component c = getComponent();
        Rectangle rect = c.getBounds();

        int dx = getDeleteButtonRect().x - callRolloverIcon.getWidth(null) - 8;
        int dy = (rect.y + rect.height) / 2 - callRolloverIcon.getHeight(null)/2;

        return new Rectangle(   dx,
                                dy,
                                callRolloverIcon.getWidth(null),
                                callRolloverIcon.getHeight(null));
    }

    /**
     * If we are in the case of disabled delete button, we simply call the
     * parent implementation of this method, otherwise we recalculate the editor
     * rectangle in order to leave place for the delete button.
     * @return the visible editor rectangle
     */
    @Override
    protected Rectangle getVisibleEditorRect()
    {
        Rectangle rect = super.getVisibleEditorRect();

        // Fixes NullPointerException if the rectangle is null for some reason.
        if (rect == null)
            return null;

        if ((rect.width > 0) && (rect.height > 0))
        {
            rect.x += searchIcon.getIconWidth() + 5;
            rect.width -= (searchIcon.getIconWidth() + 5);

            if (isCallIconVisible)
                rect.width -= (callRolloverIcon.getWidth(null) + 12);
            else
                rect.width -= 8;

            return rect;
        }
        return null;
    }

    /**
     * Reloads UI icons.
     */
    @Override
    public void loadSkin()
    {
        super.loadSkin();

        ResourceManagementService r = DesktopUtilActivator.getResources();

        searchIcon = r.getImage("service.gui.icons.SEARCH_ICON");
        if (isCallButtonEnabled)
        {
            callIcon
                = r.getImage("service.gui.buttons.SEARCH_CALL_ICON").getImage();
            callRolloverIcon
                = r.getImage("service.gui.buttons.SEARCH_CALL_ROLLOVER_ICON")
                    .getImage();
            callPressedIcon
                = r.getImage("service.gui.buttons.SEARCH_CALL_PRESSED_ICON")
                    .getImage();
            separatorIcon
                = r.getImage("service.gui.icons.SEARCH_SEPARATOR").getImage();
        }
    }

    /**
     * Updates the call button when the mouse was clicked.
     * @param e the <tt>MouseEvent</tt> that notified us of the click
     */
    @Override
    public void mouseClicked(MouseEvent e)
    {
        super.mouseClicked(e);

        if(isCallButtonEnabled)
            updateCallIcon(e);
    }

    /**
     * Updates the delete icon when the mouse is dragged over.
     * @param e the <tt>MouseEvent</tt> that notified us
     */
    @Override
    public void mouseDragged(MouseEvent e)
    {
        super.mouseDragged(e);

        if(isCallButtonEnabled)
            updateCallIcon(e);
    }

    /**
     * Updates the call button when the mouse is enters the component area.
     * @param e the <tt>MouseEvent</tt> that notified us
     */
    @Override
    public void mouseEntered(MouseEvent e)
    {
        super.mouseEntered(e);

        if(isCallButtonEnabled)
            updateCallIcon(e);
    }

    /**
     * Updates the call button when the mouse exits the component area.
     * @param e the <tt>MouseEvent</tt> that notified us
     */
    @Override
    public void mouseExited(MouseEvent e)
    {
        super.mouseExited(e);

        if(isCallButtonEnabled)
            updateCallIcon(e);
    }

    /**
     * Updates the delete icon when the mouse is moved over.
     * @param e the <tt>MouseEvent</tt> that notified us
     */
    @Override
    public void mouseMoved(MouseEvent e)
    {
        super.mouseMoved(e);

        if(isCallButtonEnabled)
            updateCallIcon(e);
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
        super.mousePressed(e);

        if(isCallButtonEnabled)
            updateCallIcon(e);
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {
        super.mouseReleased(e);

        if(isCallButtonEnabled)
            updateCallIcon(e);
    }

    /**
     * Implements parent paintSafely method and enables antialiasing.
     * @param g the <tt>Graphics</tt> object that notified us
     */
    @Override
    protected void paintSafely(Graphics g)
    {
        customPaintBackground(g);
        super.paintSafely(g);
    }

    /**
     * Enables/disabled the call button in the search field.
     *
     * @param isEnabled indicates if the call button is enabled
     */
    public void setCallButtonEnabled(boolean isEnabled)
    {
        this.isCallButtonEnabled = isEnabled;
    }

    /**
     * Updates the delete icon, changes the cursor and deletes the content of
     * the associated text component when the mouse is pressed over the delete
     * icon.
     *
     * @param evt the mouse event that has prompted us to update the delete
     * icon.
     */
    protected void updateCallIcon(MouseEvent evt)
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

            if (evt.getID() == MouseEvent.MOUSE_PRESSED)
            {
                isCallMouseOver = false;
                isCallMousePressed = true;
            }
            else
            {
                isCallMouseOver = true;
                isCallMousePressed = false;
            }

            // Update the default cursor.
            getComponent().setCursor(Cursor.getDefaultCursor());
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
            isCallMousePressed = false;
        }

        getComponent().repaint();
    }
}
