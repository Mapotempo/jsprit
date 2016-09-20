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

import com.graphhopper.jsprit.core.problem.cost.ForwardTransportTime;
import com.graphhopper.jsprit.core.problem.cost.VehicleRoutingActivityCosts;
import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import com.graphhopper.jsprit.core.problem.solution.route.activity.ActivityVisitor;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivity;
import com.graphhopper.jsprit.core.util.ActivityTimeTracker;


/**
 * Updates arrival and end times of activities.
 * <p>
 * <p>Note that this modifies arrTime and endTime of each activity in a route.
 *
 * @author stefan
 */
public class UpdateLateness implements ActivityVisitor, StateUpdater {

    private ActivityTimeTracker timeTracker;

    private VehicleRoute route;

    private StateManager stateManager;

    private double lateness;

    /**
     * Updates arrival and end times of activities.
     * <p>
     * <p>Note that this modifies arrTime and endTime of each activity in a route.
     * <p>
     * <p>ArrTimes and EndTimes can be retrieved by <br>
     * <code>activity.getArrTime()</code> and
     * <code>activity.getEndTime()</code>
     */
    public UpdateLateness(ForwardTransportTime transportTime, VehicleRoutingActivityCosts activityCosts, StateManager stateManager) {
        super();
        timeTracker = new ActivityTimeTracker(transportTime,activityCosts);
        this.stateManager = stateManager;
    }

    public UpdateLateness(ForwardTransportTime transportTime, ActivityTimeTracker.ActivityPolicy activityPolicy, VehicleRoutingActivityCosts activityCosts, StateManager stateManager) {
        timeTracker = new ActivityTimeTracker(transportTime, activityPolicy, activityCosts);
        this.stateManager = stateManager;
    }

    @Override
    public void begin(VehicleRoute route) {
        timeTracker.begin(route);
        this.route = route;
        lateness = 0.;
    }

    @Override
    public void visit(TourActivity activity) {
        timeTracker.visit(activity);
        if(activity.getHasExtendedTimeWindow())
            lateness++;
    }

    @Override
    public void finish() {
        timeTracker.finish();
        stateManager.putTypedInternalRouteState(route, InternalStates.LATENESS, lateness);
    }

}
