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
package net.java.sip.communicator.impl.gui.main.call.conference;

import java.awt.*;
import java.beans.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.call.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.skin.*;

/**
 * The <tt>ConferenceMemberPanel</tt> renders <tt>ConferenceMember</tt> details.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class ConferenceMemberPanel
    extends BasicConferenceParticipantPanel<ConferenceMember>
    implements PropertyChangeListener,
               Skinnable
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The underlying conference member.
     */
    private final ConferenceMember member;

    /**
     * Creates a <tt><ConferenceMemberPanel</tt> by specifying the corresponding
     * <tt>member</tt> that it represents.
     *
     * @param callRenderer the parent call renderer
     * @param member the <tt>ConferenceMember</tt> shown in this panel
     * @param isVideo indicates if the video conference interface is enabled.
     */
    public ConferenceMemberPanel(
            SwingCallRenderer callRenderer,
            ConferenceMember member,
            boolean isVideo)
    {
        super(callRenderer, member, isVideo);

        this.member = member;

        this.member.addPropertyChangeListener(this);

        setParticipantName(member.getDisplayName());
        setParticipantState(member.getState().toString());

        setTitleBackground(
                new Color(
                        GuiActivator.getResources().getColor(
                                "service.gui.CALL_MEMBER_NAME_BACKGROUND")));
    }

    /**
     * Releases the resources (which require explicit disposal such as listeners
     * added by this view to its model) acquired by this instance throughout its
     * lifetime and prepares it for garbage collection.
     */
    void dispose()
    {
        member.removePropertyChangeListener(this);
    }

    /**
     * Returns the underlying <tt>ConferenceMember</tt>.
     *
     * @return the underlying <tt>ConferenceMember</tt>.
     */
    public ConferenceMember getConferenceMember()
    {
        return member;
    }

    /**
     * Reloads title background color.
     */
    @Override
    public void loadSkin()
    {
        super.loadSkin();

        setTitleBackground(
                new Color(
                        GuiActivator.getResources().getColor(
                                "service.gui.CALL_MEMBER_NAME_BACKGROUND")));
    }

    /**
     * Updates the display name and the state of the <tt>ConferenceMember</tt>
     * depicted by this instance whenever the values of the respective
     * properties change.
     *
     * @param ev a <tt>PropertyChangeEvent</tt> which specifies the name of the
     * <tt>ConferenceMember</tt> property which had its value changed and the
     * old and new values of that property
     */
    public void propertyChange(PropertyChangeEvent ev)
    {
        String propertyName = ev.getPropertyName();

        if (propertyName.equals(ConferenceMember.DISPLAY_NAME_PROPERTY_NAME))
        {
            String displayName = (String) ev.getNewValue();

            setParticipantName(displayName);

            revalidate();
            repaint();
        }
        else if (propertyName.equals(ConferenceMember.STATE_PROPERTY_NAME))
        {
            ConferenceMemberState state
                = (ConferenceMemberState) ev.getNewValue();

            setParticipantState(state.toString());

            revalidate();
            repaint();
        }
    }
}
