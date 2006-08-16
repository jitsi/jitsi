package net.java.sip.communicator.impl.netaddr;

import net.java.stun4j.*;

/**
 * The class is used to deliver results from a STUN Discovery Process. It
 * contains information about the NAT Server (or firewall )this client is behind,
 * and a mapped address value (if discovered)
 *
 *
 * <p>Organisation: <p> Louis Pasteur University, Strasbourg, France</p>
 * <p>Network Research Team (http://www-r2.u-strasbg.fr)</p></p>
 * @author Emil Ivov
 * @version 0.1
 */

public class StunDiscoveryReport
{
    /**
     * Indicates that NAT detection has failed or not yet initiated.
     */
    public static final String UNKNOWN               = "Unknown Network Configuration";

    /**
     * Means, there's no NAT or firewall.
     */
    public static final String OPEN_INTERNET         = "Open Internet Configuration";

    /**
     * Indicates that UDP communication is not possible.
     */
    public static final String UDP_BLOCKING_FIREWALL = "UDP Blocking Firewall";

    /**
     * Means we are behind a symmetric udp firewall.
     */
    public static final String SYMMETRIC_UDP_FIREWALL= "Symmetric UDP Firewall";

    /**
     * NAT type is full cone.
     */
    public static final String FULL_CONE_NAT         = "Full Cone NAT";

    /**
     * We are behind a symmetric nat.
     */
    public static final String SYMMETRIC_NAT         = "Symmetric NAT";

    /**
     * NAT type is Restricted Cone.
     */
    public static final String RESTRICTED_CONE_NAT   = "Restricted Cone NAT";

    /**
     * NAT type is port restricted cone.
     */
    public static final String PORT_RESTRICTED_CONE_NAT= "Port Restricted Cone NAT";

    private String natType = UNKNOWN;


    private StunAddress publicAddress = null;

    /**
     * Creates a discovery report with natType = UNKNOWN and a null public
     * address.
     */
    StunDiscoveryReport()
    {
    }

    /**
     * Returns the type of the NAT described in the report.
     * @return the type of the NAT that this report is about.
     */
    public String getNatType()
    {
        return natType;
    }

    /**
     * Sets the type of the NAT indicated by the report.
     * @param natType the type of the NAT.
     */
    void setNatType(String natType)
    {
        this.natType = natType;
    }

    /**
     * Returns the public addressed discovered by a discovery process.
     * @return an Inetner address for public use.
     */
    public StunAddress getPublicAddress()
    {
        return publicAddress;
    }

    /**
     * Sets a public address.
     * @param stunAddress An address that's accesible from everywhere.
     */
    void setPublicAddress(StunAddress stunAddress)
    {
        this.publicAddress = stunAddress;
    }


    /**
     * Compares this object with obj. Two reports are considered equal if and
     * only if both have the same nat type and their public addresses are
     * equal or are both null.
     * @param obj the object to compare against.
     * @return true if the two objects are equal and false otherwise.
     */
    public boolean equals(Object obj)
    {
        if(obj == null
           || !(obj instanceof StunDiscoveryReport))
           return false;

        if(obj == this)
            return true;

        StunDiscoveryReport target = (StunDiscoveryReport)obj;

        return (   target.getNatType() == getNatType()
                && ( getPublicAddress() == null && target.getPublicAddress() == null
                    || target.getPublicAddress().equals(getPublicAddress())));
    }

    /**
     * Returns a readable representation of the report.
     * @return a readable representation of the report.
     */
    public String toString()
    {
        return   "The detected network configuration is: " + getNatType() + "\n"
               + "Your mapped public address is: " + getPublicAddress();
    }


}
