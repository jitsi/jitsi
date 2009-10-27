/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia;

import java.util.*;

import javax.swing.table.*;

import net.java.sip.communicator.impl.neomedia.codec.*;

/**
 * @author Lubomir Marinov
 */
public class EncodingConfigurationTableModel
    extends AbstractTableModel
{
    public static final int AUDIO = DeviceConfigurationComboBoxModel.AUDIO;

    private static final String[] NO_ENCODINGS = new String[0];

    public static final int VIDEO = DeviceConfigurationComboBoxModel.VIDEO;

    private final EncodingConfiguration encodingConfiguration;

    private String[] encodings;

    private final int type;

    public EncodingConfigurationTableModel(
        EncodingConfiguration encodingConfiguration, int type)
    {
        if (encodingConfiguration == null)
            throw new IllegalArgumentException("encodingConfiguration");
        if ((type != AUDIO) && (type != VIDEO))
            throw new IllegalArgumentException("type");

        this.encodingConfiguration = encodingConfiguration;
        this.type = type;
    }

    public Class<?> getColumnClass(int columnIndex)
    {
        return (columnIndex == 0) ? Boolean.class : super
            .getColumnClass(columnIndex);
    }

    public int getColumnCount()
    {
        return 2;
    }

    private String[] getEncodings()
    {
        if (encodings != null)
            return encodings;

        String[] availableEncodings;
        switch (type)
        {
        case AUDIO:
            availableEncodings =
                encodingConfiguration.getAvailableAudioEncodings();
            break;
        case VIDEO:
            availableEncodings =
                encodingConfiguration.getAvailableVideoEncodings();
            break;
        default:
            throw new IllegalStateException("type");
        }

        final int encodingCount = availableEncodings.length;
        if (encodingCount < 1)
            encodings = NO_ENCODINGS;
        else
        {
            encodings = new String[encodingCount];
            System
                .arraycopy(availableEncodings, 0, encodings, 0, encodingCount);
            Arrays.sort(encodings, 0, encodingCount, new Comparator<String>()
            {
                public int compare(String encoding0, String encoding1)
                {
                    return encodingConfiguration.getPriority(encoding1) -
                        encodingConfiguration.getPriority(encoding0);
                }
            });
        }
        return encodings;
    }

    private int[] getPriorities()
    {
        String[] encodings = getEncodings();
        final int count = encodings.length;
        int[] priorities = new int[count];
        for (int i = 0; i < count; i++)
        {
            int priority = encodingConfiguration.getPriority(encodings[i]);
            priorities[i] = (priority > 0) ? (count - i) : 0;
        }
        return priorities;
    }

    public int getRowCount()
    {
        return getEncodings().length;
    }

    public Object getValueAt(int rowIndex, int columnIndex)
    {
        String encoding = getEncodings()[rowIndex];
        switch (columnIndex)
        {
        case 0:
            return (encodingConfiguration.getPriority(encoding) > 0);
        case 1:
            return MediaUtils.rtpPayloadTypeToJmfEncoding(encoding);
        default:
            return null;
        }
    }

    public boolean isCellEditable(int rowIndex, int columnIndex)
    {
        return (columnIndex == 0);
    }

    public int move(int rowIndex, boolean up)
    {
        if (up)
        {
            if (rowIndex <= 0)
                throw new IllegalArgumentException("rowIndex");

            return move(rowIndex - 1, false) - 1;
        }

        if (rowIndex >= (getRowCount() - 1))
            throw new IllegalArgumentException("rowIndex");

        int[] priorities = getPriorities();
        final int nextRowIndex = rowIndex + 1;
        if (priorities[rowIndex] > 0)
            priorities[rowIndex] = priorities.length - nextRowIndex;
        if (priorities[nextRowIndex] > 0)
            priorities[nextRowIndex] = priorities.length - rowIndex;
        setPriorities(priorities);

        String swap = encodings[rowIndex];
        encodings[rowIndex] = encodings[nextRowIndex];
        encodings[nextRowIndex] = swap;

        fireTableRowsUpdated(rowIndex, nextRowIndex);
        return nextRowIndex;
    }

    private void setPriorities(int[] priorities)
    {
        final int count = encodings.length;
        if (priorities.length != count)
            throw new IllegalArgumentException("priorities");

        for (int i = 0; i < count; i++)
            encodingConfiguration.setPriority(encodings[i], priorities[i]);
    }

    public void setValueAt(Object value, int rowIndex, int columnIndex)
    {
        if ((columnIndex == 0) && (value instanceof Boolean))
        {
            int[] priorities = getPriorities();
            priorities[rowIndex] =
                ((Boolean) value) ? (priorities.length - rowIndex) : 0;
            setPriorities(priorities);

            fireTableCellUpdated(rowIndex, columnIndex);
        }
    }
}
