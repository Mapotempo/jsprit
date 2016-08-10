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

import com.graphhopper.jsprit.core.algorithm.state.InternalStates;
import com.graphhopper.jsprit.core.problem.Capacity;
import com.graphhopper.jsprit.core.problem.misc.JobInsertionContext;
import com.graphhopper.jsprit.core.problem.solution.route.activity.*;
import com.graphhopper.jsprit.core.problem.solution.route.state.RouteAndActivityStateGetter;


/**
 * Ensures load constraint for inserting ServiceActivity.
 * <p>
 * <p>When using this, you need to use<br>
 *
 * @author schroeder
 */
public class ServiceLoadActivityLevelConstraint implements HardActivityConstraint {

    private RouteAndActivityStateGetter stateManager;

    private Capacity defaultValue;

    public ServiceLoadActivityLevelConstraint(RouteAndActivityStateGetter stateManager) {
        super();
        this.stateManager = stateManager;
        defaultValue = Capacity.Builder.newInstance().build();
    }

    @Override
    public ConstraintsStatus fulfilled(JobInsertionContext iFacts, TourActivity prevAct, TourActivity newAct, TourActivity nextAct, double prevActDepTime) {
        Capacity futureMaxLoad;
        Capacity futureMinLoad;
        Capacity pastMaxLoad;
        Capacity pastMinLoad;
        Capacity currentLoad;
        Capacity cumulativeShipmentLoad;
        Capacity loadAtDepot = stateManager.getRouteState(iFacts.getRoute(), InternalStates.LOAD_AT_BEGINNING, Capacity.class);
        cumulativeShipmentLoad = defaultValue;
        if (loadAtDepot == null) {
            loadAtDepot = defaultValue;
            if(iFacts.getNewVehicle().getInitialCapacity() != null)
                loadAtDepot = iFacts.getNewVehicle().getInitialCapacity();
        }
        Capacity loadAtEnd = stateManager.getRouteState(iFacts.getRoute(), InternalStates.LOAD_AT_END, Capacity.class);
        if (loadAtEnd == null) {
            loadAtEnd = defaultValue;
            if(iFacts.getNewVehicle().getInitialCapacity() != null)
                loadAtEnd = iFacts.getNewVehicle().getInitialCapacity();
        }
        if (prevAct instanceof Start) {
            currentLoad = loadAtDepot;
            futureMaxLoad = stateManager.getRouteState(iFacts.getRoute(), InternalStates.MAXLOAD, Capacity.class);
            if (futureMaxLoad == null) {
                futureMaxLoad = loadAtDepot;
            }
            futureMinLoad = stateManager.getRouteState(iFacts.getRoute(), InternalStates.MINLOAD, Capacity.class);
            if (futureMinLoad == null) {
                futureMinLoad = loadAtDepot;
            }
            pastMaxLoad = loadAtDepot;
            pastMinLoad = loadAtDepot;
        } else {
            currentLoad = stateManager.getActivityState(prevAct, InternalStates.LOAD, Capacity.class);
            cumulativeShipmentLoad = stateManager.getActivityState(prevAct, InternalStates.CUMULATIVE_SHIPMENT_LOAD, Capacity.class);
            futureMaxLoad = stateManager.getActivityState(prevAct, InternalStates.FUTURE_MAXLOAD, Capacity.class);
            futureMinLoad = stateManager.getActivityState(prevAct, InternalStates.FUTURE_MINLOAD, Capacity.class);
            pastMinLoad = stateManager.getActivityState(prevAct, InternalStates.PAST_MINLOAD, Capacity.class);
            pastMaxLoad = stateManager.getActivityState(prevAct, InternalStates.PAST_MAXLOAD, Capacity.class);
        }
        if (newAct instanceof PickupService || newAct instanceof ServiceActivity) {
            if (!Capacity.addup(futureMaxLoad, newAct.getSize()).isLessOrEqual(iFacts.getNewVehicle().getType().getCapacityDimensions())) {
                if (iFacts.getNewVehicle().getInitialCapacity() != null || iFacts.getNewVehicle().getInitialCapacity() == null
                    && (!Capacity.subtract(loadAtDepot, newAct.getSize()).isGreaterOrEqual(defaultValue)
                    || !Capacity.subtract(pastMinLoad, newAct.getSize()).isGreaterOrEqual(defaultValue))
                    ) {
                    return ConstraintsStatus.NOT_FULFILLED;
                }
            }
            if(!cumulativeShipmentLoad.isLessOrEqual(defaultValue)) {
                if(iFacts.getNewVehicle().getInitialCapacity() != null && !Capacity.addup(currentLoad, newAct.getSize()).isLessOrEqual(iFacts.getNewVehicle().getType().getCapacityDimensions())
                || iFacts.getNewVehicle().getInitialCapacity() == null && !Capacity.subtract(Capacity.addup(currentLoad, newAct.getSize()), Capacity.subtract(loadAtDepot, pastMinLoad)).isLessOrEqual(cumulativeShipmentLoad)
                ) {
                    return ConstraintsStatus.NOT_FULFILLED;
                }
            }
        }
        if (newAct instanceof DeliverService) {
            if(!Capacity.addup(futureMinLoad, newAct.getSize()).isGreaterOrEqual(defaultValue)) {
                if(iFacts.getNewVehicle().getInitialCapacity() != null || iFacts.getNewVehicle().getInitialCapacity() == null
                        && (!Capacity.subtract(loadAtDepot, newAct.getSize()).isLessOrEqual(iFacts.getNewVehicle().getType().getCapacityDimensions())
                        || !Capacity.subtract(pastMaxLoad, newAct.getSize()).isLessOrEqual(iFacts.getNewVehicle().getType().getCapacityDimensions()))
                        ) {
                    return ConstraintsStatus.NOT_FULFILLED;
                }
            }
            if (!cumulativeShipmentLoad.isLessOrEqual(defaultValue)) {
                if(iFacts.getNewVehicle().getInitialCapacity() != null && !Capacity.addup(currentLoad, newAct.getSize()).isGreaterOrEqual(cumulativeShipmentLoad)
                || iFacts.getNewVehicle().getInitialCapacity() == null && !(Capacity.addup(Capacity.addup(currentLoad,  newAct.getSize()),Capacity.subtract(pastMaxLoad, loadAtDepot))).isGreaterOrEqual(cumulativeShipmentLoad)
                ) {
                    return ConstraintsStatus.NOT_FULFILLED;
                }
            }
        }

        return ConstraintsStatus.FULFILLED;
    }
}
