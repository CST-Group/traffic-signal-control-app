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
package br.unicamp.cst.trafficUnjammer.experiments.entity;

import java.io.Serializable;
import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * @author andre
 *
 */
@JsonIgnoreProperties(ignoreUnknown=true)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
public class ControlledLanesIDs implements Serializable 
{
	private static final long serialVersionUID = 7105295659607280687L;
	
	private ArrayList<String> controlledIncomingLanesIDs;
	
	private ArrayList<String> controlledOutgoingLanesIDs;
	
	private String phaseIncomingLanes;

	/**
	 * @return the controlledIncomingLanesIDs
	 */
	public synchronized ArrayList<String> getControlledIncomingLanesIDs() 
	{
		return controlledIncomingLanesIDs;
	}

	/**
	 * @param controlledIncomingLanesIDs the controlledIncomingLanesIDs to set
	 */
	public synchronized void setControlledIncomingLanesIDs(ArrayList<String> controlledIncomingLanesIDs) 
	{
		this.controlledIncomingLanesIDs = controlledIncomingLanesIDs;
	}

	/**
	 * @return the controlledOutgoingLanesIDs
	 */
	public synchronized ArrayList<String> getControlledOutgoingLanesIDs() 
	{
		return controlledOutgoingLanesIDs;
	}

	/**
	 * @param controlledOutgoingLanesIDs the controlledOutgoingLanesIDs to set
	 */
	public synchronized void setControlledOutgoingLanesIDs(ArrayList<String> controlledOutgoingLanesIDs)
	{
		this.controlledOutgoingLanesIDs = controlledOutgoingLanesIDs;
	}

	/**
	 * @return the phaseIncomingLanes
	 */
	public synchronized String getPhaseIncomingLanes() {
		return phaseIncomingLanes;
	}

	/**
	 * @param phaseIncomingLanes the phaseIncomingLanes to set
	 */
	public synchronized void setPhaseIncomingLanes(String phaseIncomingLanes) {
		this.phaseIncomingLanes = phaseIncomingLanes;
	}
}
