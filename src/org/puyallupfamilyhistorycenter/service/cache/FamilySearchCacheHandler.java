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

package org.puyallupfamilyhistorycenter.service.cache;

import java.io.IOException;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
//import org.familysearch.api.client.ft.FamilySearchFamilyTree;
//import org.familysearch.api.client.ft.FamilyTreePersonState;
//import org.familysearch.platform.ct.ChildAndParentsRelationship;
//import org.gedcomx.common.ResourceReference;
//import org.gedcomx.rs.client.PersonState;
//import org.gedcomx.rs.client.StateTransitionOption;

/**
 *
 * @author tibbitts
 */


public class FamilySearchCacheHandler extends AbstractHandler {

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
//        FamilySearchFamilyTree tree = new FamilySearchFamilyTree(true).authenticateViaOAuth2AuthCode(null, null, null).ifSuccessful();
//        
//        String personId = "person-id";
//        StateTransitionOption[] options = new StateTransitionOption[] {};
//        PersonState person = tree.readPersonForCurrentUser();
//        FamilyTreePersonState ftPerson = tree.readPersonWithRelationshipsById(person.getSelfRel(), options);
//        
//        List<ChildAndParentsRelationship> childrenRelationships = ftPerson.getChildAndParentsRelationshipsToChildren();
//        
//        for (ChildAndParentsRelationship relationship : childrenRelationships) {
//            ResourceReference child = relationship.getChild();
//            ResourceReference father = relationship.getFather();
//            ResourceReference mother = relationship.getMother();
//        }
    }
    
}
