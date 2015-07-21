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
package net.java.sip.communicator.impl.protocol.sip.xcap;

import net.java.sip.communicator.impl.protocol.sip.xcap.model.commonpolicy.*;

/**
 * XCAP pres-rules client interface.
 * <p/>
 * Compliant with rfc4745, rfc5025
 *
 * @author Grigorii Balutsel
 */
public interface PresRulesClient
{
    /**
     * Pres-rules uri format
     */
    public static String DOCUMENT_FORMAT = "pres-rules/users/%2s/presrules";

    /**
     * Pres-rules content type
     */
    public static String CONTENT_TYPE = "application/auth-policy+xml";

    /**
     * Pres-rules namespace
     */
    public static String NAMESPACE = "urn:ietf:params:xml:ns:pres-rules";

    /**
     * Puts the pres-rules to the server.
     *
     * @param presRules the pres-rules to be saved on the server.
     * @throws IllegalStateException if the user has not been connected.
     * @throws XCapException         if there is some error during operation.
     */
    public void putPresRules(RulesetType presRules)
            throws XCapException;

    /**
     * Gets the pres-rules from the server.
     *
     * @return the pres-rules.
     * @throws IllegalStateException if the user has not been connected.
     * @throws XCapException         if there is some error during operation.
     */
    public RulesetType getPresRules()
            throws XCapException;

    /**
     * Deletes the pres-rules from the server.
     *
     * @throws IllegalStateException if the user has not been connected.
     * @throws XCapException         if there is some error during operation.
     */
    public void deletePresRules()
            throws XCapException;

}
