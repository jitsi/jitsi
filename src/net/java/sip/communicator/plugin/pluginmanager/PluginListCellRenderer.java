/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.plugin.pluginmanager;

import java.awt.*;

import javax.swing.*;
import javax.swing.table.*;

import org.osgi.framework.*;

/**
 * The <tt>ContactListCellRenderer</tt> is the custom cell renderer used in the
 * SIP-Communicator's <tt>ContactList</tt>. It extends JPanel instead of JLabel,
 * which allows adding different buttons and icons to the contact cell.
 * The cell border and background are repainted. 
 * 
 * @author Yana Stamcheva
 */
public class PluginListCellRenderer extends JPanel 
    implements TableCellRenderer
{
    /**
     * The end color used to paint a gradient selected background.
     */
    private static final Color SELECTED_START_COLOR
        = new Color(Resources.getColor("listSelectionColor"));

    /**
     * The start color used to paint a gradient selected background.
     */
    private static final Color SELECTED_END_COLOR
        = new Color(Resources.getColor("gradientLightColor"));
    
    private JPanel mainPanel = new JPanel(new BorderLayout());
    
    private JPanel nameVersionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    
    private JLabel nameLabel = new JLabel();
    
    private JLabel versionLabel = new JLabel();
    
    private JLabel descriptionLabel = new JLabel();
    
    private JLabel stateLabel = new JLabel();
    
    private JLabel iconLabel = new JLabel();
    
    private JLabel systemLabel
        = new JLabel("( " + Resources.getString("system") + " )");
    
    private boolean isSelected = false;
    
    private String direction;    
    
    /**
     * Initialize the panel containing the node.
     */
    public PluginListCellRenderer()
    {
        super(new BorderLayout(8, 8));

        this.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        this.setBackground(Color.WHITE);
        
        this.setOpaque(true);
        
        this.mainPanel.setOpaque(false);
        this.nameVersionPanel.setOpaque(false);
                
        this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        this.nameLabel.setIconTextGap(2);
        
        this.nameLabel.setFont(this.getFont().deriveFont(Font.BOLD));
        this.systemLabel.setFont(this.getFont().deriveFont(Font.BOLD));
        
        this.nameVersionPanel.add(nameLabel);
        this.nameVersionPanel.add(versionLabel);
        
        this.mainPanel.add(nameVersionPanel, BorderLayout.NORTH);
        this.mainPanel.add(descriptionLabel, BorderLayout.CENTER);
        
        this.add(iconLabel, BorderLayout.WEST);
        
        this.add(mainPanel, BorderLayout.CENTER);
        this.add(stateLabel, BorderLayout.WEST);
    }

    /**
     * Implements the <tt>ListCellRenderer</tt> method.
     * 
     * Returns this panel that has been configured to display the meta contact
     * and meta contact group cells.
     */
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int rowIndex, int vColIndex)
    {   
        Bundle bundle = (Bundle) value;
        
        Object bundleName
            = bundle.getHeaders().get(Constants.BUNDLE_NAME);
        Object bundleVersion
            = bundle.getHeaders().get(Constants.BUNDLE_VERSION);
        Object bundleDescription
            = bundle.getHeaders().get(Constants.BUNDLE_DESCRIPTION);
        Object bundleIconPath = bundle.getHeaders().get("Bundle-Icon-Path");
        Icon stateIcon = getStateIcon(bundle.getState());
        
        if(bundleName != null)
            this.nameLabel.setText(bundleName.toString());
        else
            this.nameLabel.setText("unknown");
        
        if(bundleVersion != null)
            this.versionLabel.setText(bundleVersion.toString());
        else
            this.versionLabel.setText("");
        
        if(bundleDescription != null)
            this.descriptionLabel.setText(bundleDescription.toString());
        else
            this.descriptionLabel.setText("");
            
        if(stateIcon != null)
            this.stateLabel.setIcon(stateIcon);
        
        Object sysBundleProp
            = bundle.getHeaders().get("System-Bundle");
        
        this.nameVersionPanel.remove(systemLabel);
        
        if(sysBundleProp != null && sysBundleProp.equals("yes"))
            this.nameVersionPanel.add(systemLabel);
        
        this.isSelected = isSelected;

        return this;
    }
    
    private ImageIcon getStateIcon(int state)
    {
        switch (state)
        {
            case Bundle.INSTALLED:
                return Resources.getImage("installedStateIcon");
            case Bundle.RESOLVED:
                return Resources.getImage("desactivatedStateIcon");
            case Bundle.STARTING:
                return Resources.getImage("startingStateIcon");
            case Bundle.ACTIVE:
                return Resources.getImage("activeStateIcon");
            case Bundle.STOPPING:
                return Resources.getImage("stoppingStateIcon");
        }
        return null;
    }
        
    /**
     * Paint a background for all groups and a round blue border and background
     * when a cell is selected. 
     */
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        if (this.isSelected)
        {
            GradientPaint p = new GradientPaint(this.getWidth()/2, 0,
                    SELECTED_START_COLOR,
                    this.getWidth()/2,
                    this.getHeight(),
                    SELECTED_END_COLOR);

            g2.setPaint(p);            
            g2.fillRoundRect(1, 1, this.getWidth(), this.getHeight() - 1, 7, 7);
        }
        
        g2.setColor(SELECTED_START_COLOR);
        g2.drawLine(0, this.getHeight() - 1,
                this.getWidth(), this.getHeight() - 1);
    }
}
