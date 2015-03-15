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
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.codec.binary.Base64;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.puyallupfamilyhistorycenter.service.models.ImageAndMetadata;
import org.puyallupfamilyhistorycenter.service.models.KeyAndHeaders;

/**
 *
 * @author tibbitts
 */


public class FamilySearchImageCacheHandler extends AbstractHandler {

    private final Source<KeyAndHeaders, ImageAndMetadata> source;
    public FamilySearchImageCacheHandler(Source<KeyAndHeaders, ImageAndMetadata> source) {
        this.source = source;
    }
    

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String query = request.getQueryString();
        String[] pairs = query.split("&");
        String ref = null;
        for (String pair : pairs) {
            if (pair.startsWith("ref=")) {
                ref = pair.substring(4);
            }
        }
        
        if (ref == null) {
            response.sendError(400, "Query parameter 'ref' required");
            return;
        }
        
        String[] urlSplit = URLDecoder.decode(ref, StandardCharsets.US_ASCII.name()).split("\\?");
        String id = URLEncoder.encode(urlSplit[0], StandardCharsets.US_ASCII.name());
        String queryParams = urlSplit.length > 1 ? urlSplit[1] : "";
        
        String accessToken = null;
        Map<String, String> headers = new HashMap<>();
        for (String param : queryParams.split("&")) {
            String[] split = param.split("=");
            if ("access_token".equals(split[0])) {
                accessToken = split[1];
            } else if (split[0].length() > 0) {
                headers.put(split[0], split[1]);
            }
        }
        KeyAndHeaders idAndHeaders = new KeyAndHeaders(id, headers);
        ImageAndMetadata image = source.get(idAndHeaders, accessToken);
        
        if (image == null) {
            response.sendError(500, "Failed to retrieve cached file for resource " + ref);
            return;
        }
        
        for (String headerKey : image.metadata.keySet()) {
            response.setHeader(headerKey, image.metadata.get(headerKey));
        }
        
        response.getOutputStream().write(image.getImageBytes());
        
        response.setStatus(200);
    }
}
