/**
 * 
 */
package br.unicamp.cst.trafficUnjammer.codeRack.motorCodelets;

import it.polito.appeal.traci.ChangeLightsStateQuery;
import it.polito.appeal.traci.TLState;
import it.polito.appeal.traci.TrafficLight;

import java.io.IOException;
import java.util.List;

import br.unicamp.cst.core.entities.Codelet;
import br.unicamp.cst.core.entities.MemoryObject;
import br.unicamp.cst.core.exceptions.CodeletActivationBoundsException;
import br.unicamp.cst.trafficUnjammer.rawMemory.MemoryObjectTypesTrafficLightController;

/**
 * @author andre
 *
 */
public class TrafficLightActuator extends Codelet 
{	
	private TrafficLight trafficLight;	
	private List<TLState> trafficLightPhases;
	
	private MemoryObject phaseMO;
	private MemoryObject forcedPhaseMO;

	public TrafficLightActuator(TrafficLight trafficLight, List<TLState> TLStates) 
	{
		this.trafficLight = trafficLight;
		this.trafficLightPhases = TLStates;
	}

	/* (non-Javadoc)
	 * @see br.unicamp.cogsys.core.entities.Codelet#accessMemoryObjects()
	 */
	@Override
	public void accessMemoryObjects() 
	{
		int index=0;
		
		if(phaseMO==null)
			phaseMO = this.getInput(MemoryObjectTypesTrafficLightController.PHASE, index);
		
		if(forcedPhaseMO==null)
			forcedPhaseMO = this.getInput(MemoryObjectTypesTrafficLightController.FORCED_PHASE, index);

	}

	/* (non-Javadoc)
	 * @see br.unicamp.cogsys.core.entities.Codelet#calculateActivation()
	 */
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

	/* (non-Javadoc)
	 * @see br.unicamp.cogsys.core.entities.Codelet#proc()
	 */
	@Override
	public void proc() 
	{
		if(forcedPhaseMO!=null && forcedPhaseMO.getI()!=null && !( (String) forcedPhaseMO.getI()).equalsIgnoreCase("-1"))
		{
			try
			{
				TLState forcedPhase = new TLState((String) forcedPhaseMO.getI());
				
				ChangeLightsStateQuery lstQ  = trafficLight.queryChangeLightsState();
				lstQ.setValue(forcedPhase);			
				try 
				{
					lstQ.run();
				} catch (IOException e) 
				{
					e.printStackTrace();
				}
			}catch(Exception e)
			{
				e.printStackTrace();
			}			
		}else
		{
			int phaseIndex = -1;
			if(phaseMO.getI()!=null)
			{
				try
				{
					phaseIndex = Integer.valueOf((String) phaseMO.getI());
				}catch(Exception e)
				{
					e.printStackTrace();
				}
			}
								
			if(phaseIndex>=0&&trafficLight!=null&&trafficLightPhases!=null&&trafficLightPhases.size()>phaseIndex)
			{
				ChangeLightsStateQuery lstQ  = trafficLight.queryChangeLightsState();
				lstQ.setValue(trafficLightPhases.get(phaseIndex));			
				try 
				{
					lstQ.run();
				} catch (IOException e) 
				{
					e.printStackTrace();
				}

			}
		}		
	}
}
