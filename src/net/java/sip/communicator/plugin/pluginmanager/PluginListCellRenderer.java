/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.pluginmanager;

import java.awt.*;
import java.util.*;

import javax.swing.*;
import javax.swing.table.*;

import net.java.sip.communicator.util.swing.*;

import org.osgi.framework.*;

/**
 * The <tt>ContactListCellRenderer</tt> is the custom cell renderer used in the
 * SIP-Communicator's <tt>ContactList</tt>. It extends JPanel instead of JLabel,
 * which allows adding different buttons and icons to the contact cell.
 * The cell border and background are repainted.
 *
 * @author Yana Stamcheva
 */
public class PluginListCellRenderer
    extends JPanel
    implements TableCellRenderer
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The end color used to paint a gradient selected background.
     */
    private static final Color SELECTED_START_COLOR
        = new Color(Resources.getColor("service.gui.LIST_SELECTION_COLOR"));

    /**
     * The start color used to paint a gradient selected background.
     */
    private static final Color SELECTED_END_COLOR
        = new Color(Resources.getColor("service.gui.GRADIENT_LIGHT_COLOR"));

    /**
     * The panel containing name and version information.
     */
    private JPanel nameVersionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

    /**
     * The name label.
     */
    private JLabel nameLabel = new JLabel();

    /**
     * The version label.
     */
    private JLabel versionLabel = new JLabel();

    /**
     * The description label.
     */
    private JLabel descriptionLabel = new JLabel();

    /**
     * The state label.
     */
    private JLabel stateLabel = new JLabel();

    /**
     * The icon label.
     */
    private JLabel iconLabel = new JLabel();

    /**
     * The system label indicating that a bundle is system (i.e. not optional).
     */
    private JLabel systemLabel
        = new JLabel("( " + Resources.getString("plugin.pluginmanager.SYSTEM")
                    + " )");

    /**
     * Indicates if a skin is selected.
     */
    private boolean isSelected = false;

    /**
     * The cache of the <code>ImageIcon</code> values returned by
     * {@link #getStateIcon(int)} because the method in question is called
     * whenever a table cell is painted and reading the image data out of a file
     * and loading it into a new <code>ImageIcon</code> at such a time
     * noticeably affects execution the speed.
     */
    private final ImageIcon[] stateIconCache = new ImageIcon[5];

    /**
     * Initialize the panel containing the node.
     */
    public PluginListCellRenderer()
    {
        super(new BorderLayout(8, 8));

        JPanel mainPanel = new JPanel(new BorderLayout());

        this.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        this.setBackground(Color.WHITE);

        this.setOpaque(true);

        mainPanel.setOpaque(false);
        this.nameVersionPanel.setOpaque(false);

        this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        this.nameLabel.setIconTextGap(2);

        this.nameLabel.setFont(this.getFont().deriveFont(Font.BOLD));
        this.systemLabel.setFont(this.getFont().deriveFont(Font.BOLD));

        this.nameVersionPanel.add(nameLabel);
        this.nameVersionPanel.add(versionLabel);

        mainPanel.add(nameVersionPanel, BorderLayout.NORTH);
        mainPanel.add(descriptionLabel, BorderLayout.CENTER);

        this.add(iconLabel, BorderLayout.WEST);

        this.add(mainPanel, BorderLayout.CENTER);
        this.add(stateLabel, BorderLayout.WEST);
    }

    /**
     * Implements the <tt>ListCellRenderer</tt> method.
     * Returns this panel that has been configured to display bundle name,
     * version and description.
     * @param table the parent table
     * @param value the value of the rendered cell
     * @param isSelected indicates if the rendered cell is selected
     * @param hasFocus indicates if the rendered cell has the focus
     * @param rowIndex the row index of the rendered cell
     * @param vColIndex the column index of the rendered cell
     * @return the rendering component
     */
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int rowIndex, int vColIndex)
    {
        Bundle bundle = (Bundle) value;

        Dictionary<?, ?> headers = bundle.getHeaders();
        Object bundleName = headers.get(Constants.BUNDLE_NAME);
        Object bundleVersion = headers.get(Constants.BUNDLE_VERSION);
        Object bundleDescription = headers.get(Constants.BUNDLE_DESCRIPTION);

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

        this.nameVersionPanel.remove(systemLabel);

        if(PluginManagerActivator.isSystemBundle(bundle))
            this.nameVersionPanel.add(systemLabel);

        this.isSelected = isSelected;

        return this;
    }

    /**
     * Returns an icon corresponding to the given <tt>state</tt>.
     * @param state the state, for which we're looking for an icon
     * @return the icon corresponding to the given state
     */
    private ImageIcon getStateIcon(int state)
    {
        int cacheIndex;
        String imageID;
        switch (state)
        {
            case Bundle.INSTALLED:
                cacheIndex = 0;
                imageID = "plugin.pluginmanager.INSTALLED_STATE";
                break;
            case Bundle.RESOLVED:
                cacheIndex = 1;
                imageID = "plugin.pluginmanager.DEACTIVATED_STATE";
                break;
            case Bundle.STARTING:
                cacheIndex = 2;
                imageID = "plugin.pluginmanager.STARTING_STATE";
                break;
            case Bundle.STOPPING:
                cacheIndex = 3;
                imageID = "plugin.pluginmanager.STOPPING_STATE";
                break;
            case Bundle.ACTIVE:
                cacheIndex = 4;
                imageID = "plugin.pluginmanager.ACTIVATE_STATE";
                break;
            default:
                return null;
        }
        ImageIcon stateIcon = stateIconCache[cacheIndex];
        if (stateIcon == null)
            stateIconCache[cacheIndex] =
                stateIcon = Resources.getResources().getImage(imageID);
        return stateIcon;
    }

    /**
     * Paint a background for all groups and a round blue border and background
     * when a cell is selected.
     * @param g the <tt>Graphics</tt> object used for painting
     */
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        g = g.create();
        try
        {
            internalPaintComponent(g);
        }
        finally
        {
            g.dispose();
        }
    }

    /**
     * Paints a custom gradient background for selected cells.
     * @param g the <tt>Graphics</tt> object used for painting
     */
    private void internalPaintComponent(Graphics g)
    {
        AntialiasingManager.activateAntialiasing(g);

        Graphics2D g2 = (Graphics2D) g;
        int width = getWidth();
        int height = getHeight();

        if (this.isSelected)
        {
            GradientPaint p =
                new GradientPaint(width / 2, 0, SELECTED_START_COLOR,
                    width / 2, height, SELECTED_END_COLOR);

            g2.setPaint(p);
            g2.fillRoundRect(1, 1, width, height - 1, 7, 7);
        }

        g2.setColor(SELECTED_START_COLOR);
        g2.drawLine(0, height - 1, width, height - 1);
    }
}
