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
import com.graphhopper.jsprit.core.problem.job.Delivery;

public final class DeliverService extends AbstractActivity implements DeliveryActivity {

    private Delivery delivery;

    private Capacity capacity;

    private double arrTime;

    private double endTime;

    private double theoreticalEarliest = 0;

    private double theoreticalLatest = Double.MAX_VALUE;
    
    private double softEarliest = 0.;
    
    private double softLatest = Double.MAX_VALUE;

    private double setup = 0.0;

    public DeliverService(Delivery delivery) {
        super();
        this.delivery = delivery;
        capacity = Capacity.invert(delivery.getSize());
        this.setup = delivery.getSetupDuration();
    }

    private DeliverService(DeliverService deliveryActivity) {
        this.delivery = deliveryActivity.getJob();
        this.arrTime = deliveryActivity.getArrTime();
        this.endTime = deliveryActivity.getEndTime();
        capacity = deliveryActivity.getSize();
        setIndex(deliveryActivity.getIndex());
        this.theoreticalEarliest = deliveryActivity.getTheoreticalEarliestOperationStartTime();
        this.theoreticalLatest = deliveryActivity.getTheoreticalLatestOperationStartTime();
        this.softEarliest = deliveryActivity.getSoftLowerBoundOperationStartTime();
        this.softLatest = deliveryActivity.getSoftUpperBoundOperationStartTime();
        this.setup = deliveryActivity.getSetupTime();
    }

    @Override
    public String getName() {
        return delivery.getType();
    }

    @Override
    public Location getLocation() {
        return delivery.getLocation();
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
    public double getTheoreticalEarliestOperationStartTime() {
        return theoreticalEarliest;
    }

    @Override
    public double getTheoreticalLatestOperationStartTime() {
        return theoreticalLatest;
    }

    @Override
    public double getOperationTime() {
        return delivery.getServiceDuration();
    }

    @Override
    public double getArrTime() {
        return arrTime;
    }

    @Override
    public double getEndTime() {
        return endTime;
    }

    @Override
    public void setArrTime(double arrTime) {
        this.arrTime = arrTime;
    }

    @Override
    public void setEndTime(double endTime) {
        this.endTime = endTime;
    }

    @Override
    public TourActivity duplicate() {
        return new DeliverService(this);
    }

    @Override
    public Delivery getJob() {
        return delivery;
    }

    public String toString() {
        return "[type=" + getName() + "][locationId=" + getLocation().getId()
            + "][size=" + getSize().toString()
            + "][twStart=" + Activities.round(getTheoreticalEarliestOperationStartTime())
            + "][twEnd=" + Activities.round(getTheoreticalLatestOperationStartTime())
            + "][Setup=" + Activities.round(getSetupTime()) + "]";
    }

    @Override
    public Capacity getSize() {
        return capacity;
    }

	@Override
	public double getSoftLowerBoundOperationStartTime() {
        return softEarliest;
	}

	@Override
	public double getSoftUpperBoundOperationStartTime() {
        return softLatest;
	}

    @Override
	public void setSetupTime(double setup) {
		this.setup = setup;
	}

	@Override
	public double getSetupTime() {
		return setup;
	}
}
