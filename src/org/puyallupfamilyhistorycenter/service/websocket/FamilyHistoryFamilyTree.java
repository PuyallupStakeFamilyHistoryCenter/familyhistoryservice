package org.puyallupfamilyhistorycenter.service.websocket;

import org.familysearch.api.client.ft.FamilySearchFamilyTree;

/**
 *
 * @author tibbitts
 */


public class FamilyHistoryFamilyTree extends FamilySearchFamilyTree {

    public FamilyHistoryFamilyTree(boolean sandbox) {
        super(sandbox);
    }
    
    public FamilySearchFamilyTree authenticate(String accessToken) {
        return (FamilySearchFamilyTree) authenticateWithAccessToken(accessToken);
    }
}
