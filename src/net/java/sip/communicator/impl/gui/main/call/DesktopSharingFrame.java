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
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.beans.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.plugin.desktoputil.transparent.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.Logger;

import org.jitsi.service.neomedia.*;
import org.jitsi.util.*;

/**
 *
 * @author Yana Stamcheva
 */
public class DesktopSharingFrame
{
    /**
     * Used for logging.
     */
    private static final Logger logger
        = Logger.getLogger(DesktopSharingFrame.class);

    /**
     * The icon shown to indicate the resize drag area.
     */
    private static final ImageIcon resizeIcon
        = GuiActivator.getResources().getImage(
            "service.gui.icons.WINDOW_RESIZE_ICON");

    /**
     * The indent of the sharing region from the frame.
     */
    private static int SHARING_REGION_INDENT = 2;

    /**
     * The x coordinate of the frame, which started the regional sharing.
     */
    private static int initialFrameX = -1;

    /**
     * The y coordinate of the frame, which started the regional sharing.
     */
    private static int initialFrameY = -1;

    /**
     * The width of the sharing region, which started the sharing.
     */
    private static int sharingRegionWidth = -1;

    /**
     * The height of the sharing region, which started the sharing.
     */
    private static int sharingRegionHeight = -1;

    /**
     * A mapping of a desktop sharing frame created by this class and a call.
     */
    private static final Map<Call, JFrame> callDesktopFrames
        = new Hashtable<Call, JFrame>();

    /**
     * Creates the transparent desktop sharing frame.
     *
     * @param protocolProvider the protocol provider, through which the desktop
     * sharing will pass
     * @param contactAddress the address of the contact to call
     * @param uiContact the <tt>UIContactImpl</tt> for which we create a
     * desktop sharing frame
     * @param initialFrame indicates if this is the frame which initiates the
     * desktop sharing
     * @return the created desktop sharing frame
     */
    public static TransparentFrame createTransparentFrame(
                                    ProtocolProviderService protocolProvider,
                                    String contactAddress,
                                    UIContactImpl uiContact,
                                    boolean initialFrame)
    {
        TransparentFrame frame = TransparentFrame.createTransparentFrame();

        initContentPane(frame, initialFrame);

        JComponent sharingRegion = createSharingRegion(initialFrame);

        frame.getContentPane().add(sharingRegion, BorderLayout.NORTH);

        JPanel buttonPanel = initButtons(
            frame, sharingRegion, initialFrame, null,
            protocolProvider, contactAddress, uiContact);

        frame.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        frame.pack();

        return frame;
    }

    /**
     * Creates the transparent desktop sharing frame.
     *
     * @param call the current call
     * @param initialFrame indicates if this is the frame which initiates the
     * desktop sharing
     * @return the created desktop sharing frame
     */
    public static TransparentFrame createTransparentFrame(
                                                        Call call,
                                                        boolean initialFrame)
    {
        TransparentFrame frame = TransparentFrame.createTransparentFrame();

        initContentPane(frame, initialFrame);

        JComponent sharingRegion = createSharingRegion(initialFrame);

        frame.getContentPane().add(sharingRegion, BorderLayout.NORTH);

        JPanel buttonPanel = initButtons(
            frame, sharingRegion, initialFrame, call, null, null, null);

        frame.getContentPane().add(buttonPanel, BorderLayout.SOUTH);

        // If the desktop sharing has started we store the frame to call mapping.
        if (!initialFrame)
        {
            callDesktopFrames.put(call, frame);
            addCallListener(call, frame);
            addFrameListener(call, frame, sharingRegion);
            addDesktopSharingListener(call, frame);

            logger.info("The sharing region width: " + sharingRegionWidth);

            if (sharingRegionWidth > -1 && sharingRegionHeight > -1)
                sharingRegion.setPreferredSize(
                    new Dimension(sharingRegionWidth, sharingRegionHeight));

            frame.pack();

            if (initialFrameX != -1 || initialFrameY != -1)
                frame.setLocation(initialFrameX, initialFrameY);
            else
                // By default we position the frame in the center of the screen.
                // It's important to call this method after the pack(), because
                // it requires the frame size to calculate the location.
                frame.setLocationRelativeTo(null);
        }
        else
        {
            frame.pack();

            // By default we position the frame in the center of the screen.
            // It's important to call this method after the pack(), because
            // it requires the frame size to calculate the location.
            frame.setLocationRelativeTo(null);
        }

        return frame;
    }

    /**
     * Get the frame for a <tt>Call</tt>.
     *
     * @param call the <tt>Call</tt>
     * @return JFrame for the call or null if not found
     */
    public static JFrame getFrameForCall(Call call)
    {
        return callDesktopFrames.get(call);
    }

    /**
     * Adds a call listener, which listens for call ended events and would
     * close any related desktop sharing frames when a call is ended.
     *
     * @param call the call, for which we're registering a listener
     * @param frame the frame to be closed on call ended
     */
    private static void addCallListener(Call call, JFrame frame)
    {
        OperationSetBasicTelephony<?> basicTelephony
            = call.getProtocolProvider().getOperationSet(
                    OperationSetBasicTelephony.class);

        if (basicTelephony != null) // This should always be true.
        {
            basicTelephony.addCallListener(
                    new CallListener()
                    {
                        /**
                         * Implements {@link CallListener#callEnded(CallEvent)}.
                         * Disposes of the frame related to the ended call.
                         *
                         * @param ev a <tt>CallEvent</tt> which identifies the
                         * ended call 
                         */
                        public void callEnded(CallEvent ev)
                        {
                            Call call = ev.getSourceCall();
                            JFrame desktopFrame = callDesktopFrames.get(call);

                            if (desktopFrame != null)
                            {
                                desktopFrame.dispose();
                                callDesktopFrames.remove(call);
                            }
                        }

                        public void incomingCallReceived(CallEvent ev) {}

                        public void outgoingCallCreated(CallEvent ev) {}
                    });
        }
    }

    /**
     * Adds the desktop sharing listener.
     *
     * @param call the call, for which we're registering a listener
     * @param frame the frame to be closed on call ended
     */
    private static void addDesktopSharingListener(final Call call, JFrame frame)
    {
        OperationSetVideoTelephony videoTelephony
            = call.getProtocolProvider().getOperationSet(
                    OperationSetVideoTelephony.class);

        videoTelephony.addPropertyChangeListener(
                call,
                new PropertyChangeListener()
                {
                    public void propertyChange(PropertyChangeEvent ev)
                    {
                        if (OperationSetVideoTelephony.LOCAL_VIDEO_STREAMING
                                    .equals(ev.getPropertyName())
                                && MediaDirection.RECVONLY.equals(
                                        ev.getNewValue()))
                        {
                            JFrame desktopFrame = callDesktopFrames.get(call);

                            if (desktopFrame != null)
                            {
                                desktopFrame.dispose();
                                callDesktopFrames.remove(call);
                            }
                        }
                    }
                });
    }

    /**
     * Initializes the content pane of the given window.
     *
     * @param frame the parent frame
     * @param initialFrame indicates if this is the frame which initiates the
     * desktop sharing
     */
    private static void initContentPane(JFrame frame, boolean initialFrame)
    {
        JPanel contentPane = new JPanel()
        {
            /**
             * Serial version UID.
             */
            public static final long serialVersionUID = 0L;

            @Override
            protected void paintComponent(Graphics g)
            {
                super.paintComponent(g);

                g = g.create();

                try
                {
                    AntialiasingManager.activateAntialiasing(g);

                    Graphics2D g2d = (Graphics2D)g;

                    g2d.setStroke(new BasicStroke(4));

                    if (TransparentFrame.isTranslucencySupported)
                    {
                        g2d.setColor(new Color( Color.DARK_GRAY.getRed(),
                                                Color.DARK_GRAY.getGreen(),
                                                Color.DARK_GRAY.getBlue(), 180));

                        g2d.drawRoundRect(
                            0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
                    }
                    else
                    {
                        g.setColor(Color.DARK_GRAY);
                        g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
                    }
                }
                finally
                {
                    g.dispose();
                }
            }
        };

        contentPane.setOpaque(false);
        contentPane.setDoubleBuffered(false);
        contentPane.setLayout(new BorderLayout());
        contentPane.setBorder(BorderFactory.createEmptyBorder(
            SHARING_REGION_INDENT, SHARING_REGION_INDENT,
            SHARING_REGION_INDENT, SHARING_REGION_INDENT));

        frame.setContentPane(contentPane);
        ComponentMover.registerComponent(frame);

        // On Linux transparency is supported but mouse events do not pass
        // through.
        if (TransparentFrame.isTranslucencySupported
            && !OSUtils.IS_LINUX)
            frame.setAlwaysOnTop(true);
    }

    /**
     * Creates the sharing region.
     *
     * @param initialFrame indicates if this is the frame which initiates the
     * desktop sharing
     * @return the created sharing region
     */
    private static JComponent createSharingRegion(boolean initialFrame)
    {
        JComponent sharingRegion = new TransparentPanel(new BorderLayout());

        // The preferred width on MacOSX should be a multiple of 16, that's why
        // we put 592 as a default width. On the other hand the width on
        // Windows or Linux should be preferably a multiple of 2.
        if (OSUtils.IS_MAC)
            sharingRegion.setPreferredSize(new Dimension(592, 400));
        else
            sharingRegion.setPreferredSize(new Dimension(600, 400));

        sharingRegion.setDoubleBuffered(false);

        if (!TransparentFrame.isTranslucencySupported
            && !initialFrame)
        {
            JLabel label = new JLabel(GuiActivator.getResources()
                .getI18NString("service.gui.DRAG_FOR_SHARING"), JLabel.CENTER);

            label.setForeground(Color.GRAY);
            sharingRegion.add(label);
        }

        return sharingRegion;
    }

    /**
     * Creates and initializes the button panel.
     *
     * @param frame the parent frame
     * @param sharingRegion the sharing region component
     * @param initialFrame indicates if this is the frame which initiates the
     * desktop sharing
     * @param call the current call, if we're in a call
     * @param protocolProvider the protocol provider
     * @param contact the contact, which is the receiver of the call
     * @param uiContact the <tt>UIContactImpl</tt> for which we create the
     * desktop sharing frame
     *
     * @return the created button panel
     */
    private static JPanel initButtons(
                                final JFrame frame,
                                final JComponent sharingRegion,
                                boolean initialFrame,
                                final Call call,
                                final ProtocolProviderService protocolProvider,
                                final String contact,
                                final UIContactImpl uiContact)
    {
        JPanel buttonPanel = new JPanel(new GridBagLayout())
        {
            /**
             * Serial version UID.
             */
            public static final long serialVersionUID = 0L;

            @Override
            public void paintComponent(Graphics g)
            {
                // We experience some problems making this component
                // semi-transparent on Linux.
                if (!TransparentFrame.isTranslucencySupported
                    || OSUtils.IS_LINUX)
                {
                    super.paintComponent(g);
                    return;
                }

                // On all other operating systems supporting transparency we'll
                // make this component semi-transparent.
                Graphics2D g2d = (Graphics2D) g.create();

                AntialiasingManager.activateAntialiasing(g2d);

                g2d.setColor(new Color( Color.DARK_GRAY.getRed(),
                                        Color.DARK_GRAY.getGreen(),
                                        Color.DARK_GRAY.getBlue(), 180));

                GeneralPath shape = new GeneralPath();
                int x = -SHARING_REGION_INDENT + 2;
                int y = 0;
                int width = getWidth() + SHARING_REGION_INDENT*2 - 4;
                int height = getHeight() + SHARING_REGION_INDENT*2 - 2;

                shape.moveTo(x, y);
                shape.lineTo(width, y);
                shape.lineTo(width, height - 12);
                shape.curveTo(width, height - 12,
                                width, height,
                                width - 12, height);
                shape.lineTo(12, height);
                shape.curveTo(12, height,
                                x, height,
                                x, height - 12);
                shape.lineTo(x, y);
                shape.closePath();

                g2d.fill(shape);
                g2d.setColor(getBackground());
            }
        };

        GridBagConstraints constraints = new GridBagConstraints();

        // We experience some problems making this component semi-transparent on
        // Linux.
        if (TransparentFrame.isTranslucencySupported
            && !OSUtils.IS_LINUX)
        {
            buttonPanel.setOpaque(false);
        }
        else
        {
            buttonPanel.setBackground(Color.DARK_GRAY);
        }

        buttonPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        buttonPanel.setPreferredSize(
            new Dimension(sharingRegion.getWidth(), 30));

        if (initialFrame)
        {
            JButton startButton = createButton(
                GuiActivator.getResources()
                    .getI18NString("service.gui.START_SHARING"));

            JButton cancelButton = createButton(
                GuiActivator.getResources()
                    .getI18NString("service.gui.CANCEL"));

            constraints.gridx = 0;
            constraints.gridy = 0;
            constraints.weightx = 1.0;
            constraints.insets = new Insets(0, 0, 0, 5);
            constraints.anchor = GridBagConstraints.EAST;
            buttonPanel.add(cancelButton, constraints);

            constraints.gridx = 1;
            constraints.gridy = 0;
            constraints.weightx = 1.0;
            constraints.anchor = GridBagConstraints.WEST;
            buttonPanel.add(startButton, constraints);

            constraints.gridx = 3;
            constraints.gridy = 0;
            constraints.weightx = 0;
            constraints.insets = new Insets(0, 0, 0, 0);
            constraints.anchor = GridBagConstraints.SOUTHWEST;
            buttonPanel.add(
                createResizeLabel(frame, sharingRegion, buttonPanel),
                constraints);

            startButton.setCursor(Cursor.getDefaultCursor());
            cancelButton.setCursor(Cursor.getDefaultCursor());

            cancelButton.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent arg0)
                {
                    frame.dispose();
                }
            });

            startButton.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    Point location = sharingRegion.getLocationOnScreen();
                    sharingRegionWidth = sharingRegion.getWidth();
                    sharingRegionHeight = sharingRegion.getHeight();
                    initialFrameX = frame.getX();
                    initialFrameY = frame.getY();

                    frame.dispose();

                    if (call != null)
                        CallManager.enableRegionDesktopSharing(
                                call,
                                location.x,
                                location.y,
                                sharingRegionWidth,
                                sharingRegionHeight);
                    else
                        CallManager.createRegionDesktopSharing(
                                protocolProvider,
                                contact,
                                uiContact,
                                location.x,
                                location.y,
                                sharingRegionWidth,
                                sharingRegionHeight);
                }
            });
        }
        else
        {
            JButton stopButton = createButton(
                GuiActivator.getResources()
                    .getI18NString("service.gui.STOP_SHARING"));

            buttonPanel.add(stopButton);

            stopButton.setCursor(Cursor.getDefaultCursor());

            stopButton.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    if (call != null)
                        CallManager.enableDesktopSharing(call, false);

                    frame.dispose();
                }
            });
        }

        return buttonPanel;
    }

    /**
     * Creates a button with the given text.
     *
     * @param text the text of the button
     * @return the created button
     */
    private static JButton createButton(String text)
    {
        JButton button = new JButton(text);

        button.setOpaque(false);

        return button;
    }

    /**
     * Creates the label allowing to resize the given frame.
     *
     * @param frame the frame to resize
     * @param sharingRegion the sharing region
     * @param buttonPanel the button panel, where the created label would be
     * added
     * @return the created resize label
     */
    private static JLabel createResizeLabel(final JFrame frame,
                                            final JComponent sharingRegion,
                                            final JComponent buttonPanel)
    {
        final JLabel resizeLabel = new JLabel(resizeIcon);
        resizeLabel.setCursor(
            Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));

        resizeLabel.addMouseMotionListener(new MouseMotionAdapter()
        {
            @Override
            public void mouseDragged(MouseEvent e)
            {
                Point p = e.getPoint();
                SwingUtilities.convertPointToScreen(p, resizeLabel);

                Point regionLocation = sharingRegion.getLocationOnScreen();

                int sharingWidth = (int) ( p.getX()
                                    - regionLocation.getX()
                                    - 2*SHARING_REGION_INDENT);

                int newSharingHeight = (int) (p.getY()
                                                - frame.getY()
                                                - buttonPanel.getHeight()
                                                - 2*SHARING_REGION_INDENT);

                // We should make sure that the width on MacOSX is a multiple
                // of 16.
                if (OSUtils.IS_MAC && sharingWidth%16 > 0)
                {
                    sharingWidth = Math.round(sharingWidth/16f)*16;
                }
                else if (sharingWidth%2 > 0)
                {
                    sharingWidth = Math.round(sharingWidth/2f)*2;
                }

                sharingRegion.setPreferredSize(
                    new Dimension(sharingWidth, newSharingHeight));

                frame.validate();

                int height = (int) (p.getY() - frame.getY());

                frame.setSize(sharingWidth + 2*SHARING_REGION_INDENT, height);
            }
        });

        return resizeLabel;
    }

    /**
     * Adds a listener for the given frame and call
     *
     * @param call the underlying call
     * @param frame the frame to which the listener would be added
     * @param sharingRegion the sharing region
     */
    private static void addFrameListener(   final Call call,
                                            final JFrame frame,
                                            final Component sharingRegion)
    {
        frame.addComponentListener(new ComponentListener()
        {
            public void componentResized(ComponentEvent e) {}

            public void componentMoved(ComponentEvent e)
            {
                OperationSetDesktopStreaming desktopOpSet
                    = call.getProtocolProvider().getOperationSet(
                        OperationSetDesktopStreaming.class);

                if (desktopOpSet == null)
                    return;

                Point location = new Point( sharingRegion.getX(),
                                            sharingRegion.getY());

                SwingUtilities.convertPointToScreen(location,
                                                    frame.getContentPane());

                desktopOpSet.movePartialDesktopStreaming(
                    call, location.x, location.y);
            }

            public void componentShown(ComponentEvent e) {}

            public void componentHidden(ComponentEvent arg0) {}
        });
    }
}
