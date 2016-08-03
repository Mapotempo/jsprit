package com.graphhopper.jsprit.core.algorithm.ruin;

import com.graphhopper.jsprit.core.algorithm.state.StateManager;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;

public class ClusterRuinStrategyFactory implements RuinStrategyFactory  {

    private int initialNumberJobsToRemove;
    private JobNeighborhoods jobNeighborhoods;

    public ClusterRuinStrategyFactory(int initialNumberJobsToRemove, JobNeighborhoods jobNeighborhoods) {
        super();
        this.initialNumberJobsToRemove = initialNumberJobsToRemove;
        this.jobNeighborhoods = jobNeighborhoods;
    }

    @Override
    public RuinStrategy createStrategy(VehicleRoutingProblem vrp, StateManager stateManager) {
        return new RuinClusters(vrp, stateManager, initialNumberJobsToRemove, jobNeighborhoods);
    }
}