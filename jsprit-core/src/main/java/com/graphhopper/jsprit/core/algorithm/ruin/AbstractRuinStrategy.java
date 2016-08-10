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

package com.graphhopper.jsprit.core.algorithm.ruin;


import com.graphhopper.jsprit.core.algorithm.ruin.listener.RuinListener;
import com.graphhopper.jsprit.core.algorithm.ruin.listener.RuinListeners;
import com.graphhopper.jsprit.core.algorithm.state.InternalStates;
import com.graphhopper.jsprit.core.algorithm.state.StateManager;
import com.graphhopper.jsprit.core.problem.Capacity;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.job.Delivery;
import com.graphhopper.jsprit.core.problem.job.Job;
import com.graphhopper.jsprit.core.problem.job.Pickup;
import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivity;
import com.graphhopper.jsprit.core.util.RandomNumberGeneration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Random;

public abstract class AbstractRuinStrategy implements RuinStrategy {

    private final static Logger logger = LoggerFactory.getLogger(AbstractRuinStrategy.class);

    private RuinListeners ruinListeners;

    protected Random random = RandomNumberGeneration.getRandom();

    protected VehicleRoutingProblem vrp;

    private StateManager stateManager;

    public void setRandom(Random random) {
        this.random = random;
    }

    protected RuinShareFactory ruinShareFactory;

    public void setRuinShareFactory(RuinShareFactory ruinShareFactory) {
        this.ruinShareFactory = ruinShareFactory;
    }

    public RuinShareFactory getRuinShareFactory() {
        return ruinShareFactory;
    }

    protected AbstractRuinStrategy(VehicleRoutingProblem vrp, StateManager stateManager) {
        this.vrp = vrp;
        this.stateManager = stateManager;
        ruinListeners = new RuinListeners();
    }

    @Override
    public Collection<Job> ruin(Collection<VehicleRoute> vehicleRoutes) {
        ruinListeners.ruinStarts(vehicleRoutes);
        Collection<Job> unassigned = ruinRoutes(vehicleRoutes);
        logger.trace("ruin: [ruined={}]", unassigned.size());
        ruinListeners.ruinEnds(vehicleRoutes, unassigned);
        return unassigned;
    }

    public abstract Collection<Job> ruinRoutes(Collection<VehicleRoute> vehicleRoutes);

    @Override
    public void addListener(RuinListener ruinListener) {
        ruinListeners.addListener(ruinListener);
    }

    @Override
    public void removeListener(RuinListener ruinListener) {
        ruinListeners.removeListener(ruinListener);
    }

    @Override
    public Collection<RuinListener> getListeners() {
        return ruinListeners.getListeners();
    }

    protected boolean removeJob(Job job, Collection<VehicleRoute> vehicleRoutes) {


        if (jobIsInitial(job)) return false;
        for (VehicleRoute route : vehicleRoutes) {
            if (removeJob(job, route)) {
                stateManager.reCalculateStates(route);
                return true;
            }
        }
        return false;
    }

    private boolean jobIsInitial(Job job) {
        return !vrp.getJobs().containsKey(job.getId()); //for initial jobs (being not contained in problem
    }

    protected boolean removeJob(Job job, VehicleRoute route) {
        if (jobIsInitial(job)) return false;
        TourActivity act = null;
        for(TourActivity iter : route.getActivities()) {
            if(iter.getIndex() == job.getIndex())
                act = iter;
        }
        Capacity defaultValue = Capacity.Builder.newInstance().build();
        Capacity roofValue = route.getVehicle().getType().getCapacityDimensions();
        Capacity loadAtDepot = Capacity.Builder.newInstance().build();
        Capacity futureCumulativeShipmentLoad = Capacity.Builder.newInstance().build();
        Capacity currentLoad = Capacity.Builder.newInstance().build();
        if(act != null) {
            loadAtDepot = stateManager.getRouteState(route, InternalStates.LOAD_AT_BEGINNING, Capacity.class);
            if(loadAtDepot == null)
                loadAtDepot = defaultValue;
            if(route.getVehicle().getInitialCapacity() != null)
                loadAtDepot = route.getVehicle().getInitialCapacity();
            currentLoad = stateManager.getActivityState(act, InternalStates.LOAD, Capacity.class);
            if(currentLoad == null)
                currentLoad = defaultValue;
            futureCumulativeShipmentLoad = stateManager.getActivityState(act, InternalStates.FUTURE_MAX_SHIPMENT_LOAD, Capacity.class);
            if(futureCumulativeShipmentLoad == null)
                futureCumulativeShipmentLoad = defaultValue;
        }
        if(act != null && job instanceof Pickup) {
            Capacity pastMaxLoad = stateManager.getActivityState(act, InternalStates.PAST_MAXLOAD, Capacity.class);
            if (pastMaxLoad == null) pastMaxLoad = defaultValue;
            Capacity futureMinLoad = stateManager.getActivityState(act, InternalStates.FUTURE_MINLOAD, Capacity.class);
            if(futureMinLoad == null) futureMinLoad = defaultValue;
            if (!Capacity.subtract(futureMinLoad, job.getSize()).isGreaterOrEqual(defaultValue))
                if(route.getVehicle().getInitialCapacity() != null || route.getVehicle().getInitialCapacity() == null && (!Capacity.addup(loadAtDepot, job.getSize()).isLessOrEqual(route.getVehicle().getType().getCapacityDimensions())
                                                                                                 || !Capacity.addup(pastMaxLoad, job.getSize()).isLessOrEqual(route.getVehicle().getType().getCapacityDimensions()))) {
                return false;
            }
            if(!futureCumulativeShipmentLoad.isLessOrEqual(defaultValue)) {
                if(route.getVehicle().getInitialCapacity() != null && !Capacity.subtract(currentLoad, job.getSize()).isGreaterOrEqual(futureCumulativeShipmentLoad)
                        || route.getVehicle().getInitialCapacity() == null && !Capacity.subtract(Capacity.subtract(Capacity.subtract(currentLoad, job.getSize()), route.getVehicle().getType().getCapacityDimensions()), pastMaxLoad).isGreaterOrEqual(futureCumulativeShipmentLoad))
                    return false;
            }
        }
        if (act != null && job instanceof Delivery) {
            Capacity pastMinLoad = stateManager.getActivityState(act, InternalStates.PAST_MINLOAD, Capacity.class);
            if (pastMinLoad == null) pastMinLoad = defaultValue;
            Capacity futureMaxLoad = stateManager.getActivityState(act, InternalStates.FUTURE_MAXLOAD, Capacity.class);
            if (futureMaxLoad == null) futureMaxLoad = defaultValue;
            if (!Capacity.addup(futureMaxLoad, job.getSize()).isLessOrEqual(roofValue))
                if (route.getVehicle().getInitialCapacity() != null || route.getVehicle().getInitialCapacity() == null && (!Capacity.subtract(loadAtDepot, job.getSize()).isGreaterOrEqual(defaultValue)
                                                                                        || !Capacity.subtract(pastMinLoad, job.getSize()).isGreaterOrEqual(defaultValue))) {
                return false;
            }
        }
        boolean removed = route.getTourActivities().removeJob(job);
        if (removed) {
            logger.trace("ruin: {}", job.getId());
            ruinListeners.removed(job, route);
            return true;
        }
        return false;
    }
}
