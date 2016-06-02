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
package com.graphhopper.jsprit.core.problem.vehicle;

import java.util.ArrayList;
import java.util.List;

import com.graphhopper.jsprit.core.problem.AbstractVehicle;
import com.graphhopper.jsprit.core.problem.Capacity;
import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.Skills;

/**
 * Key to identify similar vehicles
 * <p>
 * <p>Two vehicles are equal if they share the same type, the same start and end-location and the same earliestStart and latestStart.
 *
 * @author stefan
 */
public class VehicleTypeKey extends AbstractVehicle.AbstractTypeKey {

    public final String type;
    public final Location startLocation;
    public final Location endLocation;
    public final double earliestStart;
    public final double latestEnd;
    public final Skills skills;
    public List<Skills> alternativeSkills = new ArrayList<Skills>();
    public final boolean returnToDepot;
    public final Capacity initCapa;
    public Double maximumRouteDuration = null;

    public VehicleTypeKey(String typeId, Location startLocation, Location endLocation, double earliestStart, double latestEnd, Skills skills, List<Skills> alternativeSkills, boolean returnToDepot, Capacity initCapa, double maximumRouteDuration) {
        super();
        this.type = typeId;
        this.startLocation = startLocation;
        this.endLocation = endLocation;
        this.earliestStart = earliestStart;
        this.latestEnd = latestEnd;
        this.skills = skills;
        this.alternativeSkills = alternativeSkills;
        this.returnToDepot = returnToDepot;
        this.initCapa = initCapa;
        this.maximumRouteDuration = maximumRouteDuration;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VehicleTypeKey that = (VehicleTypeKey) o;

        if (Double.compare(that.earliestStart, earliestStart) != 0) return false;
        if (Double.compare(that.latestEnd, latestEnd) != 0) return false;
        if (returnToDepot != that.returnToDepot) return false;
        if (endLocation != null && that.endLocation != null && !endLocation.getId().equals(that.endLocation.getId())) return false;
        if (!skills.equals(that.skills)) return false;
        if (startLocation != null && that.endLocation != null && !startLocation.getId().equals(that.startLocation.getId())) return false;
        if (!alternativeSkills.equals(that.alternativeSkills)) return false;
        if (!type.equals(that.type)) return false;
        if (that.maximumRouteDuration == null ^ maximumRouteDuration == null) return false;
        if (that.maximumRouteDuration != null && maximumRouteDuration != null
                && Double.compare(that.maximumRouteDuration, maximumRouteDuration) != 0) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = type.hashCode();
        if(startLocation != null)
        result = 31 * result + startLocation.getId().hashCode();
        if(endLocation != null)
        result = 31 * result + endLocation.getId().hashCode();
        temp = Double.doubleToLongBits(earliestStart);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(latestEnd);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + skills.hashCode();
        if(alternativeSkills != null)
            result = 31 * result + alternativeSkills.hashCode();
        result = 31 * result + (returnToDepot ? 1 : 0);
        if(maximumRouteDuration != null)
            temp = Double.doubleToLongBits(maximumRouteDuration);
        else
            temp = Double.doubleToLongBits(Double.MAX_VALUE);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(type);
        if(startLocation != null)
            stringBuilder.append("_").append(startLocation.getId());
        if(endLocation != null)
            stringBuilder.append("_").append(endLocation.getId());
        stringBuilder.append("_").append(Double.toString(earliestStart)).append("_").append(Double.toString(latestEnd)).append("_").append(maximumRouteDuration);
            if(maximumRouteDuration != null)
                stringBuilder.append("_").append(maximumRouteDuration);
        return stringBuilder.toString();
    }


}
