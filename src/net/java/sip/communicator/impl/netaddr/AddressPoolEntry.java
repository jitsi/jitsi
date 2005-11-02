package net.java.sip.communicator.impl.netaddr;

import java.net.*;

/**
 * An entry in the Address Pool. An addresspool entry contains an address
 * belonging to any of the local network interfaces together with properties
 * that characterize them, such as - whether or not the address is publicly
 * routable or not, It's corresponding NAT entry obtained by stun. The type of
 * NAT that the address is located behind (if any). The lifetime of the bindings
 * behind this NAT. Any TURN bindings. Whether or not TCP connections seem to be
 * supported. Whether or not UDP connections seem to be supported, and etc. etc.
 *
 * Concerning the address, this is an immutable object, in other words, the
 * address of a single entry is not supposed to change once it has been created.
 * In case this address is modified while the application is running we'll
 * rather have a new address entry instead of modifying this one. The only thing
 * that can change about an address pool entry is its properties.
 *
 * @author Emil Ivov
 */
public class AddressPoolEntry
{
    private InetAddress address = null;

    /**
     * The AddressPreference qualifyer is used to indicate preference of this
     * address compared to others available on the current host.
     */
    private AddressPreference preference;

    /**
     * Only used for ipv4 addresses. Actually it would have been better to use
     * the isLinkLocal method of InetAddress but since it only works for ipv6
     * we're using this one. in the case of an ip address
     */
    private boolean isLinkLocal = false;

    private NetworkInterface ownerInterface = null;

    private FirewallDescriptor firewallDescriptor = null;

    private InetAddress turnAddress = null;

    public AddressPoolEntry(InetAddress address, NetworkInterface ownerIface)
    {
        if(address == null)
            throw new NullPointerException("Address param cannot be null");
        this.address = address;
        this.ownerInterface = ownerIface;
    }

    /**
     * Returns the ip address that this address pool entry represents.
     * @return InetAddress
     */
    public InetAddress getInetAddress()
    {
        return address;
    }

    /**
     * Determines whether or not the address pool entry represents an IPv6
     * address.
     * @return true if the address is ipv6 and false in case of ipv4.
     */
    public boolean isIPv6()
    {
        return (address instanceof Inet6Address);
    }

    /**
     * Determines whether this is a link local or publicly routable address.
     * Works for both IPv4 and IPv6 addresses.
     * @return true if the address is link local and thus not globally routable
     * and false if otherwise.
     */
    public boolean isLinkLocal()
    {
        if( address instanceof Inet6Address){
            if(address.isLinkLocalAddress()){
                return true;
            }
            else{
                return false;
            }
        }

        return isLinkLocal;
    }

    /**
     * Specifies whether or not the address is an IPv4 link local address.
     * @param linkLocal true if t
     */
    void setLinkLocal(boolean linkLocal)
    {
        this.isLinkLocal = linkLocal;
    }


   /**
    * Determines whether the address is the result of windows auto configuration.
    * (i.e. One that is in the 169.254.0.0 network)
    * @param add the address to inspect
    * @return true if the address is autoconfigured by windows, false otherwise.
    */
      /**
    * Determines whether the address is the result of windows auto configuration.
    * (i.e. One that is in the 169.254.0.0 network)
    * @param add the address to inspect
    * @return true if the address is autoconfigured by windows, false otherwise.
    */
   public static boolean isIPv4LinkLocalAutoconf(InetAddress add)
   {
       return (add.getAddress()[0] & 0xFF) == 169
           && (add.getAddress()[1] & 0xFF) == 254;
   }

   /**
    * Determines whether the address encapsulated by this entry is the result of
    * windows auto configuration (i.e. One that is in the 169.254.0.0 network)
    * @return true if the address is autoconfigured by windows, false otherwise.
    */
   public boolean isIPv4LinkLocalAutoconf()
   {
       return isIPv4LinkLocalAutoconf(address);
   }

    /**
     * Determines whether the address is an IPv4 link local address. IPv4 link
     * local addresses are those in the following networks:
     *
     * 10.0.0.0    to 10.255.255.255
     * 172.16.0.0  to 172.31.255.255
     * 192.168.0.0 to 192.168.255.255
     *
     * @param add the address to inspect
     * @return true if add is a link local ipv4 address and false if not.
     */
    public static boolean isLinkLocalIPv4Address(InetAddress add)
    {
        if(add instanceof Inet4Address)
        {
            byte address[] = add.getAddress();
            if ( (address[0] & 0xFF) == 10)
                return true;
            if ( (address[0] & 0xFF) == 172
                && (address[1] & 0xFF) >= 16 && address[1] <= 31)
                return true;
            if ( (address[0] & 0xFF) == 192
                && (address[1] & 0xFF) == 168)
                return true;
            return false;
        }
        return false;
    }


    /**
     * Determines whether the address encapsulated by this entry is an IPv4 link
     * local address. IPv4 link local addresses are those in the following
     * networks:
     *
     * 10.0.0.0    to 10.255.255.255
     * 172.16.0.0  to 172.31.255.255
     * 192.168.0.0 to 192.168.255.255
     *
     * @return true if add is a link local ipv4 address and false if not.
     */
    public boolean isLinkLocalIPv4Address()
    {
        return isLinkLocalIPv4Address(this.address);
    }

    /**
     * Determines whether the address encapsulated by this entry is an IPv6 link
     * local address. IPv6 link local addresses are briefly those that start
     * with fe80.
     *
     * @return true if add is a link local ipv6 address and false if not.
     */
    public boolean isLinkLocalIPv6Address()
    {
        return address instanceof Inet6Address && address.isLinkLocalAddress();
    }

    /**
     * Determines whether this is the localhost address (127.0.0.1 or ::1).
     * This is a simple wrapper of the InetAddress.isLoopbackAddress() method.
     * @return true if this is and IPv6 or IPv4 loopback address and false
     * otherwise.
     */
    public boolean isLoopback()
    {
        return address.isLoopbackAddress();
    }

    /**
     * Determines whether the adderss encapsulated by this address entry is
     * a globally routable inet address, or in other words is it possible
     * (at least in theory) to directly send packets to it from any point of
     * the internet.
     * @return true if this is a public ip addr and false otherwise.
     */
    public boolean isGloballyRoutable()
    {
        return !isLinkLocalIPv6Address()
            && !isIPv4LinkLocalAutoconf()
            && !isLinkLocalIPv4Address()
            && !isLoopback();
    }

    /**
     * Determines whether the adderss encapsulated by this address entry is
     * a 6to4 translation address (2002::/16)
     *
     * @return true if this is a 6to4 ip addr (belongs to 2002::/16) and false
     * otherwise.
     */
    public boolean is6to4()
    {
        return address instanceof Inet6Address
            && (address.getAddress()[0] & 0xff) == 0x20
            && (address.getAddress()[0] & 0xc0) == 0x02;
    }

    /**
     * Returns the interface that this address belongs to.
     * @return a reference to the interface that this address belongs to.
     */
    public NetworkInterface getOwnerInterface()
    {
        return ownerInterface;
    }

    /**
     * Returns a string representation of this address entry
     * @return a String containing key characteristics of this address pool
     * entry
     */
    public String toString()
    {
        return      "AddressPoolEntry:"
                +   address.getHostAddress()
                + "@"+getOwnerInterface().getDisplayName() + " "
                +   "isIPv6=" + isIPv6()  + ", "
                +   "isLoopback=" + isLoopback()  + ", "
                +   "isGloballyRoutable=" + isGloballyRoutable()  + ", "
                +   "isLinkLocal=" + isLinkLocal()
                +   "AddressPreference=["+preference+"]";
    }

    /**
     * Sets the AddressPreference that address diagnostics have calculated for
     * the address corresponding to this entry.
     * @param preference the preference to assign to this entry
     */
    void setAddressPreference(AddressPreference preference)
    {
        this.preference = preference;
    }

    /**
     * Returns the AddressPreference assigned to this AddressEntry. The
     * AddressPreference is what establishes an order in the "usability" of the
     * addresses on the local machine.
     * @return AddressPreference the AddressPreference assigned to this entry.
     */
    public AddressPreference getAddressPreference()
    {
        return this.preference;
    }

}
