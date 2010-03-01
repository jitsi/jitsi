/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util.swing;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import org.jvnet.lafwidget.animation.*;

/**
 * The <tt>SIPCommMenu</tt> is very similar to a JComboBox. The main
 * component here is a JLabel only with an icon. When user clicks on the icon a
 * popup menu is opened, containing a list of icon-text pairs from which the
 * user could choose one item. When user selects the desired item, the icon of
 * the selected item is set to the main component label.
 * 
 * @author Yana Stamcheva
 */
public class SIPCommMenu
    extends JMenu
{
    private static final long serialVersionUID = 1L;
    private Object selectedObject;

    /**
     * Creates an instance of <tt>SIPCommMenu</tt>.
     */
    public SIPCommMenu()
    {
        super();
        init();
    }

    /**
     * Creates an instance of <tt>SIPCommMenu</tt> by specifying
     * the text and the icon.
     */
    public SIPCommMenu(String text, Icon defaultIcon)
    {
        super(text);

        this.setIcon(defaultIcon);
        init();
    }

    /**
     * Creates an instance of <tt>SIPCommMenu</tt> by specifying the
     * initialy selected item.
     * 
     * @param text The item that is initialy selected.
     */
    public SIPCommMenu(String text)
    {
        super(text);
        init();
    }

    private void init()
    {
        MouseRolloverHandler mouseHandler = new MouseRolloverHandler();

        this.addMouseListener(mouseHandler);
        this.addMouseMotionListener(mouseHandler);
    }

    /**
     * Adds an item to the "choice list" of this selector box.
     * 
     * @param text The text of the item.
     * @param icon The icon of the item.
     * @param actionListener The <tt>ActionListener</tt>, which handles the
     * case, when the item is selected.
     */
    public void addItem(String text, Icon icon, ActionListener actionListener)
    {
        JMenuItem item = new JMenuItem(text, icon);

        item.addActionListener(actionListener);

        this.add(item);
    }

    /**
     * Selects the given item.
     * 
     * @param selectedObject The object to select.
     */
    public void setSelected(SelectedObject selectedObject)
    {
        if (selectedObject.getIcon() != null)
            this.setIcon(selectedObject.getIcon());

        if (selectedObject.getText() != null)
            this.setText(selectedObject.getText());

        if (selectedObject.getObject() != null)
            this.setSelectedObject(selectedObject.getObject());
    }

    /**
     * Selects the given object.
     * 
     * @param o The <tt>Object</tt> to select.
     */
    public void setSelectedObject(Object o)
    {
        this.selectedObject = o;
    }

    /**
     * Returns the selected object.
     * 
     * @return the selected object.
     */
    public Object getSelectedObject()
    {
        return this.selectedObject;
    }

    /**
     * Sets the isMouseOver property value and repaints this component.
     * 
     * @param isMouseOver <code>true</code> to indicate that the mouse is over
     * this component, <code>false</code> - otherwise.
     */
    public void setMouseOver(boolean isMouseOver)
    {
        this.repaint();
    }

    /**
     * Paints this component.
     * @param g the <tt>Graphics</tt> object used for painting
     */
    public void paintComponent(Graphics g)
    {
        g = g.create();
        try
        {
            internalPaintComponent(g);
        }
        finally
        {
            g.dispose();
        }
    }

    /**
     * Paints a rollover effect when the mouse is over this menu.
     * @param g the <tt>Graphics</tt> object used for painting
     */
    private void internalPaintComponent(Graphics g)
    {
        AntialiasingManager.activateAntialiasing(g);

        // Paint a roll over fade out.
        FadeTracker fadeTracker = FadeTracker.getInstance();

        float visibility = getModel().isRollover() ? 1.0f : 0.0f;
        if (fadeTracker.isTracked(this, FadeKind.ROLLOVER))
        {
            visibility = fadeTracker.getFade(this, FadeKind.ROLLOVER);
        }

        visibility /= 2;

        g.setColor(new Color(1.0f, 1.0f, 1.0f, visibility));

        g.fillRoundRect(0, 0, this.getWidth(), this.getHeight(), 20, 20);

        g.setColor(UIManager.getColor("Menu.foreground"));

        super.paintComponent(g);
    }

    /**
     * The <tt>ButtonRepaintCallback</tt> is charged to repaint this button
     * when the fade animation is performed.
     */
    private class ButtonRepaintCallback implements FadeTrackerCallback
    {
        public void fadeEnded(FadeKind arg0)
        {
            repaintLater();
        }

        public void fadePerformed(FadeKind arg0, float arg1)
        {
            repaintLater();
        }

        private void repaintLater()
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    SIPCommMenu.this.repaint();
                }
            });
        }

        public void fadeReversed(FadeKind arg0, boolean arg1, float arg2)
        {
        }
    }

    /**
     * Perform a fade animation on mouse over.
     */
    private class MouseRolloverHandler
        implements  MouseListener,
                    MouseMotionListener
    {
        public void mouseMoved(MouseEvent e)
        {
        }

        public void mouseExited(MouseEvent e)
        {
            if (isEnabled())
            {
                getModel().setRollover(false);

                FadeTracker fadeTracker = FadeTracker.getInstance();

                fadeTracker.trackFadeOut(FadeKind.ROLLOVER,
                    SIPCommMenu.this,
                    true,
                    new ButtonRepaintCallback());
            }
        }

        public void mouseClicked(MouseEvent e)
        {
        }

        public void mouseEntered(MouseEvent e)
        {
            if (isEnabled())
            {
                getModel().setRollover(true);

                FadeTracker fadeTracker = FadeTracker.getInstance();

                fadeTracker.trackFadeIn(FadeKind.ROLLOVER,
                    SIPCommMenu.this,
                    true,
                    new ButtonRepaintCallback());
            }
        }

        public void mousePressed(MouseEvent e)
        {
        }

        public void mouseReleased(MouseEvent e)
        {
        }

        public void mouseDragged(MouseEvent e)
        {
        }
    }
}
