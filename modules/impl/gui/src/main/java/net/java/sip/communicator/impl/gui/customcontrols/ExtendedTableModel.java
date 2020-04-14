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
package net.java.sip.communicator.impl.gui.customcontrols;

import java.util.*;

import javax.swing.table.*;

/**
 * The <tt>ExtendedTableModel</tt> is a <tt>DefaultTableModel</tt> with one
 * method in addition that allow to obtain the row index from a value.
 *
 * @author Yana Stamcheva
 */
public class ExtendedTableModel extends DefaultTableModel
{
    private static final long serialVersionUID = 0L;

    /**
     * Returns the index of the row, in which the given value is contained.
     * @param value the value to search for
     * @return the index of the row, in which the given value is contained.
     */
    @SuppressWarnings("unchecked") //DefaultTableModel legacy code
    public int rowIndexOf(Object value)
    {
        Vector<Vector<Object>> dataVec = this.getDataVector();

        for(int i = 0; i < dataVector.size(); i ++) {
            Vector<Object> rowVector = dataVec.get(i);

            if(rowVector.contains(value)) {
                return i;
            }
        }
        return -1;
    }


    @Override
    public boolean isCellEditable(int row, int col)
    {
        return false;
    }
}
