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
package br.unicamp.cst.trafficUnjammer.experiments;

import it.polito.appeal.traci.ControlledLink;
import it.polito.appeal.traci.Lane;
import it.polito.appeal.traci.Logic;
import it.polito.appeal.traci.Phase;
import it.polito.appeal.traci.SumoTraciConnection;
import it.polito.appeal.traci.TLState;
import it.polito.appeal.traci.TrafficLight;
import it.polito.appeal.traci.Vehicle;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import br.unicamp.cst.consciousness.SpotlightBroadcastController;
import br.unicamp.cst.core.entities.Codelet;
import br.unicamp.cst.core.entities.MemoryObject;
import br.unicamp.cst.core.entities.Mind;
import br.unicamp.cst.core.exceptions.CodeletActivationBoundsException;
import br.unicamp.cst.trafficUnjammer.codeRack.behavioralCodelets.OpenSlowNearLightVehiclesLane;
import br.unicamp.cst.trafficUnjammer.codeRack.motorCodelets.TrafficLightActuator;
import br.unicamp.cst.trafficUnjammer.codeRack.sensoryCodelets.LaneSensor;
import br.unicamp.cst.trafficUnjammer.experiments.communication.JsonHandler;
import br.unicamp.cst.trafficUnjammer.experiments.entity.ControlledLanesIDs;
import br.unicamp.cst.trafficUnjammer.rawMemory.MemoryObjectTypesTrafficLightController;

/**
 * @author andre
 *
 */
public class ExperimentSUMO 
{	
	static SumoTraciConnection sumo;
	
	static ArrayList<LaneSensor> listLaneSensor = new ArrayList<LaneSensor>();
	
	/**
	 * @param args
	 */
	public static void main(String[] args) 
	{
		if(args.length!=2)
		{
			System.out.println("Usage: ExperimentSUMO <P1> <P2>");
			System.out.println("<P1> = Server IP");
			System.out.println("<P2> = Server port");
			return;
		}
		
		String ipServidor = args[0];
		int port = Integer.valueOf(args[1]);
		
		Mind mind = new Mind();	
		
		try 
		{
			sumo = new SumoTraciConnection(InetAddress.getByName(ipServidor), port);	
				
			/*
			 * Compondo o controlador
			 */
			
			Map<String,Lane> mapAllLanes = sumo.getLaneRepository().getAll();
			
			Map<String, TrafficLight> mapTrafficLights = sumo.getTrafficLightRepository().getAll();
			for(Entry<String, TrafficLight> trafficLightPairs : mapTrafficLights.entrySet())
			{
				TrafficLight trafficLight = trafficLightPairs.getValue();								

				/*
				 * Traffic Light Phases
				 */
				ArrayList<TLState> TLStates = new ArrayList<TLState>();
				Logic[] logics = trafficLight.queryReadCompleteDefinition().get().getLogics();

				for(Logic logic: logics)
				{
					Phase[] phases = logic.getPhases();
					for(Phase phase :phases)
					{
						TLStates.add(phase.getState());
					}
				}			

				//actuator		
				MemoryObject trafficLightPhaseMO = mind.createMemoryObject(MemoryObjectTypesTrafficLightController.PHASE, "-1");	
				MemoryObject forcedPhaseMO = mind.createMemoryObject(MemoryObjectTypesTrafficLightController.FORCED_PHASE, "-1");
				Codelet trafficLightsActuator = mind.insertCodelet(new TrafficLightActuator(trafficLight,TLStates));
				trafficLightsActuator.addInput(trafficLightPhaseMO);	
				trafficLightsActuator.addInput(forcedPhaseMO);
				
				//--- Subsumption Actions --------
				
				OpenSlowNearLightVehiclesLane openSlowNearLightVehiclesLane = new OpenSlowNearLightVehiclesLane("OpenSlowNearLightVehiclesLane - TL "+trafficLight.getID());
				openSlowNearLightVehiclesLane.setTLStates(TLStates);				
				openSlowNearLightVehiclesLane.addOutputs(trafficLightsActuator.getInputs());					
				mind.insertCodelet(openSlowNearLightVehiclesLane);
				
				/*
				 * Controlled Lanes
				 */							
				ArrayList<Lane> controlledIncomingLanes = new ArrayList<Lane>();
				ArrayList<String> controlledIncomingLanesIDs = new ArrayList<String>();
				ArrayList<Lane> controlledOutgoingLanes = new ArrayList<Lane>();
				ArrayList<String> controlledOutgoingLanesIDs = new ArrayList<String>();
				ControlledLink[][] controlledLinks = trafficLight.queryReadControlledLinks().get().getLinks();
				for(int i=0;i<controlledLinks.length;i++)
				{											
					for(int j=0;j<controlledLinks[i].length;j++)
					{
						ControlledLink controlledLink = controlledLinks[i][j];
						
						Lane incomingLane = controlledLink.getIncomingLane();	
						controlledIncomingLanes.add(incomingLane);	
						controlledIncomingLanesIDs.add(incomingLane.getID());
						
						Lane outgoingLane = controlledLink.getOutgoingLane();
						controlledOutgoingLanes.add(outgoingLane);		
						controlledOutgoingLanesIDs.add(outgoingLane.getID());
						
						//Sensor														
						MemoryObject laneAverageVelocityMO = mind.createMemoryObject(MemoryObjectTypesTrafficLightController.VELOCITIES, "-1");	
						MemoryObject laneVehicleOccupancyMO = mind.createMemoryObject(MemoryObjectTypesTrafficLightController.DISTANCES_FROM_LIGHT, "-1");
						MemoryObject laneVehicleNumberMO = mind.createMemoryObject(MemoryObjectTypesTrafficLightController.VEHICLE_NUMBER, "-1");
						LaneSensor laneSensor = new LaneSensor(incomingLane);					
						laneSensor.addOutput(laneAverageVelocityMO);
						laneSensor.addOutput(laneVehicleOccupancyMO);
						laneSensor.addOutput(laneVehicleNumberMO);									
						mind.insertCodelet(laneSensor);
						listLaneSensor.add(laneSensor);
						
						//--- Subsumption Actions --------						
						
						openSlowNearLightVehiclesLane.addInputs(laneSensor.getOutputs());												
					}						
				}
				
				openSlowNearLightVehiclesLane.setControlledIncomingLanes(controlledIncomingLanes);
				openSlowNearLightVehiclesLane.setControlledOutgoingLanes(controlledOutgoingLanes);
				openSlowNearLightVehiclesLane.setMapAllLanes(mapAllLanes);
				
				//create a domain of controlled lanes (incoming, outgoing, across) so it can be put in the subsumption and exported as a JSON to the memory object that will be broadcasted
				ControlledLanesIDs controlledLanesIDs = new ControlledLanesIDs();
				controlledLanesIDs.setControlledIncomingLanesIDs(controlledIncomingLanesIDs);
				controlledLanesIDs.setControlledOutgoingLanesIDs(controlledOutgoingLanesIDs);
				
				JsonHandler jsonHandler = new JsonHandler();
				String controlledLaneIDsJSON = jsonHandler.fromObjectToJsonData(controlledLanesIDs);		
				
				MemoryObject moTL_ID = mind.createMemoryObject(MemoryObjectTypesTrafficLightController.CONSCIOUS_TRAFFIC_LIGHT_LANES_IDS, controlledLaneIDsJSON);
				openSlowNearLightVehiclesLane.addOutput(moTL_ID);
			}
			
			//--- Consciousness --------		
			
			Codelet consciousness = mind.insertCodelet(new SpotlightBroadcastController(mind.getCodeRack()));
			
			//--- Codelet that controls the simulator --------	
			
			Codelet simulation = mind.insertCodelet(new Codelet() 
			{				
				@Override
				public void proc() 
				{
					try 
					{
						int timeStep = sumo.getSimulationData().queryCurrentSimTime().get();
						
						int numVeiculos = 0;

						Collection<Vehicle> vehicles = sumo.getVehicleRepository().getAll().values();
						numVeiculos = vehicles.size();
						
						while(timeStep < 5 || numVeiculos >0)
						{															
							/*
							 * Next step
							 */
							
							sumo.nextSimStep();
							
							timeStep++;	
							vehicles = sumo.getVehicleRepository().getAll().values();
							numVeiculos = vehicles.size();		
													
							if(listLaneSensor!=null)
							{
								synchronized (listLaneSensor) 
								{
									for(LaneSensor laneSensor : listLaneSensor)
									{									
										laneSensor.setVehicles(vehicles);
									}
								}							
							}						
						}	
						
						sumo.close();							
					} catch (IllegalStateException e) 
					{
						e.printStackTrace();
					} catch (IOException e) 
					{
						e.printStackTrace();
					} catch (InterruptedException e) 
					{
						e.printStackTrace();
					}finally
					{
						System.exit(-1);
					}
				}
				
				@Override
				public void calculateActivation() 
				{
					try 			
					{
						setActivation(0.0d);
					} catch (CodeletActivationBoundsException e) 
					{					
						e.printStackTrace();
					}					
				}
				
				@Override
				public void accessMemoryObjects() 
				{
					// nothing	
				}
			});
		
			mind.start();

		} catch (UnknownHostException e) 
		{			
			e.printStackTrace();
		} catch (IOException e) 
		{		 
			e.printStackTrace();
		} catch (InterruptedException e) 
		{			
			e.printStackTrace();
		}
	}
}
