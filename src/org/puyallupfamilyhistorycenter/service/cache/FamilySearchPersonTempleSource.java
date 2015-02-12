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
package org.puyallupfamilyhistorycenter.service.cache;

import org.puyallupfamilyhistorycenter.service.models.PersonTemple;

/**
 *
 * @author tibbitts
 */
public class FamilySearchPersonTempleSource implements Source<PersonTemple> {
    
    /*
    SAMPLE REQUEST
    
    GET /tree-data/reservations/person/KWCB-HZV/ordinances?_=1423624410559&locale=en&owner=MMJ3-XMN&restrictOrdinancesToImmediateFamilyEx=false&tz=480 HTTP/1.1
    Host: familysearch.org
    Connection: keep-alive
    Accept: application/json, text/plain, *\/*
    ADRUM: isAjax:true
    User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/40.0.2214.94 Safari/537.36
    Referer: https://familysearch.org/tree/
    Accept-Encoding: gzip, deflate, sdch
    Accept-Language: en-US,en;q=0.8,pt-BR;q=0.6,pt;q=0.4,ar;q=0.2
    Cookie: optimizelySegments=%7B%22536411379%22%3A%22none%22%2C%22536691475%22%3A%22referral%22%2C%22544410221%22%3A%22gc%22%2C%22549330157%22%3A%22false%22%2C%221038690166%22%3A%22true%22%7D; optimizelyEndUserId=oeu1420928617111r0.32142603234387934; optimizelyBuckets=%7B%7D; fs-adminRoleE6DA0D=%7B%22hasRoles%22%3Afalse%2C%22assignmentList%22%3A%5B%5D%2C%22activeRole%22%3Anull%7D; connect.sid=s%3AuOeERcW8p8_Hg8VCgh3Mb-VP.7znTDLEbQzZHpyPWpafJEOfAAFXBlIswk8%2BpmRe7q3E; SourceBoxCheckBoxExperiment=true; PhotoSourceExperiment=true; userServiceExperiment=true; outageEx=false; URLCheckExperiment=false; ClientRSAccessPlatformExperiment=false; ClientRSAccessExperiment=true; SourceTypeExperiment=true; LoadStatusVisible=true; LinksExperiment=false; fs_search_history=https%3A//familysearch.org/search/record/results%3Fcount%3D20%26query%3D%252Bdeath_place%253A%2522at%2520sea%2522%7E%26offset%3D20; fs_ex_developers=%7B%22stamp%22%3A%22a3a2e858d77d49eae35cf3382ea892b3%22%2C%22bucket%22%3A65%2C%22features%22%3A%7B%22featureOne%22%3Afalse%7D%2C%22dirtyFeatures%22%3A%5B%5D%7D; __utmt=1; fssessionid=USYS3FACA64C1F94D7F07B05986FC550F01E_idses-prod02.a.fsglobal.net; fs-adminRole3FACA6=%7B%22hasRoles%22%3Afalse%2C%22assignmentList%22%3A%5B%5D%2C%22activeRole%22%3Anull%7D; fs-highconf=true%24%24USYS3FACA64C1F; WRUID=97636527.1990099670; WRIgnore=true; fs-templeinfo=true%24%24USYS3FACA64C1F; ADRUM=s=1423624406383&r=https%3A%2F%2Ffamilysearch.org%2Ftemple%2Fopportunities%3F0; RT=nu=https%3A%2F%2Ffamilysearch.org%2Ftemple%2Fopportunities&cl=1423624388994&r=https%3A%2F%2Ffamilysearch.org%2Ftemple%2Fopportunities&ul=1423624406383&hd=1423624406397; mbox=check#true#1423624467|session#1423624235182-949262#1423626267|PC#1423624235182-949262.28_18#1424834007; fs-tf=0; fs_experiments=u%3Dtibbittsg%2Ca%3Dshared-ui%2Cs%3D9f3d28dee060afe4978702ae72754309%2Cv%3D010011001101000001011111111100111010111010001101000001000%2Cb%3D75%26a%3Dfrontier-tree%2Cs%3D625f3817dfea0fd5fa3449c89474108c%2Cv%3D100011010110101010001100000001100110011000%2Cb%3D0%26a%3Didentity%2Cs%3D4cc8f741a6a374ecf8efa839fe67c90f%2Cv%3D00000001%2Cb%3D26%26a%3Dtemple%2Cs%3Da80bde8dd5e4ba8fae1570d76acbeb4f%2Cv%3D0110%2Cb%3D76; __utma=17181222.1432549812.1419470396.1423443258.1423623831.27; __utmb=17181222.8.10.1423623831; __utmc=17181222; __utmz=17181222.1423623831.27.27.utmcsr=google|utmccn=(organic)|utmcmd=organic|utmctr=(not%20provided); s_cc=true; __CT_Data=gpv=5&apv_42_www04=4&cpv_42_www04=1; s_ppv=FamilySearch%253A%2520Frontier-Tree%253A%2520Homepage%2C100%2C100%2C728; ip_cc=US; s_fid=07703BC535C2CA19-20D45F1F2167FF15; s_sq=%5B%5BB%5D%5D; s_vi=[CS]v1|2A4DB31D05013ABB-6000011440000435[CE]
    
    
    SAMPLE RESPONSE
    
    {
        "status": "OK",
        "statusText": null,
        "data": {
            "id": "LJP7-JNK",
            "name": "Arthur G Ellis",
            "givenName": "Arthur G",
            "familyName": "Ellis",
            "nameFormScript": null,
            "lifespan": "1876-Deceased",
            "gender": "Male",
            "baptism": {
                "type": "Baptism",
                "status": "Completed",
                "whyNotQualifying": [
                    "This ordinance is completed."
                ],
                "statusText": "Completed",
                "hoverText": "Baptism completed at Seattle Washington Temple on 28 October 2014.",
                "bornInCovenant": false,
                "owner": null,
                "ownerName": null,
                "assignedToTemple": false,
                "canAssign": false,
                "canTransfer": false,
                "canUnreserve": false,
                "reserve": false,
                "canPrint": false,
                "reservedDate": null,
                "comparableReservedDate": null,
                "lastModifiedDate": null,
                "comparableLastModifiedDate": null,
                "completedDate": "28 October 2014",
                "completedPlace": "Seattle Washington Temple",
                "relationships": {
                    "fatherId": null,
                    "fatherName": null,
                    "motherId": null,
                    "motherName": null,
                    "spouseId": null,
                    "spouseName": null,
                    "spouseGender": null,
                    "spouseLifeSpan": null,
                    "fatherLifeSpan": null,
                    "motherLifeSpan": null
                }
            },
            "confirmation": {
                "type": "Confirmation",
                "status": "Completed",
                "whyNotQualifying": [
                    "This ordinance is completed."
                ],
                "statusText": "Completed",
                "hoverText": "Confirmation completed at Twin Falls Idaho Temple on 6 November 2014.",
                "bornInCovenant": false,
                "owner": null,
                "ownerName": null,
                "assignedToTemple": false,
                "canAssign": false,
                "canTransfer": false,
                "canUnreserve": false,
                "reserve": false,
                "canPrint": false,
                "reservedDate": null,
                "comparableReservedDate": null,
                "lastModifiedDate": null,
                "comparableLastModifiedDate": null,
                "completedDate": "6 November 2014",
                "completedPlace": "Twin Falls Idaho Temple",
                "relationships": {
                    "fatherId": null,
                    "fatherName": null,
                    "motherId": null,
                    "motherName": null,
                    "spouseId": null,
                    "spouseName": null,
                    "spouseGender": null,
                    "spouseLifeSpan": null,
                    "fatherLifeSpan": null,
                    "motherLifeSpan": null
                }
            },
            "initiatory": {
                "type": "Initiatory",
                "status": "Completed",
                "whyNotQualifying": [
                    "This ordinance is completed."
                ],
                "statusText": "Completed",
                "hoverText": "Initiatory completed at Seattle Washington Temple on 21 November 2014.",
                "bornInCovenant": false,
                "owner": null,
                "ownerName": null,
                "assignedToTemple": false,
                "canAssign": false,
                "canTransfer": false,
                "canUnreserve": false,
                "reserve": false,
                "canPrint": false,
                "reservedDate": null,
                "comparableReservedDate": null,
                "lastModifiedDate": null,
                "comparableLastModifiedDate": null,
                "completedDate": "21 November 2014",
                "completedPlace": "Seattle Washington Temple",
                "relationships": {
                    "fatherId": null,
                    "fatherName": null,
                    "motherId": null,
                    "motherName": null,
                    "spouseId": null,
                    "spouseName": null,
                    "spouseGender": null,
                    "spouseLifeSpan": null,
                    "fatherLifeSpan": null,
                    "motherLifeSpan": null
                }
            },
            "endowment": {
                "type": "Endowment",
                "status": "SharedInProgressNotPrinted",
                "whyNotQualifying": [
                    "This person's record contains enough information for ordinances to be performed."
                ],
                "statusText": "Shared Not Printed",
                "hoverText": "Endowment not printed. Shared with the temple system.",
                "bornInCovenant": false,
                "owner": "MMJ3-XMN",
                "ownerName": "Graham Tibbitts",
                "assignedToTemple": true,
                "canAssign": true,
                "canTransfer": true,
                "canUnreserve": true,
                "reserve": true,
                "canPrint": false,
                "reservedDate": "4 October 2014",
                "comparableReservedDate": "2014-10-05T00:35:12.118",
                "lastModifiedDate": "4 October 2014",
                "comparableLastModifiedDate": "2014-10-05T00:38:01.894",
                "completedDate": null,
                "completedPlace": null,
                "relationships": {
                    "fatherId": null,
                    "fatherName": null,
                    "motherId": null,
                    "motherName": null,
                    "spouseId": null,
                    "spouseName": null,
                    "spouseGender": null,
                    "spouseLifeSpan": null,
                    "fatherLifeSpan": null,
                    "motherLifeSpan": null
                }
            },
            "requiresPermission": false,
            "sealingsToParents": [
                {
                    "type": "SealingToParents",
                    "status": "SharedInProgressNotPrinted",
                    "whyNotQualifying": [
                        "This person's record contains enough information for ordinances to be performed."
                    ],
                    "statusText": "Shared Not Printed",
                    "hoverText": "Sealing to Parents not printed. Shared with the temple system.",
                    "bornInCovenant": false,
                    "owner": "MMJ3-XMN",
                    "ownerName": "Graham Tibbitts",
                    "assignedToTemple": true,
                    "canAssign": true,
                    "canTransfer": true,
                    "canUnreserve": true,
                    "reserve": true,
                    "canPrint": false,
                    "reservedDate": "4 October 2014",
                    "comparableReservedDate": "2014-10-05T00:35:12.118",
                    "lastModifiedDate": "4 October 2014",
                    "comparableLastModifiedDate": "2014-10-05T00:38:01.894",
                    "completedDate": null,
                    "completedPlace": null,
                    "relationships": {
                        "fatherId": "LJP7-NBY",
                        "fatherName": "A L Ellis",
                        "motherId": "LJP7-J9B",
                        "motherName": "Mary A Pound",
                        "spouseId": null,
                        "spouseName": null,
                        "spouseGender": null,
                        "spouseLifeSpan": null,
                        "fatherLifeSpan": null,
                        "motherLifeSpan": null
                    }
                }
            ],
            "sealingsToSpouses": [
                {
                    "type": "SealingToSpouse",
                    "status": "Ready",
                    "whyNotQualifying": [
                        "This person's record contains enough information for ordinances to be performed."
                    ],
                    "statusText": "Request",
                    "hoverText": "Sealing to Spouse can be requested.",
                    "bornInCovenant": false,
                    "owner": null,
                    "ownerName": null,
                    "assignedToTemple": false,
                    "canAssign": false,
                    "canTransfer": false,
                    "canUnreserve": false,
                    "reserve": false,
                    "canPrint": false,
                    "reservedDate": null,
                    "comparableReservedDate": null,
                    "lastModifiedDate": null,
                    "comparableLastModifiedDate": null,
                    "completedDate": null,
                    "completedPlace": null,
                    "relationships": {
                        "fatherId": null,
                        "fatherName": null,
                        "motherId": null,
                        "motherName": null,
                        "spouseId": "LVDS-CN2",
                        "spouseName": "Georgia M Ellis",
                        "spouseGender": "Female",
                        "spouseLifeSpan": null,
                        "fatherLifeSpan": null,
                        "motherLifeSpan": null
                    }
                }
            ],
            "hasPossibleDuplicates": null
        },
        "statuses": null
    }
    
    */

    @Override
    public boolean has(String id) {
        return true;
    }

    @Override
    public PersonTemple get(String id, String accessToken) {
        return null;
    }
    
}
