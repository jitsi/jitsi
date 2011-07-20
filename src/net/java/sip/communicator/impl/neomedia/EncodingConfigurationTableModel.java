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
import net.java.sip.communicator.impl.neomedia.format.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.neomedia.format.*;

/**
 * Implements {@link TableModel} for {@link EncodingConfiguration}.
 *
 * @author Lyubomir Marinov
 */
public class EncodingConfigurationTableModel
    extends AbstractTableModel
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    private final EncodingConfiguration encodingConfiguration;

    private MediaFormat[] encodings;

    private final MediaType type;

    public EncodingConfigurationTableModel(
        EncodingConfiguration encodingConfiguration, int type)
    {
        if (encodingConfiguration == null)
            throw new IllegalArgumentException("encodingConfiguration");
        this.encodingConfiguration = encodingConfiguration;

        switch (type)
        {
        case DeviceConfigurationComboBoxModel.AUDIO:
            this.type = MediaType.AUDIO;
            break;
        case DeviceConfigurationComboBoxModel.VIDEO:
            this.type = MediaType.VIDEO;
            break;
        default:
            throw new IllegalArgumentException("type");
        }
    }

    @Override
    public Class<?> getColumnClass(int columnIndex)
    {
        return
            (columnIndex == 0)
                ? Boolean.class
                : super.getColumnClass(columnIndex);
    }

    public int getColumnCount()
    {
        return 2;
    }

    private MediaFormat[] getEncodings()
    {
        if (encodings != null)
            return encodings;

        MediaFormat[] availableEncodings
            = encodingConfiguration.getAvailableEncodings(type);
        int encodingCount = availableEncodings.length;

        if (encodingCount < 1)
            encodings = MediaUtils.EMPTY_MEDIA_FORMATS;
        else
        {
            /*
             * The MediaFormats will be displayed by encoding (name) and clock
             * rate and EncodingConfiguration will store them that way so this
             * TableModel should better display unique encoding-clock rate
             * pairs.
             */
            HashMap<String, MediaFormat> availableEncodingSet
                = new HashMap<String, MediaFormat>();

            for (MediaFormat availableEncoding : availableEncodings)
            {
                availableEncodingSet.put(
                        availableEncoding.getEncoding()
                            + "/"
                            + availableEncoding.getClockRateString(),
                        availableEncoding);
            }
            availableEncodings
                = availableEncodingSet.values().toArray(
                        MediaUtils.EMPTY_MEDIA_FORMATS);
            encodingCount = availableEncodings.length;

            encodings = new MediaFormat[encodingCount];
            System
                .arraycopy(availableEncodings, 0, encodings, 0, encodingCount);
            // Display the encodings in decreasing priority.
            Arrays
                .sort(encodings, 0, encodingCount, new Comparator<MediaFormat>()
                {
                    public int compare(MediaFormat format0, MediaFormat format1)
                    {
                        int ret
                            = encodingConfiguration.getPriority(format1)
                                - encodingConfiguration.getPriority(format0);

                        if (ret == 0)
                        {
                            /*
                             * In the cases of equal priorities, display them
                             * sorted by encoding name in increasing order.
                             */
                            ret
                                = format0.getEncoding().compareToIgnoreCase(
                                        format1.getEncoding());
                            if (ret == 0)
                            {
                                /*
                                 * In the cases of equal priorities and equal
                                 * encoding names, display them sorted by clock
                                 * rate in decreasing order.
                                 */
                                ret
                                    = Double.compare(
                                            format1.getClockRate(),
                                            format0.getClockRate());
                            }
                        }
                        return ret;
                    }
                });
        }
        return encodings;
    }

    private int[] getPriorities()
    {
        MediaFormat[] encodings = getEncodings();
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
        MediaFormat encoding = getEncodings()[rowIndex];

        switch (columnIndex)
        {
        case 0:
            return (encodingConfiguration.getPriority(encoding) > 0);
        case 1:
            if (MediaType.VIDEO.equals(encoding.getMediaType())
                    && (VideoMediaFormatImpl.DEFAULT_CLOCK_RATE
                            == encoding.getClockRate()))
                return encoding.getEncoding();
            else
            {
                String e = encoding.getEncoding();
                /*
                 * RFC 1890 erroneously assigned 8 kHz to the RTP clock rate for
                 * the G722 payload format. The actual sampling rate for G.722
                 * audio is 16 kHz.
                 */
                double cr
                    = "G722".equalsIgnoreCase(e)
                        ? 16000
                        : encoding.getClockRate();

                return e + "/" + ((long) cr);
            }
        default:
            return null;
        }
    }

    @Override
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

        MediaFormat swap = encodings[rowIndex];

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
        {
            encodingConfiguration.setPriority(encodings[i], priorities[i]);
            encodingConfiguration.setPriorityConfig(encodings[i], priorities[i]);
        }
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex)
    {
        if ((columnIndex == 0) && (value instanceof Boolean))
        {
            int priority
                = ((Boolean) value) ? (getPriorities().length - rowIndex) : 0;
            MediaFormat encoding = encodings[rowIndex];

            encodingConfiguration.setPriority(encoding, priority);

            // We fire the update event before setting the configuration
            // property in order to have more reactive user interface.
            fireTableCellUpdated(rowIndex, columnIndex);

            encodingConfiguration.setPriorityConfig(encoding, priority);
        }
    }
}
