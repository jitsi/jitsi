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
package net.java.sip.communicator.util;

import java.text.*;

/**
 * Utility class to format byte sizes. Formats 12345 bytes in "12.1 K".
 *
 * @author Damian Minkov
 */
public class ByteFormat
{
    /**
     * Formats a long which represent a number of bytes to human readable form.
     * @param bytes the value to format
     *
     * @return formatted string
     */
    public static String format(long bytes)
    {
        long check = 1;

        // sizes
        String[] sufixes = {"", " bytes", " K", " MB", " GB"};

        for(int i = 1; i <= 4; i++)
        {
            long tempCheck = check * 1024;

            if(bytes < tempCheck || i == 4)
            {
                return new DecimalFormat(check == 1 ? "#,##0" :
                    "#,##0.0").format((double)bytes/check) + sufixes[i];
            }

            check = tempCheck;
        }
        // we are not suppose to come to here
        return null;
    }
}
