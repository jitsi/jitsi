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
import java.awt.Container;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.*;
import javax.swing.tree.*;

/**
 * SIPCommTreeUI implementation.
 *
 * @author Yana Stamcheva
 */
public class SIPCommTreeUI
    extends BasicTreeUI
    implements HierarchyListener,
               TreeSelectionListener
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
    @Override
    public void installUI(JComponent c)
    {
        if ( c == null )
            throw new NullPointerException(
                "null component passed to BasicTreeUI.installUI()" );

        tree = (JTree)c;

        JViewport v = getFirstParentViewport(tree);
        if(v != null)
            this.parentViewport = v;
        else
            tree.addHierarchyListener(this);

        tree.getSelectionModel().addTreeSelectionListener(this);

        super.installUI(c);
    }

    /**
     * Returns the first parent view port found.
     * @param c the component parents we search
     * @return the first parent view port found.
     */
    private JViewport getFirstParentViewport(Container c)
    {
        if(c == null)
            return null;
        else
            if(c instanceof JViewport)
                return (JViewport)c;
            else
                return getFirstParentViewport(c.getParent());
    }

    /**
     * On uninstalling the ui remove the listeners.
     * @param c
     */
    @Override
    public void uninstallUI(JComponent c)
    {
        tree.getSelectionModel().clearSelection();
        tree.getSelectionModel().removeTreeSelectionListener(this);
        tree.removeHierarchyListener(this);

        super.uninstallUI(c);
    }

    /**
     * HierarchyListener's method.
     * @param e the event.
     */
    public void hierarchyChanged(HierarchyEvent e)
    {
        if (e.getID() == HierarchyEvent.HIERARCHY_CHANGED
            && (e.getChangeFlags() & HierarchyEvent.PARENT_CHANGED) != 0
            && e.getChangedParent() instanceof JViewport)
        {
            parentViewport = (JViewport) e.getChangedParent();
        }
    }

    /**
     * The TreeSelectionListener's method.
     * @param e the event.
     */
    public void valueChanged(TreeSelectionEvent e)
    {
        // Update cell size.
        selectionChanged(   e.getOldLeadSelectionPath(),
                            e.getNewLeadSelectionPath());
    }

    /**
     * Installs the defaults of this UI.
     */
    @Override
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
    @Override
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
    @Override
    protected void selectPathForEvent(TreePath path, MouseEvent event)
    {
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
        @Override
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
