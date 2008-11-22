/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.configforms;

import java.awt.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.utils.*;

/**
 * The <tt>ContactListCellRenderer</tt> is the custom cell renderer used in the
 * SIP-Communicator's <tt>ContactList</tt>. It extends JPanel instead of JLabel,
 * which allows adding different buttons and icons to the contact cell.
 * The cell border and background are repainted. 
 * 
 * @author Yana Stamcheva
 */
public class ConfigFormListCellRenderer
    extends JPanel 
    implements ListCellRenderer
{
    
    /**
     * The size of the gradient used for painting the selected background of
     * some components.
     */
    public static final int SELECTED_GRADIENT_SIZE = 5;

    /**
     * The start color used to paint a gradient selected background.
     */
    private static final Color SELECTED_END_COLOR
        = new Color(240, 240, 240);

    /**
     * The end color used to paint a gradient selected background.
     */
    private static final Color SELECTED_START_COLOR
        = new Color(209, 212, 225);
    
    private JLabel textLabel = new JLabel();

    private JLabel iconLabel = new JLabel();

    private boolean isSelected = false;
    
    /**
     * Initialize the panel containing the node.
     */
    public ConfigFormListCellRenderer()
    {
        this.setBackground(Color.WHITE);

        this.setOpaque(true);
        
        this.setPreferredSize(new Dimension(100, 65));

        this.setLayout(new BorderLayout(0, 0));

        this.setBorder(BorderFactory.createEmptyBorder(3, 3, 5, 3));

        this.textLabel.setFont(this.getFont().deriveFont(Font.BOLD, 10));
        
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
     * Paint a background for all groups and a round blue border and background
     * when a cell is selected. 
     */
    /**
     * Overrides the <code>paintComponent</code> method of <tt>JPanel</tt>
     * to provide a custom look for this panel. A gradient background is
     * painted when the panel is selected and when the mouse is over it.
     */
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;
        int width = getWidth();
        int height = getHeight();

        if (isSelected)
        {
            GradientPaint p =
                new GradientPaint(width / 2, 0, SELECTED_START_COLOR,
                    width / 2, SELECTED_GRADIENT_SIZE, SELECTED_END_COLOR);

            GradientPaint p1 =
                new GradientPaint(width / 2, height - SELECTED_GRADIENT_SIZE,
                    SELECTED_END_COLOR, width / 2, height - 1,
                    SELECTED_START_COLOR);

            g2.setPaint(p);
            g2.fillRect(0, 0, width, SELECTED_GRADIENT_SIZE);

            g2.setColor(SELECTED_END_COLOR);
            g2.fillRect(0, SELECTED_GRADIENT_SIZE, width, height
                - SELECTED_GRADIENT_SIZE);

            g2.setPaint(p1);
            g2.fillRect(0, height - SELECTED_GRADIENT_SIZE, width, height - 1);

            g2.setColor(Constants.LIST_SELECTION_BORDER_COLOR);
            g2.drawRoundRect(0, 0, width - 1, height - 1, 5, 5);
        }
        else
        {
            g2.setColor(SELECTED_START_COLOR);
            g2.drawLine(0, height - 1, width, height - 1);
        }
    }
}
