/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.util.swing.plaf;

import java.awt.*;
import java.awt.event.*;

import javax.swing.plaf.metal.*;
import javax.swing.text.*;

import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.skin.*;
import net.java.sip.communicator.util.swing.*;

/**
 * SIPCommTextFieldUI implementation.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class SIPCommTextFieldUI
    extends MetalTextFieldUI
    implements Skinnable
{
    /**
     * Indicates if the mouse is currently over the delete button.
     */
    protected boolean isDeleteMouseOver = false;

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
    protected void installListeners()
    {
        super.installListeners();

        getComponent().addMouseListener(
            new TextFieldMouseListener());

        getComponent().addMouseMotionListener(
            new TextFieldMouseMotionListener());
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
            String roundedSet = UtilActivator.getResources().
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
            g2.setColor(Color.WHITE);

            if(isRounded)
            {
                g2.fillRoundRect(1, 1, c.getWidth() - 2, c.getHeight() - 2,
                        20, 20);
            }
            else
            {
                g2.fillRect(1, 1, c.getWidth() - 2, c.getHeight() - 2);
            }

            Rectangle deleteButtonRect = getDeleteButtonRect();

            int dx = deleteButtonRect.x;
            int dy = deleteButtonRect.y;

            if (c.getText() != null
                    && c.getText().length() > 0
                    && isDeleteButtonEnabled)
            {
                if (isDeleteMouseOver)
                    g2.drawImage(deleteButtonRolloverImg, dx, dy, null);
                else
                    g2.drawImage(deleteButtonImg, dx, dy, null);

                isDeleteIconVisible = true;
            }
            else
                isDeleteIconVisible = false;

            g2.setStroke(new BasicStroke(1f));
            g2.setColor(Color.GRAY);

            if(isRounded)
            {
                g2.drawRoundRect(0, 0, c.getWidth() - 1, c.getHeight() - 1,
                        20, 20);
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
        int x = evt.getX();
        int y = evt.getY();

        if (!isDeleteButtonEnabled)
            return;

        Rectangle deleteRect = getDeleteButtonRect();

        if (isDeleteIconVisible && deleteRect.contains(x, y))
        {
            isDeleteMouseOver = true;
            getComponent().setCursor(Cursor.getDefaultCursor());

            if (evt.getID() == MouseEvent.MOUSE_CLICKED)
                getComponent().setText("");
        }
        else
            isDeleteMouseOver = false;

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

        Rectangle rect = c.getBounds();

        int dx = rect.x + rect.width - deleteButton.getWidth() - BUTTON_GAP - 5;
        int dy = (rect.y + rect.height) / 2 - deleteButton.getHeight()/2;

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
    protected Rectangle getVisibleEditorRect()
    {
        if (!isDeleteButtonEnabled)
        {
            return super.getVisibleEditorRect();
        }

        JTextComponent c = getComponent();

        Rectangle alloc = c.getBounds();

        if ((alloc.width > 0) && (alloc.height > 0))
        {
            alloc.x = alloc.y = 0;
            Insets insets = c.getInsets();
            alloc.x += insets.left;
            alloc.y += insets.top;
            alloc.width -= insets.left + insets.right
                + deleteButton.getWidth();
            alloc.height -= insets.top + insets.bottom;
            return alloc;
        }

        return null;
    }

    /**
     * Reloads skin information.
     */
    public void loadSkin()
    {
        deleteButtonImg = UtilActivator.getResources()
            .getImage("service.gui.lookandfeel.DELETE_TEXT_ICON").getImage();

        deleteButtonRolloverImg = UtilActivator.getResources()
            .getImage("service.gui.lookandfeel.DELETE_TEXT_ROLLOVER_ICON")
                .getImage();

        if(deleteButton != null)
        {
            deleteButton.setBackgroundImage(deleteButtonImg);
            deleteButton.setIconImage(deleteButtonRolloverImg);
        }
        else
        {
            deleteButton = new SIPCommButton(
                    deleteButtonImg, deleteButtonRolloverImg);
        }

        deleteButton.setSize (  deleteButtonImg.getWidth(null),
                                deleteButtonImg.getHeight(null));
    }

    /**
     * The <tt>MouseListener</tt> that listens for mouse events in order to
     * update the delete icon.
     */
    protected class TextFieldMouseListener implements MouseListener
    {
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

        public void mousePressed(MouseEvent e) {}

        public void mouseReleased(MouseEvent e) {}
    }

    /**
     * The <tt>MouseMotionListener</tt> that listens for mouse events in order
     * to update the delete icon.
     */
    protected class TextFieldMouseMotionListener implements MouseMotionListener
    {
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
    }

    private void updateCursor(MouseEvent mouseEvent)
    {
        if (getVisibleEditorRect().contains(mouseEvent.getPoint()))
        {
            getComponent().setCursor(
                Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        }
    }
}
