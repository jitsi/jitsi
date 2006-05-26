package net.java.sip.communicator.impl.netaddr;

/**
 * The AddressPreference class is used to assign preference to the various
 * network addresses on a host. Preferencies are in essence integers that
 * may vary between MAX_PREF and MIN_PREF, where MIN_PREF indicates low or
 * inexisting connectivity through the specified address and MAX_PREF -
 * complete, flowless open internet access (the perfect connection ;-) )
 *
 * @author Emil Ivov
 */
public class AddressPreference
        implements Comparable
{
    public static final int MAX_PREF = 100;
    public static final int MIN_PREF = 0;

    public static final AddressPreference MAX = new AddressPreference(MAX_PREF);
    public static final AddressPreference MIN = new AddressPreference(MIN_PREF);

    /**
     * The numerical value of this AddressPreference instance.
     */
    private int preference = (MAX_PREF - MIN_PREF)/2;

    /**
     * Creates an AddressPreference instance with the specified preference.
     * @param preference the preference integer corresponding to this
     * AddressPreference
     */
    AddressPreference(int preference)
    {
        this.preference = preference;
    }

    /**
     * Creates an AddressPreference object with a default preference value.
     */
    AddressPreference()
    {
    }

    /**
     * Sets the preference value of this AddressPreference instance to be
     * @param preference int
     */
    void setPreference(int preference)
    {
        this.preference = preference;
    }


    /**
     * Returns the exact preference value of this AddressPreference instance.
     * @return the exact preference value (an ineteger between MAX_PREF and
     * MIN_PREF) representing this AddressPreference instance.
     */
    public int getPreference()
    {
        return preference;
    }

    /**
     * Compares this address preference with the specified object for order.
     * Returns a negative integer, zero, or a positive integer as this
     * AddressPreference is less than, equal to, or greater than the specified
     * object.<p>
     *
     * @param   o the Object to be compared.
     * @return  a negative integer, zero, or a positive integer as this object
     *          is less than, equal to, or greater than the specified object.
     *
     * @throws ClassCastException if the specified object's type is not an
     * instance or a descendant of AddressPreference.
     */
    public int compareTo(Object o)
    {
        AddressPreference other = (AddressPreference)o;

        return this.preference - other.preference;
    }

    /**
     * Returns true if <tt>obj</tt> is the same object as this
     * AddressPreference or is at least an instance of AddressPreference and
     * has the same numerical value. In all other cases the method returns
     * false.
     * @param obj the object to compare with
     * @return true if both objects represent the same preference value and
     * false otherwise.
     */
    public boolean equals(Object obj)
    {
        if (! (obj instanceof AddressPreference)
            || obj == null)
            return false;

        if (obj == this
            || ((AddressPreference)obj).preference == preference )
            return true;

        return false;
    }

    public String toString()
    {
        return "preference="+getPreference();
    }
}
