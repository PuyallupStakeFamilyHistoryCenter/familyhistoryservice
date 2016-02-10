/*
 * Copyright (c) 2014, tibbitts
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
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 *
 * @author tibbitts
 */


public class Person {
    public final String id;
    public final String name;
    public final String gender;
    public String relationship;
    public final boolean living;
    public final Fact[] facts;
    public final PersonReference[] parents;
    public final PersonReference[] spouses;
    public final PersonReference[] children;
    public final String[] images;
    public final String[] stories;
    //TODO: Finish this (quickly!)

    Person(String id, String name, String gender, Boolean living, Fact[] facts, PersonReference[] parents, PersonReference[] spouses, PersonReference[] children, String[] images, String[] stories) {
        this.id = id;
        this.name = name;
        this.gender = gender;
        this.living = (living == null ? true : living);
        this.facts = facts;
        this.parents = parents;
        this.spouses = spouses;
        this.children = children;
        this.images = images;
        this.stories = stories;
    }
    
    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }
    
    public Fact getFact(String type) {
        if (facts == null || type == null) {
            return null;
        }
        
        Fact selectedFact = null;
        for (Fact fact : facts) {
            if (type.equalsIgnoreCase(fact.type)) {
                selectedFact = fact;
                break;
            }
        }
        
        return selectedFact;
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this, new ArrayList<String>());
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj, new ArrayList());
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
