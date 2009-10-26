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
import net.java.sip.communicator.service.protocol.*;
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
     * Creates a new <code>Component</code> representing a UI means to transfer
     * the <code>Call</code> of the associated <code>callPeer</code> or
     * <tt>null</tt> if call-transfer is unsupported.
     *
     * @param callPeer the <tt>CallPeer</tt>, for which we create the button
     * @return a new <code>Component</code> representing the UI means to
     *         transfer the <code>Call</code> of <code>callPeer</code> or
     *         <tt>null</tt> if call-transfer is unsupported
     */
    public static Component createTransferCallButton(CallPeer callPeer)
    {
        if (callPeer != null)
        {
            OperationSetAdvancedTelephony telephony =
                callPeer.getProtocolProvider()
                    .getOperationSet(OperationSetAdvancedTelephony.class);

            if (telephony != null)
                return new TransferCallButton(callPeer);
        }
        return null;
    }

    /**
     * Creates a new <tt>Component</tt> through which the user would be able to
     * enter in full screen mode.
     *
     * @param renderer the renderer through which we enter in full screen mode
     * @return the newly created component
     */
    public static Component createEnterFullScreenButton(
        final CallPeerRenderer renderer)
    {
        SIPCommButton button =
            new SIPCommButton(ImageLoader
                .getImage(ImageLoader.ENTER_FULL_SCREEN_BUTTON));

        button.setToolTipText(GuiActivator.getResources().getI18NString(
            "service.gui.ENTER_FULL_SCREEN_TOOL_TIP"));
        button.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent event)
            {
                renderer.enterFullScreen();
            }
        });
        return button;
    }

    /**
     * Creates a new <tt>Component</tt> through which the user would be able to
     * exit the full screen mode.
     *
     * @param renderer the renderer through which we exit the full screen mode
     * @return the newly created component
     */
    public static Component createExitFullScreenButton(
            final CallPeerRenderer renderer)
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
                Object source = event.getSource();
                Frame fullScreenFrame =
                    (source instanceof Component) ? TransferCallButton
                        .getFrame((Component) source) : null;

                renderer.exitFullScreen(fullScreenFrame);
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
}
