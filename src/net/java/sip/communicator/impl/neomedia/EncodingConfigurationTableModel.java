/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.impl.neomedia;

import java.util.*;

import javax.swing.table.*;

import net.java.sip.communicator.plugin.desktoputil.*;

import org.jitsi.impl.neomedia.*;
import org.jitsi.impl.neomedia.format.*;
import org.jitsi.service.neomedia.*;
import org.jitsi.service.neomedia.codec.*;
import org.jitsi.service.neomedia.format.*;

/**
 * Implements {@link TableModel} for {@link EncodingConfiguration}.
 *
 * @author Lyubomir Marinov
 */
public class EncodingConfigurationTableModel
    extends MoveableTableModel
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    private final EncodingConfiguration encodingConfiguration;

    private MediaFormat[] encodings;

    private final MediaType type;

    /**
     * Constructor.
     *
     * @param encodingConfiguration the encoding configuration
     * @param type media type
     */
    public EncodingConfigurationTableModel(int type,
        EncodingConfiguration encodingConfiguration)
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
            = encodingConfiguration.getAllEncodings(type);
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
                return encoding.getEncoding()
                    + "/"
                    + encoding.getRealUsedClockRateString();
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

    /**
     * Move the row.
     *
     * @param rowIndex index of the row
     * @param up true to move up, false to move down
     * @return the next row index
     */
    @Override
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


            // We fire the update event before setting the configuration
            // property in order to have more reactive user interface.
            fireTableCellUpdated(rowIndex, columnIndex);

            encodingConfiguration.setPriority(encoding, priority);
        }
    }
}
