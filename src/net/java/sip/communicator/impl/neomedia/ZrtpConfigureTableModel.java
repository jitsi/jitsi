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

import gnu.java.zrtp.*;

import java.util.*;

import javax.swing.table.*;

/**
 * @author Werner Dittmann
 */
public class ZrtpConfigureTableModel<T extends Enum<T>>
    extends AbstractTableModel
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    private final ZrtpConfigure active;
    private final ZrtpConfigure inActive;

    // used to identify the Enum class when calling ZrtpConfigure methods.
    private final T algorithm;
    private final Class<T> clazz;

    boolean onOff[];

    public ZrtpConfigureTableModel(T algo, ZrtpConfigure act,
            ZrtpConfigure inAct, String savedConf)
    {
        active = act;
        inActive = inAct;
        algorithm = algo;

        clazz = algorithm.getDeclaringClass();

        initialize(savedConf);
    }

    private void initialize(String savedConf)
    {
        // Get all enums constants of this Enum to process
        T enumValues[] = clazz.getEnumConstants();

        // first build a list of all available algorithms of this type
        ArrayList<String> fullList = new ArrayList<String>(enumValues.length);
        for (T sh : enumValues)
            fullList.add(sh.name());

        String savedAlgos[] = savedConf.split(";");
        // Configure saved algorithms as active, remove them from full list of
        // algos
        for (String str : savedAlgos)
        {
            try
            {
                T algoEnum = Enum.valueOf(clazz, str);
                if (algoEnum != null)
                {
                    active.addAlgo(algoEnum);
                    fullList.remove(str);
                }
            }
            catch (IllegalArgumentException e)
            {
                continue;
            }
        }
        // rest of algorithms are inactive
        for (String str : fullList)
        {
            T algoEnum = Enum.valueOf(clazz, str);
            if (algoEnum != null)
                inActive.addAlgo(algoEnum);
        }
    }

    public int getColumnCount()
    {
        return 2;
    }

    public int getRowCount()
    {
        return active.getNumConfiguredAlgos(algorithm)
            + inActive.getNumConfiguredAlgos(algorithm);
    }

    public Object getValueAt(int row, int col)
    {
        switch (col)
        {
        case 0:
            if (row >= active.getNumConfiguredAlgos(algorithm)) {
                return (new Boolean(false));
            }
            return (new Boolean(true));
        case 1:
            if (row >= active.getNumConfiguredAlgos(algorithm)) {
                row -= active.getNumConfiguredAlgos(algorithm);
                return (inActive.getAlgoAt(row, algorithm).name());
            }
            else
                return (active.getAlgoAt(row, algorithm).name());
        default:
            return null;
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex)
    {
        return (columnIndex == 0);
    }

    @Override
    public Class<?> getColumnClass(int columnIndex)
    {
        return (columnIndex == 0) ? Boolean.class : super
            .getColumnClass(columnIndex);
    }

    @Override
    public void setValueAt(Object value, int row, int column)
    {
        if ((column == 0) && (value instanceof Boolean))
        {
            if (row >= active.getNumConfiguredAlgos(algorithm))
            {
                row -= active.getNumConfiguredAlgos(algorithm);
                active.addAlgo(inActive.getAlgoAt(row, algorithm));
                inActive.removeAlgo(inActive.getAlgoAt(row, algorithm));
            }
            else
            {
                inActive.addAlgo(active.getAlgoAt(row, algorithm));
                active.removeAlgo(active.getAlgoAt(row, algorithm));
            }
            fireTableRowsUpdated(0, getRowCount());
        }
    }

    /**
     * Move a Configuration entry up or down one position.
     *
     * The "move up" is Converted to a "move down" with modified row index
     * and flags.
     *
     * @param row
     *        Which row to move
     * @param up
     *        If true move up, else move down
     * @param upSave
     *        Because the functions converts a move up into a move down
     *        this flag shows what the caller intented. Needed to adjust
     *        an index return value.
     * @return new row index of entry
     */
    public int move(int row, boolean up, boolean upSave)
    {
        if (up)
        {
            if (row <= 0)
                throw new IllegalArgumentException("rowIndex");

            return move(row - 1, false, upSave) - 1;
        }
        T swap;
        if (row >= (getRowCount() - 1))
            throw new IllegalArgumentException("rowIndex");

        // Can't move down last last entry of active list.
//        if (row == active.getNumConfiguredPubKeys() - 1) {
//            // this was a "move up" of the first inActive member adjust index
//            return upSave ? row + 2 : row;
//        }

//        if (row >= active.getNumConfiguredPubKeys()) {
//            if (inActive.getNumConfiguredPubKeys() <= 1) {
//                return row + 2;
//            }
//            row -= active.getNumConfiguredPubKeys();
//            swap = inActive.getPubKeyAlgoAt(row);
//            inActive.removePubKeyAlgo(swap);
//            inActive.addPubKeyAlgoAt(row + 1, swap);
//            row++;                           // take active rows into account
//        }
//        else {
            if (active.getNumConfiguredAlgos(algorithm) <= 1) {
                return row;
            }
            swap = active.getAlgoAt(row, algorithm);
            active.removeAlgo(swap);
            active.addAlgoAt(row + 1, swap);
//        }

        fireTableRowsUpdated(0, getRowCount());
        return row + 1;
    }

    public boolean checkEnableUp(int row)
    {
        return (row < active.getNumConfiguredAlgos(algorithm));
    }

    public boolean checkEnableDown(int row)
    {
        return (row < active.getNumConfiguredAlgos(algorithm) - 1);
    }

    /**
     * Saves the ZrtpConfigure data for this algorithm to configure file
     */
    public void saveConfig()
    {
        StringBuffer algoStr = new StringBuffer();
        for (T sh: active.algos(algorithm))
        {
            algoStr.append(sh.name());
            algoStr.append(';');
        }
        // save in configuration data using the appropriate key
    }

    /**
     * Sets the ZrtpConfigure data for this algorithm to a predefined set.
     *
     * The caller prepared active ZrtpConfigureto contain a standard set of
     * algorithms. Get the names and construct a string, then call initialize
     * to setup the inActive ZrtpConfigure data.
     */
    public void setStandardConfig()
    {
        StringBuffer algoStr = new StringBuffer();
        for (T sh: active.algos(algorithm))
        {
            algoStr.append(sh.name());
            algoStr.append(';');
        }
        initialize(algoStr.toString());
        fireTableRowsUpdated(0, getRowCount());
    }
}
