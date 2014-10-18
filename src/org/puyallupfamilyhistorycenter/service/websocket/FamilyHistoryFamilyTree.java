package org.puyallupfamilyhistorycenter.service.websocket;

import java.net.URI;
import java.net.URISyntaxException;
import org.familysearch.api.client.ft.FamilySearchFamilyTree;
import org.puyallupfamilyhistorycenter.service.SpringContextInitializer;

/**
 *
 * @author tibbitts
 */


public class FamilyHistoryFamilyTree extends FamilySearchFamilyTree {

    private FamilyHistoryFamilyTree(URI uri) {
        super(uri);
    }
    
    private static final URI uri;
    static {
        AppKeyConfig appKeyConfig = (AppKeyConfig) SpringContextInitializer.getContext().getBean("app-key-config");
        try {
            uri = new URI("https://"+appKeyConfig.environment+".familysearch.org/platform/collections/tree");
        } catch (URISyntaxException ex) {
            throw new IllegalStateException("Failed to create FamilySearch URI", ex);
        }
    }
    
    public static FamilySearchFamilyTree getInstance(String accessToken) {
        return (FamilySearchFamilyTree) new FamilySearchFamilyTree(uri).authenticateWithAccessToken(accessToken);
    }
}
