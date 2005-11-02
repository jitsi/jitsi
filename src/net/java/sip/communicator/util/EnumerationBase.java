/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * The base class for all enumerated types. One uses this class by extending it
 * and defining "static final" constants, like this:
 * 
 * <code>
 * public class Color extends EnumerationBase {
 * 	
 *  public static final Color Red = new Color("Red");
 *  public static final Color Blue = new Color("Blue");
 *  public static final Color Green = new Color("Green");
 * 
 * 	protected Color(String description) {
 * 		super(description);
 *  }
 *  
 * }
 * </code>
 * 
 * 
 * Note: always compare the enum constants with <code>equals</code>, because
 * serialization and multiple class loaders can cause alot of hard to trace bugs
 * 
 * For more information on the issue see
 * 
 * @link http://www.javaworld.com/javaworld/javatips/jw-javatip122.html and
 * @link http://www.javaworld.com/javaworld/javatips/jw-javatip133.html
 * 
 * @author Alexander Pelov
 */
public abstract class EnumerationBase {

	private String description;

	/**
	 * Constructs an enumeration element
	 * 
	 * @param description
	 *            The description of this element. Should be unique
	 */
	protected EnumerationBase(String description) {
		Assert.assertNonNull(description, "Enumeration description should be non-null");

		this.description = description;
	}

	public final String toString() {
		return this.description;
	}
	
	/**
	 * This method searches through all static fields defined in a class
	 * which are of type derived from EnumerationBase and returns the one whose 
	 * description matches the desired one. It is the "inverse" of toString.
	 * 
	 * Typical usage: 
	 *  WeekdaysEnumeration monday = (WeekdaysEnumeration)
	 *  	EnumerationBase.fromString(WeekdaysEnumeration.class, "Monday");
	 * 
	 * Note: You should test to see the exact type of the object. A "blind" cast
	 * may cause an exception if the object is of a type superior to the one
	 * you are testing.
	 * 
	 * @param clazz The class whose static fields will be tested. The 
	 * 			static fields defined in it and its parents will be tested
	 * 			for description match.
	 * @param description The desired description to be matched.
	 * @return The field matching the description. Null if no field matches the
	 * 			description.
	 */
	public static EnumerationBase fromString(Class clazz,
			String description)
	{
		if(clazz == null || description == null) {
			return null;
		}
		
		EnumerationBase retVal = null;
		
		Field[] fields = clazz.getFields();
		
		for(int i = 0; i < fields.length; i++) {
			if((Modifier.STATIC & fields[i].getModifiers()) != 0 &&
					EnumerationBase.class.isAssignableFrom(fields[i].getType()))
			{
				try {
					EnumerationBase e = (EnumerationBase)fields[i].get(null);
					if(e.descriptionEquals(description)) {
						retVal = e;
						break;
					}
				} catch (Exception e) {}
			}
		}
		
		return retVal;
	}

	public boolean equals(Object obj) {
		if (obj instanceof EnumerationBase) {
			return this.description.equals(((EnumerationBase) obj).description);
		}
		return false;
	}

	public boolean descriptionEquals(String str) {
		return this.description.equals(str);
	}

	public boolean descriptionEqualsIgnoreCase(String str) {
		return this.description.equalsIgnoreCase(str);
	}
	
}
