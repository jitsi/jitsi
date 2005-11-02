/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.history.records;

import net.java.sip.communicator.util.Assert;

/**
 * @author Alexander Pelov
 */
public class HistoryRecordStructure {

	public static final TextType DEFAULT_TEXT_TYPE = TextType.LONG;
	
	private String[] propertyNames;
	private TextType[] valueTypes;

	/**
	 * Creates an entry structure object used to define the shape of the data
	 * stored in the history. All valueTypes are set to TextType.LONG.
	 * 
	 * Note that the property values are not unique, i.e. a single property 
	 * may have 0, 1 or more values.
	 * 
	 * @param propertyNames
	 */
	public HistoryRecordStructure(String[] propertyNames) {
		Assert.assertNonNull(propertyNames, "Parameter propertyNames should be non-null.");		
		this.propertyNames = new String[propertyNames.length];
		System.arraycopy(propertyNames, 0, this.propertyNames, 0, this.propertyNames.length);
		
		this.valueTypes = new TextType[this.propertyNames.length];
		for(int i = 0; i < this.valueTypes.length; i++) {
			this.valueTypes[i] = HistoryRecordStructure.DEFAULT_TEXT_TYPE; 
		}
	}

	/**
	 * Creates an entry structure object used to define the shape of the data
	 * stored in the history.
	 * 
	 * Note that the property values are not unique, i.e. a single property 
	 * may have 0, 1 or more values.
	 * 
	 * @param propertyNames
	 * @param valueTypes
	 */
	public HistoryRecordStructure(String[] propertyNames, TextType[] valueTypes) {		
		Assert.assertNonNull(propertyNames, "Parameter propertyNames should be non-null.");
		Assert.assertNonNull(valueTypes, "Parameter valueTypes should be non-null.");
		Assert.assertTrue(propertyNames.length == valueTypes.length, 
				"The length of the propertyNames and valueTypes should be equal.");
		
		this.propertyNames = new String[propertyNames.length];
		this.valueTypes = new TextType[valueTypes.length];
		
		System.arraycopy(propertyNames, 0, this.propertyNames, 0, this.propertyNames.length);
		System.arraycopy(valueTypes, 0, this.valueTypes, 0, this.valueTypes.length);
	}
	
	public String[] getPropertyNames() {
		return this.propertyNames;
	}
	
	public TextType[] getValueTypes() {
		return this.valueTypes;
	}
	
	public int getPropertyCount() {
		return this.propertyNames.length;
	}
	
}
