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
package net.java.sip.communicator.service.history.records;

/**
 * @author Alexander Pelov
 */
public class HistoryRecordStructure
{

    private String[] propertyNames;

    /**
     * Creates an entry structure object used to define the shape of the data
     * stored in the history.
     *
     * Note that the property names are not unique, i.e. a single property
     * may have 0, 1 or more values.
     *
     * @param propertyNames
     */
    public HistoryRecordStructure(String[] propertyNames)
    {
        // TODO: Validate: Assert.assertNonNull(propertyNames, "Parameter propertyNames should be non-null.");

        this.propertyNames = new String[propertyNames.length];
        System.arraycopy(propertyNames, 0, this.propertyNames, 0,
                         this.propertyNames.length);
    }

    public String[] getPropertyNames()
    {
        return this.propertyNames;
    }

    public int getPropertyCount()
    {
        return this.propertyNames.length;
    }

}
