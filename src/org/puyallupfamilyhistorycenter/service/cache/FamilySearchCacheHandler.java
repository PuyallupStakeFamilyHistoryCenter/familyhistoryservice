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
        
        
//        {
//    "status": "OK",
//    "statusText": null,
//    "data": {
//        "parents": [
//            {
//                "husband": {
//                    "id": "KWCB-HZV",
//                    "lifeSpanDates": "",
//                    "isDeleted": false,
//                    "obsoleteIdWasRequested": false,
//                    "name": "Willis Aaron Dial",
//                    "gender": "Male",
//                    "lifeSpan": "1897-1985",
//                    "spaceId": "MMMM-MMM",
//                    "isLiving": false,
//                    "inPrivateSpace": false,
//                    "readOnly": false,
//                    "principlePerson": false,
//                    "living": false
//                },
//                "wife": {
//                    "id": "KWCB-HZ2",
//                    "lifeSpanDates": "",
//                    "isDeleted": false,
//                    "obsoleteIdWasRequested": false,
//                    "name": "Ida Geneva Beckstrand",
//                    "gender": "Female",
//                    "lifeSpan": "1901-2000",
//                    "spaceId": "MMMM-MMM",
//                    "isLiving": false,
//                    "inPrivateSpace": false,
//                    "readOnly": false,
//                    "principlePerson": false,
//                    "living": false
//                },
//                "current": true,
//                "relationshipId": "MDTN-Q8Z",
//                "coupleId": "KWCB-HZV_KWCB-HZ2",
//                "event": {
//                    "originalPlace": "Salt Lake City, Salt Lake, Utah, United States",
//                    "standardPlace": "Salt Lake City, Salt Lake, Utah, United States",
//                    "originalDate": "13 February 1925",
//                    "standardDate": "13 February 1925",
//                    "type": "MARRIAGE"
//                },
//                "children": [
//                    {
//                        "id": "KW8W-CBX",
//                        "lifeSpanDates": "",
//                        "isDeleted": false,
//                        "obsoleteIdWasRequested": false,
//                        "name": "Marian Lovina Dial",
//                        "gender": "Female",
//                        "lifeSpan": "1926-2012",
//                        "spaceId": "MMMM-MMM",
//                        "isLiving": false,
//                        "inPrivateSpace": false,
//                        "readOnly": false,
//                        "principlePerson": false,
//                        "relationshipId": "M4H1-493",
//                        "lineageConclusions": [],
//                        "living": false
//                    },
//                    {
//                        "id": "KWZZ-7PD",
//                        "lifeSpanDates": "",
//                        "isDeleted": false,
//                        "obsoleteIdWasRequested": false,
//                        "name": "Erma Dial",
//                        "gender": "Female",
//                        "lifeSpan": "1933-2004",
//                        "spaceId": "MMMM-MMM",
//                        "isLiving": false,
//                        "inPrivateSpace": false,
//                        "readOnly": false,
//                        "principlePerson": false,
//                        "relationshipId": "M7VV-JW5",
//                        "lineageConclusions": [],
//                        "living": false
//                    },
//                    {
//                        "id": "KWC6-X7D",
//                        "lifeSpanDates": "",
//                        "isDeleted": false,
//                        "obsoleteIdWasRequested": false,
//                        "name": "Glen &quot;B&quot; Dial",
//                        "gender": "Male",
//                        "lifeSpan": "1929-1967",
//                        "spaceId": "MMMM-MMM",
//                        "isLiving": false,
//                        "inPrivateSpace": false,
//                        "readOnly": false,
//                        "principlePerson": false,
//                        "relationshipId": "M8YK-7W2",
//                        "lineageConclusions": [],
//                        "living": false
//                    },
//                    {
//                        "id": "KWJJ-4XH",
//                        "lifeSpanDates": "",
//                        "isDeleted": false,
//                        "obsoleteIdWasRequested": false,
//                        "name": "Merlin &quot;B&quot; Dial",
//                        "gender": "Male",
//                        "lifeSpan": "1927-1974",
//                        "spaceId": "MMMM-MMM",
//                        "isLiving": false,
//                        "inPrivateSpace": false,
//                        "readOnly": false,
//                        "principlePerson": true,
//                        "relationshipId": "MWXJ-RF4",
//                        "lineageConclusions": [],
//                        "living": false
//                    }
//                ]
//            }
//        ],
//        "spouses": [
//            {
//                "husband": {
//                    "id": "KWJJ-4XH",
//                    "lifeSpanDates": "",
//                    "isDeleted": false,
//                    "obsoleteIdWasRequested": false,
//                    "name": "Merlin &quot;B&quot; Dial",
//                    "gender": "Male",
//                    "lifeSpan": "1927-1974",
//                    "spaceId": "MMMM-MMM",
//                    "isLiving": false,
//                    "inPrivateSpace": false,
//                    "readOnly": false,
//                    "principlePerson": true,
//                    "living": false
//                },
//                "wife": null,
//                "current": true,
//                "relationshipId": null,
//                "coupleId": null,
//                "event": null,
//                "children": null
//            }
//        ]
//    },
//    "statuses": null
//}
        
        
//        {
//    "properties": {
//        "readOnly": false,
//        "lifespan": {
//            "years": "1927 - 1974",
//            "value": "29 December 1927 - 2 January 1974"
//        },
//        "living": false
//    },
//    "id": "KWJJ-4XH",
//    "parentChildRelationship": null,
//    "conclusions": {
//        "list": [
//            {
//                "genderConclusion": {
//                    "type": "MALE",
//                    "id": "V.777-777H",
//                    "behavior": "SINGLE_VALUE_NON_DELETABLE",
//                    "justification": {
//                        "reason": null,
//                        "confidence": "LOW"
//                    },
//                    "conclusionType": "GENDER",
//                    "contribution": {
//                        "timeStamp": 1338051636348,
//                        "contributorId": "MMWW-DVV",
//                        "submitterId": null
//                    }
//                }
//            },
//            {
//                "nameConclusion": {
//                    "type": "BIRTH",
//                    "preferred": true,
//                    "primaryForm": {
//                        "script": "UNSPECIFIED",
//                        "prefixPart": null,
//                        "givenPart": "Merlin \" B\"",
//                        "familyPart": "Dial",
//                        "suffixPart": null,
//                        "fullText": "Merlin \"B\" Dial"
//                    },
//                    "style": "EUROTYPIC",
//                    "alternateForms": [],
//                    "id": "V.777-777N",
//                    "behavior": "SINGLE_VALUE_NON_DELETABLE",
//                    "justification": {
//                        "reason": null,
//                        "confidence": "LOW"
//                    },
//                    "conclusionType": "NAME",
//                    "contribution": {
//                        "timeStamp": 1210208693896,
//                        "contributorId": "MMXJ-KTG",
//                        "submitterId": null
//                    }
//                }
//            },
//            {
//                "eventConclusion": {
//                    "type": "BIRTH",
//                    "date": {
//                        "localizedText": null,
//                        "normalizedText": null,
//                        "originalText": "29 December 1927",
//                        "julianDateRange": null,
//                        "modifier": null
//                    },
//                    "place": {
//                        "geoCode": null,
//                        "localizedText": null,
//                        "normalizedText": null,
//                        "originalText": "Logan, Cache, Utah, United States"
//                    },
//                    "id": "V.777-777P",
//                    "behavior": "SINGLE_VALUE_NON_DELETABLE",
//                    "justification": {
//                        "reason": null,
//                        "confidence": null
//                    },
//                    "conclusionType": "BIRTH",
//                    "contribution": {
//                        "timeStamp": 1365996685122,
//                        "contributorId": "MMXJ-KTG",
//                        "submitterId": null
//                    }
//                }
//            },
//            {
//                "eventConclusion": {
//                    "type": "DEATH",
//                    "date": {
//                        "localizedText": null,
//                        "normalizedText": null,
//                        "originalText": "2 January 1974",
//                        "julianDateRange": null,
//                        "modifier": null
//                    },
//                    "place": {
//                        "geoCode": null,
//                        "localizedText": null,
//                        "normalizedText": null,
//                        "originalText": "Fresno, Fresno, California, United States"
//                    },
//                    "id": "V.777-7774",
//                    "behavior": "SINGLE_VALUE_NON_DELETABLE",
//                    "justification": {
//                        "reason": null,
//                        "confidence": null
//                    },
//                    "conclusionType": "DEATH",
//                    "contribution": {
//                        "timeStamp": 1365996700113,
//                        "contributorId": "MMXJ-KTG",
//                        "submitterId": null
//                    }
//                }
//            },
//            {
//                "eventConclusion": {
//                    "type": "BURIAL",
//                    "date": {
//                        "localizedText": "7 January 1974",
//                        "normalizedText": "7 January 1974",
//                        "originalText": "7 January 1974",
//                        "julianDateRange": {
//                            "latestDay": 2442055,
//                            "earliestDay": 2442055
//                        },
//                        "modifier": null
//                    },
//                    "place": {
//                        "geoCode": null,
//                        "localizedText": "San Diego, San Diego, California, United States",
//                        "normalizedText": "San Diego, San Diego, California, United States",
//                        "originalText": "El Camino Memorial Park, San Diego, San Diego, California, United States"
//                    },
//                    "id": "V.777-7771",
//                    "behavior": "SINGLE_VALUE_DELETABLE",
//                    "justification": {
//                        "reason": null,
//                        "confidence": "LOW"
//                    },
//                    "conclusionType": "BURIAL",
//                    "contribution": {
//                        "timeStamp": 1338051636348,
//                        "contributorId": "MMXJ-KTG",
//                        "submitterId": null
//                    }
//                }
//            },
//            {
//                "livingStatusConclusion": {
//                    "type": "DECEASED",
//                    "id": "V.777-777L",
//                    "behavior": "SINGLE_VALUE_NON_DELETABLE",
//                    "justification": {
//                        "reason": null,
//                        "confidence": null
//                    },
//                    "conclusionType": "LIVING_STATUS",
//                    "contribution": {
//                        "timeStamp": 1410472282324,
//                        "contributorId": "MMMM-MMK",
//                        "submitterId": null
//                    }
//                }
//            }
//        ]
//    },
//    "link": null,
//    "spaceId": "MMMM-MMM",
//    "coupleRelationship": null
//}
    }
    
}
