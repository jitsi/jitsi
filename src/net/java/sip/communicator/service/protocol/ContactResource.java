/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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
    private int priority;

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
                            int priority)
    {
        this.contact = contact;
        this.resourceName = resourceName;
        this.presenceStatus = presenceStatus;
        this.priority = priority;
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
}
