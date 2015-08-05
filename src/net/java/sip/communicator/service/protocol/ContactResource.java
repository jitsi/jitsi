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
package net.java.sip.communicator.service.protocol;

/**
 * The <tt>ContactResource</tt> class represents a resource, from which a
 * <tt>Contact</tt> is connected.
 *
 * @author Yana Stamcheva
 */
public class ContactResource
{
    /**
     * A static instance of this class representing the base resource. If this
     * base resource is passed as a parameter for any operation (send message,
     * call) the operation should explicitly use the base contact address. This
     * is meant to force a call or a message sending to all the resources for
     * the corresponding contact.
     */
    public static ContactResource BASE_RESOURCE = new ContactResource();

    /**
     * The contact, to which this resource belongs.
     */
    private Contact contact;

    /**
     * The name of this contact resource.
     */
    private String resourceName;

    /**
     * The presence status of this contact resource.
     */
    protected PresenceStatus presenceStatus;

    /**
     * The priority of this contact source.
     */
    protected int priority;

    /**
     * Whether this contact resource is a mobile one.
     */
    protected boolean mobile = false;

    /**
     * Creates an empty instance of <tt>ContactResource</tt> representing the
     * base resource.
     */
    public ContactResource() {}

    /**
     * Creates a <tt>ContactResource</tt> by specifying the
     * <tt>resourceName</tt>, the <tt>presenceStatus</tt> and the
     * <tt>priority</tt>.
     *
     * @param contact the parent <tt>Contact</tt> this resource is about
     * @param resourceName the name of this resource
     * @param presenceStatus the presence status of this resource
     * @param priority the priority of this resource
     */
    public ContactResource( Contact contact,
                            String resourceName,
                            PresenceStatus presenceStatus,
                            int priority,
                            boolean mobile)
    {
        this.contact = contact;
        this.resourceName = resourceName;
        this.presenceStatus = presenceStatus;
        this.priority = priority;
        this.mobile = mobile;
    }

    /**
     * Returns the <tt>Contact</tt>, this resources belongs to.
     *
     * @return the <tt>Contact</tt>, this resources belongs to
     */
    public Contact getContact()
    {
        return contact;
    }

    /**
     * Returns the name of this resource.
     *
     * @return the name of this resource
     */
    public String getResourceName()
    {
        return resourceName;
    }

    /**
     * Returns the presence status of this resource.
     *
     * @return the presence status of this resource
     */
    public PresenceStatus getPresenceStatus()
    {
        return presenceStatus;
    }

    /**
     * Returns the priority of the resources.
     *
     * @return the priority of this resource
     */
    public int getPriority()
    {
        return priority;
    }

    /**
     * Whether contact is mobile one. Logged in only from mobile device.
     * @return whether contact is mobile one.
     */
    public boolean isMobile()
    {
        return mobile;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result =
            prime * result
                + ((resourceName == null) ? 0 : resourceName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ContactResource other = (ContactResource) obj;
        if (resourceName == null)
        {
            if (other.resourceName != null)
                return false;
        }
        else if (!resourceName.equals(other.resourceName))
            return false;
        return true;
    }
}
