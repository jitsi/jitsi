package net.java.sip.communicator.impl.protocol.dict;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

public class OperationSetDirectoryDictImpl
	implements OperationSetDirectory
{
	private static final Logger logger
				= Logger.getLogger(OperationSetDirectoryDictImpl.class);
	
	/**
	 * The protocol provider that created us.
	 */
	private ProtocolProviderServiceDictImpl parentProvider = null;

	/**
	 * Creates an instance of this operation set keeping a reference to the
	 * parent protocol provider and presence operation set.
	 *
	 * @param provider The provider instance that creates us.
	 */
	public OperationSetDirectoryDictImpl(ProtocolProviderServiceDictImpl provider)
	{
		this.parentProvider = provider;
	}
	
	/**
	 * Returns directory entries according to a search and a return type
	 *
	 * @param search the search, may be an empty String
	 * @param returnType entries type
	 * @return an HashTable which links ids with visual infos - NULL otherwise
	 * @throws Exception Sends a exception if the host isn't reachable or if there isn't any dictionary
	 */
	public Hashtable<String,String> getEntries(String search, String returnType) throws Exception
	{
		DictAdapter adapter = this.parentProvider.getDictAdapter();
		Hashtable<String, String> result = null;
		
		String temp[];
		
		result = new Hashtable<String,String>();
		Vector<String> dlist = adapter.getDatabases();
		for (int i=0; i<dlist.size(); i++)
		{
			temp = dlist.get(i).split(" ", 2);
			temp[1] = temp[1].replace("\"", "");
			
			result.put(temp[0], temp[1]);
		}
		
		return result;
	}
}

