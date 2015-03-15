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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.puyallupfamilyhistorycenter.service.models.ImageAndMetadata;
import org.puyallupfamilyhistorycenter.service.models.KeyAndHeaders;

/**
 *
 * @author tibbitts
 */
public class FamilySearchImageSource implements Source<KeyAndHeaders, ImageAndMetadata> {

    @Override
    public boolean has(KeyAndHeaders id) {
        return true;
    }

    @Override
    public ImageAndMetadata get(KeyAndHeaders id, String accessToken) {
        HttpURLConnection connection = null;
        try {
            URL refUrl = new URL(URLDecoder.decode(id.key, StandardCharsets.US_ASCII.name()) + (accessToken != null && accessToken.length() > 0 ? "&access_token=" + accessToken : ""));
            connection = (HttpURLConnection) refUrl.openConnection();

            for (String headerName : id.headers.keySet()) {
                connection.setRequestProperty(headerName, id.headers.get(headerName));
            }
            
            if (connection.getResponseCode() / 100 == 2) { // Shorthand for 'is response successful?'
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                try (OutputStream out = new GZIPOutputStream(bout);) {
                    IOUtils.copy(connection.getInputStream(), out);
                }
                
                Map<String, String> metadata = new HashMap<>();
                for (String headerName : connection.getHeaderFields().keySet()) {
                    if (keepHeader(headerName)) {
                        metadata.put(headerName, connection.getHeaderField(headerName));
                    }
                }
                
                byte[] array = bout.toByteArray();
                metadata.put("Content-Encoding", "gzip");
                metadata.put("Content-Length", Integer.toString(array.length));
                
                return new ImageAndMetadata(array, metadata);
            } else {
                throw new IllegalStateException("Failed to fetch image " + connection.getResponseCode() + ": " + connection.getResponseMessage());
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to get image " + id.key, ex);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }


    private static final Set<String> keepHeaders = new HashSet<String>() {{
        add("Content-Type");
        add("Last-Modified");
        add("Etag");
    }};
    private boolean keepHeader(String headerName) {
        return keepHeaders.contains(headerName);
    }
}
