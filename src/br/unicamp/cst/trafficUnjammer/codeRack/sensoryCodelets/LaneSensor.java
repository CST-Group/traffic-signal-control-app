/**
 * 
 */
package br.unicamp.cst.trafficUnjammer.codeRack.sensoryCodelets;

import it.polito.appeal.traci.Lane;
import it.polito.appeal.traci.ReadObjectVarQuery;
import it.polito.appeal.traci.Vehicle;

import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import br.unicamp.cst.core.entities.Codelet;
import br.unicamp.cst.core.entities.MemoryObject;
import br.unicamp.cst.core.exceptions.CodeletActivationBoundsException;
import br.unicamp.cst.trafficUnjammer.rawMemory.MemoryObjectTypesTrafficLightController;

/**
 * @author andre
 *
 */
public class LaneSensor extends Codelet 
{
	private Lane controlledLane;
	
	private MemoryObject vehiclesVelocitiesMO;
	
	private MemoryObject vehiclesDistancesFromLightMO;
	
	private MemoryObject vehiclesNumberMO;
	
	private ArrayList<Vehicle> vehicles;
	
	private Point2D lightPosition;
	
	
	public LaneSensor(Lane incomingLane) 
	{
		this.controlledLane = incomingLane;	
		
		if(controlledLane!=null)
		{			
			ReadObjectVarQuery<Path2D> qP = controlledLane.queryReadShape();
			if(qP!=null)
			{
				if(qP.hasValue())
					qP.setObsolete();
				try 
				{
					Path2D lanePath = qP.get();
					if(lanePath!=null)
						lightPosition = lanePath.getCurrentPoint();					
				} catch (IOException e) 
				{
					e.printStackTrace();
				}
			} 
		}
	}

	/* (non-Javadoc)
	 * @see br.unicamp.cogsys.core.entities.Codelet#accessMemoryObjects()
	 */
	@Override
	public void accessMemoryObjects() 
	{
		int index=0;
		if(vehiclesVelocitiesMO==null)
			vehiclesVelocitiesMO = this.getOutput(MemoryObjectTypesTrafficLightController.VELOCITIES, index);
		if(vehiclesDistancesFromLightMO==null)
			vehiclesDistancesFromLightMO = this.getOutput(MemoryObjectTypesTrafficLightController.DISTANCES_FROM_LIGHT, index);
		if(vehiclesNumberMO==null)
			vehiclesNumberMO = this.getOutput(MemoryObjectTypesTrafficLightController.VEHICLE_NUMBER, index);
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
		ReadObjectVarQuery<Integer> qrVN = controlledLane.queryReadLastStepVehicleNumber();
		if(qrVN.hasValue())
			qrVN.setObsolete();
		Integer numberVehicle = 0;

		try
		{
			numberVehicle = qrVN.get();
		}catch(IllegalStateException eill)
		{
			//do nothing, it just does not have any value on it, leave it as 0
		}catch (IOException e) 
		{			
			//do nothing, it just does not have any value on it, leave it as 0
		}

		vehiclesNumberMO.updateI(String.valueOf(numberVehicle));			

		if(numberVehicle>0)
		{				
			ReadObjectVarQuery<List<String>> qrVIDL =  controlledLane.queryReadLastStepVehicleIDList();
			if(qrVIDL.hasValue())
				qrVIDL.setObsolete();

			List<String> vehicleIdList = null;

			try
			{
				vehicleIdList = qrVIDL.get();
			}catch(IllegalStateException eill)
			{
				//do nothing, it just does not have any value on it, leave it as null
			}catch (IOException e) 
			{			
				//do nothing, it just does not have any value on it, leave it as null
			}

			if(vehicleIdList!=null&&vehicleIdList.size()>0)
			{
				List<Vehicle> listVehicle = getVehicleList(vehicleIdList);
				if(listVehicle!=null&&listVehicle.size()>0)
				{										

					StringBuffer sbV = new StringBuffer();
					StringBuffer sbX = new StringBuffer();
					for(Vehicle v : listVehicle)
					{
						
						ReadObjectVarQuery<Double> qrV = v.queryReadSpeed();
						if(qrV.hasValue())
							qrV.setObsolete();

						Double V = null;
						try
						{
							V = qrV.get();
						}catch(IllegalStateException eill)
						{
							synchronized (vehicles) 				
							{
								vehicles.remove(v);
							}
							break;
						}catch (IOException e) 
						{			
							synchronized (vehicles) 				
							{
								vehicles.remove(v);
							}
							break;
						}

						if(V!=null)
						{
							sbV.append(V);
							sbV.append(",");
						}

						ReadObjectVarQuery<Point2D> qrX = v.queryReadPosition();
						if(qrX.hasValue())
							qrX.setObsolete();

						Point2D X = null;
						try
						{
							X = qrX.get();
						}catch(IllegalStateException eill)
						{
							synchronized (vehicles) 				
							{
								vehicles.remove(v);
							}
							break;
						}catch (IOException e) 
						{			
							synchronized (vehicles) 				
							{
								vehicles.remove(v);
							}
							break;
						}

						if(X!=null)
						{
							sbX.append(X.distance(lightPosition));
							sbX.append(",");
						}						
					}
					
					if(sbV.length()>0)
						sbV.deleteCharAt(sbV.length()-1);
					if(sbX.length()>0)
						sbX.deleteCharAt(sbX.length()-1);

					vehiclesVelocitiesMO.updateI(sbV.toString());
					//System.out.println("V[]: "+sbV.toString());
					vehiclesDistancesFromLightMO.updateI(sbX.toString());
					//System.out.println("X[]: "+sbX.toString());				
				}
			}
		}

	}

	private synchronized List<Vehicle> getVehicleList(List<String> vehicleIdList) 
	{
		List<Vehicle> listVehicle = null; 
		
		if(vehicleIdList!=null&&vehicleIdList.size()>0&&vehicles!=null&&vehicles.size()>0)
		{
			listVehicle = new ArrayList<Vehicle>();
			
			for(String idVehicle : vehicleIdList)
			{
				synchronized (vehicles) 				
				{
					for(Vehicle vehicle : vehicles)
					{
						if(vehicle.getID().equalsIgnoreCase(idVehicle))
						{
							listVehicle.add(vehicle);
							break;
						}
							
					}
				}				
			}			
		}
		
		return listVehicle;
	}

	/**
	 * @param vehicles the vehicles to set
	 */
	public synchronized void setVehicles(Collection<Vehicle> vehicles) 
	{
			
			this.vehicles = new ArrayList<Vehicle>();
			
			synchronized (this.vehicles) 				
			{
				for(Vehicle vehicle: vehicles)
				{
					this.vehicles.add(vehicle);
				}
			}		
	}
}
