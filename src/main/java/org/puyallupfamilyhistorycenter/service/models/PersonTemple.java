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

import java.util.List;

/**
 *
 * @author tibbitts
 */
public class PersonTemple {
    public static final String ORDINANCE_READY = "Ready";
    
    public final String id;
    public final String name;
    public final String gender;
    public final Ordinance baptism;
    public final Ordinance confirmation;
    public final Ordinance initiatory;
    public final Ordinance endowment;
    public final List<Ordinance> sealingsToSpouses;
    public final List<Ordinance> sealingsToParents;
    public final boolean requiresPermission;
    
    //TODO: Use this field to improve results
    //public final Object hasPossibleDuplicates;

    public PersonTemple(String id, String name, String gender, Ordinance baptism, Ordinance confirmation, Ordinance initiatory, Ordinance endowment, List<Ordinance> sealingsToSpouses, List<Ordinance> sealingsToParents, boolean requiresPermission) {
        this.id = id;
        this.name = name;
        this.gender = gender;
        this.baptism = baptism;
        this.confirmation = confirmation;
        this.initiatory = initiatory;
        this.endowment = endowment;
        this.sealingsToSpouses = sealingsToSpouses;
        this.sealingsToParents = sealingsToParents;
        this.requiresPermission = requiresPermission;
    }

    
    
    public boolean hasOrdinancesReady() {
        if (requiresPermission) {
            return false;
        }
        
        if (ORDINANCE_READY.equals(baptism.getStatus())) {
            return true;
        }
        
        if (ORDINANCE_READY.equals(confirmation.getStatus())) {
            return true;
        }
        
        if (ORDINANCE_READY.equals(initiatory.getStatus())) {
            return true;
        }
        
        if (ORDINANCE_READY.equals(endowment.getStatus())) {
            return true;
        }
        
        for (Ordinance sealingToSpouse : sealingsToSpouses) {
            if (ORDINANCE_READY.equals(sealingToSpouse.getStatus())) {
                return true;
            }
        }
        
        for (Ordinance sealingToParents : sealingsToParents) {
            if (ORDINANCE_READY.equals(sealingToParents.getStatus())) {
                return true;
            }
        }
        
        return false;
    }
}
