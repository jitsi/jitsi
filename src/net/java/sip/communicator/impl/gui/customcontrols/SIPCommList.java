/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.customcontrols;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * Creates a custom list for the needs of the configuration window.
 * 
 * @author Yana Stamcheva
 */
public class SIPCommList extends JScrollPane {

    private JPanel mainPanel = new JPanel();

    private JPanel listPanel = new JPanel();

    /**
     * Creates an instance of <tt>SIPCommList</tt>
     */
    public SIPCommList() {

        this.mainPanel.setBackground(Color.WHITE);
        this.mainPanel.setOpaque(true);

        this.mainPanel.setLayout(new BorderLayout());
        this.listPanel
                .setLayout(new BoxLayout(this.listPanel, BoxLayout.Y_AXIS));

        this.mainPanel.add(listPanel, BorderLayout.NORTH);
        this.getViewport().add(mainPanel);
    }

    /**
     * Adds a cell to this list. 
     * @param cell The <tt>ListCellPanel</tt> to add.
     */
    public void addCell(ListCellPanel cell) {
        this.listPanel.add(cell);
    }

    /**
     * Deselects all previously selected cells when a new cell is selected.
     * @param cellSelected The newly selected cell.
     */
    public void refreshCellStatus(ListCellPanel cellSelected) {

        for (int i = 0; i < this.listPanel.getComponentCount(); i++) {
            ListCellPanel cpanel = (ListCellPanel) this.listPanel
                    .getComponent(i);

            if (!cpanel.equals(cellSelected)) {
                cpanel.setSelected(false);
            }
        }
    }
}
