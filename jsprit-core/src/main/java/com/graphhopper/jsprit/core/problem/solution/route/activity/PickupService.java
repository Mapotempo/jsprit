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
package com.graphhopper.jsprit.core.problem.solution.route.activity;

import com.graphhopper.jsprit.core.problem.AbstractActivity;
import com.graphhopper.jsprit.core.problem.Capacity;
import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.job.Pickup;
import com.graphhopper.jsprit.core.problem.job.Service;

public final class PickupService extends AbstractActivity implements PickupActivity {

    private Service pickup;

    private double arrTime;

    private double depTime;

    private double theoreticalEarliest = 0;

    private double theoreticalLatest = Double.MAX_VALUE;

    private double softEarliest = 0.;

    private double softLatest = Double.MAX_VALUE;

    public PickupService(Pickup pickup) {
        super();
        this.pickup = pickup;
    }

    public PickupService(Service service) {
        this.pickup = service;
    }

    private PickupService(PickupService pickupActivity) {
        this.pickup = pickupActivity.getJob();
        this.arrTime = pickupActivity.getArrTime();
        this.depTime = pickupActivity.getEndTime();
        setIndex(pickupActivity.getIndex());
        this.theoreticalEarliest = pickupActivity.getTheoreticalEarliestOperationStartTime();
        this.theoreticalLatest = pickupActivity.getTheoreticalLatestOperationStartTime();
        this.softEarliest = pickupActivity.getSoftLowerBoundOperationStartTime();
        this.softLatest = pickupActivity.getSoftUpperBoundOperationStartTime();
    }

    @Override
    public String getName() {
        return pickup.getType();
    }

    @Override
    public Location getLocation() {
        return pickup.getLocation();
    }

    @Override
    public double getTheoreticalEarliestOperationStartTime() {
        return theoreticalEarliest;
    }

    @Override
    public double getTheoreticalLatestOperationStartTime() {
        return theoreticalLatest;
    }

    @Override
    public void setTheoreticalEarliestOperationStartTime(double earliest) {
        theoreticalEarliest = earliest;
        if(this.softEarliest < earliest)
            this.softEarliest = earliest;
    }

    @Override
    public void setTheoreticalLatestOperationStartTime(double latest) {
        theoreticalLatest = latest;
        if(this.softLatest > latest)
            this.softLatest = latest;
    }

    public void setSoftEarliestoperationStartTime(double earliest) {
        this.softEarliest = earliest;
        if(this.theoreticalEarliest > earliest)
            this.theoreticalEarliest = earliest;
    }

    public void setSoftLatestOperationStartTime(double latest) {
        this.softLatest = latest;
        if(this.theoreticalLatest < latest)
            this.theoreticalLatest = latest;
    }

    @Override
    public double getOperationTime() {
        return pickup.getServiceDuration();
    }

    @Override
    public double getArrTime() {
        return arrTime;
    }

    @Override
    public double getEndTime() {
        return depTime;
    }

    @Override
    public void setArrTime(double arrTime) {
        this.arrTime = arrTime;
    }

    @Override
    public void setEndTime(double endTime) {
        this.depTime = endTime;
    }

    @Override
    public TourActivity duplicate() {
        return new PickupService(this);
    }

    @Override
    public Service getJob() {
        return pickup;
    }

    public String toString() {
        return "[type=" + getName() + "][locationId=" + getLocation().getId()
            + "][size=" + getSize().toString()
            + "][twStart=" + Activities.round(getTheoreticalEarliestOperationStartTime())
            + "][twEnd=" + Activities.round(getTheoreticalLatestOperationStartTime()) + "]";
    }

    @Override
    public Capacity getSize() {
        return pickup.getSize();
    }

	@Override
	public double getSoftLowerBoundOperationStartTime() {
        return softEarliest;
	}

	@Override
	public double getSoftUpperBoundOperationStartTime() {
        return softLatest;
	}
}
