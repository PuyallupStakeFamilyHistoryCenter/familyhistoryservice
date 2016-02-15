/*
 * Copyright (c) 2015, tibbitts
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.puyallupfamilyhistorycenter.service.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.puyallupfamilyhistorycenter.service.models.Fact;
import org.puyallupfamilyhistorycenter.service.models.Person;
import org.puyallupfamilyhistorycenter.service.utils.CountryCanonicalization;

/**
 *
 * @author tibbitts
 */
public class Statistics {

    public final int count;
    public final List<Integer[]> lifeSpanData;
    public final List<Integer[]> familySizeData;
    public final List<String> birthCountries;
    public final List<String> deathCountries;
    public final List<Object[]> emmigrants;
    
    public Statistics(List<Person> people) {
        count = people.size();
        
        lifeSpanData = new ArrayList<>();
        familySizeData = new ArrayList<>();
        birthCountries = new ArrayList<>();
        deathCountries = new ArrayList<>();
        emmigrants = new ArrayList<>();
        
        for (Person person : people) {
            Fact birth = person.getFact("birth");
            Fact death = person.getFact("death");
            
            if (birth != null && death != null && birth.year != null && death.year != null) {
                lifeSpanData.add(new Integer[] {birth.year, death.year - birth.year});
            }
            
            if (birth != null && birth.year != null && person.children != null) {
                familySizeData.add(new Integer[] {birth.year, person.children.length});
            }
            
            String birthCountry = null;
            if (birth != null && birth.place != null) {
                String[] split = birth.place.split(", *");
                birthCountry = CountryCanonicalization.canonicalize(split[split.length-1]);
                birthCountries.add(birthCountry);
            }
            
            String deathCountry = null;
            if (death != null && death.place != null) {
                String[] split = death.place.split(", *");
                deathCountry = CountryCanonicalization.canonicalize(split[split.length-1]);
                deathCountries.add(deathCountry);
            }
            
            if (birthCountry != null && deathCountry != null && !birthCountry.equals(deathCountry)) {
                emmigrants.add(new Object[] {
                    person.name,
                    birth.year,
                    birthCountry,
                    death.year,
                    deathCountry
                });
            }
        }
    }
}
