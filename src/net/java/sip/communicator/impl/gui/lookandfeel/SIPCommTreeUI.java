/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.lookandfeel;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.*;
import javax.swing.tree.*;

import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.impl.gui.main.contactlist.contactsource.*;

/**
 * SIPCommTreeUI implementation.
 * 
 * @author Yana Stamcheva
 */
public class SIPCommTreeUI
    extends BasicTreeUI
{
    private static JTree tree;

    private JViewport parentViewport;

    private VariableLayoutCache layoutCache;

    /**
     * Last selected index.
     */
    private int lastSelectedIndex;

    /**
     * Creates the UI for the given component.
     * @param c the component for which we're create an UI
     * @return this UI implementation
     */
    public static ComponentUI createUI(JComponent c)
    {
        return new SIPCommTreeUI();
    }

    /**
     * Installs this UI to the given component.
     * @param c the component to which to install this UI
     */
    public void installUI(JComponent c)
    {
        if ( c == null )
            throw new NullPointerException(
                "null component passed to BasicTreeUI.installUI()" );

        tree = (JTree)c;

        tree.getSelectionModel().addTreeSelectionListener(
            new TreeSelectionListener()
        {
            public void valueChanged(TreeSelectionEvent e)
            {
                // Update cell size.
                selectionChanged(   e.getOldLeadSelectionPath(),
                                    e.getNewLeadSelectionPath());
            }
        });

        tree.addHierarchyListener(new HierarchyListener()
        {
            public void hierarchyChanged(HierarchyEvent e)
            {
                if (e.getID() == HierarchyEvent.HIERARCHY_CHANGED
                    && (e.getChangeFlags() & HierarchyEvent.PARENT_CHANGED) != 0
                    && e.getChangedParent() instanceof JViewport)
                {
                    parentViewport = (JViewport) e.getChangedParent();
                }
            }
        });

        super.installUI(c);
    }

    /**
     * Installs the defaults of this UI.
     */
    protected void installDefaults()
    {
        if(tree.getBackground() == null ||
           tree.getBackground() instanceof UIResource) {
            tree.setBackground(UIManager.getColor("Tree.background"));
        } 
        if(getHashColor() == null || getHashColor() instanceof UIResource) {
            setHashColor(UIManager.getColor("Tree.hash"));
        }
        if (tree.getFont() == null || tree.getFont() instanceof UIResource)
            tree.setFont( UIManager.getFont("Tree.font") );
            // JTree's original row height is 16.  To correctly display the
            // contents on Linux we should have set it to 18, Windows 19 and
            // Solaris 20.  As these values vary so much it's too hard to
            // be backward compatable and try to update the row height, we're
            // therefor NOT going to adjust the row height based on font. If the
            // developer changes the font, it's there responsibility to update
            // the row height.

        setExpandedIcon(null);
        setCollapsedIcon(null);

        setLeftChildIndent(0);
        setRightChildIndent(0);

        LookAndFeel.installProperty(tree, "rowHeight",
                        UIManager.get("Tree.rowHeight"));

        largeModel = (tree.isLargeModel() && tree.getRowHeight() > 0);

        Object scrollsOnExpand = UIManager.get("Tree.scrollsOnExpand");
        if (scrollsOnExpand != null) {
            LookAndFeel.installProperty(
                tree, "scrollsOnExpand", scrollsOnExpand);
        }

        UIManager.getDefaults().put("Tree.paintLines", false);
        UIManager.getDefaults().put("Tree.lineTypeDashed", false);
    }

    /**
     * Creates the object responsible for managing what is expanded, as
     * well as the size of nodes.
     * @return the created layout cache
     */
    protected AbstractLayoutCache createLayoutCache()
    {
        layoutCache = new VariableLayoutCache();
        return layoutCache;
    }

    /**
     * Do not select the <tt>ShowMoreContact</tt>.
     *
     * @param path the <tt>TreePath</tt> to select
     * @param event the <tt>MouseEvent</tt> that provoked the select
     */
    protected void selectPathForEvent(TreePath path, MouseEvent event)
    {
        Object lastComponent = path.getLastPathComponent();

        // Open right button menu when right mouse is pressed.
        if (lastComponent instanceof ContactNode)
        {
            UIContact uiContact
                = ((ContactNode) lastComponent).getContactDescriptor();

            if (!(uiContact instanceof ShowMoreContact))
            {
                super.selectPathForEvent(path, event);
            }
        }
        else
            super.selectPathForEvent(path, event);
    }

    /**
     * A custom layout cache that recalculates the width of the cell the match
     * the width of the tree (i.e. expands the cell to the right).
     */
    private class VariableLayoutCache extends VariableHeightLayoutCache
    {
        /**
         * Returns the preferred width of the receiver.
         * @param path the path, which bounds we obtain
         * @param placeIn the initial rectangle of the path
         * @return the bounds of the path
         */
        public Rectangle getBounds(TreePath path, Rectangle placeIn)
        {
            Rectangle rect =  super.getBounds(path, placeIn);

            if (rect != null && parentViewport != null)
            {
                rect.width = parentViewport.getWidth() - 2;
            }

            return rect;
        }
    }

    /**
     * Ensures the tree size.
     */
    private void ensureTreeSize()
    {
        // Update tree height.
        updateSize();

        // Finally repaint in order the change to take place.
        tree.repaint();
    }

    /**
     * Refreshes row sizes corresponding to the given paths.
     *
     * @param oldPath the old selection path
     * @param newPath the new selection path
     */
    public void selectionChanged(TreePath oldPath, TreePath newPath)
    {
        if (oldPath != null)
            layoutCache.invalidatePathBounds(oldPath);

        if (newPath != null)
        {
            layoutCache.invalidatePathBounds(newPath);
            lastSelectedIndex = tree.getRowForPath(newPath);
        }
        // If the selection has disappeared, for example when the selected row
        // has been removed, refresh the previously selected row.
        else
        {
            int nextRow = (tree.getRowCount() > lastSelectedIndex)
                ? lastSelectedIndex : tree.getRowCount() - 1;

            layoutCache.invalidatePathBounds(
                tree.getPathForRow(nextRow));
        }

        ensureTreeSize();
    }
}
