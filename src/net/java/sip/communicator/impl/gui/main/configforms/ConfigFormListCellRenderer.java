/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.configforms;

import java.awt.*;

import javax.swing.*;

import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>ConfigFormListCellRenderer</tt> is the custom cell renderer used in
 * the Jitsi's <tt>ConfigFormList</tt>. It extends TransparentPanel
 * instead of JLabel, which allows adding different buttons and icons to the
 * cell.
 * <br>
 * The cell border and background are repainted.
 * 
 * @author Yana Stamcheva
 */
public class ConfigFormListCellRenderer
    extends TransparentPanel 
    implements ListCellRenderer
{
    
    /**
     * The size of the gradient used for painting the selected background of
     * some components.
     */
    public static final int SELECTED_GRADIENT_SIZE = 5;

    private final JLabel textLabel = new EmphasizedLabel("");

    private final JLabel iconLabel = new JLabel();

    private boolean isSelected = false;

    /**
     * Initialize the panel containing the node.
     */
    public ConfigFormListCellRenderer()
    {
        this.setBorder(BorderFactory.createEmptyBorder(3, 3, 5, 3));
        this.setLayout(new BorderLayout(0, 0));
        this.setPreferredSize(new Dimension(60, 50));

        Font font = getFont();
        this.textLabel.setFont(font.deriveFont(11f));

        this.iconLabel.setHorizontalAlignment(JLabel.CENTER);
        this.textLabel.setHorizontalAlignment(JLabel.CENTER);

        this.add(iconLabel, BorderLayout.CENTER);
        this.add(textLabel, BorderLayout.SOUTH);
    }

    /**
     * Implements the <tt>ListCellRenderer</tt> method.
     * 
     * Returns this panel that has been configured to display the meta contact
     * and meta contact group cells.
     */
    public Component getListCellRendererComponent(JList list, Object value,
            int index, boolean isSelected, boolean cellHasFocus)
    {
        ConfigFormDescriptor cfDescriptor = (ConfigFormDescriptor) value;

        ImageIcon icon = cfDescriptor.getConfigFormIcon();
        if(icon != null)            
            iconLabel.setIcon(icon);

        String title = cfDescriptor.getConfigFormTitle();
        if (title != null)
            textLabel.setText(title);

        this.isSelected = isSelected;

        return this;
    }

    /**
     * Overrides the <code>paintComponent</code> method of <tt>JPanel</tt>
     * to provide a custom look for this panel. A gradient background is
     * painted when the panel is selected and when the mouse is over it.
     * @param g the <tt>Graphics</tt> object
     */
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        AntialiasingManager.activateAntialiasing(g);

        Graphics2D g2 = (Graphics2D) g;

        if (isSelected)
        {
            g2.setPaint(new Color(100, 100, 100, 100));
            g2.fillRoundRect(   0, 0,
                               this.getWidth(), this.getHeight(),
                               10, 10);

            g2.setColor(Color.GRAY);
            g2.drawRoundRect(   0, 0,
                               this.getWidth() - 1, this.getHeight() - 1,
                               10, 10);
        }
    }
}
