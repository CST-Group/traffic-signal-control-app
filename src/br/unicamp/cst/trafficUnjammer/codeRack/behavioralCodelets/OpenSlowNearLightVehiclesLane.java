/**
 * 
 */
package br.unicamp.cst.trafficUnjammer.codeRack.behavioralCodelets;

import it.polito.appeal.traci.Lane;
import it.polito.appeal.traci.LightState;
import it.polito.appeal.traci.Link;
import it.polito.appeal.traci.TLState;

import java.util.ArrayList;
import java.util.Map;

import br.unicamp.cst.core.entities.Codelet;
import br.unicamp.cst.core.entities.MemoryObject;
import br.unicamp.cst.core.exceptions.CodeletActivationBoundsException;
import br.unicamp.cst.trafficUnjammer.experiments.communication.JsonHandler;
import br.unicamp.cst.trafficUnjammer.experiments.entity.ControlledLanesIDs;
import br.unicamp.cst.trafficUnjammer.rawMemory.MemoryObjectTypesTrafficLightController;

/**
 * @author andre
 *
 */
public class OpenSlowNearLightVehiclesLane extends Codelet
{
	private static final Double alpha = 0.01;
	private static final Double beta = 0.001;
	
	private ArrayList<MemoryObject> velocityMOList = new ArrayList<MemoryObject>();
	private ArrayList<MemoryObject> distanceMOList = new ArrayList<MemoryObject>();
	private ArrayList<MemoryObject> vehicleNumberMOList = new ArrayList<MemoryObject>();
	
	private MemoryObject phaseMO;
	private MemoryObject forcedPhaseMO;
	private MemoryObject broadcastedConsciousTrafficLightLanesIDSMO;
	private MemoryObject trafficLightPhaseAndLanesIDSMO;
	
	private Map<String,Lane> mapAllLanes;
	private ArrayList<Lane> controlledIncomingLanes;
	private ArrayList<Lane> controlledOutgoingLanes;
	private ArrayList<TLState> TLStates;		
		
	private ArrayList<Double> activationList;
	
	private ArrayList<String> maskTL;

	public OpenSlowNearLightVehiclesLane(String name) 
	{
		this.setName(name);		
	}
	
	@Override
	public void proc() 
	{		
		/*
		 * 
		 * 1 - build a string mask of what should be the phases
		 * 2 - Integrate the way it is calculated today with the mask 
		 * 3 - Calculate the mask if there is conscious content
		 */
		
		JsonHandler jsonHandler = new JsonHandler();
		maskTL = new ArrayList<String>();
		
		if(broadcastedConsciousTrafficLightLanesIDSMO!=null)
		{		
			
			ControlledLanesIDs broadcastedConsciousTLControlledLanesIDs = (ControlledLanesIDs) jsonHandler.fromJsonDataToObject(ControlledLanesIDs.class, (String) broadcastedConsciousTrafficLightLanesIDSMO.getI());
			
			StringBuffer sb = new StringBuffer();
			
			for(Lane controlledIncomingLane : controlledIncomingLanes)
			{
				int connectedBroadcastedConsciousTLControlledLaneIDIndex =  isConnected(controlledIncomingLane,broadcastedConsciousTLControlledLanesIDs.getControlledIncomingLanesIDs(),0.0d);
				if(connectedBroadcastedConsciousTLControlledLaneIDIndex > -1)
				{
//					System.out.println("ControlledIncomingLane: "+controlledIncomingLane.getID()+", CONECTADO");
					String connectedBroadcastedConsciousTLControlledLaneLight = "";
					if(broadcastedConsciousTLControlledLanesIDs!=null && broadcastedConsciousTLControlledLanesIDs.getPhaseIncomingLanes()!=null && broadcastedConsciousTLControlledLanesIDs.getPhaseIncomingLanes().length() > connectedBroadcastedConsciousTLControlledLaneIDIndex)
						connectedBroadcastedConsciousTLControlledLaneLight = broadcastedConsciousTLControlledLanesIDs.getPhaseIncomingLanes().substring(connectedBroadcastedConsciousTLControlledLaneIDIndex, connectedBroadcastedConsciousTLControlledLaneIDIndex);
					
					if(connectedBroadcastedConsciousTLControlledLaneLight.equalsIgnoreCase("R")|| connectedBroadcastedConsciousTLControlledLaneLight.equalsIgnoreCase("r"))
					{
						maskTL.add("R");	
						sb.append("R");
					}else
					{
						maskTL.add("G");	
						sb.append("G");
					}				    
				} else
				{
//					System.out.println("ControlledIncomingLane: "+controlledIncomingLane.getID()+", DESCONECTADO");
					maskTL.add("G");	
					sb.append("G");
				}
			}
			
			String phase = sb.toString();
			
			phaseMO.updateI("-1");
			forcedPhaseMO.updateI(phase);
			
			ControlledLanesIDs tLPhaseAndControlledLanesIDs = (ControlledLanesIDs) jsonHandler.fromJsonDataToObject(ControlledLanesIDs.class, (String) trafficLightPhaseAndLanesIDSMO.getI());
			if(tLPhaseAndControlledLanesIDs!=null)
			{
				tLPhaseAndControlledLanesIDs.setPhaseIncomingLanes(phase);				
				String controlledLaneIDsJSON = jsonHandler.fromObjectToJsonData(tLPhaseAndControlledLanesIDs);	
				trafficLightPhaseAndLanesIDSMO.updateI(controlledLaneIDsJSON);
			}
			
		}else
		{
			for(Lane lane : controlledIncomingLanes)
			{
				maskTL.add("G");							
			}
			
			int phaseIndex = findBestPhase(maskTL);

			if(phaseIndex>=0)
			{
				String phase = String.valueOf(phaseIndex);
				phaseMO.updateI(phase);
				
				ControlledLanesIDs tLPhaseAndControlledLanesIDs = (ControlledLanesIDs) jsonHandler.fromJsonDataToObject(ControlledLanesIDs.class, (String) trafficLightPhaseAndLanesIDSMO.getI());
				if(tLPhaseAndControlledLanesIDs!=null)
				{
					tLPhaseAndControlledLanesIDs.setPhaseIncomingLanes(phase);				
					String controlledLaneIDsJSON = jsonHandler.fromObjectToJsonData(tLPhaseAndControlledLanesIDs);	
					trafficLightPhaseAndLanesIDSMO.updateI(controlledLaneIDsJSON);
				}
				
			}			
			
			forcedPhaseMO.updateI("-1");
		}						
		
//		int phaseIndex = findBestPhase(maskTL);
//
//		if(phaseIndex>=0)
//			phaseMO.updateInfo(String.valueOf(phaseIndex));
	}

	private int isConnected(Lane controlledIncomingLane,ArrayList<String> consciousTLControlledLanesIDs, Double distanceTravelledSoFar) 
	{
		if(distanceTravelledSoFar > 1000.0d)
			return -1;
		
		for(int i = 0; i < consciousTLControlledLanesIDs.size(); i++)		
		{
			String consciousTLControlledIncomingLaneID = consciousTLControlledLanesIDs.get(i);
			if(controlledIncomingLane.getID().equalsIgnoreCase(consciousTLControlledIncomingLaneID))
				return i;
		}
		
		try
		{			
			ArrayList<Link> nextLinks = (ArrayList<Link>) controlledIncomingLane.getLinks();
			if(nextLinks!=null)
			{
//				System.out.println("ControlledIncomingLane: "+controlledIncomingLane.getID()+", número de links: "+nextLinks.size());
				for(Link nextLink : nextLinks)
				{
//					Lane nextInternalLane = nextLink.getNextInternalLane();
//					if(nextInternalLane!=null && distanceTravelledSoFar > 0.0d)
//						return false;
												
					Lane nextNonInternalLane = nextLink.getNextNonInternalLane();				
					if(nextNonInternalLane!=null&&!nextNonInternalLane.getID().equalsIgnoreCase(controlledIncomingLane.getID()))
					{
						distanceTravelledSoFar += nextNonInternalLane.getLength();
						return isConnected(nextNonInternalLane, consciousTLControlledLanesIDs,distanceTravelledSoFar);
					}else
					{
						return -1;
					}				
				}
			}else
			{
				return -1;
			}
		} catch (Exception e) 
		{			
			e.printStackTrace();
			return -1;
		}
		
		return -1;
	}

	private int findBestPhase(ArrayList<String> maskTL) 
	{
		int bestPhase = -1;
		int bestPhaseValue = Integer.MIN_VALUE;
		
		for(int i=0; i<TLStates.size();i++)
		{
			TLState tlState = TLStates.get(i); 			
			int phaseValue = 0;
			for(int j=0; j < tlState.lightStates.length;j++)
			{
				LightState tls = tlState.lightStates[j];
				if( (maskTL.get(j).equalsIgnoreCase("G")&&tls.isGreen()) || (maskTL.get(j).equalsIgnoreCase("R")&&tls.isRed()) )
				{
					phaseValue+=activationList.get(j);
				}
			}
			
			if(phaseValue>bestPhaseValue)
			{
				bestPhaseValue = phaseValue;
				bestPhase = i;
			}
		}

		return bestPhase;
	}

	/* (non-Javadoc)
	 * @see br.unicamp.cogsys.core.entities.Codelet#accessMemoryObjects()
	 */
	@Override
	public void accessMemoryObjects() 
	{
		int index=0;
		
		broadcastedConsciousTrafficLightLanesIDSMO = null;
		broadcastedConsciousTrafficLightLanesIDSMO = this.getBroadcast(MemoryObjectTypesTrafficLightController.CONSCIOUS_TRAFFIC_LIGHT_LANES_IDS, index);
		
		trafficLightPhaseAndLanesIDSMO = null;
		trafficLightPhaseAndLanesIDSMO = this.getOutput(MemoryObjectTypesTrafficLightController.CONSCIOUS_TRAFFIC_LIGHT_LANES_IDS, index);		
		
		if(phaseMO==null)
			phaseMO = this.getOutput(MemoryObjectTypesTrafficLightController.PHASE, index);
		
		if(forcedPhaseMO==null)
			forcedPhaseMO = this.getOutput(MemoryObjectTypesTrafficLightController.FORCED_PHASE, index);
		
		if(distanceMOList==null||distanceMOList.size()==0)
		{
			for(int i=0;i<controlledIncomingLanes.size();i++)
			{
				distanceMOList.add(this.getInput(MemoryObjectTypesTrafficLightController.DISTANCES_FROM_LIGHT, i));
			}
		}	
		
		if(velocityMOList==null||velocityMOList.size()==0)
		{
			for(int i=0;i<controlledIncomingLanes.size();i++)
			{
				velocityMOList.add(this.getInput(MemoryObjectTypesTrafficLightController.VELOCITIES, i));
			}
		}	
		
		if(vehicleNumberMOList==null||vehicleNumberMOList.size()==0)
		{
			for(int i=0;i<controlledIncomingLanes.size();i++)
			{
				vehicleNumberMOList.add(this.getInput(MemoryObjectTypesTrafficLightController.VEHICLE_NUMBER,i));
			}
		}

	}

	/* (non-Javadoc)
	 * @see br.unicamp.cogsys.core.entities.Codelet#calculateActivation()
	 */
	@Override
	public void calculateActivation() 
	{
		double activation=0.0d;	

		activationList = new ArrayList<Double>();;
		
		for(int i=0;i<controlledIncomingLanes.size();i++)
		{
			if(vehicleNumberMOList.get(i)!=null&&vehicleNumberMOList.get(i).getI()!=null&&!vehicleNumberMOList.get(i).getI().equals("")&&!vehicleNumberMOList.get(i).getI().equals("null"))
			{
				Integer vehicleNumber = Integer.valueOf((String) vehicleNumberMOList.get(i).getI());
				
				if(vehicleNumber>0)
				{
					double laneActivation=0.0d;	

					String[] velocidades = null;
					String[] distancias = null;
					if(velocityMOList.get(i)!=null&&velocityMOList.get(i).getI()!=null&&!velocityMOList.get(i).getI().equals("")&&!velocityMOList.get(i).getI().equals("null"))
					{						
						velocidades = ((String) velocityMOList.get(i).getI()).split("[,]");						
					}
					if(distanceMOList.get(i)!=null&&distanceMOList.get(i).getI()!=null&&!distanceMOList.get(i).getI().equals("")&&!distanceMOList.get(i).getI().equals("null"))
					{						
						distancias = ((String)distanceMOList.get(i).getI()).split("[,]");	
					}
					if(velocidades!=null&&distancias!=null)
					{
						for(int j=0;j<velocidades.length&&j<distancias.length;j++)
						{							
							Double at = 1 - alpha*Double.valueOf(velocidades[j]) - beta*Double.valueOf(distancias[j]);							
							laneActivation += at;
						}												
					}			

					activationList.add(laneActivation);
					activation+= laneActivation;

				}else
				{

					activationList.add(0.0d);
				}				
			}else
			{

				activationList.add(0.0d);
			}
			
		}
		
		activation /= controlledIncomingLanes.size();
		
		if(activation>0)
			activation = 1 - 1/activation;
		

		try 
		{
			if(activation<0.0d)
				activation=0.0d;
			if(activation>1.0d)
				activation=1.0d;
			this.setActivation(activation);
		} catch (CodeletActivationBoundsException e) 
		{			
			e.printStackTrace();
		}

	}

	/**
	 * @return the tLStates
	 */
	public synchronized ArrayList<TLState> getTLStates() 	
	{
		return TLStates;
	}

	/**
	 * @param tLStates the tLStates to set
	 */
	public synchronized void setTLStates(ArrayList<TLState> tLStates) 
	{
		TLStates = tLStates;
	}

	/**
	 * @return the mapAllLanes
	 */
	public synchronized Map<String, Lane> getMapAllLanes()
	{
		return mapAllLanes;
	}

	/**
	 * @param mapAllLanes the mapAllLanes to set
	 */
	public synchronized void setMapAllLanes(Map<String, Lane> mapAllLanes) 
	{
		this.mapAllLanes = mapAllLanes;
	}

	/**
	 * @return the controlledIncomingLanes
	 */
	public synchronized ArrayList<Lane> getControlledIncomingLanes() 
	{
		return controlledIncomingLanes;
	}

	/**
	 * @param controlledIncomingLanes the controlledIncomingLanes to set
	 */
	public synchronized void setControlledIncomingLanes(ArrayList<Lane> controlledIncomingLanes) 
	{
		this.controlledIncomingLanes = controlledIncomingLanes;
	}

	/**
	 * @return the controlledOutgoingLanes
	 */
	public synchronized ArrayList<Lane> getControlledOutgoingLanes() 
	{
		return controlledOutgoingLanes;
	}

	/**
	 * @param controlledOutgoingLanes the controlledOutgoingLanes to set
	 */
	public synchronized void setControlledOutgoingLanes(ArrayList<Lane> controlledOutgoingLanes) 
	{
		this.controlledOutgoingLanes = controlledOutgoingLanes;
	}
}
