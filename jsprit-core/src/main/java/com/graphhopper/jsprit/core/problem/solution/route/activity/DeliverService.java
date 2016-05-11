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
package com.graphhopper.jsprit.core.problem.solution.route.activity;

import com.graphhopper.jsprit.core.problem.AbstractActivity;
import com.graphhopper.jsprit.core.problem.Capacity;
import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.job.Delivery;

public final class DeliverService extends AbstractActivity implements DeliveryActivity {

    private Delivery delivery;

    private Capacity capacity;

    private double arrTime;

    public double readyTime;

    private double endTime;

    private double theoreticalEarliest = 0;

    private double theoreticalLatest = Double.MAX_VALUE;

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
        this.readyTime = deliveryActivity.getReadyTime();
        this.endTime = deliveryActivity.getEndTime();
        capacity = deliveryActivity.getSize();
        setIndex(deliveryActivity.getIndex());
        this.theoreticalEarliest = deliveryActivity.getTheoreticalEarliestOperationStartTime();
        this.theoreticalLatest = deliveryActivity.getTheoreticalLatestOperationStartTime();
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
    }

    @Override
    public void setTheoreticalLatestOperationStartTime(double latest) {
        theoreticalLatest = latest;
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
	public void setSetupTime(double setup) {
		this.setup = setup;
	}

	@Override
	public double getSetupTime() {
		return setup;
	}

    @Override
    public double getReadyTime() {
        return readyTime;
    }

    @Override
    public void setReadyTime(double readyTime) {
        this.readyTime = readyTime;
    }
}
