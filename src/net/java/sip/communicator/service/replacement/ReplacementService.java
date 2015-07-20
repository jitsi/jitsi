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
package net.java.sip.communicator.service.replacement;

/**
 * A service used to provide substitution for any text in chat messages, like
 * smileys, video and image previews, etc.
 *
 * @author Purvesh Sahoo
 */
public interface ReplacementService
{
    /**
     * The source name property name.
     */
    public final String SOURCE_NAME = "SOURCE";

    /**
     * Returns the text replacements if any or returns the original source
     * string.
     *
     * @param sourceString the original source string.
     * @return the replacement string for the source string provided; the
     *         original string in case of no match.
     */
    public String getReplacement(String sourceString);

    /**
     * Returns the name of the replacement source.
     *
     * @return the replacement source name
     */
    public String getSourceName();

    /**
     * Returns the pattern used to match the source URL.
     *
     * @return the pattern of the source
     */
    public String getPattern();
}
