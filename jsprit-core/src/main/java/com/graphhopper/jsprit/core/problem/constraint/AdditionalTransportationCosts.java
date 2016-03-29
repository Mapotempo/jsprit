/*
 * Licensed to GraphHopper GmbH under one or more contributor
 * license agreements. See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 *
 * GraphHopper GmbH licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.graphhopper.jsprit.core.problem.constraint;

import com.graphhopper.jsprit.core.problem.cost.SoftTimeWindowCost;
import com.graphhopper.jsprit.core.problem.cost.VehicleRoutingActivityCosts;
import com.graphhopper.jsprit.core.problem.cost.VehicleRoutingTransportCosts;
import com.graphhopper.jsprit.core.problem.misc.JobInsertionContext;
import com.graphhopper.jsprit.core.problem.solution.route.activity.End;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivity;

/**
 * Calculates additional transportation costs induced by inserting newAct.
 *
 * @author schroeder
 */
class AdditionalTransportationCosts implements SoftActivityConstraint {

    private VehicleRoutingTransportCosts routingCosts;

    private VehicleRoutingActivityCosts activityCosts;

    private SoftTimeWindowCost softCosts;

    /**
     * Constructs the calculator that calculates additional transportation costs induced by inserting new activity.
     * <p>
     * <p>It is calculated at local level, i.e. the additional costs of inserting act_new between act_i and act_j is c(act_i,act_new,newVehicle)+c(act_new,act_j,newVehicle)-c(act_i,act_j,oldVehicle)
     * <p>If newVehicle.isReturnToDepot == false then the additional costs of inserting act_new between act_i and end is c(act_i,act_new) [since act_new is then the new end-of-route]
     *
     * @param routingCosts
     * @param activityCosts
     */
    public AdditionalTransportationCosts(VehicleRoutingTransportCosts routingCosts, SoftTimeWindowCost softCosts, VehicleRoutingActivityCosts activityCosts) {
        super();
        this.routingCosts = routingCosts;
        this.activityCosts = activityCosts;
        this.softCosts = softCosts;
    }

    /**
     * Returns additional transportation costs induced by inserting newAct.
     * <p>
     * <p>It is calculated at local level, i.e. the additional costs of inserting act_new between act_i and act_j is c(act_i,act_new,newVehicle)+c(act_new,act_j,newVehicle)-c(act_i,act_j,oldVehicle)
     * <p>If newVehicle.isReturnToDepot == false then the additional costs of inserting act_new between act_i and end is c(act_i,act_new) [since act_new is then the new end-of-route]
     */
    @Override
    public double getCosts(JobInsertionContext iFacts, TourActivity prevAct, TourActivity newAct, TourActivity nextAct, double depTimeAtPrevAct) {
        double coef = 1.0;
        if(iFacts.getNewVehicle() != null)
        	coef = iFacts.getNewVehicle().getCoefSetupTime();
    	
    	double setup_time_prevAct_newAct = 0.0;
    	if(!prevAct.getLocation().equals(newAct.getLocation()))
    		setup_time_prevAct_newAct = newAct.getSetupTime() * coef;
    	double setup_cost_prevAct_newAct = setup_time_prevAct_newAct * iFacts.getNewVehicle().getType().getVehicleCostParams().perTransportTimeUnit;
        double tp_costs_prevAct_newAct = setup_cost_prevAct_newAct + routingCosts.getTransportCost(prevAct.getLocation(), newAct.getLocation(), depTimeAtPrevAct, iFacts.getNewDriver(), iFacts.getNewVehicle());
        double tp_time_prevAct_newAct = setup_time_prevAct_newAct + routingCosts.getTransportTime(prevAct.getLocation(), newAct.getLocation(), depTimeAtPrevAct, iFacts.getNewDriver(), iFacts.getNewVehicle());

        double newAct_arrTime = depTimeAtPrevAct + tp_time_prevAct_newAct;
        double newAct_endTime = Math.max(newAct_arrTime, newAct.getTheoreticalEarliestOperationStartTime()) + activityCosts.getActivityDuration(newAct,newAct_arrTime,iFacts.getNewDriver(),iFacts.getNewVehicle());

        //open routes
        if (nextAct instanceof End) {
            if (!iFacts.getNewVehicle().isReturnToDepot()) {
                return tp_costs_prevAct_newAct;
            }
        }

        double tp_costs_newAct_nextAct = routingCosts.getTransportCost(newAct.getLocation(), nextAct.getLocation(), newAct_endTime, iFacts.getNewDriver(), iFacts.getNewVehicle());

        double setup_time_newAct_nextAct = 0.0;
        if(!newAct.getLocation().equals(nextAct.getLocation()))
            setup_time_newAct_nextAct = nextAct.getSetupTime() * coef;
        double setup_cost_newAct_nextAct = setup_time_newAct_nextAct * iFacts.getNewVehicle().getType().getVehicleCostParams().perTransportTimeUnit;

        double soft_cost_newAct = softCosts.getSoftTimeWindowCost(iFacts.getRoute(), prevAct, newAct, nextAct, depTimeAtPrevAct);
        double totalCosts = tp_costs_prevAct_newAct + tp_costs_newAct_nextAct + soft_cost_newAct + setup_cost_newAct_nextAct;

        double oldCosts;
        if (iFacts.getRoute().isEmpty()) {
            double tp_costs_prevAct_nextAct = routingCosts.getTransportCost(prevAct.getLocation(), nextAct.getLocation(), depTimeAtPrevAct, iFacts.getNewDriver(), iFacts.getNewVehicle());
            oldCosts = tp_costs_prevAct_nextAct;
        } else {
        	double setup_time_prevAct_nextAct = 0.0;
            if(!prevAct.getLocation().equals(nextAct.getLocation()))
            	setup_time_prevAct_nextAct = nextAct.getSetupTime() * coef;
            double setup_costs_prevAct_nextAct = setup_time_prevAct_nextAct * iFacts.getNewVehicle().getType().getVehicleCostParams().perTransportTimeUnit;
            double tp_costs_prevAct_nextAct = setup_costs_prevAct_nextAct + routingCosts.getTransportCost(prevAct.getLocation(), nextAct.getLocation(), prevAct.getEndTime(), iFacts.getRoute().getDriver(), iFacts.getRoute().getVehicle());
            oldCosts = tp_costs_prevAct_nextAct;
        }

        double additionalCosts = totalCosts - oldCosts;
        return additionalCosts;
    }

}
