package org.puyallupfamilyhistorycenter.service.websocket;

import java.net.URI;
import org.familysearch.api.client.ft.FamilySearchFamilyTree;

/**
 *
 * @author tibbitts
 */


public class FamilyHistoryFamilyTree extends FamilySearchFamilyTree {

    public FamilyHistoryFamilyTree(boolean sandbox) {
        super(sandbox);
    }
    
    public FamilyHistoryFamilyTree(URI uri) {
        super(uri);
    }
    
    public FamilySearchFamilyTree authenticate(String accessToken) {
        return (FamilySearchFamilyTree) authenticateWithAccessToken(accessToken);
    }
}
