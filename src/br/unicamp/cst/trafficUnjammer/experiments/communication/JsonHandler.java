/*******************************************************************************
 * Copyright (c) 2016  DCA-FEEC-UNICAMP
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, Version 2.0
 * which accompanies this distribution, and is available at
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Contributors:
 *     A. L. O. Paraense, R. R. Gudwin - initial implementation
 ******************************************************************************/
package br.unicamp.cst.trafficUnjammer.experiments.communication;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

/**
 * @author andre
 *
 */
public class JsonHandler 
{


	public JsonHandler() {
	}

	/**
	 * Convert a String to a Object from a specific class
	 * @param <T>
	 * @param objectClass
	 * @param jsonData
	 * @return
	 */
	public <T> Object fromJsonDataToObject(Class<T> objectClass, String jsonData )
	{
		
		ObjectMapper mapper = new ObjectMapper();
		ObjectReader reader =  mapper.reader(objectClass);
		T parsed = null;
		try 
		{
			parsed = reader.readValue(jsonData);
		} catch (JsonProcessingException e) 
		{
			
		} catch (IOException e) 
		{
			
		}

		return parsed;
	}

	/**
	 * Convert an object to String json formatted using GOOGLE GSON API
	 * @param object
	 * @return
	 */
	public String fromObjectToJsonData(Object object )
	{
		ObjectMapper mapper = new ObjectMapper();
		Writer strWriter = new StringWriter();

		try {
			mapper.writeValue(strWriter, object);
		} catch (JsonGenerationException e) {
			
		} catch (JsonMappingException e) {
			
		} catch (IOException e) {
			
		}

		String jsonData = strWriter.toString();
		return jsonData;
	}
	
	/**
	 *
	 * @param typerefence
	 * @param objectNodeBase
	 * @param jsonData
	 * @return
	 */
	public<T> ArrayList<Object> fromJsonDataToListOfObject( TypeReference<T> typerefence,  String jsonData )
	{

		ObjectMapper mapper = new ObjectMapper();
		ArrayList<Object> parsed = null;
		try 
		{

			JsonNode node = mapper.readTree(jsonData);
			parsed = mapper.readValue(node.traverse(), typerefence);


		} catch (JsonProcessingException e) 
		{
			e.printStackTrace();
		} catch (IOException e) 
		{
			e.printStackTrace();
		}

		return parsed;
	}
}
