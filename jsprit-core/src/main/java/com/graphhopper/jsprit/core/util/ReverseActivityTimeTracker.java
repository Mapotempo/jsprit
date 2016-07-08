/*******************************************************************************
 * Copyright (C) 2014  Stefan Schroeder
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.graphhopper.jsprit.core.util;

import com.graphhopper.jsprit.core.problem.cost.TransportTime;
import com.graphhopper.jsprit.core.problem.cost.VehicleRoutingActivityCosts;
import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import com.graphhopper.jsprit.core.problem.solution.route.activity.ReverseActivityVisitor;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivity;

public class ReverseActivityTimeTracker implements ReverseActivityVisitor {

    public static enum ReverseActivityPolicy {

        AS_SOON_AS_TIME_WINDOW_OPENS, AS_SOON_AS_ARRIVED

    }

    private final TransportTime transportTime;

    private final VehicleRoutingActivityCosts activityCosts;

    private TourActivity prevAct = null;

    private double startAtPrevAct;

    private VehicleRoute route;

    private boolean beginFirst = false;

    private double actArrTime;

    private double actEndTime;

    private ReverseActivityPolicy activityPolicy = ReverseActivityPolicy.AS_SOON_AS_TIME_WINDOW_OPENS;

    public ReverseActivityTimeTracker(TransportTime transportTime, VehicleRoutingActivityCosts activityCosts) {
        super();
        this.transportTime = transportTime;
        this.activityCosts = activityCosts;
    }

    public ReverseActivityTimeTracker(TransportTime transportTime, ReverseActivityPolicy activityPolicy, VehicleRoutingActivityCosts activityCosts) {
        super();
        this.transportTime = transportTime;
        this.activityPolicy = activityPolicy;
        this.activityCosts = activityCosts;
    }

    public double getActArrTime() {
        return actArrTime;
    }

    public double getActEndTime() {
        return actEndTime;
    }

    @Override
    public void begin(VehicleRoute route) {
        prevAct = route.getEnd();
        startAtPrevAct = prevAct.getArrTime();
        actArrTime = startAtPrevAct;
        this.route = route;
        beginFirst = true;
    }

    @Override
    public void visit(TourActivity activity) {
        if (!beginFirst) throw new IllegalStateException("never called begin. this however is essential here");
        double transportTime = this.transportTime.getBackwardTransportTime(prevAct.getLocation(), activity.getLocation(), startAtPrevAct, route.getDriver(), route.getVehicle());
        double arrivalTimeAtCurrAct = startAtPrevAct - transportTime;

        actEndTime = arrivalTimeAtCurrAct;
        double operationEndTime = arrivalTimeAtCurrAct;

        double operationStartTime = operationEndTime - activityCosts.getActivityDuration(activity,actEndTime,route.getDriver(),route.getVehicle());

        if (activityPolicy.equals(ReverseActivityPolicy.AS_SOON_AS_TIME_WINDOW_OPENS)) {
            actArrTime = Math.min(activity.getTheoreticalLatestOperationStartTime(), operationStartTime);
        } else if (activityPolicy.equals(ReverseActivityPolicy.AS_SOON_AS_ARRIVED)) {
            actArrTime = actEndTime;
        } else operationEndTime = actEndTime;

        prevAct = activity;
        startAtPrevAct = actArrTime;

    }

    @Override
    public void finish() {
        double transportTime = this.transportTime.getBackwardTransportTime(prevAct.getLocation(), route.getStart().getLocation(), startAtPrevAct, route.getDriver(), route.getVehicle());
        double arrivalTimeAtCurrAct = startAtPrevAct - transportTime;

        actArrTime = arrivalTimeAtCurrAct;
        actEndTime = arrivalTimeAtCurrAct;

        beginFirst = false;
    }


}
