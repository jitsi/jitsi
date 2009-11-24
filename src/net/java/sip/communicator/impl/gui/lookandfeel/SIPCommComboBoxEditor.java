/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.lookandfeel;

import java.awt.*;
import java.awt.geom.*;

import javax.swing.border.*;
import javax.swing.plaf.metal.*;

import net.java.sip.communicator.util.swing.*;
import net.java.sip.communicator.util.swing.plaf.*;

/**
 * The default editor for SIPCommunicator editable combo boxes.
 * 
 * @author Yana Stamcheva
 */
public class SIPCommComboBoxEditor extends MetalComboBoxEditor {

    public SIPCommComboBoxEditor()
    {
        editor.setBorder(new EditorBorder());

        // enables delete button
        if (editor.getUI() instanceof SIPCommTextFieldUI)
        {
            ((SIPCommTextFieldUI) editor.getUI())
                .setDeleteButtonEnabled(true);
        }
    }

    protected static final Insets editorBorderInsets 
        = new Insets(2, 2, 2, 0);
    private static final Insets SAFE_EDITOR_BORDER_INSETS 
        = new Insets(2, 2, 2, 0);

    private static class EditorBorder
        extends AbstractBorder
    {
        public void paintBorder(Component c, Graphics g, int x, int y, int w,
                int h) {
            g = g.create();
            try
            {
                internalPaintBorder(g, x, y, w, h);
            }
            finally
            {
                g.dispose();
            }
        }

        private void internalPaintBorder(Graphics g, int x, int y,
                int w, int h)
        {
            Graphics2D g2d = (Graphics2D)g;
            
            AntialiasingManager.activateAntialiasing(g2d);
            
            g2d.translate(x, y);
            
            g2d.setColor(SIPCommLookAndFeel.getControlDarkShadow());
            
            GeneralPath path = new GeneralPath();
            int round = 2;
            
            path.moveTo(w, h-1);
            path.lineTo(round, h-1);
            path.curveTo(round, h-1, 0, h-1, 0, h-round-1);
            path.lineTo(0, round);
            path.curveTo(0, round, 0, 0, round, 0);
            path.lineTo(w, 0);
            
            g2d.draw(path);
            
            g2d.translate(-x, -y);
        }

        public Insets getBorderInsets( Component c ) {
            if (System.getSecurityManager() != null) {
                return SAFE_EDITOR_BORDER_INSETS;
            } else {
                return editorBorderInsets;
            }
        }
    }
    
    /**
     * A subclass of SIPCommComboBoxEditor that implements UIResource.
     * SIPCommComboBoxEditor doesn't implement UIResource
     * directly so that applications can safely override the
     * cellRenderer property with BasicListCellRenderer subclasses.     
     */
    public static class UIResource extends SIPCommComboBoxEditor
        implements javax.swing.plaf.UIResource {
    }
}
