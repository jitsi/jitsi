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
package net.java.sip.communicator.impl.protocol.sip.xcap.model.xcaperror;

/**
 * The base XCAP error.
 *
 * @author Grigorii Balutsel
 */
public abstract class BaseXCapError implements XCapError
{
    /**
     * The phrase attribute.
     */
    private String phrase;

    /**
     * Creates the XCAP error with phrase attribute.
     *
     * @param phrase the phrase to set.
     */
    public BaseXCapError(String phrase)
    {
        this.phrase = phrase;
    }

    /**
     * Gets the phrase attribute.
     *
     * @return User readable error description.
     */
    public String getPhrase()
    {
        return phrase;
    }

    /**
     * Sets the value of the phrase property.
     *
     * @param phrase the phrase to set.
     */
    void setPhrase(String phrase)
    {
        this.phrase = phrase;
    }
}
