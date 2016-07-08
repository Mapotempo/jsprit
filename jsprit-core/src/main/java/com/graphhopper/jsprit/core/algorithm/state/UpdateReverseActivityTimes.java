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
package com.graphhopper.jsprit.core.algorithm.state;

import com.graphhopper.jsprit.core.problem.cost.TransportTime;
import com.graphhopper.jsprit.core.problem.cost.VehicleRoutingActivityCosts;
import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import com.graphhopper.jsprit.core.problem.solution.route.activity.ReverseActivityVisitor;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivity;
import com.graphhopper.jsprit.core.util.ReverseActivityTimeTracker;
import com.graphhopper.jsprit.core.util.ReverseActivityTimeTracker.ReverseActivityPolicy;

public class UpdateReverseActivityTimes implements ReverseActivityVisitor, StateUpdater {

    private ReverseActivityTimeTracker timeTracker;

    private VehicleRoute route;

    public UpdateReverseActivityTimes(TransportTime transportTime, VehicleRoutingActivityCosts activityCosts) {
        super();
        timeTracker = new ReverseActivityTimeTracker(transportTime,activityCosts);
    }

    public UpdateReverseActivityTimes(TransportTime transportTime, ReverseActivityPolicy activityPolicy, VehicleRoutingActivityCosts activityCosts) {
        timeTracker = new ReverseActivityTimeTracker(transportTime, activityPolicy, activityCosts);
    }

    @Override
    public void begin(VehicleRoute route) {
        timeTracker.begin(route);
        this.route = route;
    }

    @Override
    public void visit(TourActivity activity) {
        timeTracker.visit(activity);
    }

    @Override
    public void finish() {
        timeTracker.finish();
        route.getStart().setEndTime(timeTracker.getActEndTime());
    }

}
