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
package com.graphhopper.jsprit.core.util;

import com.graphhopper.jsprit.core.problem.cost.ForwardTransportTime;
import com.graphhopper.jsprit.core.problem.cost.SetupTime;
import com.graphhopper.jsprit.core.problem.cost.VehicleRoutingActivityCosts;
import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import com.graphhopper.jsprit.core.problem.solution.route.activity.ActivityVisitor;
import com.graphhopper.jsprit.core.problem.solution.route.activity.Start;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivity;

public class ActivityTimeTracker implements ActivityVisitor {

    public static enum ActivityPolicy {

        AS_SOON_AS_TIME_WINDOW_OPENS, AS_SOON_AS_ARRIVED

    }

    private final ForwardTransportTime transportTime;

    private final VehicleRoutingActivityCosts activityCosts;

    private SetupTime setupCosts = new SetupTime();

    private TourActivity prevAct = null;

    private double startAtPrevAct;

    private VehicleRoute route;

    private boolean beginFirst = false;

    private double actArrTime;

    private double actReadyTime;

    private double actEndTime;

    private ActivityPolicy activityPolicy = ActivityPolicy.AS_SOON_AS_TIME_WINDOW_OPENS;

    public ActivityTimeTracker(ForwardTransportTime transportTime, VehicleRoutingActivityCosts activityCosts) {
        super();
        this.transportTime = transportTime;
        this.activityCosts = activityCosts;
    }

    public ActivityTimeTracker(ForwardTransportTime transportTime, ActivityPolicy activityPolicy, VehicleRoutingActivityCosts activityCosts) {
        super();
        this.transportTime = transportTime;
        this.activityPolicy = activityPolicy;
        this.activityCosts = activityCosts;
    }

    public double getActArrTime() {
        return actArrTime;
    }

    public double getActReadyTime() {
        return actReadyTime;
    }

    public double getActEndTime() {
        return actEndTime;
    }

    @Override
    public void begin(VehicleRoute route) {
        prevAct = route.getStart();
        startAtPrevAct = prevAct.getEndTime();
        actReadyTime = startAtPrevAct;
        actEndTime = startAtPrevAct;
        this.route = route;
        beginFirst = true;
    }

    @Override
    public void visit(TourActivity activity) {
        if (!beginFirst) throw new IllegalStateException("never called begin. this however is essential here");
        double setup_time_prevAct_activity = setupCosts.getSetupTime(prevAct, activity, route.getVehicle());
        
        double transportTime = this.transportTime.getTransportTime(prevAct.getLocation(), activity.getLocation(), startAtPrevAct, route.getDriver(), route.getVehicle());
    
        double arrivalTimeAtCurrAct = startAtPrevAct + transportTime;
        actArrTime = arrivalTimeAtCurrAct;
        actReadyTime = arrivalTimeAtCurrAct + setup_time_prevAct_activity;
        double operationStartTime;

        if (activityPolicy.equals(ActivityPolicy.AS_SOON_AS_TIME_WINDOW_OPENS)) {
            operationStartTime = Math.max(activity.getTheoreticalEarliestOperationStartTime(), actReadyTime);
        } else if (activityPolicy.equals(ActivityPolicy.AS_SOON_AS_ARRIVED)) {
            operationStartTime = actReadyTime;
        } else operationStartTime = actReadyTime;

        double operationEndTime = operationStartTime + activityCosts.getActivityDuration(activity,actReadyTime,route.getDriver(),route.getVehicle());

        actEndTime = operationEndTime;

        prevAct = activity;
        startAtPrevAct = operationEndTime;

    }

    @Override
    public void finish() {
        double transportTime = this.transportTime.getTransportTime(prevAct.getLocation(), route.getEnd().getLocation(), startAtPrevAct, route.getDriver(), route.getVehicle());
        double arrivalTimeAtCurrAct = startAtPrevAct + transportTime;

        actArrTime = arrivalTimeAtCurrAct;
        actReadyTime = arrivalTimeAtCurrAct;
        actEndTime = arrivalTimeAtCurrAct;

        beginFirst = false;
    }


}
