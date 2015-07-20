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
import java.awt.print.*;
import java.util.List;

import javax.swing.*;
import javax.swing.plaf.*;

import net.java.sip.communicator.plugin.whiteboard.gui.whiteboardshapes.*;
import net.java.sip.communicator.plugin.desktoputil.*;

/**
 * Panel for drawing shapes
 *
 * @author Julien Waechter
 */
public class WhiteboardPanel
    extends JPanel
    implements Printable
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Shapes to display
     */
    private final List<WhiteboardShape> displayList;

    /**
     * Default grid space
     */
    private int defaultGrid = 25;

    private AffineTransform affineTrans;

    /**
     * True: display grid, false no grid.
     */
    private boolean grid = false;

    /**
     * Parent WhiteboardFrame
     */
    private final WhiteboardFrame wf;

    /**
     * WhiteboardPanel constructor.
     *
     * @param displayList Shapes to display
     * @param wf WhiteboardFrame
     */
    public WhiteboardPanel(List<WhiteboardShape> displayList, WhiteboardFrame wf)
    {
        this.wf = wf;
        this.displayList = displayList;
        affineTrans = new AffineTransform();
        affineTrans.setToScale(1, 1);
        setBackground(Color.white);
        initComponents();
    }

    /**
     * Method to draw/hide grid
     *
     * @param grid if true, draw grid
     */
    public void drawGrid(boolean grid)
    {
        this.grid = grid;
    }

    /**
     * Calls the UI delegate's paint method, if the UI delegate is non-<code>null</code>.
     * We pass the delegate a copy of the <code>Graphics</code> object to
     * protect the rest of the paint code from irrevocable changes (for example,
     * <code>Graphics.translate</code>).
     * <p>
     * The passed in <code>Graphics</code> object might have a transform other
     * than the identify transform installed on it. In this case, you might get
     * unexpected results if you cumulatively apply another transform.
     *
     * @param g the <code>Graphics</code> object to protect
     * @see #paint
     * @see ComponentUI
     */
    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        g = g.create();
        try
        {
            AntialiasingManager.activateAntialiasing(g);

            if (grid)
            {
                for (int x = 0; x < this.getWidth(); x += defaultGrid)
                {
                    for (int y = 0; y < this.getHeight(); y += defaultGrid)
                    {
                        g.setColor(Color.LIGHT_GRAY);
                        g.fillOval(x, y, 2, 2);
                    }
                }
            }

            for (WhiteboardShape s : displayList)
                s.paint(g, affineTrans);
        }
        finally
        {
            g.dispose();
        }
    }

    /**
     * Prints the page at the specified index into the specified
     * {@link Graphics} context in the specified format. A
     * <code>PrinterJob</code> calls the <code>Printable</code> interface to
     * request that a page be rendered into the context specified by
     * <code>graphics</code>. The format of the page to be drawn is specified
     * by <code>pageFormat</code>. The zero based index of the requested page
     * is specified by <code>pageIndex</code>. If the requested page does not
     * exist then this method returns NO_SUCH_PAGE; otherwise PAGE_EXISTS is
     * returned. The <code>Graphics</code> class or subclass implements the
     * {@link PrinterGraphics} interface to provide additional information. If
     * the <code>Printable</code> object aborts the print job then it throws a
     * {@link PrinterException}.
     *
     * @param graphics the context into which the page is drawn
     * @param pageFormat the size and orientation of the page being drawn
     * @param pageIndex the zero based index of the page to be drawn
     * @return PAGE_EXISTS if the page is rendered successfully or NO_SUCH_PAGE
     *         if <code>pageIndex</code> specifies a non-existent page.
     */
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex)
    {
        return NO_SUCH_PAGE;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code
    // ">//GEN-BEGIN:initComponents
    private void initComponents()
    {
        popupMenu = new javax.swing.JPopupMenu();
        copyPopupMenuItem = new javax.swing.JMenuItem();
        pastePopupMenuItem = new javax.swing.JMenuItem();
        colorPopupMenuItem = new javax.swing.JMenuItem();
        propetiesPopupMenuItem = new javax.swing.JMenuItem();
        deletePopupMenuItem = new javax.swing.JMenuItem();

        copyPopupMenuItem.setText("Copy");
        popupMenu.add(copyPopupMenuItem);

        pastePopupMenuItem.setText("Paste");
        popupMenu.add(pastePopupMenuItem);

        colorPopupMenuItem.setText("Color");
        colorPopupMenuItem
            .addActionListener(new java.awt.event.ActionListener()
            {
                public void actionPerformed(java.awt.event.ActionEvent evt)
                {
                    colorPopupMenuItemActionPerformed(evt);
                }
            });

        popupMenu.add(colorPopupMenuItem);

        propetiesPopupMenuItem.setText("Properties");
        popupMenu.add(propetiesPopupMenuItem);

        deletePopupMenuItem.setText("Delete");
        deletePopupMenuItem
            .addActionListener(new java.awt.event.ActionListener()
            {
                public void actionPerformed(java.awt.event.ActionEvent evt)
                {
                    deletePopupMenuItemActionPerformed(evt);
                }
            });

        popupMenu.add(deletePopupMenuItem);

        setLayout(null);

        addMouseListener(new java.awt.event.MouseAdapter()
        {
            @Override
            public void mousePressed(java.awt.event.MouseEvent evt)
            {
                formMousePressed(evt);
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent evt)
            {
                formMouseReleased(evt);
            }
        });

    }// </editor-fold>//GEN-END:initComponents

    /**
     * Invoked when an action occurs on the delete popup menu.
     *
     * @param evt
     */
    private void deletePopupMenuItemActionPerformed(
        java.awt.event.ActionEvent evt)
    {// GEN-FIRST:event_deletePopupMenuItemActionPerformed
        wf.deleteSelected();
    }// GEN-LAST:event_deletePopupMenuItemActionPerformed

    /**
     * Invoked when an action occurs on the color popup menu.
     *
     * @param evt
     */
    private void colorPopupMenuItemActionPerformed(
        java.awt.event.ActionEvent evt)
    {// GEN-FIRST:event_colorPopupMenuItemActionPerformed
        wf.chooseColor();
    }// GEN-LAST:event_colorPopupMenuItemActionPerformed

    /**
     * Invoked when a mouse button has been released on the WhiteboardPanel.
     *
     * @param evt
     */
    private void formMouseReleased(java.awt.event.MouseEvent evt)
    {// GEN-FIRST:event_formMouseReleased
        checkPopupEvent(evt);
    }// GEN-LAST:event_formMouseReleased

    /**
     * Invoked when a mouse button has been pressed on the WhiteboardPanel.
     *
     * @param evt
     */
    private void formMousePressed(java.awt.event.MouseEvent evt)
    {// GEN-FIRST:event_formMousePressed
        checkPopupEvent(evt);
    }// GEN-LAST:event_formMousePressed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem colorPopupMenuItem;

    private javax.swing.JMenuItem copyPopupMenuItem;

    private javax.swing.JMenuItem deletePopupMenuItem;

    private javax.swing.JMenuItem pastePopupMenuItem;

    private javax.swing.JPopupMenu popupMenu;

    private javax.swing.JMenuItem propetiesPopupMenuItem;

    // End of variables declaration//GEN-END:variables

    /**
     * Manage popup event
     *
     * @param e MouseEvent
     */
    private void checkPopupEvent(MouseEvent e)
    {
        copyPopupMenuItem.setEnabled(false);
        pastePopupMenuItem.setEnabled(false);
        colorPopupMenuItem.setEnabled(false);
        propetiesPopupMenuItem.setEnabled(false);
        deletePopupMenuItem.setEnabled(false);

        if (e.isPopupTrigger())
        {
            if (wf.getSelectedShape() != null)
            {
                copyPopupMenuItem.setEnabled(true);
                colorPopupMenuItem.setEnabled(true);
                propetiesPopupMenuItem.setEnabled(true);
                deletePopupMenuItem.setEnabled(true);
            }

            if (wf.getCopiedShape() != null)
            {
                pastePopupMenuItem.setEnabled(true);
            }
            if (e.getButton() == MouseEvent.BUTTON3)
                popupMenu.show(e.getComponent(), e.getX(), e.getY());
        }
    }
}
