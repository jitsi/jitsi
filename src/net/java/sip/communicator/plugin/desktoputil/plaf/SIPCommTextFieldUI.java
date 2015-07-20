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
package net.java.sip.communicator.plugin.desktoputil.plaf;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.plaf.metal.*;
import javax.swing.text.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.util.skin.*;

/**
 * SIPCommTextFieldUI implementation.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class SIPCommTextFieldUI
    extends MetalTextFieldUI
    implements Skinnable,
               MouseMotionListener,
               MouseListener
{
    /**
     * Indicates if the mouse is currently over the delete button.
     */
    protected boolean isDeleteMouseOver = false;

    /**
     * Indicates if the mouse is currently pressed on the delete button.
     */
    protected boolean isDeleteMousePressed = false;

    /**
     * The gap between the delete button and the text in the field.
     */
    protected static int BUTTON_GAP = 5;

    /**
     * The image of the delete text button.
     */
    private Image deleteButtonImg;

    /**
     * The rollover image of the delete text button.
     */
    private Image deleteButtonRolloverImg;

    /**
     * The image for the pressed state of the delete text button.
     */
    private Image deleteButtonPressedImg;

    /**
     * Indicates if the text field contains a
     * delete button allowing to delete all the content at once.
     */
    private boolean isDeleteButtonEnabled = false;

    /**
     * The delete text button shown on the right of the field.
     */
    protected SIPCommButton deleteButton;

    /**
     * Indicates if the delete icon is visible.
     */
    private boolean isDeleteIconVisible = false;

    /**
     * The start background gradient color.
     */
    private Color bgStartColor
        = new Color(DesktopUtilActivator.getResources().getColor(
            "service.gui.SEARCH_BACKGROUND"));

    /**
     * The end background gradient color.
     */
    private Color bgEndColor
        = new Color(DesktopUtilActivator.getResources().getColor(
            "service.gui.SEARCH_GRADIENT"));

    /**
     * The start background gradient color.
     */
    private Color bgBorderStartColor
        = new Color(DesktopUtilActivator.getResources().getColor(
            "service.gui.SEARCH_BORDER"));

    /**
     * The end background gradient color.
     */
    private Color bgBorderEndColor
        = new Color(DesktopUtilActivator.getResources().getColor(
            "service.gui.SEARCH_BORDER_GRADIENT"));

    /**
     * Creates a <tt>SIPCommTextFieldUI</tt>.
     */
    public SIPCommTextFieldUI()
    {
        loadSkin();
    }

    /**
     * Returns <code>true</code> if the delete buttons is enabled and false -
     * otherwise.
     * @return <code>true</code> if the delete buttons is enabled and false -
     * otherwise
     */
    public boolean isDeleteButtonEnabled()
    {
        return isDeleteButtonEnabled;
    }

    /**
     * Updates the isDeleteButtonEnabled field.
     *
     * @param isDeleteButtonEnabled indicates if the delete buttons is enabled
     * or not
     */
    public void setDeleteButtonEnabled(boolean isDeleteButtonEnabled)
    {
        this.isDeleteButtonEnabled = isDeleteButtonEnabled;
    }

    /**
     * Adds the custom mouse listeners defined in this class to the installed
     * listeners.
     */
    @Override
    protected void installListeners()
    {
        super.installListeners();

        getComponent().addMouseListener(this);

        getComponent().addMouseMotionListener(this);
    }

    /**
     * Uninstalls listeners for the UI.
     */
    @Override
    protected void uninstallListeners()
    {
        super.uninstallListeners();

        getComponent().removeMouseListener(this);
        getComponent().removeMouseMotionListener(this);
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
            String roundedSet = DesktopUtilActivator.getResources().
            getSettingsString(
                    "impl.gui.IS_SIP_COMM_TEXT_FIELD_ROUNDED");

            boolean isRounded = true;

            if(roundedSet != null)
            {
                isRounded = new Boolean(roundedSet)
                    .booleanValue();
            }

            AntialiasingManager.activateAntialiasing(g2);
            JTextComponent c = this.getComponent();

            GradientPaint bgGradientColor =
                new GradientPaint(  c.getWidth() / 2, 0,
                                    bgStartColor,
                                    c.getWidth() / 2, c.getHeight(),
                                    bgEndColor);

            g2.setPaint(bgGradientColor);

            if(isRounded)
            {
                g2.fillRoundRect(1, 1, c.getWidth() - 1, c.getHeight() - 1,
                        8, 8);
            }
            else
            {
                g2.fillRect(1, 1, c.getWidth() - 1, c.getHeight() - 1);
            }

            Rectangle deleteButtonRect = getDeleteButtonRect();

            if(deleteButtonRect != null)
            {
                int dx = deleteButtonRect.x;
                int dy = deleteButtonRect.y;

                if (c.getText() != null
                        && c.getText().length() > 0
                        && isDeleteButtonEnabled)
                {
                    if (isDeleteMousePressed)
                        g2.drawImage(deleteButtonPressedImg, dx, dy, null);
                    if (isDeleteMouseOver)
                        g2.drawImage(deleteButtonRolloverImg, dx, dy, null);
                    else
                        g2.drawImage(deleteButtonImg, dx, dy, null);

                    isDeleteIconVisible = true;
                }
                else
                    isDeleteIconVisible = false;
            }
            else
                isDeleteIconVisible = false;

            g2.setStroke(new BasicStroke(1f));
            GradientPaint bgBorderGradientColor
                = new GradientPaint(  c.getWidth() / 2, 0,
                                    bgBorderStartColor,
                                    c.getWidth() / 2, c.getHeight(),
                                    bgBorderEndColor);

            g2.setPaint(bgBorderGradientColor);

            if(isRounded)
            {
                g2.drawRoundRect(
                    0, 0, c.getWidth() - 1, c.getHeight() - 1, 8, 8);
            }
            else
            {
                g2.drawRect(0, 0, c.getWidth() - 1, c.getHeight() - 1);
            }
        }
        finally
        {
            g2.dispose();
        }
    }

    /**
     * Updates the delete icon, changes the cursor and deletes the content of
     * the associated text component when the mouse is pressed over the delete
     * icon.
     *
     * @param evt the mouse event that has prompted us to update the delete
     * icon.
     */
    protected void updateDeleteIcon(MouseEvent evt)
    {
        // If the component is null we have nothing more to do here. Fixes a
        // NullPointerException in getDeleteButtonRectangle().
        if (getComponent() == null)
            return;

        int x = evt.getX();
        int y = evt.getY();

        if (!isDeleteButtonEnabled)
            return;

        Rectangle deleteRect = getDeleteButtonRect();

        if (isDeleteIconVisible && deleteRect.contains(x, y))
        {
            if (evt.getID() == MouseEvent.MOUSE_PRESSED)
            {
                isDeleteMouseOver = false;
                isDeleteMousePressed = true;
            }
            else
            {
                isDeleteMouseOver = true;
                isDeleteMousePressed = false;
            }

            getComponent().setCursor(Cursor.getDefaultCursor());

            if (evt.getID() == MouseEvent.MOUSE_CLICKED)
                getComponent().setText("");
        }
        else
        {
            isDeleteMouseOver = false;
            isDeleteMousePressed = false;
        }

        getComponent().repaint();
    }

    /**
     * Calculates the delete button rectangle.
     *
     * @return the delete button rectangle
     */
    protected Rectangle getDeleteButtonRect()
    {
        JTextComponent c = getComponent();

        if(c == null)
            return null;

        Rectangle rect = c.getBounds();

        int dx = rect.width - deleteButton.getWidth() - BUTTON_GAP;
        int dy = rect.height / 2 - deleteButton.getHeight()/2;

        return new Rectangle(   dx,
                                dy,
                                deleteButton.getWidth(),
                                deleteButton.getHeight());
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
        if (!isDeleteIconVisible)
        {
            return super.getVisibleEditorRect();
        }

        JTextComponent c = getComponent();

        if(c == null)
            return null;

        Rectangle alloc = c.getBounds();

        if ((alloc.width > 0) && (alloc.height > 0))
        {
            alloc.x = alloc.y = 0;
            Insets insets = c.getInsets();
            alloc.x += insets.left;
            alloc.y += insets.top;
            alloc.width -= insets.left + insets.right
                + getDeleteButtonRect().getWidth();
            alloc.height -= insets.top + insets.bottom;
            return alloc;
        }

        return null;
    }

    /**
     * @param bgStartColor the bgStartColor to set
     */
    public void setBgStartColor(Color bgStartColor)
    {
        this.bgStartColor = bgStartColor;
    }

    /**
     * @param bgEndColor the bgEndColor to set
     */
    public void setBgEndColor(Color bgEndColor)
    {
        this.bgEndColor = bgEndColor;
    }

    /**
     * @param bgBorderStartColor the bgBorderStartColor to set
     */
    public void setBgBorderStartColor(Color bgBorderStartColor)
    {
        this.bgBorderStartColor = bgBorderStartColor;
    }

    /**
     * @param bgBorderEndColor the bgBorderEndColor to set
     */
    public void setBgBorderEndColor(Color bgBorderEndColor)
    {
        this.bgBorderEndColor = bgBorderEndColor;
    }

    /**
     * Reloads skin information.
     */
    public void loadSkin()
    {
        deleteButtonImg = DesktopUtilActivator.getResources()
            .getImage("service.gui.lookandfeel.DELETE_TEXT_ICON").getImage();

        deleteButtonRolloverImg = DesktopUtilActivator.getResources()
            .getImage("service.gui.lookandfeel.DELETE_TEXT_ROLLOVER_ICON")
                .getImage();

        deleteButtonPressedImg = DesktopUtilActivator.getResources()
            .getImage("service.gui.lookandfeel.DELETE_TEXT_PRESSED_ICON")
                .getImage();

        if(deleteButton != null)
        {
            deleteButton.setBackgroundImage(deleteButtonImg);
            deleteButton.setRolloverImage(deleteButtonRolloverImg);
            deleteButton.setPressedImage(deleteButtonPressedImg);
        }
        else
        {
            deleteButton = new SIPCommButton(
                    deleteButtonImg,
                    deleteButtonRolloverImg,
                    deleteButtonPressedImg,
                    null, null, null);
        }

        deleteButton.setSize (  deleteButtonImg.getWidth(null),
                                deleteButtonImg.getHeight(null));
    }

    /**
     * Updates the delete icon when the mouse was clicked.
     * @param e the <tt>MouseEvent</tt> that notified us of the click
     */
    public void mouseClicked(MouseEvent e)
    {
        updateDeleteIcon(e);
        updateCursor(e);
    }

    /**
     * Updates the delete icon when the mouse is enters the component area.
     * @param e the <tt>MouseEvent</tt> that notified us
     */
    public void mouseEntered(MouseEvent e)
    {
        updateDeleteIcon(e);
        updateCursor(e);
    }

    /**
     * Updates the delete icon when the mouse exits the component area.
     * @param e the <tt>MouseEvent</tt> that notified us
     */
    public void mouseExited(MouseEvent e)
    {
        updateDeleteIcon(e);
        updateCursor(e);
    }

    public void mousePressed(MouseEvent e)
    {
        updateDeleteIcon(e);
    }

    public void mouseReleased(MouseEvent e)
    {
        updateDeleteIcon(e);
    }

    /**
     * Updates the delete icon when the mouse is dragged over.
     * @param e the <tt>MouseEvent</tt> that notified us
     */
    public void mouseDragged(MouseEvent e)
    {
        updateDeleteIcon(e);
        updateCursor(e);
    }

    /**
     * Updates the delete icon when the mouse is moved over.
     * @param e the <tt>MouseEvent</tt> that notified us
     */
    public void mouseMoved(MouseEvent e)
    {
        updateDeleteIcon(e);
        updateCursor(e);
    }

    /**
     * Updates the cursor type depending on a given <tt>MouseEvent</tt>.
     *
     * @param mouseEvent the <tt>MouseEvent</tt> on which the cursor depends
     */
    private void updateCursor(MouseEvent mouseEvent)
    {
        Rectangle rect = getVisibleEditorRect();
        if (rect != null && rect.contains(mouseEvent.getPoint()))
        {
            getComponent().setCursor(
                Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        }
    }

    /**
     * Creates a UI for a SIPCommTextFieldUI.
     *
     * @param c the text field
     * @return the UI
     */
    public static ComponentUI createUI(JComponent c)
    {
        return new SIPCommTextFieldUI();
    }
}
