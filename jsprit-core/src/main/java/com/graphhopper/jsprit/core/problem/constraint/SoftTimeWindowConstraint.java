package com.graphhopper.jsprit.core.problem.constraint;

import com.graphhopper.jsprit.core.algorithm.state.InternalStates;
import com.graphhopper.jsprit.core.algorithm.state.StateManager;
import com.graphhopper.jsprit.core.problem.cost.VehicleRoutingTransportCosts;
import com.graphhopper.jsprit.core.problem.misc.JobInsertionContext;
import com.graphhopper.jsprit.core.problem.solution.route.activity.End;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivity;
import com.graphhopper.jsprit.core.problem.vehicle.Vehicle;

public class SoftTimeWindowConstraint implements SoftActivityConstraint{
    
    private StateManager states;

    private final VehicleRoutingTransportCosts routingCosts;

    public SoftTimeWindowConstraint(StateManager states, VehicleRoutingTransportCosts routingCosts) {
        this.states = states;
        this.routingCosts = routingCosts;
    }
	
	public double getSoftTimeWindowCost(TourActivity act, double arrTime, Vehicle vehicle){
        double act_OperationStart = Math.max(arrTime, act.getTheoreticalEarliestOperationStartTime());
        double cost = 0.;
        if(act_OperationStart < act.getSoftLowerBoundOperationStartTime())
            cost += (act.getSoftLowerBoundOperationStartTime() - act_OperationStart)*vehicle.getType().getVehicleCostParams().perLowerLatenessTimeUnit;
        if(act_OperationStart > act.getSoftUpperBoundOperationStartTime())
            cost += (act_OperationStart - act.getSoftUpperBoundOperationStartTime())*vehicle.getType().getVehicleCostParams().perUpperLatenessTimeUnit;
        return cost;
	}
	
    @Override
    public double getCosts(JobInsertionContext iFacts, TourActivity prevAct, TourActivity newAct, TourActivity nextAct,
            double prevActDepTime) {
        double cost = 0.;

        double newActArrTime = prevActDepTime + routingCosts.getTransportTime(prevAct.getLocation(), newAct.getLocation(), prevActDepTime, iFacts.getNewDriver(), iFacts.getNewVehicle());
        double newActEndTime = Math.max(newActArrTime, newAct.getTheoreticalEarliestOperationStartTime()) + newAct.getOperationTime();
        cost += getSoftTimeWindowCost(newAct, newActArrTime, iFacts.getNewVehicle());
        double nextActArrTime = newActEndTime + routingCosts.getTransportTime(newAct.getLocation(), nextAct.getLocation(), newActEndTime, iFacts.getNewDriver(), iFacts.getNewVehicle());
        if(!(nextAct instanceof End))
            cost += (nextActArrTime - prevActDepTime) * states.getActivityState(nextAct, InternalStates.NEXT_LATE, Integer.class);
        return cost;
	}
}
