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
package net.java.sip.communicator.launcher;

import java.io.*;
import lombok.*;
import lombok.extern.slf4j.*;

/**
 * Provides an {@link OutputStream} that captures written data and redirects it
 * to the logger once a newline is written.
 */
@Slf4j
@RequiredArgsConstructor
public class LoggerStdOut
    extends OutputStream
{
    private final boolean error;

    private final ByteArrayOutputStream buffer =
        new ByteArrayOutputStream(1000);

    @Override
    public void write(int b)
    {
        if (b == '\n')
        {
            if (error)
            {
                logger.error(buffer.toString());
            }
            else
            {
                logger.info(buffer.toString());
            }
            buffer.reset();
        }
        else
        {
            buffer.write(b);
        }
    }
}
