package net.java.sip.communicator.impl.netaddr;

import java.net.*;

/**
 * todo: add a type for the open internet
 *
 *
 * @author Emil Ivov
 */
public class FirewallDescriptor
{

    /**
     * The amount of preference points that a firewall corresponding to this
     * descriptor should remove from the address preference.
     */
    private AddressPreference preferenceSubtrahend =
        new AddressPreference(AddressPreference.MIN_PREF);


    /**
     * Means no firewall or nat. In the case of a privvate IPv4 address this
     * would meand no internet connectivity through this address. The same
     * applies to IPv6 link local addresses. In the case of a public (IPv4 or
     * IPv6) address this means that there is unlimited connectivity.
     */
    public static final int TYPE_OPEN_INTERNET = 1;

    /**
     * Full cone NAT or firewall.
     */
    public static final int TYPE_FULL_CONE = 2;

    /**
     * Restricted cone NAT or firewall.
     */
    public static final int TYPE_RESTRICTED_CONE = 3;

    /**
     * Port restricted cone NAT or firewall.
     */
    public static final int TYPE_PORT_RESTRICTED_CONE = 4;

    /**
     * Symmetric NAT or firewall.
     */
    public static final int TYPE_SYMMETRIC = 5;

    /**
     * Determines firewall type. Could be one of symmetric, full cone,
     * restricted cone, port restricted cone.
     */
    private int type = -1;

    /**
     * The time (in seconds) that address port bindings remain active on this
     * firewall without the node sendind any packets. Bindings may and most
     * probably will change during runtime. They are initially set to be equal
     * to net.java.sip.communicator.service.netaddr.INITIAL_BINDINGS_LIFETIME.
     */
    private int bindingsLifetime = 30;

    /**
     * Indicates whether or not the firewall corresponding to this descriptor
     * is also acting as NAT (are we using a globally routable ip address or
     * not).
     */
    private boolean isTranslatingAddresses = false;

    /**
     * The public IP address of the firewall. In the case of a NAT this is
     * the address that other would have to use to contact us.
     */
    private InetAddress publicAddress = null;

    /**
     * Constructs an empty firewall descirptor
     */
    FirewallDescriptor()
    {
        super();
    }

    /**
     * Set the type of this firewall.
     * @param type the type of the firewall referenced by this descriptor.
     */
    void setType(int type)
    {
        this.type = type;
    }

    /**
     * Returns the type of the firewall referenced by this desciptor
     * @return the type of the firewall referenced by this desciptor. One of the
     * TYPE_XXX fields of this class.
     */
    public int getType()
    {
        return this.type;
    }

    /**
     * Sets the address preference subtrahend corresponding to the firewall
     * referenced by this descriptor. The subtrahend is a value that is
     * subtracted from AddressPreference-s of addresses behind this firewall.
     *
     * @param subtrahend the amount of address preference points that need to
     * be subtracted from address preferences of oaddresses behind this
     * firewall.
     */
    void setPreferenceSubtrahend(AddressPreference subtrahend)
    {
        this.preferenceSubtrahend = subtrahend;
    }

    /**
     * Returns the address preference subtrahend corresponding to the firewall
     * referenced by this descriptor. The subtrahend is a value that is
     * subtracted from AddressPreference-s of addresses behind this firewall.
     *
     * @return the amount of address preference points that need to
     * be subtracted from address preferences of oaddresses behind this
     * firewall.
     */
    public AddressPreference getAddressPreferenceSubtrahend()
    {
        return this.preferenceSubtrahend;
    }

    /**
     * Sets the public IP address of the firewall. In the case of a NAT this is
     * the address that others would have to use to contact us.
     *
     * @param address the ip address of the nat
     */
    void setPublicAddress(InetAddress address)
    {
        this.publicAddress = address;
    }

    /**
     * Returns the public IP address of the firewall. In the case of a NAT this
     * is the address that others would have to use to contact us.
     *
     * @return the ip address of the nat
     */
    public InetAddress getPublicAddress()
    {
        return this.publicAddress;
    }

    /**
     * Sets the time (in seconds) that address port bindings remain active on
     * this firewall without the node sendind any packets. Bindings may and most
     * probably will change during runtime. They are initially set to be equal
     * to net.java.sip.communicator.service.netaddr.INITIAL_BINDINGS_LIFETIME.
     *
     * @param lifetime the number of seconds that bindings on this firewall
     * remain active
     */
    void setBindingsLifetime(int lifetime)
    {
        this.bindingsLifetime = lifetime;
    }

    /**
     * Returns the time (in seconds) that address port bindings remain active on
     * this firewall without the node sendind any packets. Bindings may and most
     * probably will change during runtime. They are initially set to be equal
     * to net.java.sip.communicator.service.netaddr.INITIAL_BINDINGS_LIFETIME.
     *
     * @return lifetime the number of seconds that bindings on this firewall
     * remain active
     */
    public int getBindingsLifetime()
    {
        return this.bindingsLifetime;
    }

    /**
     * Specifies whether this is an address translating firewall (NAT) or not.
     * @param isNAT a boolean specifying whether the firewall referenced by this
     * descriptor is a NAT.
     */
    void setTranslatingAddresses(boolean isNAT)
    {
        this.isTranslatingAddresses = isNAT;
    }

    /**
     * Determines whether this is an address translating firewall (NAT) or not.
     * @return true if the firewall referenced by this descriptor is a NAT and
     * false otherwise.
     */
    public boolean isTranslatingAddresses()
    {
        return isTranslatingAddresses;
    }
}
