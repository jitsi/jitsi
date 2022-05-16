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
package net.java.sip.communicator.service.gui;

public interface ConfigurationContainer
{
    /**
     * Shows or hides this configuration container depending on the value of
     * parameter <code>visible</code>.
     *
     * @param visible if <code>true</code>, shows the main application window;
     *            otherwise, hides the main application window.
     */
    public void setVisible(boolean visible);

    /**
     * Selects the given <tt>ConfigurationForm</tt> if it exists in this
     * container.
     *
     * @param configForm the <tt>ConfigurationForm</tt> to select
     */
    public void setSelected(ConfigurationForm configForm);

    /**
     * Validates the currently selected configuration form. This method is meant
     * to be used by configuration forms the re-validate when a new component
     * has been added or size has changed.
     */
    public void validateCurrentForm();
}
