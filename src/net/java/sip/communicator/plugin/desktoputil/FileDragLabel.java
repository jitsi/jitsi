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
package net.java.sip.communicator.plugin.desktoputil;

import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.io.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.util.*;

/**
 * The <tt>FileDragLabel</tt> extends <tt>JLabel</tt> and associates to it a
 * file. The label is made draggable and it is possible to drag it directly to
 * the file browser of the operating system.
 *
 * @author Yana Stamcheva
 */
public class FileDragLabel
    extends JLabel
    implements  DropTargetListener,
                DragSourceListener,
                DragGestureListener
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    private static final Logger logger = Logger.getLogger(FileDragLabel.class);

    private final DragSource dragSource = DragSource.getDefaultDragSource();

    private File file;

    /**
     * Creates a <tt>FileDragLabel</tt>.
     */
    public FileDragLabel()
    {
        dragSource.createDefaultDragGestureRecognizer(
            this, DnDConstants.ACTION_COPY, this);
    }

    /**
     * Sets the file associated with this file drag label.
     *
     * @param file the file associated with this file drag label
     */
    public void setFile(File file)
    {
        this.file = file;
    }

    /**
     * Called while a drag operation is ongoing, when the mouse pointer enters
     * the operable part of the drop site for the <code>DropTarget</code>
     * registered with this listener.
     *
     * @param dropTargetDragEvent the <code>DropTargetDragEvent</code>
     */
    public void dragEnter(DropTargetDragEvent dropTargetDragEvent)
    {
        dropTargetDragEvent.acceptDrag(DnDConstants.ACTION_COPY_OR_MOVE);
    }

    /**Called when the drag operation has terminated with a drop on
     * the operable part of the drop site for the <code>DropTarget</code>
     * registered with this listener.
     */
    public synchronized void drop(DropTargetDropEvent event)
    {
        try
        {
            Transferable transferable = event.getTransferable();

            if (transferable.isDataFlavorSupported(
                    DataFlavor.javaFileListFlavor))
            {
                event.acceptDrop(DnDConstants.ACTION_COPY);
                event.getDropTargetContext().dropComplete(true);
            }
            else
            {
                event.rejectDrop();
            }
        }
        catch (Exception ex)
        {
            if (logger.isDebugEnabled())
                logger.debug("Unable to drop label.", ex);
            event.rejectDrop();
        }
    }

    /**
     * A <code>DragGestureRecognizer</code> has detected
     * a platform-dependent drag initiating gesture and
     * is notifying this listener
     * in order for it to initiate the action for the user.
     * <P>
     * @param dragGestureEvent the <code>DragGestureEvent</code> describing
     * the gesture that has just occurred
     */
    public void dragGestureRecognized(DragGestureEvent dragGestureEvent)
    {
        if (file == null)
        {
            // Nothing selected, nothing to drag
            getToolkit().beep();
        }
        else
        {
            FileTransferable transferable = new FileTransferable(file);
            dragGestureEvent.startDrag( DragSource.DefaultCopyDrop,
                                        transferable,
                                        this);
        }
    }

    public void dragDropEnd(DragSourceDropEvent DragSourceDropEvent) {}

    public void dragEnter(DragSourceDragEvent DragSourceDragEvent) {}

    public void dragExit(DragSourceEvent DragSourceEvent) {}

    public void dragOver(DragSourceDragEvent DragSourceDragEvent) {}

    public void dropActionChanged(DragSourceDragEvent DragSourceDragEvent) {}

    public void dragExit(DropTargetEvent dropTargetEvent) {}

    public void dragOver(DropTargetDragEvent dropTargetDragEvent) {}

    public void dropActionChanged(DropTargetDragEvent dropTargetDragEvent) {}

    /**
     * File transferable.
     */
    @SuppressWarnings("deprecation") //can't find an alternative.
    private class FileTransferable
        extends Vector<File>
        implements Transferable
    {
        /**
         * Serial version UID.
         */
        private static final long serialVersionUID = 0L;

        final static int FILE = 0;
        final static int STRING = 1;
        final static int PLAIN = 2;

        // Don't have other possibility for now instead of using the deprecated
        // plainTextFlavor method.
        DataFlavor flavors[] = {DataFlavor.javaFileListFlavor,
                                DataFlavor.stringFlavor,
                                DataFlavor.plainTextFlavor};

        public FileTransferable(File file)
        {
            addElement(file);
        }

        public synchronized DataFlavor[] getTransferDataFlavors()
        {
            return flavors;
        }

        public boolean isDataFlavorSupported(DataFlavor flavor)
        {
            return
                flavor.equals(flavors[FILE])
                    || flavor.equals(flavors[STRING])
                    || flavor.equals(flavors[PLAIN]);
        }

        public synchronized Object getTransferData(DataFlavor flavor)
            throws UnsupportedFlavorException, IOException
        {
            if (flavor.equals(flavors[FILE]))
            {
                return this;
            }
            else if (flavor.equals(flavors[PLAIN]))
            {
                return new StringReader(file.getAbsolutePath());
            }
            else if (flavor.equals(flavors[STRING]))
            {
                return (file.getAbsolutePath());
            }
            else
            {
                throw new UnsupportedFlavorException(flavor);
            }
        }
    }
}
