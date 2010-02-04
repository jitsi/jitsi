/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util.swing;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.*;

import javax.swing.*;
import javax.swing.text.*;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.util.*;

/**
 * A TransferHandler that we use to handle copying, pasting and DnD operations.
 * The string handler is heavily inspired by Sun's
 * <tt>DefaultTransferHandler</tt> with the main difference being that
 * we only accept pasting of plain text. We do this in order to avoid html
 * support problems that appear when pasting formatted text into our editable
 * area.
 * 
 * @author Emil Ivov
 * @author Yana Stamcheva
 */
public class ExtendedTransferHandler
    extends TransferHandler
{
    /**
     * The data flavor used when transferring <tt>MetaContact</tt>s.
     */
    protected static final DataFlavor metaContactDataFlavor
        = new DataFlavor(MetaContact.class, "MetaContact");

    /**
     * Returns the type of transfer actions supported by the source;
     * any bitwise-OR combination of <tt>COPY</tt>, <tt>MOVE</tt>
     * and <tt>LINK</tt>.
     * <p>
     * Some models are not mutable, so a transfer operation of <tt>MOVE</tt>
     * should not be advertised in that case. Returning <tt>NONE</tt>
     * disables transfers from the component.
     *
     * @param c  the component holding the data to be transferred;
     *           provided to enable sharing of <code>TransferHandler</code>s
     * @return {@code COPY} if the transfer property can be found,
     *          otherwise returns <code>NONE</code>
     */
    public int getSourceActions(JComponent c)
    {
        return TransferHandler.COPY;
    }

    /**
     * Indicates whether a component will accept an import of the given
     * set of data flavors prior to actually attempting to import it. We return
     * <tt>true</tt> to indicate that the transfer with at least one of the
     * given flavors would work and <tt>false</tt> to reject the transfer.
     * <p>
     * @param comp component
     * @param flavor the data formats available
     * @return  true if the data can be inserted into the component, false
     * otherwise
     * @throws NullPointerException if <code>support</code> is {@code null}
     */
    public boolean canImport(JComponent comp, DataFlavor flavor[])
    {
        for (int i = 0, n = flavor.length; i < n; i++)
        {
            if (flavor[i].equals(DataFlavor.javaFileListFlavor))
            {
                return true;
            }
            else if (flavor[i].equals(DataFlavor.stringFlavor))
            {
                if (comp instanceof JTextComponent)
                {
                    JTextComponent c = (JTextComponent) comp;

                    if (c.isEditable() && c.isEnabled())
                    {
                        return true;
                    }
                }

                return false;
            }
        }
        return false;
    }

    /**
     * Creates a transferable for text pane components in order to enable drag
     * and drop of text.
     * @param component the component for which to create a
     * <tt>Transferable</tt>
     * @return the created <tt>Transferable</tt>
     */
    public Transferable createTransferable(JComponent component)
    {
        if (component instanceof JTextPane
            || component instanceof JTextField)
        {
            return new SelectedTextTransferable((JTextComponent) component);
        }

        return super.createTransferable(component);
    }

    /**
     * Handles transport (cut and copy) from the chat panel to
     * <tt>clipboard</tt>. This method will only transfer plain text and would
     * explicitly ignore any formatting.
     * <p>
     * @param comp  the component holding the data to be transferred;
     *              provided to enable sharing of <code>TransferHandler</code>s
     * @param clipboard the clipboard to transfer the data into
     * @param action the transfer action requested; this should
     *  be a value of either <code>COPY</code> or <code>MOVE</code>;
     *  the operation performed is the intersection  of the transfer
     *  capabilities given by getSourceActions and the requested action;
     *  the intersection may result in an action of <code>NONE</code>
     *  if the requested action isn't supported
     * @throws IllegalStateException if the clipboard is currently unavailable
     * @see Clipboard#setContents(Transferable, ClipboardOwner)
     */
    public void exportToClipboard(JComponent comp,
                                  Clipboard clipboard,
                                  int action)
        throws IllegalStateException
    {
        if (comp instanceof JTextComponent)
        {
            JTextComponent textComponent = (JTextComponent)comp;
            int startIndex = textComponent.getSelectionStart();
            int endIndex = textComponent.getSelectionEnd();
            if (startIndex != endIndex)
            {
                try
                {
                    Document doc = textComponent.getDocument();
                    String srcData = doc.getText(startIndex,
                                                 endIndex - startIndex);
                    StringSelection contents = new StringSelection(srcData);

                    // this may throw an IllegalStateException,
                    // but it will be caught and handled in the
                    // action that invoked this method
                    clipboard.setContents(contents, null);

                    if (action == TransferHandler.MOVE)
                    {
                        doc.remove(startIndex, endIndex - startIndex);
                    }
                }
                catch (BadLocationException ble)
                {
                    //we simply ignore
                }
            }
        }
    }

    /**
     * Transferable for text pane components that enables drag and drop of text.
     */
    public class SelectedTextTransferable implements Transferable
    {
        private JTextComponent textComponent;

        /**
         * Creates an instance of <tt>SelectedTextTransferable</tt>.
         * @param component the text component
         */
        public SelectedTextTransferable(JTextComponent component)
        {
            this.textComponent = component;
        }

        /**
         * Returns supported flavors.
         * @return an array of supported flavors
         */
        public DataFlavor[] getTransferDataFlavors()
        {
            return new DataFlavor[]{DataFlavor.stringFlavor};
        }

        /**
         * Returns <tt>true</tt> if the given <tt>flavor</tt> is supported,
         * otherwise returns <tt>false</tt>.
         * @param flavor the data flavor to verify
         * @return <tt>true</tt> if the given <tt>flavor</tt> is supported,
         * otherwise returns <tt>false</tt>
         */
        public boolean isDataFlavorSupported(DataFlavor flavor)
        {
            return DataFlavor.stringFlavor.equals(flavor);
        }

        /**
         * Returns the selected text.
         * @param flavor the flavor
         * @return the selected text
         * @exception IOException if the data is no longer available in the
         * requested flavor.
         * @exception UnsupportedFlavorException if the requested data flavor
         * is not supported.
         */
        public Object getTransferData(DataFlavor flavor)
            throws  UnsupportedFlavorException,
                    IOException
        {
            if (!DataFlavor.stringFlavor.equals(flavor))
            {
                throw new UnsupportedFlavorException(flavor);
            }

            return textComponent.getSelectedText();
        }
    }

    /**
     * Overrides <tt>TransferHandler.getVisualRepresentation(Transferable t)</tt>
     * in order to return a custom drag icon.
     * <p>
     * The default parent implementation of this method returns null.
     *
     * @param t  the data to be transferred; this value is expected to have been
     * created by the <code>createTransferable</code> method
     * @return the icon to show when dragging
     */
    public Icon getVisualRepresentation(Transferable t)
    {
        Icon icon = null;
        if (t.isDataFlavorSupported(DataFlavor.stringFlavor))
        {
            String text = null;
            try
            {
                text = (String) t.getTransferData(DataFlavor.stringFlavor);
            }
            catch (UnsupportedFlavorException e) {}
            catch (IOException e) {}

            if (text != null)
            {
                Rectangle2D bounds = GuiUtils.getDefaultStringSize(text);
                BufferedImage image = new BufferedImage(
                    (int) Math.ceil(bounds.getWidth()),
                    (int) Math.ceil(bounds.getHeight()),
                    BufferedImage.TYPE_INT_ARGB);

                Graphics g = image.getGraphics();
                AntialiasingManager.activateAntialiasing(g);
                g.setColor(Color.BLACK);
                // Don't know why if we draw the string on y = 0 it doesn't
                // appear in the visible area.
                g.drawString(text, 0, 10);

                icon = new ImageIcon(image);
            }
        }

        return icon;
    }

    // Patch for bug 4816922 "No way to set drag icon:
    // TransferHandler.getVisualRepresentation() is not used".
    // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4816922
    // The following workaround comes from bug comments section!
    private static SwingDragGestureRecognizer recognizer = null;

    private static class SwingDragGestureRecognizer
        extends DragGestureRecognizer
    {
        SwingDragGestureRecognizer(DragGestureListener dgl)
        {
            super(DragSource.getDefaultDragSource(), null, NONE, dgl);
        }

        void gestured(JComponent c, MouseEvent e, int srcActions, int action)
        {
            setComponent(c);
            setSourceActions(srcActions);
            appendEvent(e);
            fireDragGestureRecognized(action, e.getPoint());
        }

        /**
         * Registers this DragGestureRecognizer's Listeners with the Component.
         */
        protected void registerListeners() {}

        /**
         * Unregister this DragGestureRecognizer's Listeners with the Component.
         * <p/>
         * Subclasses must override this method.
         */
        protected void unregisterListeners() {}
    }

    /**
     * Overrides <tt>TransferHandler.exportAsDrag</tt> method in order to call
     * our own <tt>SwingDragGestureRecognizer</tt>, which takes care of the
     * visual representation icon.
     *
     * @param comp the component holding the data to be transferred; this
     * argument is provided to enable sharing of <code>TransferHandler</code>s
     * by multiple components
     * @param e the event that triggered the transfer
     * @param action the transfer action initially requested; this should
     * be a value of either <code>COPY</code> or <code>MOVE</code>;
     * the value may be changed during the course of the drag operation
     */
    public void exportAsDrag(JComponent comp, InputEvent e, int action)
    {
        int srcActions = getSourceActions(comp);
        int dragAction = srcActions & action;

        // only mouse events supported for drag operations
        if (! (e instanceof MouseEvent))
            action = NONE;

        if (action != NONE && !GraphicsEnvironment.isHeadless())
        {
            if (recognizer == null)
            {
                recognizer = new SwingDragGestureRecognizer(new DragHandler());
            }
            recognizer.gestured(comp, (MouseEvent) e, srcActions, dragAction);
        }
        else
        {
            exportDone(comp, null, NONE);
        }
    }

    /**
     * This is the default drag handler for drag and drop operations that
     * use the <code>TransferHandler</code>.
     */
    private static class DragHandler
        implements  DragGestureListener,
                    DragSourceListener
    {
        private boolean scrolls;

        // --- DragGestureListener methods -----------------------------------

        /**
         * A Drag gesture has been recognized.
         * @param dge the <tt>DragGestureEvent</tt> that notified us
         */
        public void dragGestureRecognized(DragGestureEvent dge)
        {
            JComponent c = (JComponent) dge.getComponent();
            ExtendedTransferHandler th
                = (ExtendedTransferHandler) c.getTransferHandler();

            Transferable t = th.createTransferable(c);
            if (t != null)
            {
                scrolls = c.getAutoscrolls();
                c.setAutoscrolls(false);
                try
                {
                    Image img = null;
                    Icon icn = th.getVisualRepresentation(t);

                    if (icn != null)
                    {
                        if (icn instanceof ImageIcon)
                        {
                            img = ((ImageIcon) icn).getImage();
                        }
                        else
                        {
                            img = new BufferedImage(icn.getIconWidth(),
                                icn.getIconHeight(),
                                BufferedImage.TYPE_4BYTE_ABGR);
                            Graphics g = img.getGraphics();
                            icn.paintIcon(c, g, 0, 0);
                        }
                    }
                    if (img == null)
                    {
                        dge.startDrag(null, t, this);
                    }
                    else
                    {
                        dge.startDrag(null, img,
                            new Point(0, -1 * img.getHeight(null)), t, this);
                    }

                    return;
                }
                catch (RuntimeException re)
                {
                    c.setAutoscrolls(scrolls);
                }
            }

            th.exportDone(c, t, NONE);
        }

        // --- DragSourceListener methods -----------------------------------

        /**
         * As the hotspot enters a platform dependent drop site.
         * @param e the <tt>DragSourceDragEvent</tt> containing the details of
         * the drag
         */
        public void dragEnter(DragSourceDragEvent e)
        {}

        /**
         * As the hotspot moves over a platform dependent drop site.
         * @param e the <tt>DragSourceDragEvent</tt> containing the details of
         * the drag
         */
        public void dragOver(DragSourceDragEvent e)
        {
        }

        /**
         * As the hotspot exits a platform dependent drop site.
         * @param e the <tt>DragSourceDragEvent</tt> containing the details of
         * the drag
         */
        public void dragExit(DragSourceEvent e)
        {}

        /**
         * As the operation completes.
         * @param e the <tt>DragSourceDragEvent</tt> containing the details of
         * the drag
         */
        public void dragDropEnd(DragSourceDropEvent e)
        {
            DragSourceContext dsc = e.getDragSourceContext();
            JComponent c = (JComponent) dsc.getComponent();

            if (e.getDropSuccess())
            {
                ((ExtendedTransferHandler) c.getTransferHandler())
                    .exportDone(c, dsc.getTransferable(), e.getDropAction());
            }
            else
            {
                ((ExtendedTransferHandler) c.getTransferHandler())
                    .exportDone(c, dsc.getTransferable(), NONE);
            }
            c.setAutoscrolls(scrolls);
        }

        public void dropActionChanged(DragSourceDragEvent dsde) {}
    }
}
