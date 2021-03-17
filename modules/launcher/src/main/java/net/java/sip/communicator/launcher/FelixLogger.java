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

import lombok.extern.slf4j.*;
import org.apache.felix.framework.*;

@Slf4j
public class FelixLogger
    extends Logger
{
    @Override
    protected void doLog(int level, String msg, Throwable throwable)
    {
        switch (level)
        {
        case LOG_DEBUG:
            logger.debug(msg, throwable);
            break;
        case LOG_INFO:
            logger.info(msg, throwable);
            break;
        case LOG_WARNING:
            logger.warn(msg, throwable);
            break;
        case LOG_ERROR:
            logger.error(msg, throwable);
            break;
        }
    }
}
