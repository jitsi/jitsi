/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.notificationconfiguration;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.table.*;

/**
 * @author Alexandre Maillard
 */
class ListModel extends DefaultTableModel
{
    ListModel(Object[] columns, int rowCount)
    {
        super(columns, rowCount);
    }

    public Class<?> getColumnClass(int c)
    {
        return getValueAt(0, c).getClass();
    }

    /* non editable cell */
    public boolean isCellEditable(int row, int col)
    {
        return false;
    }

}

class MyRenderer implements TableCellRenderer
{
    // Create a JLabel for use as a renderer and pre-load this label
    // with an icon image.

    private JLabel l;

    MyRenderer(JLabel JLIcon)
    {
        this.l = JLIcon;
    }

    public Component getTableCellRendererComponent (
            JTable table,
            Object value,
            boolean isSelected,
            boolean hasFocus,
            int row,
            int column)
    {
        // Extract the original header renderer for this column.

        TableCellRenderer tcr = table.getTableHeader().getDefaultRenderer ();

        // Extract the component used to render the column header.

        Component c = tcr.getTableCellRendererComponent (
                table,
                value,
                isSelected,
                hasFocus,
                row,
                column);

        // Establish the font, foreground color, and border for the
        // JLabel so that the rendered header will look the same as the
        // other rendered headers.

        l.setFont (c.getFont ());
        l.setForeground (c.getForeground ());
        l.setBorder (((JComponent) c).getBorder ());

        // Establish the column name.

        l.setText ((String) value);

        // Return the cached JLabel a the renderer for this column
        // header.

        return l;
    }
}

class MyTableRenderer extends DefaultTableCellRenderer
{
    MyTableRenderer()
    {
        super();
    }
    public Component getTableCellRendererComponent(
            JTable table,
            Object value,
            boolean isSelected,
            boolean hasFocus,
            int row,
            int column)
    {
        if(column == 0)
        {
            if(((String)value).equals("enable") == true)
            {
                setIcon(new ImageIcon(Resources.getImageInBytes(
                    "plugin.notificationconfig.ACTIVATED_ICON")));
                setText(null);
                setHorizontalAlignment(SwingConstants.CENTER);
            }
            else
            {
                setIcon(new ImageIcon(Resources.getImageInBytes(
                            "plugin.notificationconfig.DEACTIVATED_ICON")));
                setText(null);
                setHorizontalAlignment(SwingConstants.CENTER);
            }
        }
        else if(column == 1)
        {
            if(((String)value).equals("Yes") == true)
            {
                setIcon(new ImageIcon(Resources.getImageInBytes(
                            "plugin.notificationconfig.PROG_ICON")));
                setText(null);
                setHorizontalAlignment(SwingConstants.CENTER);
            }
            else
            {
                setIcon(null);
                setText(null);
            }
        }
        else if(column == 2)
        {
            if(((String)value).equals("Yes") == true)
            {
                setIcon(new ImageIcon(Resources.getImageInBytes(
                            "plugin.notificationconfig.POPUP_ICON")));
                setText(null);
                setHorizontalAlignment(SwingConstants.CENTER);
            }
            else
            {
                setIcon(null);
                setText(null);
            }
        }
        else if(column == 3)
        {
            if(((String)value).equals("Yes") == true)
            {
                setIcon(new ImageIcon(Resources.getImageInBytes(
                            "plugin.notificationconfig.SOUND_ICON")));
                setText(null);
                setHorizontalAlignment(SwingConstants.CENTER);
            }
            else
            {
                setIcon(null);
                setText(null);
            }
        }
        else
        {
            setIcon(null);
            setText((String)value);
            setHorizontalAlignment(SwingConstants.LEFT);
        }
        if(isSelected)
        {
            this.setOpaque(true);
            this.setBackground(new Color(209, 212, 225));
        }
        else
        {
            this.setBackground(Color.WHITE);
        }
        return this;
    }
}

public class ListMulti extends JPanel
{
    private MyJTable listMulti;
    private ListModel model;

    ListMulti(Object columns[], String colunmToolTips [])
    {
        super(new GridLayout(1, 0));
        String strTmp = new String();

        model = new ListModel(columns, 0);

        listMulti = new MyJTable(model);

        listMulti.setRowSelectionAllowed(true);
        listMulti.getTableHeader().setReorderingAllowed(false);
        listMulti.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        for(int i = 0; i< columns.length; i ++)
        {
            TableColumn tmp = listMulti.getColumnModel().getColumn(i);
            if(columns[i].getClass() != strTmp.getClass())
            {
                tmp.setHeaderRenderer(new MyRenderer((JLabel)columns[i]));
                tmp.setHeaderValue("");
            }
            else
            {
                tmp.setHeaderValue((String)columns[i]);
            }

            if(i == 0)
            {
                tmp.setMaxWidth(67);
                tmp.setMinWidth(67);
                tmp.setPreferredWidth(67);
            }
            else if(i < columns.length - 1)
            {
                tmp.setMaxWidth(25);
                tmp.setMinWidth(25);
                tmp.setPreferredWidth(25);
            }
        }

        /* for headers */
        JScrollPane scrollPane = new JScrollPane(listMulti);
        this.add(scrollPane);
        listMulti.setDefaultRenderer(Object.class, new MyTableRenderer());
    }

    public void addLine(NotificationsTableEntry dataNTE)
    {
        Object row[] = new Object[5];

        row[0] = dataNTE.getEnabled()
                ? new String("enable")
                : new String("disable");
        row[1] = (dataNTE.getProgram()
                && (dataNTE.getProgramFile().trim().length() > 0))
                ? new String("Yes")
                : new String("No");
        row[2] = dataNTE.getPopup() ? new String("Yes") : new String("No");
        row[3] = (dataNTE.getSound()
                && (dataNTE.getSoundFile().trim().length() > 0))
                ? new String("Yes")
                : new String("No");
        row[4] = dataNTE.getEvent();

        this.addLine(row);
    }

    public void addLine(Object data[])
    {
        if(data.length != model.getColumnCount())
        {
            return;
        }

        model.addRow(data);

    }

    public int removeLine(int num)
    {
        model.removeRow(num);
        return 0;
    }

    public int getLine()
    {
        return listMulti.getSelectedRow() ;
    }

    public Object getRowValue(int line, int column)
    {
        return listMulti.getValueAt(line, column);
    }

    public String getValue(int line, int column)
    {
        return (String)listMulti.getValueAt(line, column);
    }

    public void setValue(String value, int line, int column)
    {
        listMulti.setValueAt(value, line, column);
    }

    /*
     * Gives the number of lines of the Table.
     */
    public int getRowCount()
    {
        return listMulti.getRowCount();

    }

    public void setLine(NotificationsTableEntry dataNTE, int line)
    {
        Object row[] = new Object[5];

        row[0] = dataNTE.getEnabled()
                ? new String("enable")
                : new String("disable");
        row[1] = (dataNTE.getProgram()
                && (dataNTE.getProgramFile().trim().length() > 0))
                ? new String("Yes")
                : new String("No");
        row[2] = dataNTE.getPopup() ? new String("Yes") : new String("No");
        row[3] = (dataNTE.getSound()
                && (dataNTE.getSoundFile().trim().length() > 0))
                ? new String("Yes")
                : new String("No");
        row[4] = dataNTE.getEvent();

        this.setLine(row,line);
    }

    public void setLine(Object data[], int line)
    {
        int i;
        for(i = 0; i < model.getColumnCount(); i ++)
        {
            setValue((String)data[i], line, i);
        }
    }

    /*
     * Adding a mouse listener on the table.
     */
    public void addMouseListener(MouseListener mL)
    {

       listMulti.addMouseListener(mL);
    }

    /*
     * Allows selection of a line or a group of lines.
     */
    public void setRowSelectionInterval(int row, int col)
    {
        listMulti.setRowSelectionInterval(row,col);
    }

    /*
     * Returne the current line number
     */
    public int rowAtPoint(Point p)
    {
        return listMulti.rowAtPoint(p);
    }


    /*
     * Extends the JTable to make easier to use whith the pluggin
     */
    static class MyJTable extends JTable
    {
        MyJTable(TableModel model)
        {
            super(model);
        }

        protected JTableHeader createDefaultTableHeader()
        {
            return new JTableHeader(columnModel)
            {
                public String getToolTipText(MouseEvent e)
                {
                    java.awt.Point p = e.getPoint();
                    int index = columnModel.getColumnIndexAtX(p.x);
                    int realIndex =
                            columnModel.getColumn(index).getModelIndex();
                    return NotificationConfigurationPanel.columnToolTips[
                            realIndex];
                }
            };
        }
    }
}
