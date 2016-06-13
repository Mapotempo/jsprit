package com.graphhopper.jsprit.core.problem.constraint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;

import com.graphhopper.jsprit.core.algorithm.state.InternalStates;
import com.graphhopper.jsprit.core.algorithm.state.StateManager;
import com.graphhopper.jsprit.core.problem.Capacity;
import com.graphhopper.jsprit.core.problem.Skills;
import com.graphhopper.jsprit.core.problem.job.Service;
import com.graphhopper.jsprit.core.problem.misc.JobInsertionContext;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivity;
import com.graphhopper.jsprit.core.problem.vehicle.Vehicle;

public class NoFirstANDSecondSkillConstraint implements HardActivityConstraint {
    
    private Hashtable<String, Integer> linkedSkills = new Hashtable<String, Integer>();
    
    private StateManager stateManager;

    public NoFirstANDSecondSkillConstraint(Hashtable<String, Integer> linkedSkills, StateManager stateManager) {
        this.linkedSkills = linkedSkills;
        this.stateManager = stateManager;
    }

    @Override
    public ConstraintsStatus fulfilled(JobInsertionContext jobInsertionContext, TourActivity prevAct, TourActivity newAct, TourActivity nextAct, double departureTimeAtPrevAct) {
        Capacity loadAtPrevAct = getLoadAtPreviousAct(prevAct);
        // elements to compare
        if(jobInsertionContext.getNewVehicle().getAlternativeSkills() == null || newAct instanceof Service)
            return ConstraintsStatus.FULFILLED;
        
        // Recreate the two Skills array
        List<Skills> alternates = jobInsertionContext.getNewVehicle().getAlternativeSkills();
        for(Skills alter : alternates)
            for(Skills alter_2 : alternates)
                if(!alter.equals(alter_2)) {
                    for(String iter : alter.values())
                        for(String iter_2 : alter_2.values())
                            if(!alter.containsSkill(iter_2) && !alter_2.containsSkill(iter)) {
                                if (isPickup(newAct)) {
                                    if (isSkillPickup(iter_2, newAct) && hasSkillInVehicle(iter, loadAtPrevAct)) {
                                        return ConstraintsStatus.NOT_FULFILLED;
                                    }
                                    if (isSkillPickup(iter_2, newAct) && isSkillPickup(iter, nextAct)) {
                                        return ConstraintsStatus.NOT_FULFILLED;
                                    }
                                }
                        
                                if (isDelivery(newAct)) {
                                    if (isSkillDelivery(iter_2, newAct) && !isSkillDelivery(iter, newAct) && hasSkillInVehicle(iter, loadAtPrevAct)) {
                                        return ConstraintsStatus.NOT_FULFILLED_BREAK;
                                    }
                                }
                            }
                }
        return ConstraintsStatus.FULFILLED; 
    }

    private boolean hasSkillInVehicle(String skill, Capacity loadAtPrevAct) {
        if(linkedSkills.containsKey(skill))
            if(loadAtPrevAct.get(linkedSkills.get(skill)) > 0){
                return true;
            }
        return false;
    }

    private boolean isSkillPickup(String skill, TourActivity act) {
        if(linkedSkills.containsKey(skill))
            if(act.getSize().get(linkedSkills.get(skill)) > 0) {
                return true;
            }
        return false;
    }

    private boolean isSkillDelivery(String skill, TourActivity act) {
        if(linkedSkills.containsKey(skill))
            if(act.getSize().get(linkedSkills.get(skill)) < 0) {
                return true;
            }
        return false;
    }

    private boolean isPickup(TourActivity newAct) {
        return newAct.getName().equals("pickupShipment");
    }

    private boolean isDelivery(TourActivity newAct) {
        return newAct.getName().equals("deliverShipment");
    }

    private Capacity getLoadAtPreviousAct(TourActivity prevAct) {
        Capacity prevLoad = stateManager.getActivityState(prevAct, InternalStates.LOAD, Capacity.class);
        if (prevLoad != null) return prevLoad;
        else {
            return Capacity.Builder.newInstance().build();
        }
    }
}