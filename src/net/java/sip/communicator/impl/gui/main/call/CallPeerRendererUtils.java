/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.util.swing.*;

/**
 * An utility class that reassembles common methods used by different
 * <tt>CallPeerRenderer</tt>s.
 *
 * @author Lubomir Marinov
 * @author Yana Stamcheva
 */
public class CallPeerRendererUtils
{
    /**
     * Creates a new <tt>Component</tt> through which the user would be able to
     * exit the full screen mode.
     *
     * @param renderer the renderer through which we exit the full screen mode
     * @return the newly created component
     */
    public static Component createExitFullScreenButton(
            final CallRenderer renderer)
    {
        JButton button =
            new SIPCommButton(
                ImageLoader.getImage(ImageLoader.FULL_SCREEN_BUTTON_BG),
                ImageLoader.getImage(ImageLoader.EXIT_FULL_SCREEN_BUTTON));

        button.setToolTipText(GuiActivator.getResources().getI18NString(
            "service.gui.EXIT_FULL_SCREEN_TOOL_TIP"));
        button.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent event)
            {
                renderer.exitFullScreen();
            }
        });
        return button;
    }

    /**
     * Sets the given <tt>background</tt> color to the given <tt>component</tt>.
     *
     * @param component the component to which we set the background
     * @param background the background color to set
     */
    public static void setBackground(Component component, Color background)
    {
        component.setBackground(background);
        if (component instanceof Container)
        {
            Component[] components = ((Container) component).getComponents();
            for (Component c : components)
                setBackground(c, background);
        }
    }

    /**
     * Adds the given <tt>KeyListener l</tt> to the given <tt>component</tt>.
     *
     * @param component the component to which we add <tt>l</tt>
     * @param l the <tt>KeyListener</tt> to add
     */
    public static void addKeyListener(Component component, KeyListener l)
    {
        component.addKeyListener(l);
        if (component instanceof Container)
        {
            Component[] components = ((Container) component).getComponents();
            for (Component c : components)
                addKeyListener(c, l);
        }
    }

    /**
     * Creates a buttons bar from the given list of button components.
     *
     * @param heavyweight indicates if the created button bar should be heavy
     * weight component (useful in full screen mode)
     * @param buttons the list of buttons to add in the created button bar
     * @return the created button bar
     */
    public static Component createButtonBar(boolean heavyweight,
                                            Component[] buttons)
    {
        Container buttonBar
            = heavyweight ? new Container() : new TransparentPanel();

        buttonBar.setLayout(new FlowLayout(FlowLayout.CENTER, 3, 3));

        for (Component button : buttons)
        {
            if (button != null)
                buttonBar.add(button);
        }
        return buttonBar;
    }

    /**
     * Gets the first <tt>Frame</tt> in the ancestor <tt>Component</tt>
     * hierarchy of a specific <tt>Component</tt>.
     * <p>
     * The located <tt>Frame</tt> (if any) is often used as the owner of
     * <tt>Dialog</tt>s opened by the specified <tt>Component</tt> in
     * order to provide natural <tt>Frame</tt> ownership.
     *
     * @param component the <tt>Component</tt> which is to have its
     * <tt>Component</tt> hierarchy examined for <tt>Frame</tt>
     * @return the first <tt>Frame</tt> in the ancestor
     * <tt>Component</tt> hierarchy of the specified <tt>Component</tt>;
     * <tt>null</tt>, if no such <tt>Frame</tt> was located
     */
    public static Frame getFrame(Component component)
    {
        while (component != null)
        {
            Container container = component.getParent();

            if (container instanceof Frame)
                return (Frame) container;

            component = container;
        }
        return null;
    }
}
