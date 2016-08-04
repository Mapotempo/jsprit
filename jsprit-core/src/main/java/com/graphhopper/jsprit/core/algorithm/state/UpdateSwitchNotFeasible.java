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
package com.graphhopper.jsprit.core.algorithm.state;

import com.graphhopper.jsprit.core.problem.Capacity;
import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.cost.SetupTime;
import com.graphhopper.jsprit.core.problem.cost.VehicleRoutingActivityCosts;
import com.graphhopper.jsprit.core.problem.cost.VehicleRoutingTransportCosts;
import com.graphhopper.jsprit.core.problem.solution.route.RouteVisitor;
import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivity;
import com.graphhopper.jsprit.core.problem.vehicle.Vehicle;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

public class UpdateSwitchNotFeasible implements RouteVisitor, StateUpdater{

    @Override
    public void visit(VehicleRoute route) {
        begin(route);
        Iterator<TourActivity> revIterator = route.getTourActivities().reverseActivityIterator();
        while (revIterator.hasNext()) {
            visit(revIterator.next());
        }
        finish();
    }

    public static interface VehiclesToUpdate {

        public Collection<Vehicle> get(VehicleRoute route);

    }

    private VehiclesToUpdate vehiclesToUpdate = new VehiclesToUpdate() {

        @Override
        public Collection<Vehicle> get(VehicleRoute route) {
            return Arrays.asList(route.getVehicle());
        }

    };

    private final StateManager stateManager;

    private final VehicleRoutingTransportCosts transportCosts;

    private final VehicleRoutingActivityCosts activityCosts;

    private SetupTime setupCosts = new SetupTime();

    private VehicleRoute route;

    private double[] latest_ReadyTimes_at_prevAct;

    private Location[] location_of_prevAct;
    
    private double[] setup_time_of_prevAct;

    private Collection<Vehicle> vehicles;
    
    public UpdateSwitchNotFeasible(StateManager stateManager, VehicleRoutingTransportCosts tpCosts, VehicleRoutingActivityCosts activityCosts) {
        super();
        this.stateManager = stateManager;
        this.transportCosts = tpCosts;
        this.activityCosts = activityCosts;
        latest_ReadyTimes_at_prevAct = new double[stateManager.getMaxIndexOfVehicleTypeIdentifiers() + 1];
        location_of_prevAct = new Location[stateManager.getMaxIndexOfVehicleTypeIdentifiers() + 1];
        setup_time_of_prevAct = new double[stateManager.getMaxIndexOfVehicleTypeIdentifiers() + 1];
    }

    public void setVehiclesToUpdate(VehiclesToUpdate vehiclesToUpdate) {
        this.vehiclesToUpdate = vehiclesToUpdate;
    }


    public void begin(VehicleRoute route) {
        this.route = route;
        vehicles = vehiclesToUpdate.get(route);
        for (Vehicle vehicle : vehicles) {
            latest_ReadyTimes_at_prevAct[vehicle.getVehicleTypeIdentifier().getIndex()] = vehicle.getLatestArrival();
            Location location = vehicle.getEndLocation();
            if(!vehicle.isReturnToDepot()){
                location = route.getEnd().getLocation();
            }
            if (vehicle.getInitialCapacity() != null && stateManager.getRouteState(route, InternalStates.LOAD_AT_BEGINNING, Capacity.class) != null
                    && !stateManager.getRouteState(route, InternalStates.LOAD_AT_BEGINNING, Capacity.class).isLessOrEqual(vehicle.getInitialCapacity()))
                stateManager.putTypedInternalRouteState(route, vehicle, InternalStates.SWITCH_NOT_FEASIBLE, true);
            else if (stateManager.getRouteState(route, InternalStates.MAXLOAD, Capacity.class)!= null && vehicle.getType().getCapacityDimensions() != null && !stateManager.getRouteState(route, InternalStates.MAXLOAD, Capacity.class).isLessOrEqual(vehicle.getType().getCapacityDimensions()))
                stateManager.putTypedInternalRouteState(route, vehicle, InternalStates.SWITCH_NOT_FEASIBLE, true);
            else
                stateManager.putTypedInternalRouteState(route, vehicle, InternalStates.SWITCH_NOT_FEASIBLE, false);
            location_of_prevAct[vehicle.getVehicleTypeIdentifier().getIndex()] = location;
            setup_time_of_prevAct[vehicle.getVehicleTypeIdentifier().getIndex()] = 0.0;
        }
    }


    public void visit(TourActivity activity) {
        for (Vehicle vehicle : vehicles) {
            double latestReadyTimeAtPrevAct = latest_ReadyTimes_at_prevAct[vehicle.getVehicleTypeIdentifier().getIndex()];
            Location prevLocation = location_of_prevAct[vehicle.getVehicleTypeIdentifier().getIndex()];
            double setup_time_activity_prevLocation = setupCosts.getSetupTime(activity.getLocation(), prevLocation, setup_time_of_prevAct[vehicle.getVehicleTypeIdentifier().getIndex()], route.getVehicle());
            double latestArrTimeAtPrevAct = latestReadyTimeAtPrevAct - setup_time_activity_prevLocation;
            double transport_time_activity_prevLocation = transportCosts.getBackwardTransportTime(activity.getLocation(), prevLocation,
                    latestArrTimeAtPrevAct, route.getDriver(), vehicle);
            double potentialLatestReadyTimeAtCurrAct = latestArrTimeAtPrevAct - transport_time_activity_prevLocation - activityCosts.getActivityDuration(activity, latestReadyTimeAtPrevAct, route.getDriver(), route.getVehicle());
            double latestReadyTime = Math.min(activity.getTheoreticalLatestOperationStartTime(), potentialLatestReadyTimeAtCurrAct);
            if (latestReadyTime < activity.getTheoreticalEarliestOperationStartTime()) {
                stateManager.putTypedInternalRouteState(route, vehicle, InternalStates.SWITCH_NOT_FEASIBLE, true);
            }
            latest_ReadyTimes_at_prevAct[vehicle.getVehicleTypeIdentifier().getIndex()] = latestReadyTime;
            location_of_prevAct[vehicle.getVehicleTypeIdentifier().getIndex()] = activity.getLocation();
            setup_time_of_prevAct[vehicle.getVehicleTypeIdentifier().getIndex()] = activity.getSetupTime();
        }
    }


    public void finish() {
    }


}
