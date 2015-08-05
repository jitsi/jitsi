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
package net.java.sip.communicator.plugin.whiteboard.gui;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

import javax.imageio.*;
import javax.swing.*;

import net.java.sip.communicator.plugin.whiteboard.*;
import net.java.sip.communicator.plugin.whiteboard.gui.whiteboardshapes.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.whiteboardobjects.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.plugin.desktoputil.*;

/**
 * The frame for the Whiteboard
 *
 * @author Julien Waechter
 * @author Valentin Martinet
 */
public class WhiteboardFrame
    extends SIPCommFrame
{
    private static final long serialVersionUID = 5441329948483907247L;

    private static final Logger logger =
        Logger.getLogger(WhiteboardFrame.class);

    /**
     * A type int constant indicating that we use the pen tool.
     */
    private static final int PEN = 1;

    /**
     * A type int constant indicating that we use the line tool
     */
    private static final int LINE = 2;

    /**
     * A type int constant indicating that we use the rectangle tool.
     */
    private static final int RECTANGLE = 3;

    /**
     * A type int constant indicating that we use the fill rectangle tool.
     */
    private static final int FILL_RECTANGLE = 4;

    /**
     * A type int constant indicating that we use the circle tool.
     */
    private static final int CIRCLE = 5;

    /**
     * A type int constant indicating that we use the fill circle tool.
     */
    private static final int FILL_CIRCLE = 6;

    /**
     * A type int constant indicating that we use the text tool.
     */
    private static final int TEXT = 7;

    /**
     * A type int constant indicating that we use the selection tool.
     */
    private static final int SELECTION = 8;

    /**
     * A type int constant indicating that we use the image tool.
     */
    private static final int IMAGE = 9;

    /**
     * A type int constant indicating that we use the polyline tool.
     */
    private static final int POLYLINE = 10;

    /**
     * A type int constant indicating that we use the fill polyline tool.
     */
    private static final int FILL_POLYLINE = 11;

    /**
     * A type int constant indicating that we use the polygon tool..
     */
    private static final int POLYGON = 12;

    /**
     * A type int constant indicating that we use the fill polygon tool..
     */
    private static final int FILL_POLYGON = 13;

    /**
     * A type int constant indicating that we use the modification tool.
     */
    private static final int MODIF = 15;

    /**
     * A type int constant indicating that we use the pan tool.
     */
    private static final int PAN = 16;

    /**
     * The last file dir open
     */
    private File lastDir;

    /**
     * The current tool (= one of the previous type int constant)
     */
    private int currentTool = 0;

    /**
     * X mouse coordinates
     */
    private int mouseX = 0;

    /**
     * Y mouse coordinates
     */
    private int mouseY = 0;

    /**
     * Old X mouse coordinates
     */
    private int previousMouseX = 0;

    /**
     * Old Y mouse coordinates
     */
    private int previousMouseY = 0;

    /**
     * Default size grid
     */
    private int defaultGrid = 25;

    /**
     * Old coordinates
     */
    private Point2D previousPoint;

    /**
     * The current selected shape (null if nothing is seleted)
     */
    private WhiteboardShape selectedShape = null;

    /**
     * The current preselected shape (null if nothing is preseleted) A shape is
     * preselected when the mouse move on.
     */
    private WhiteboardShape preselected = null;

    /**
     * The current copied shape (null if nothing is copied)
     */
    private WhiteboardShape copiedShape = null;

    /**
     * True if the draw is finish
     */
    private boolean doneDrawing = true;

    /**
     * Start X mouse coordinates
     */
    private int originX = 0;

    /**
     * Start Y mouse coordinates
     */
    private int originY = 0;

    /**
     * Start width size
     */
    private int originWidth = 0;

    /**
     * Start height size
     */
    private int originHeight = 0;

    /**
     * X coordinates for draw the shape
     */
    private int drawX = 0;

    /**
     * Y coordinates for draw the shape
     */
    private int drawY = 0;

    /**
     * The current color for the shapes (black by default)
     */
    private Color currentColor = Color.BLACK;

    /**
     * The XOR alternation color for display
     */
    private Color xorColor = Color.WHITE;

    /**
     * List of WhiteboardShape
     */
    private List<WhiteboardShape> displayList =
        new CopyOnWriteArrayList<WhiteboardShape>();

    /**
     * Aarray of WhiteboardPoint
     */
    private List<WhiteboardPoint> pathList = new ArrayList<WhiteboardPoint>();

    /**
     * WhiteboardPanel where the shapes are drawn
     */
    private WhiteboardPanel drawCanvas;

    /**
     * True if we are in "move" state
     */
    private boolean moving;

    /**
     * Default font size for the text.
     */
    private int defaultFontSize = WhiteboardShapeText.DEFAULT_FONT_SIZE;

    /**
     * Affine transform world to shape.
     */
    private AffineTransform w2s;

    /**
     * Affine transform shape to world.
     */
    private AffineTransform s2w;

    /**
     * The color chooser for choose the shape's color
     */
    private JColorChooser colorChooser = new JColorChooser();

    /**
     * The color chooser dialog created by colorChooser.
     */
    private JDialog colorChooserDialog;

    /**
     * Spinner to choose the thickness
     */
    private SpinnerNumberModel spinModel;

    /**
     * Contact associated with this WhiteboardFrame
     */
    private Contact contact;

    /**
     * WhiteboardSessionManager associated with this WhiteboardFrame.
     */
    private WhiteboardSessionManager sessionManager;

    /**
     * WhiteboardSession associated with this WhiteboardFrame
     */
    private WhiteboardSession session;

    /**
     * Constructor for WhiteboardFrame.
     *
     * @param wps WhiteboardSessionManager
     * @param session WhiteboardSession associated with this frame
     */
    public WhiteboardFrame(WhiteboardSessionManager wps,
        WhiteboardSession session)
    {
        this.drawCanvas = new WhiteboardPanel(displayList, this);
        this.sessionManager = wps;
        this.session = session;

        this.session.addWhiteboardChangeListener(
            new WhiteboardChangeListenerImpl());

        initComponents();
        initIcons();
        initMouse();

        drawCanvas.setLayout(new java.awt.BorderLayout());
        getContentPane().add(drawCanvas, java.awt.BorderLayout.CENTER);

        setSize(800, 600);
        initializeTransform();

        Integer value = 1;
        Integer min = 1;
        Integer max = 10;
        Integer step = 1;
        spinModel = new SpinnerNumberModel(value, min, max, step);
        jSpinnerThickness.setModel(spinModel);

        if (contact != null)
            this.jLabelStatus.setText("PictoChat with: "
                + contact.getDisplayName());
    }

    /**
     * Initialize all icons in the frame.
     */
    private void initIcons()
    {
        setIconImage(
            Resources.getImage("service.gui.SIP_COMMUNICATOR_LOGO").getImage());

        selectionButton
            .setIcon(Resources.getImage("plugin.whiteboard.SELECT_ICON"));
        penButton
            .setIcon(Resources.getImage("plugin.whiteboard.PEN_ICON"));
        lineButton
            .setIcon(Resources.getImage("plugin.whiteboard.LINE2_ICON"));
        rectangleButton
            .setIcon(Resources.getImage("plugin.whiteboard.RECT_ICON"));
        fillRectangleButton
            .setIcon(Resources.getImage("plugin.whiteboard.RECTF_ICON"));
        circleButton
            .setIcon(Resources.getImage("plugin.whiteboard.CIRCLE2_ICON"));
        fillCircleButton
            .setIcon(Resources.getImage("plugin.whiteboard.CIRCLEF_ICON"));
        textButton
            .setIcon(Resources.getImage("plugin.whiteboard.TEXT2_ICON"));
        colorChooserButton
            .setIcon(Resources.getImage("plugin.whiteboard.COLOR_ICON"));
        polylineButton
            .setIcon(Resources.getImage("plugin.whiteboard.POLY_LINE_ICON"));
        polygonButton
            .setIcon(Resources.getImage("plugin.whiteboard.POLY_ICON"));
        fillPolygonButton
            .setIcon(Resources.getImage("plugin.whiteboard.POLYF_ICON"));
        imageButton
            .setIcon(Resources.getImage("plugin.whiteboard.IMAGE_ICON"));
        modifButton
            .setIcon(Resources.getImage("plugin.whiteboard.MODIF_ICON"));

        jButtonNew
            .setIcon(Resources.getImage("plugin.whiteboard.FILE_NEW_ICON"));
        jButtonCopy
            .setIcon(Resources.getImage("plugin.whiteboard.EDIT_COPY_ICON"));
        jButtonPaste
            .setIcon(Resources.getImage("plugin.whiteboard.EDIT_PASTE_ICON"));
        jButtonOpen
            .setIcon(Resources.getImage("plugin.whiteboard.FILE_IMPORT_ICON"));
        jButtonSave
            .setIcon(Resources.getImage("plugin.whiteboard.FILE_SAVE_ICON"));
    }

    /**
     * Initialize all transformations.
     */
    private void initializeTransform()
    {
        w2s = new AffineTransform();
        w2s.setToScale(1, 1);
        try
        {
            s2w = w2s.createInverse();
        }
        catch (NoninvertibleTransformException e)
        {
            logger.error(e.getMessage());
        }
    }

    /**
     * Initialize all mouse events.
     */
    private void initMouse()
    {
        drawCanvas.addMouseListener(new java.awt.event.MouseListener()
        {
            /**
             * Invoked when the mouse button has been clicked (pressed and
             * released) on a component.
             */
            public void mouseClicked(MouseEvent e)
            {
                switch (currentTool)
                {
                case POLYLINE:
                    if (e.getClickCount() == 1)
                    {
                        polyOperation(e);
                    }

                    if (e.getClickCount() == 2)
                    {
                        releasedPolyline(false);
                    }
                    break;

                case FILL_POLYLINE:
                    if (e.getClickCount() == 1)
                    {
                        polyOperation(e);
                    }

                    if (e.getClickCount() == 2)
                    {
                        releasedPolyline(true);
                    }
                    break;

                case POLYGON:
                    if (e.getClickCount() == 1)
                    {
                        polyOperation(e);
                    }

                    if (e.getClickCount() == 2)
                    {
                        releasedPolygon(false);
                    }
                    break;

                case FILL_POLYGON:
                    if (e.getClickCount() == 1)
                    {
                        polyOperation(e);
                    }

                    if (e.getClickCount() == 2)
                    {
                        releasedPolygon(true);
                    }
                    break;
                }
            }

            /**
             * Invoked when a mouse button has been pressed on a component.
             */
            public void mousePressed(MouseEvent e)
            {
                selectedShape = null;
                if (currentTool == SELECTION)
                {
                    deselect();
                    for (int i = displayList.size() - 1; i >= 0; i--)
                    {
                        WhiteboardShape shape = displayList.get(i);

                        if (shape.contains(s2w.transform(e.getPoint(), null)))
                        {
                            shape.setSelected(true);
                            selectedShape = shape;
                            spinModel.setValue(selectedShape.getThickness());
                            jLabelColor.setBackground(Color.getColor("",
                                selectedShape.getColor()));
                            break;
                        }
                    }
                    repaint();
                }
                else if (currentTool == MODIF)
                {
                    deselect();
                    for (int i = displayList.size() - 1; i >= 0; i--)
                    {
                        WhiteboardShape shape = displayList.get(i);

                        WhiteboardPoint point = shape.getSelectionPoint(
                            s2w.transform(e.getPoint(), null));

                        if (point != null)
                        {
                            shape.setSelected(true);
                            shape.setModifyPoint(point);

                            selectedShape = shape;
                            spinModel.setValue(selectedShape.getThickness());
                            jLabelColor.setBackground(
                                Color.getColor("", selectedShape.getColor()));
                            break;
                        }
                    }
                    repaint();
                }
                else if (currentTool == PAN)
                {
                    previousPoint = e.getPoint();
                }
            }

            /**
             * Invoked when a mouse button has been released on a component.
             */
            public void mouseReleased(MouseEvent e)
            {
                switch (currentTool)
                {
                case SELECTION:
                    releasedMove();
                    break;

                case MODIF:
                    releasedModif();
                    break;

                case PEN:
                    releasedPen();
                    break;

                case LINE:
                    releasedLine();
                    break;

                case RECTANGLE:
                    releasedRectangle(false);
                    break;

                case FILL_RECTANGLE:
                    releasedRectangle(true);
                    break;

                case CIRCLE:
                    releasedCircle(false);
                    break;

                case FILL_CIRCLE:
                    releasedCircle(true);
                    break;

                case TEXT:
                    releasedText(e.getX(), e.getY());
                    break;

                case IMAGE:
                    releasedImage();
                    break;

                case POLYLINE:
                    polyOperation(e);
                    break;

                case POLYGON:
                    polyOperation(e);
                    break;

                case FILL_POLYGON:
                    polyOperation(e);
                    break;
                }
            }

            /**
             * Invoked when the mouse enters a component.
             */
            public void mouseEntered(MouseEvent e)
            {
                toggleCursor();
            }

            /**
             * Invoked when the mouse exits a component.
             */
            public void mouseExited(MouseEvent e)
            {
                setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }
        });

        drawCanvas
            .addMouseMotionListener(new java.awt.event.MouseMotionListener()
            {
                /**
                 * Invoked when a mouse button is pressed on a component and
                 * then dragged.
                 *
                 * @param e
                 */
                public void mouseDragged(MouseEvent e)
                {
                    switch (currentTool)
                    {
                    case SELECTION:
                        moveOperation(e);
                        break;

                    case MODIF:
                        modifOperation(e);
                        break;

                    case PEN:
                        penOperation(e);
                        break;

                    case LINE:
                        lineOperation(e);
                        break;

                    case RECTANGLE:
                        rectangleOperation(e);
                        break;

                    case FILL_RECTANGLE:
                        rectangleOperation(e);
                        break;

                    case CIRCLE:
                        circleOperation(e);
                        break;

                    case FILL_CIRCLE:
                        circleOperation(e);
                        break;

                    case IMAGE:
                        imageOperation(e);
                        break;

                    case PAN:
                        panOperation(e);
                        break;

                    case POLYLINE:
                        polyDragOperation(e);
                        break;

                    case POLYGON:
                        polyDragOperation(e);
                        break;

                    case FILL_POLYGON:
                        polyDragOperation(e);
                        break;
                    }
                }

                /**
                 * Invoked when the mouse cursor has been moved onto a component
                 * but no buttons have been pushed.
                 *
                 * @param e
                 */
                public void mouseMoved(MouseEvent e)
                {
                    WhiteboardShape shape;
                    for (int i = 0; i < displayList.size(); i++)
                    {
                        shape = displayList.get(i);
                        if (shape.contains(s2w.transform(e.getPoint(), null)))
                        {

                            if (currentTool == MODIF)
                            {
                                setCursor(Cursor.getDefaultCursor());
                            }
                            else
                                setCursor(Cursor
                                    .getPredefinedCursor(Cursor.HAND_CURSOR));

                            if (currentTool == SELECTION
                                || currentTool == MODIF)
                            {
                                if (preselected == null)
                                {
                                    Graphics g = drawCanvas.getGraphics();
                                    shape.preselect(g, w2s);
                                    preselected = shape;
                                }
                                else if (!preselected.equals(shape))
                                {
                                    repaint();
                                    Graphics g = drawCanvas.getGraphics();
                                    shape.preselect(g, w2s);
                                    preselected = shape;
                                }
                            }
                            return;
                        }
                        else if (shape.getSelectionPoint(s2w.transform(e
                            .getPoint(), null)) != null)
                        {
                            if (currentTool == MODIF)
                            {

                                setCursor(Cursor.getPredefinedCursor(
                                    Cursor.CROSSHAIR_CURSOR));

                                if (preselected == null)
                                {
                                    Graphics g = drawCanvas.getGraphics();
                                    shape.preselect(g, w2s);
                                    preselected = shape;
                                }
                                else if (!preselected.equals(shape))
                                {
                                    repaint();
                                    Graphics g = drawCanvas.getGraphics();
                                    shape.preselect(g, w2s);
                                    preselected = shape;
                                }
                            }
                            return;
                        }

                    }
                    if (preselected != null)
                    {
                        preselected = null;
                        repaint();
                    }
                    toggleCursor();
                }
            });
    }

    /**
     * Sets the appropriate cursor depending on the current tool.
     */
    private void toggleCursor()
    {
        switch (currentTool)
        {
        case SELECTION:
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            break;
        case MODIF:
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            break;
        case PAN:
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            break;

        case TEXT:
            setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
            break;

        case PEN:
        case LINE:
        case RECTANGLE:
        case FILL_RECTANGLE:
        case CIRCLE:
        case FILL_CIRCLE:
        case POLYLINE:
        case FILL_POLYLINE:
        case POLYGON:
        case FILL_POLYGON:
        case IMAGE:
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
            break;

        default:
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor. (NetBeans 5.5)
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code
    // ">//GEN-BEGIN:initComponents
    private void initComponents()
    {
        buttonGroup = new ButtonGroup();
        jLabelStatus = new JLabel();
        jToolBar1 = new JToolBar();
        jButtonNew = new JButton();
        jButtonSave = new JButton();
        jButtonOpen = new JButton();
        jButtonCopy = new JButton();
        jButtonPaste = new JButton();
        jPanel1 = new TransparentPanel();
        toolBar = new TransparentPanel();
        penButton = new JToggleButton();
        selectionButton = new JToggleButton();
        lineButton = new JToggleButton();
        rectangleButton = new JToggleButton();
        fillRectangleButton = new JToggleButton();
        textButton = new JToggleButton();
        imageButton = new JToggleButton();
        polygonButton = new JToggleButton();
        fillPolygonButton = new JToggleButton();
        polylineButton = new JToggleButton();
        circleButton = new JToggleButton();
        fillCircleButton = new JToggleButton();
        colorChooserButton = new JButton();
        jLabelColor = new JLabel();
        modifButton = new JToggleButton();
        jPanel2 = new TransparentPanel();
        jLabelThickness = new JLabel();
        jSpinnerThickness = new JSpinner();
        menuBar = new JMenuBar();
        fileMenu = new JMenu();
        newMenuItem = new JMenuItem();
        openMenuItem = new JMenuItem();
        saveMenuItem = new JMenuItem();
        printMenuItem = new JMenuItem();
        exitMenuItem = new JMenuItem();
        editMenu = new JMenu();
        gridMenuItem = new JCheckBoxMenuItem();
        deselectMenuItem = new JMenuItem();
        copyMenuItem = new JMenuItem();
        pasteMenuItem = new JMenuItem();
        deleteMenuItem = new javax.swing.JMenuItem();
        propertiesMenuItem = new JMenuItem();
        helpMenu = new JMenu();
        helpMenuItem = new JMenuItem();
        aboutMenuItem = new JMenuItem();
        leftPanel = new TransparentPanel(new BorderLayout());

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(Resources.getString("plugin.whiteboard.TITLE"));

        if (session != null)
        {
            Iterator<?> participants = session.getWhiteboardParticipants();

            while (participants.hasNext())
            {
                this.setTitle(this.getTitle() + " - " + participants.next());
            }
        }

        jLabelStatus.setText(Resources.getString("plugin.whiteboard.DRAW"));
        jLabelStatus.setBorder(javax.swing.BorderFactory
            .createEtchedBorder(javax.swing.border.EtchedBorder.RAISED));
        getContentPane().add(jLabelStatus, java.awt.BorderLayout.SOUTH);

        jButtonNew.setToolTipText(Resources.getString("service.gui.NEW"));
        jButtonNew.setEnabled(false);
        jToolBar1.add(jButtonNew);

        jButtonSave.setToolTipText(Resources.getString("service.gui.SAVE"));
        jButtonSave.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jButtonSaveActionPerformed(evt);
            }
        });

        jToolBar1.add(jButtonSave);

        jButtonOpen.setToolTipText(
            Resources.getString("plugin.whiteboard.OPEN"));
        jButtonOpen.setEnabled(false);
        jToolBar1.add(jButtonOpen);

        jButtonCopy.setToolTipText(Resources.getString("service.gui.COPY"));
        jButtonCopy.setEnabled(false);
        jToolBar1.add(jButtonCopy);

        jButtonPaste.setToolTipText(Resources.getString("service.gui.PASTE"));
        jButtonPaste.setEnabled(false);
        jToolBar1.add(jButtonPaste);

        getContentPane().add(jToolBar1, BorderLayout.NORTH);

        jPanel1.setLayout(new BorderLayout());

        jPanel1.setPreferredSize(new Dimension(110, 350));
        toolBar.setLayout(new GridLayout(0, 2, 5, 5));
        toolBar.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));

        buttonGroup.add(selectionButton);
        selectionButton.setToolTipText(
            Resources.getString("plugin.whiteboard.SELECT"));
        selectionButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                selectionButtonActionPerformed(evt);
            }
        });

        toolBar.add(selectionButton);

        buttonGroup.add(modifButton);
        modifButton.setToolTipText(
            Resources.getString("plugin.whiteboard.MODIFICATION"));
        modifButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                modifButtonActionPerformed(evt);
            }
        });

        toolBar.add(modifButton);

        buttonGroup.add(penButton);
        penButton.setToolTipText(Resources.getString("plugin.whiteboard.PEN"));
        penButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                penButtonActionPerformed(evt);
            }
        });

        toolBar.add(penButton);

        buttonGroup.add(textButton);
        textButton.setToolTipText(Resources.getString("plugin.whiteboard.TEXT"));
        textButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                textButtonActionPerformed(evt);
            }
        });

        toolBar.add(textButton);

        buttonGroup.add(lineButton);
        lineButton.setToolTipText(
            Resources.getString("plugin.whiteboard.LINE"));
        lineButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                lineButtonActionPerformed(evt);
            }
        });

        toolBar.add(lineButton);

        buttonGroup.add(polylineButton);
        polylineButton.setToolTipText(
            Resources.getString("plugin.whiteboard.POLYLINE"));
        polylineButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                polylineButtonActionPerformed(evt);
            }
        });

        toolBar.add(polylineButton);

        buttonGroup.add(rectangleButton);
        rectangleButton.setToolTipText(Resources.getString("plugin.whiteboard.RECTANGLE"));
        rectangleButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                rectangleButtonActionPerformed(evt);
            }
        });

        toolBar.add(rectangleButton);

        buttonGroup.add(fillRectangleButton);
        fillRectangleButton.setToolTipText(
            Resources.getString("plugin.whiteboard.FILLED_RECTANGLE"));
        fillRectangleButton
            .addActionListener(new java.awt.event.ActionListener()
            {
                public void actionPerformed(java.awt.event.ActionEvent evt)
                {
                    fillRectangleButtonActionPerformed(evt);
                }
            });

        toolBar.add(fillRectangleButton);

        buttonGroup.add(imageButton);
        imageButton.setToolTipText(
            Resources.getString("plugin.whiteboard.IMAGE"));
        imageButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                imageButtonActionPerformed(evt);
            }
        });

        toolBar.add(imageButton);

        buttonGroup.add(polygonButton);
        polygonButton.setToolTipText(
            Resources.getString("plugin.whiteboard.POLYGON"));
        polygonButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                polygonButtonActionPerformed(evt);
            }
        });

        toolBar.add(polygonButton);

        buttonGroup.add(fillPolygonButton);
        fillPolygonButton.setToolTipText(
            Resources.getString("plugin.whiteboard.FILLEDPOLYGON"));
        fillPolygonButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                fillPolygonButtonActionPerformed(evt);
            }
        });

        toolBar.add(fillPolygonButton);

        buttonGroup.add(circleButton);
        circleButton.setToolTipText(
            Resources.getString("plugin.whiteboard.CIRCLE"));
        circleButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                circleButtonActionPerformed(evt);
            }
        });

        toolBar.add(circleButton);

        buttonGroup.add(fillCircleButton);
        fillCircleButton.setToolTipText(
            Resources.getString("plugin.whiteboard.FILLED_CIRCLE"));
        fillCircleButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                fillCircleButtonActionPerformed(evt);
            }
        });

        toolBar.add(fillCircleButton);

        colorChooserButton.setToolTipText(
            Resources.getString("plugin.whiteboard.COLOR"));
        colorChooserButton
            .addActionListener(new java.awt.event.ActionListener()
            {
                public void actionPerformed(java.awt.event.ActionEvent evt)
                {
                    colorChooserButtonActionPerformed(evt);
                }
            });

        toolBar.add(colorChooserButton);

        jLabelColor.setOpaque(true);
        jLabelColor.setBackground(currentColor);
        jLabelColor.setBorder(BorderFactory.createLineBorder(Color.GRAY, 3));
        toolBar.add(jLabelColor);

        jPanel1.add(toolBar, BorderLayout.NORTH);

        jPanel2.setLayout(new GridBagLayout());

        jLabelThickness.setText(
            Resources.getString("plugin.whiteboard.THICKNESS"));

        jPanel2.add(jLabelThickness);

        jSpinnerThickness
            .addChangeListener(new javax.swing.event.ChangeListener()
            {
                public void stateChanged(javax.swing.event.ChangeEvent evt)
                {
                    jSpinnerThicknessStateChanged(evt);
                }
            });

        jPanel2.add(jSpinnerThickness);

        jPanel1.add(jPanel2, BorderLayout.CENTER);

        leftPanel.add(jPanel1, BorderLayout.NORTH);

        getContentPane().add(leftPanel, BorderLayout.WEST);

        fileMenu.setText(Resources.getString("service.gui.FILE"));

        newMenuItem.setText(Resources.getString("service.gui.NEW"));
        newMenuItem.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                try
                {
                    if (session != null)
                        session.join();
                    else
                        sessionManager.initWhiteboard(contact);
                }
                catch (OperationFailedException e1)
                {
                    logger.error("Creating new session failed.", e1);
                }
            }
        });
        fileMenu.add(newMenuItem);

        openMenuItem.setText(Resources.getString("plugin.whiteboard.OPEN"));
        openMenuItem.setEnabled(false);
        fileMenu.add(openMenuItem);

        saveMenuItem.setText(Resources.getString("service.gui.SAVE"));
        saveMenuItem.setEnabled(false);
        fileMenu.add(saveMenuItem);

        printMenuItem.setText(Resources.getString("service.gui.PRINT"));
        printMenuItem.setEnabled(false);
        fileMenu.add(printMenuItem);

        exitMenuItem.setText(Resources.getString("service.gui.EXIT"));
        exitMenuItem.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                session.leave();
            }
        });
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        editMenu.setText(Resources.getString("service.gui.EDIT"));
        gridMenuItem.setText(Resources.getString("plugin.whiteboard.GRID"));
        gridMenuItem.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                gridMenuItemActionPerformed(evt);
            }
        });

        editMenu.add(gridMenuItem);

        deselectMenuItem.setText(
            Resources.getString("plugin.whiteboard.DESELECT"));
        deselectMenuItem.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                deselectMenuItemActionPerformed(evt);
            }
        });

        editMenu.add(deselectMenuItem);

        copyMenuItem.setText(Resources.getString("service.gui.COPY"));
        copyMenuItem.setEnabled(false);
        editMenu.add(copyMenuItem);

        pasteMenuItem.setText(Resources.getString("service.gui.PASTE"));
        pasteMenuItem.setEnabled(false);
        editMenu.add(pasteMenuItem);

        deleteMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(
            java.awt.event.KeyEvent.VK_DELETE, 0));
        deleteMenuItem.setText(Resources.getString("plugin.whiteboard.DELETE"));
        deleteMenuItem.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                deleteMenuItemActionPerformed(evt);
            }
        });

        editMenu.add(deleteMenuItem);

        propertiesMenuItem.setText(
            Resources.getString("plugin.whiteboard.PROPERTIES"));
        propertiesMenuItem.setEnabled(false);
        editMenu.add(propertiesMenuItem);

        menuBar.add(editMenu);

        helpMenu.setText(Resources.getString("service.gui.HELP"));
        helpMenu.setEnabled(false);
        helpMenuItem.setText(Resources.getString("service.gui.HELP"));
        helpMenu.add(helpMenuItem);

        aboutMenuItem.setText(Resources.getString("service.gui.ABOUT"));
        helpMenu.add(aboutMenuItem);

        menuBar.add(helpMenu);

        setJMenuBar(menuBar);

        pack();
    }

    /**
     * Invoked when an action occurs on the modifButton.
     *
     * @param evt
     */
    private void modifButtonActionPerformed(java.awt.event.ActionEvent evt)
    {
        if (modifButton.isSelected())
            currentTool = MODIF;
    }

    /**
     * Invoked when an action occurs on the jButtonSave.
     *
     * @param evt
     */
    private void jButtonSaveActionPerformed(java.awt.event.ActionEvent evt)
    {

        SipCommFileChooser chooser = GenericFileDialog.create(
            WhiteboardFrame.this,
            "Save file...",
            SipCommFileChooser.SAVE_FILE_OPERATION);

        //chooser.removeChoosableFileFilter(chooser.getAcceptAllFileFilter());
        WhiteboardFileFilter filterJpg =
            new WhiteboardFileFilter("jpg", "JPEG Files (*.jpg)");
        WhiteboardFileFilter filterPng =
            new WhiteboardFileFilter("png", "PNG Files (*.png)");

        chooser.addFilter(filterJpg);
        chooser.addFilter(filterPng);

        File file = chooser.getFileFromDialog();

        if (file != null)
        {
            try
            {
                WhiteboardFileFilter ff =
                    (WhiteboardFileFilter) chooser.getUsedFilter();
                if (!ff.getExtension().equals(ff.getExtension(file)))
                {
                    file =
                        new File(file.getAbsolutePath() + "."
                            + ff.getExtension());
                }
                lastDir = file.getParentFile();
                BufferedImage buf =
                    new BufferedImage(drawCanvas.getWidth(), drawCanvas
                        .getHeight(), BufferedImage.TYPE_INT_RGB);
                Graphics2D g = buf.createGraphics();

                this.drawCanvas.paint(g);
                g.dispose();
                ImageIO.write(buf, ff.getExtension(), file);
            }
            catch (IOException ex)
            {
                ex.printStackTrace();
            }
        }
    }// GEN-LAST:event_jButtonSaveActionPerformed

    /**
     * Invoked when an action occurs on the delete menu.
     *
     * @param evt
     */
    private void deleteMenuItemActionPerformed(java.awt.event.ActionEvent evt)
    {// GEN-FIRST:event_deleteMenuItemActionPerformed
        this.deleteSelected();
    }// GEN-LAST:event_deleteMenuItemActionPerformed

    /**
     * Invoked when an action occurs on the deselect menu.
     *
     * @param evt
     */
    private void deselectMenuItemActionPerformed(java.awt.event.ActionEvent evt)
    {// GEN-FIRST:event_deselectMenuItemActionPerformed
        this.deselect();
    }// GEN-LAST:event_deselectMenuItemActionPerformed

    /**
     * Invoked when the jSpinnerThickness has changed its state.
     *
     * @param evt
     */
    private void jSpinnerThicknessStateChanged(
        javax.swing.event.ChangeEvent evt)
    {// GEN-FIRST:event_jSpinnerThicknessStateChanged
        if (selectedShape != null)
        {
            selectedShape.setThickness(spinModel.getNumber().intValue());
            sendMoveShape(selectedShape);
            repaint();
        }
    }// GEN-LAST:event_jSpinnerThicknessStateChanged

    /**
     * Invoked when an action occurs on the fillCircleButton.
     *
     * @param evt
     */
    private void fillCircleButtonActionPerformed(
        java.awt.event.ActionEvent evt)
    {// GEN-FIRST:event_fillCircleButtonActionPerformed
        if (fillCircleButton.isSelected())
            currentTool = FILL_CIRCLE;
    }// GEN-LAST:event_fillCircleButtonActionPerformed

    /**
     * Invoked when an action occurs on the circleButton.
     *
     * @param evt
     */
    private void circleButtonActionPerformed(java.awt.event.ActionEvent evt)
    {// GEN-FIRST:event_circleButtonActionPerformed
        if (circleButton.isSelected())
            currentTool = CIRCLE;
    }// GEN-LAST:event_circleButtonActionPerformed

    /**
     * Invoked when an action occurs on the polylineButton.
     *
     * @param evt
     */
    private void polylineButtonActionPerformed(java.awt.event.ActionEvent evt)
    {// GEN-FIRST:event_polylineButtonActionPerformed
        if (polylineButton.isSelected())
            currentTool = POLYLINE;
    }// GEN-LAST:event_polylineButtonActionPerformed

    /**
     * Invoked when an action occurs on the fillPolygonButton.
     *
     * @param evt
     */
    private void fillPolygonButtonActionPerformed(
        java.awt.event.ActionEvent evt)
    {// GEN-FIRST:event_fillPolygonButtonActionPerformed
        if (fillPolygonButton.isSelected())
            currentTool = FILL_POLYGON;
    }// GEN-LAST:event_fillPolygonButtonActionPerformed

    /**
     * Invoked when an action occurs on the polygonButton.
     *
     * @param evt
     */
    private void polygonButtonActionPerformed(java.awt.event.ActionEvent evt)
    {// GEN-FIRST:event_polygonButtonActionPerformed
        if (polygonButton.isSelected())
            currentTool = POLYGON;
    }// GEN-LAST:event_polygonButtonActionPerformed

    /**
     * Invoked when an action occurs on the imageButton.
     *
     * @param evt
     */
    private void imageButtonActionPerformed(java.awt.event.ActionEvent evt)
    {// GEN-FIRST:event_imageButtonActionPerformed
        if (imageButton.isSelected())
            currentTool = IMAGE;
    }// GEN-LAST:event_imageButtonActionPerformed

    /**
     * Invoked when an action occurs on the rectangleButton.
     *
     * @param evt
     */
    private void rectangleButtonActionPerformed(java.awt.event.ActionEvent evt)
    {// GEN-FIRST:event_rectangleButtonActionPerformed
        if (rectangleButton.isSelected())
            currentTool = RECTANGLE;
    }// GEN-LAST:event_rectangleButtonActionPerformed

    /**
     * Invoked when an action occurs on the lineButton.
     *
     * @param evt
     */
    private void lineButtonActionPerformed(java.awt.event.ActionEvent evt)
    {// GEN-FIRST:event_lineButtonActionPerformed
        if (lineButton.isSelected())
            currentTool = LINE;
    }// GEN-LAST:event_lineButtonActionPerformed

    /**
     * Invoked when an action occurs on the fillRectangleButton.
     *
     * @param evt
     */
    private void fillRectangleButtonActionPerformed(
        java.awt.event.ActionEvent evt)
    {// GEN-FIRST:event_fillRectangleButtonActionPerformed
        if (fillRectangleButton.isSelected())
            currentTool = FILL_RECTANGLE;
    }// GEN-LAST:event_fillRectangleButtonActionPerformed

    /**
     * Invoked when an action occurs on the textButto.
     *
     * @param evt
     */
    private void textButtonActionPerformed(java.awt.event.ActionEvent evt)
    {// GEN-FIRST:event_textButtonActionPerformed
        if (textButton.isSelected())
            currentTool = TEXT;
    }// GEN-LAST:event_textButtonActionPerformed

    /**
     * Invoked when an action occurs on the penButton.
     *
     * @param evt
     */
    private void penButtonActionPerformed(java.awt.event.ActionEvent evt)
    {// GEN-FIRST:event_penButtonActionPerformed
        if (penButton.isSelected())
            currentTool = PEN;
    }// GEN-LAST:event_penButtonActionPerformed

    /**
     * Invoked when an action occurs on the selectionButton.
     *
     * @param evt
     */
    private void selectionButtonActionPerformed(java.awt.event.ActionEvent evt)
    {// GEN-FIRST:event_selectionButtonActionPerformed
        if (selectionButton.isSelected())
            currentTool = SELECTION;
        deselect();
        repaint();
    }// GEN-LAST:event_selectionButtonActionPerformed

    /**
     * Invoked when an action occurs on the gridMenuItem.
     *
     * @param evt
     */
    private void gridMenuItemActionPerformed(java.awt.event.ActionEvent evt)
    {// GEN-FIRST:event_gridMenuItemActionPerformed
        this.drawCanvas.drawGrid(gridMenuItem.isSelected());
        repaint();
    }// GEN-LAST:event_gridMenuItemActionPerformed

    /**
     * Invoked when an action occurs on the colorChooserButton.
     *
     * @param evt
     */
    private void colorChooserButtonActionPerformed(
        java.awt.event.ActionEvent evt)
    {// GEN-FIRST:event_colorChooserButtonActionPerformed
        this.chooseColor();
    }// GEN-LAST:event_colorChooserButtonActionPerformed

    /**
     * Components used for the GUI:
     */
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem aboutMenuItem;

    private javax.swing.ButtonGroup buttonGroup;

    private javax.swing.JToggleButton circleButton;

    private javax.swing.JButton colorChooserButton;

    private javax.swing.JMenuItem copyMenuItem;

    private javax.swing.JMenuItem deleteMenuItem;

    private javax.swing.JMenuItem deselectMenuItem;

    private javax.swing.JMenu editMenu;

    private javax.swing.JMenuItem exitMenuItem;

    private javax.swing.JMenu fileMenu;

    private javax.swing.JToggleButton fillCircleButton;

    private javax.swing.JToggleButton fillPolygonButton;

    private javax.swing.JToggleButton fillRectangleButton;

    private javax.swing.JCheckBoxMenuItem gridMenuItem;

    private javax.swing.JMenu helpMenu;

    private javax.swing.JMenuItem helpMenuItem;

    private javax.swing.JToggleButton imageButton;

    private javax.swing.JButton jButtonCopy;

    private javax.swing.JButton jButtonNew;

    private javax.swing.JButton jButtonOpen;

    private javax.swing.JButton jButtonPaste;

    private javax.swing.JButton jButtonSave;

    private javax.swing.JLabel jLabelColor;

    private javax.swing.JLabel jLabelStatus;

    private javax.swing.JLabel jLabelThickness;

    private JPanel leftPanel;

    private JPanel jPanel1;

    private JPanel jPanel2;

    private javax.swing.JSpinner jSpinnerThickness;

    private javax.swing.JToolBar jToolBar1;

    private javax.swing.JToggleButton lineButton;

    private javax.swing.JMenuBar menuBar;

    private javax.swing.JToggleButton modifButton;

    private javax.swing.JMenuItem newMenuItem;

    private javax.swing.JMenuItem openMenuItem;

    private javax.swing.JMenuItem pasteMenuItem;

    private javax.swing.JToggleButton penButton;

    private javax.swing.JToggleButton polygonButton;

    private javax.swing.JToggleButton polylineButton;

    private javax.swing.JMenuItem printMenuItem;

    private javax.swing.JMenuItem propertiesMenuItem;

    private javax.swing.JToggleButton rectangleButton;

    private javax.swing.JMenuItem saveMenuItem;

    private javax.swing.JToggleButton selectionButton;

    private javax.swing.JToggleButton textButton;

    private JPanel toolBar;

    // End of variables declaration//GEN-END:variables

    /**
     * Action when the shape is drawn.
     *
     * @param e
     */
    private void penOperation(MouseEvent e)
    {
        Graphics g = drawCanvas.getGraphics();
        g.setColor(currentColor);

        if (doneDrawing)
        {
            reInitMouse(e);
            doneDrawing = false;
            g.drawLine(previousMouseX, previousMouseY, mouseX, mouseY);
        }

        if (hasMouseMoved(e))
        {
            mouseX = snapToX(e.getX());
            mouseY = snapToY(e.getY());
            WhiteboardPoint point = new WhiteboardPoint(mouseX, mouseY);
            pathList.add(point);
            g.drawLine(previousMouseX, previousMouseY, mouseX, mouseY);

            previousMouseX = mouseX;
            previousMouseY = mouseY;
        }
    }

    /**
     * Action when the shape is drawn.
     *
     * @param e
     */
    private void lineOperation(MouseEvent e)
    {
        Graphics g = drawCanvas.getGraphics();
        g.setColor(currentColor);

        if (doneDrawing)
        {
            reInitMouse(e);
            g.setXORMode(xorColor);
            g.drawLine(originX, originY, mouseX, mouseY);
            doneDrawing = false;
        }

        if (hasMouseMoved(e))
        {
            g.setXORMode(xorColor);
            g.drawLine(originX, originY, mouseX, mouseY);

            mouseX = snapToX(e.getX());
            mouseY = snapToX(e.getY());

            g.drawLine(originX, originY, mouseX, mouseY);
        }
    }

    /**
     * Action when the shape is drawn.
     *
     * @param e
     */
    private void rectangleOperation(MouseEvent e)
    {
        Graphics g = drawCanvas.getGraphics();
        g.setColor(currentColor);

        if (doneDrawing)
        {
            reInitMouse(e);
            doneDrawing = false;
        }

        if (hasMouseMoved(e))
        {
            g.setXORMode(drawCanvas.getBackground());
            g.drawRect(drawX, drawY, originWidth, originHeight);

            mouseX = snapToX(e.getX());
            mouseY = snapToY(e.getY());

            setActualBoundry();

            g.drawRect(drawX, drawY, originWidth, originHeight);
        }
    }

    /*
     * Action when the shape is drawn.
     *
     * @param e
     */
    /* not sure why this is here but it's never used. i am not deleting it
     * though in case we decide to revive the whiteboard one day. and this
     * appears necessary
    private void ellipseOperation(MouseEvent e)
    {
        Graphics g = drawCanvas.getGraphics();
        g.setColor(currentColor);

        if (doneDrawing)
        {
            reInitMouse(e);
            doneDrawing = false;
        }

        if (hasMouseMoved(e))
        {
            g.setXORMode(xorColor);
            g.drawOval(drawX, drawY, originWidth, originHeight);

            mouseX = snapToX(e.getX());
            mouseY = snapToY(e.getY());

            setActualBoundry();

            g.drawOval(drawX, drawY, originWidth, originHeight);
        }
    }
    */

    /**
     * Action when the shape is drawn.
     *
     * @param e
     */
    private void circleOperation(MouseEvent e)
    {
        Graphics g = drawCanvas.getGraphics();
        g.setColor(currentColor);

        if (doneDrawing)
        {
            reInitMouse(e);
            doneDrawing = false;
        }

        if (hasMouseMoved(e))
        {
            g.setXORMode(xorColor);
            g.drawOval(drawX, drawY, originWidth, originWidth);

            mouseX = snapToX(e.getX());
            mouseY = snapToY(e.getY());

            setActualBoundry();

            g.drawOval(drawX, drawY, originWidth, originWidth);
        }
    }

    private void polyDragOperation(MouseEvent e)
    {
        Graphics g = drawCanvas.getGraphics();
        g.setColor(currentColor);

        if (hasMouseMoved(e))
        {
            g.setXORMode(xorColor);
            g.drawLine(originX, originY, mouseX, mouseY);

            mouseX = snapToX(e.getX());
            mouseY = snapToX(e.getY());

            g.drawLine(originX, originY, mouseX, mouseY);
        }
    }

    /**
     * Action when the shape is drawn.
     *
     * @param e
     */
    private void polyOperation(MouseEvent e)
    {
        if (logger.isDebugEnabled())
            logger.debug("[log] : polyOperation");
        Graphics g = drawCanvas.getGraphics();
        g.setColor(currentColor);

        if (doneDrawing)
        {
            reInitMouse(e);
            doneDrawing = false;
            pathList.clear();
        }

        mouseX = snapToX(e.getX());
        mouseY = snapToX(e.getY());

        originX = snapToX(e.getX());
        originY = snapToY(e.getY());

        WhiteboardPoint point = new WhiteboardPoint(mouseX, mouseY);
        pathList.add(point);
    }

    /**
     * Action when the shape is drawn.
     *
     * @param e
     */
    private void imageOperation(MouseEvent e)
    {
        Graphics g = drawCanvas.getGraphics();
        g.setColor(currentColor);

        if (doneDrawing)
        {
            reInitMouse(e);
            doneDrawing = false;
        }

        if (hasMouseMoved(e))
        {
            g.setXORMode(drawCanvas.getBackground());
            g.drawRect(drawX, drawY, originWidth, originHeight);

            mouseX = snapToX(e.getX());
            mouseY = snapToY(e.getY());

            setActualBoundry();

            g.drawRect(drawX, drawY, originWidth, originHeight);
        }
    }

    /**
     * Action when the shape modified
     *
     * @param e
     */
    private void modifOperation(MouseEvent e)
    {
        if (selectedShape == null)
        {
            return;
        }

        moving = true;
        if (doneDrawing)
        {
            reInitMouse(e);
            doneDrawing = false;
        }

        if (hasMouseMoved(e))
        {
            int x = snapToX(e.getX());
            int y = snapToY(e.getY());

            Point2D s0 = new Point2D.Double(mouseX, mouseY);
            Point2D s1 = new Point2D.Double(x, y);

            Point2D w0 = s2w.transform(s0, null);
            Point2D w1 = s2w.transform(s1, null);

            selectedShape.translateSelectedPoint(
                                        w1.getX() - w0.getX(),
                                        w1.getY() - w0.getY());

            mouseX = x;
            mouseY = y;
            repaint();
        }
    }

    /**
     * Action when the shape is moved.
     *
     * @param e
     */
    private void moveOperation(MouseEvent e)
    {
        if (selectedShape == null)
        {
            return;
        }

        moving = true;
        if (doneDrawing)
        {
            reInitMouse(e);
            doneDrawing = false;
        }
        int x = snapToX(e.getX());
        int y = snapToY(e.getY());

        if (hasMouseMoved(e))
        {
            Point2D s0 = new Point2D.Double(mouseX, mouseY);
            Point2D s1 = new Point2D.Double(x, y);

            Point2D w0 = s2w.transform(s0, null);
            Point2D w1 = s2w.transform(s1, null);

            selectedShape.translate(w1.getX() - w0.getX(), w1.getY()
                - w0.getY());
            mouseX = x;
            mouseY = y;

            repaint();
        }
    }

    /**
     * Action when the shape is translated.
     *
     * @param e
     */
    private void panOperation(MouseEvent e)
    {
        Point2D currentPoint = e.getPoint();

        Point2D wCurrentPoint = s2w.transform(currentPoint, null);
        Point2D wPreviousPoint = s2w.transform(previousPoint, null);

        double deltaX = wCurrentPoint.getX() - wPreviousPoint.getX();
        double deltaY = wCurrentPoint.getY() - wPreviousPoint.getY();

        translate(deltaX, deltaY);

        previousPoint = currentPoint;
    }

    /**
     * Returns the X coordinate on the grid.
     *
     * @param x current x mouse coordinate
     * @return X coordinate on the grid
     */
    private int snapToX(int x)
    {
        return gridMenuItem.isSelected() ? snap(x) : x;
    }

    /**
     * Returns the Y coordinate on the grid.
     *
     * @param y current y mouse coordinate
     * @return Y coordinate on the grid
     */
    private int snapToY(int y)
    {
        return gridMenuItem.isSelected() ? snap(y) : y;
    }

    /**
     * Rounds off the position to the grid.
     *
     * @param i an approximate value
     * @return a value on the grid
     */
    private int snap(int i)
    {
        int snap = i % defaultGrid;
        if (snap < ((double) defaultGrid / (double) 2))
        {
            return (i - snap);
        }
        return (i + defaultGrid - snap);
    }

    /**
     * Returns true if mouse has moved since last test.
     *
     * @param e
     * @return true if mouse has moved
     */
    private boolean hasMouseMoved(MouseEvent e)
    {
        return (mouseX != e.getX() || mouseY != e.getY());
    }

    /**
     * Returns the current selected WhiteboardShape.
     *
     * @return selected shape
     */
    public WhiteboardShape getSelectedShape()
    {
        return selectedShape;
    }

    /**
     * Returns the current copied WhiteboardShape
     *
     * @return the copied shape
     */
    public WhiteboardShape getCopiedShape()
    {
        return copiedShape;
    }

    /**
     * Calculate the new values for the global varibles drawX and drawY
     * according to the new positions of the mouse cursor.
     */
    private void setActualBoundry()
    {
        if (mouseX < originX || mouseY < originY)
        {
            if (mouseX < originX)
            {
                originWidth = originX - mouseX;
                drawX = originX - originWidth;
            }
            else
            {
                drawX = originX;
                originWidth = mouseX - originX;
            }

            if (mouseY < originY)
            {
                originHeight = originY - mouseY;
                drawY = originY - originHeight;
            }
            else
            {
                drawY = originY;
                originHeight = mouseY - originY;
            }
        }
        else
        {
            drawX = originX;
            drawY = originY;
            originWidth = mouseX - originX;
            originHeight = mouseY - originY;
        }
    }

    /**
     * Set drawing variables to the current position of the cursor. Height and
     * width varibles are zeroed off.
     *
     * @param e MouseEvent
     */
    private void reInitMouse(MouseEvent e)
    {
        int x = snapToX(e.getX());
        int y = snapToY(e.getY());
        mouseX = x;
        mouseY = y;
        previousMouseX = x;
        previousMouseY = y;
        originX = x;
        originY = y;
        drawX = x;
        drawY = y;
        originWidth = 0;
        originHeight = 0;
    }

    /**
     * Method to create-add-send a WhiteboardShapePath when mouse released
     */
    private void releasedPen()
    {
        doneDrawing = true;
        appendAndSend(new WhiteboardShapePath(id(), spinModel.getNumber()
            .intValue(), currentColor, pathList, s2w));
        pathList.clear();
    }

    /**
     * Method to create-add-send a WhiteboardShapeRect when mouse released.
     *
     * @param fill true if is fill
     */
    private void releasedRectangle(boolean fill)
    {
        doneDrawing = true;
        appendAndSend(new WhiteboardShapeRect(id(), spinModel.getNumber()
            .intValue(), currentColor, new WhiteboardPoint(drawX, drawY),
            originWidth, originHeight, fill, s2w));
    }

    /**
     * Method to create-add-send a WhiteboardShapeImage when mouse released
     */
    private void releasedImage()
    {
        doneDrawing = true;

        SipCommFileChooser chooser = GenericFileDialog.create(
            WhiteboardFrame.this,
            "Choose image...",
            SipCommFileChooser.LOAD_FILE_OPERATION);

        File file = chooser.getFileFromDialog();
        WhiteboardFileFilter filter =
            new WhiteboardFileFilter("jpg", "JPEG Files (*.jpg)");

        chooser.addFilter(filter);

        if (file != null)
        {
            try
            {
                lastDir = file.getParentFile();

                FileInputStream in = new FileInputStream(file);
                byte[] buffer;

                try
                {
                    buffer = new byte[in.available()];
                    in.read(buffer);
                }
                finally
                {
                    in.close();
                }

                WhiteboardShapeImage image
                    = new WhiteboardShapeImage(
                            id(),
                            new WhiteboardPoint(drawX, drawY),
                            originWidth, originHeight,
                            buffer);

                appendAndSend(image);
            }
            catch (IOException ex)
            {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Method to create-add-send a WhiteboardShapeLine when mouse released
     */
    private void releasedLine()
    {
        if ((Math.abs(originX - mouseX) + Math.abs(originY - mouseY)) != 0)
        {
            doneDrawing = true;
            appendAndSend(new WhiteboardShapeLine(id(), spinModel.getNumber()
                .intValue(), currentColor,
                new WhiteboardPoint(originX, originY), new WhiteboardPoint(
                    mouseX, mouseY), s2w));
        }
    }

    /**
     * Method to create-add-send a WhiteboardShapeCircle when mouse released.
     *
     * @param fill true if is fill
     */
    private void releasedCircle(boolean fill)
    {
        doneDrawing = true;

        int r = originWidth / 2;
        int cx = drawX + r;
        int cy = drawY + r;

        appendAndSend(new WhiteboardShapeCircle(id(), spinModel.getNumber()
            .intValue(), currentColor, new WhiteboardPoint(cx, cy), r, fill,
            s2w));
    }

    /**
     * Method to create-add-send a WhiteboardShapePolyLine when mouse released.
     *
     * @param fill true if is fill
     */
    private void releasedPolyline(boolean fill)
    {
        if (logger.isDebugEnabled())
            logger.debug("[log] : releasedPolyline");
        doneDrawing = true;
        appendAndSend(new WhiteboardShapePolyLine(id(), spinModel.getNumber()
            .intValue(), currentColor, pathList, fill, s2w));
        pathList.clear();
    }

    /**
     * Method to create-add-send a WhiteboardShapePolygon when mouse released.
     *
     * @param fill true if is fill
     */
    private void releasedPolygon(boolean fill)
    {
        doneDrawing = true;
        appendAndSend(new WhiteboardShapePolygon(id(), spinModel.getNumber()
            .intValue(), currentColor, pathList, fill, s2w));
        pathList.clear();
    }

    /**
     * Method to create-add-send a WhiteboardShapeText when mouse released.
     *
     * @param x x coord
     * @param y y coord
     */
    private void releasedText(int x, int y)
    {

        doneDrawing = true;

        Graphics g = drawCanvas.getGraphics();
        g.setColor(Color.BLACK);
        g.drawLine(x, y - 10, x, y + 10);

        String t =
            (String) JOptionPane.showInputDialog(this,
                "Please enter your text", "plugin.whiteboard.TEXT",
                JOptionPane.QUESTION_MESSAGE,
                null, null, "plugin.whiteboard.TEXT");

        if (t != null && t.length() > 0)
        {
            appendAndSend(new WhiteboardShapeText(id(), currentColor,
                new WhiteboardPoint(x, y), defaultFontSize, t, s2w));
        }

    }

    /**
     * When a shap is modified we send a message with the new position rather
     * than deleting the shape
     */
    private void releasedModif()
    {
        if (moving)
        {
            if (selectedShape == null)
                return;
            doneDrawing = true;
            sendMoveShape(selectedShape);
            selectedShape = null;
            moving = false;
            repaint();
        }
    }

    /**
     * When a shap is moved we send a message with the new position rather than
     * deleting the shape
     */
    private void releasedMove()
    {
        if (moving)
        {
            if (selectedShape == null)
                return;
            doneDrawing = true;
            sendMoveShape(selectedShape);
            selectedShape = null;
            moving = false;
            repaint();
        }
    }

    /**
     * Returns a String uniquely identifying this WhiteboardFrame
     *
     * @return identifier of this WhiteboardFrame
     */
    private String id()
    {
        return String.valueOf(System.currentTimeMillis())
            + String.valueOf(super.hashCode());
    }

    /**
     * Translates current frame
     *
     * @param xTrans x coord
     * @param yTrans y coord
     */
    private void translate(double xTrans, double yTrans)
    {
        if (w2s.getDeterminant() != 0)
        {
            w2s.translate(xTrans, yTrans);
            try
            {
                s2w = w2s.createInverse();
            }
            catch (NoninvertibleTransformException e)
            {
                if (logger.isDebugEnabled())
                    logger.debug(e.getMessage());
            }
            repaint();
        }
    }

    /**
     * Deselect all previously selected shapes.
     */
    private void deselect()
    {
        WhiteboardShape shape;
        for (int i = 0; i < displayList.size(); i++)
        {
            shape = displayList.get(i);
            shape.setSelected(false);
            shape.setModifyPoint(null);
        }
        selectedShape = null;
    }

    /**
     * Appends a Shape in the shape list and send it
     *
     * @param s shape to send and append
     */
    private void appendAndSend(WhiteboardShape s)
    {
        displayList.add(s);
        repaint();
        sendShape(s);
    }

    /**
     * Method to send a shape.
     *
     * @param shape the white-board shape to send
     */
    private void sendShape(WhiteboardShape shape)
    {
        WhiteboardObject wbObject;

        try
        {
            wbObject = sessionManager.sendWhiteboardObject(session, shape);

            if (wbObject != null)
                shape.setID(wbObject.getID());
        }
        catch (OperationFailedException ex)
        {
            ex.printStackTrace();
        }
    }

    /**
     * Method to send an existing shape (to move/modify it).
     *
     * @param shape the white-board shape to send
     */
    private void sendMoveShape(WhiteboardShape shape)
    {
        sessionManager.moveWhiteboardObject(session, shape);
    }

    /**
     * Method to delete a shape at the contact.
     *
     * @param shape the white-board shape to delete
     */
    private void sendDeleteShape(WhiteboardShape shape)
    {
        sessionManager.deleteWhiteboardObject(session, shape);
    }

    /**
     * Method for add a WhiteboardObject into this frame.
     *
     * @param wbo WhiteboardObject to add
     */
    public void receiveWhiteboardObject(WhiteboardObject wbo)
    {
        if (logger.isDebugEnabled())
            logger.debug("receiveWhiteboardObject: " + wbo.getID());
        WhiteboardShape ws = createWhiteboardShape(wbo);
        for (int i = 0; i < displayList.size(); i++)
        {
            WhiteboardShape wbs = displayList.get(i);
            if (wbs.getID().equals(wbo.getID()))
            {
                displayList.set(i, ws);
                repaint();
                return;
            }
        }
        displayList.add(ws);
        repaint();
    }

    /**
     * Method for delete a WhiteboardObject in this frame.
     *
     * @param id WhiteboardObject's identifier to delete
     */
    public void receiveDeleteWhiteboardObject(String id)
    {
        if (logger.isDebugEnabled())
            logger.debug("receiveDeleteWhiteboardObject");
        int i = 0;
        while (i < displayList.size())
        {
            WhiteboardShape wbs = displayList.get(i);
            if (id.equals(wbs.getID()))
                displayList.remove(i);
            else
                i++;
        }
        repaint();
        return;
    }

    /**
     * Method to create/convert a WhiteboardShape with a WhiteboardObject.
     *
     * @param wbo WhiteboardObject to convert
     * @return WhiteboardShape
     */
    private WhiteboardShape createWhiteboardShape(WhiteboardObject wbo)
    {
        if (logger.isDebugEnabled())
            logger.debug("CreateWhiteboardShape");
        WhiteboardShape wShape = null;
        String id = wbo.getID();
        int color = wbo.getColor();
        int t = wbo.getThickness();

        if (wbo instanceof WhiteboardObjectPath)
        {
            WhiteboardObjectPath path = (WhiteboardObjectPath) wbo;
            if (logger.isDebugEnabled())
                logger.debug("[log] : WB_PATH");
            Color c = Color.getColor("", color);
            List<WhiteboardPoint> points = path.getPoints();
            wShape = new WhiteboardShapePath(id, t, c, points);
        }
        else if (wbo instanceof WhiteboardObjectPolyLine)
        {
            WhiteboardObjectPolyLine pLine = (WhiteboardObjectPolyLine) wbo;
            if (logger.isDebugEnabled())
                logger.debug("[log] : WB_POLYLINE");
            Color c = Color.getColor("", color);
            List<WhiteboardPoint> points = pLine.getPoints();
            wShape = new WhiteboardShapePolyLine(id, t, c, points, false);

        }
        else if (wbo instanceof WhiteboardObjectPolygon)
        {
            WhiteboardObjectPolygon polygon = (WhiteboardObjectPolygon) wbo;
            if (logger.isDebugEnabled())
                logger.debug("[log] : WB_POLYGON");
            Color c = Color.getColor("", color);
            List<WhiteboardPoint> points = polygon.getPoints();
            boolean fill = polygon.isFill();
            wShape = new WhiteboardShapePolygon(id, t, c, points, fill);

        }
        else if (wbo instanceof WhiteboardObjectLine)
        {
            WhiteboardObjectLine line = (WhiteboardObjectLine) wbo;
            if (logger.isDebugEnabled())
                logger.debug("[log] : WB_LINE");
            WhiteboardPoint pStart = line.getWhiteboardPointStart();
            WhiteboardPoint pEnd = line.getWhiteboardPointEnd();
            Color c = Color.getColor("", color);
            wShape = new WhiteboardShapeLine(id, t, c, pStart, pEnd);

        }
        else if (wbo instanceof WhiteboardObjectRect)
        {
            WhiteboardObjectRect rect = (WhiteboardObjectRect) wbo;
            if (logger.isDebugEnabled())
                logger.debug("[log] : WB_RECT");
            Color c = Color.getColor("", color);
            double height, width;
            WhiteboardPoint p = rect.getWhiteboardPoint();
            width = rect.getWidth();
            height = rect.getHeight();
            boolean fill = rect.isFill();
            wShape = new WhiteboardShapeRect(id, t, c, p, width, height, fill);

        }
        else if (wbo instanceof WhiteboardObjectCircle)
        {
            WhiteboardObjectCircle circle = (WhiteboardObjectCircle) wbo;
            if (logger.isDebugEnabled())
                logger.debug("[log] : WB_CIRCLE");
            Color c = Color.getColor("", color);
            WhiteboardPoint p = circle.getWhiteboardPoint();
            double r = circle.getRadius();
            boolean fill = circle.isFill();
            wShape = new WhiteboardShapeCircle(id, t, c, p, r, fill);

        }
        else if (wbo instanceof WhiteboardObjectText)
        {
            WhiteboardObjectText text = (WhiteboardObjectText) wbo;
            if (logger.isDebugEnabled())
                logger.debug("[log] : WB_TEXT");
            Color c = Color.getColor("", color);
            WhiteboardPoint p = text.getWhiteboardPoint();
            int size = text.getFontSize();
            String txt = text.getText();
            wShape = new WhiteboardShapeText(id, c, p, size, txt);

        }
        else if (wbo instanceof WhiteboardObjectImage)
        {
            WhiteboardObjectImage img = (WhiteboardObjectImage) wbo;
            if (logger.isDebugEnabled())
                logger.debug("[log] : WB_IMAGE");
            double height, width;
            WhiteboardPoint p = img.getWhiteboardPoint();
            width = img.getWidth();
            height = img.getHeight();
            byte[] b = img.getBackgroundImage();
            wShape = new WhiteboardShapeImage(id, p, width, height, b);
        }
        return wShape;
    }

    /**
     * Deletes currently selected shape.
     */
    public void deleteSelected()
    {
        WhiteboardShape s;
        int i = 0;
        while (i < displayList.size())
        {
            s = displayList.get(i);
            if (s.isSelected())
            {
                this.sendDeleteShape(s);
                displayList.remove(i);
            }
            else
                i++;
        }
        repaint();
    }

    /**
     * Call a color chooser for the current selected shape
     */
    public void chooseColor()
    {
        if (selectedShape != null)
        {
            colorChooser.setColor(new Color(selectedShape.getColor()));
        }

        if (colorChooserDialog == null)
        {
            ActionListener okListener = new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    currentColor = colorChooser.getColor();
                    jLabelColor.setBackground(currentColor);
                    colorChooserDialog.setVisible(false);

                    if (selectedShape != null)
                    {
                        selectedShape.setColor(currentColor);
                        sendMoveShape(selectedShape);
                        repaint();
                    }
                }
            };

            ActionListener cancelListener = new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    colorChooserDialog.setVisible(false);
                }
            };

            colorChooserDialog =
                JColorChooser.createDialog(this, "Choose a color", false,
                    colorChooser, okListener, cancelListener);
        }

        colorChooserDialog.setVisible(true);
    }

    /**
     * Sets the current contact. (temporary) -> WhiteboardParticipant
     *
     * @param c Contact to use in the WhiteboardFrame
     */
    public void setContact(Contact c)
    {
        this.contact = c;
        this.setTitle(getTitle() + " - " + contact.getDisplayName());
    }

    /**
     * Returns contact used in this WhiteboardFrame.
     *
     * @return used Contact in the WhiteboardFrame
     */
    public Contact getContact()
    {
        return this.contact;
    }

    /**
     * Current WhiteboardSession used in the WhiteboardFrame.
     *
     * @return current WhiteboardSession
     */
    public WhiteboardSession getWhiteboardSession()
    {
        return this.session;
    }

    @Override
    protected void close(boolean isEscaped)
    {
    }

    private class WhiteboardChangeListenerImpl
        implements WhiteboardChangeListener
    {
        public void whiteboardParticipantAdded(WhiteboardParticipantEvent evt)
        {
        }

        public void whiteboardParticipantRemoved(WhiteboardParticipantEvent evt)
        {
            if (logger.isTraceEnabled())
                logger.trace("Whiteboard participant has left.");

            WhiteboardActivator.getUiService().getPopupDialog()
                .showMessagePopupDialog(contact.getAddress()
                        + " has left the whiteboard.");
        }

        public void whiteboardStateChanged(WhiteboardChangeEvent evt)
        {
        }
    }
}
