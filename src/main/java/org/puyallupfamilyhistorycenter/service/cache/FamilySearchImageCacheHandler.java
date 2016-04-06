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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.GZIPOutputStream;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

/**
 *
 * @author tibbitts
 */


public class FamilySearchImageCacheHandler extends AbstractHandler {
    public static final File cacheDir = new File("/tmp/fhc/image-cache");
    static {
        cacheDir.mkdirs();
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
        
        String filename = URLEncoder.encode(ref.replaceAll("access_token.*", ""), StandardCharsets.UTF_8.name());
        File cachedFile = new File(cacheDir, filename + ".gz");
        File metadataFile = new File(cacheDir, filename + ".meta");
        
        if (!cachedFile.exists()) {
            URL refUrl = new URL(URLDecoder.decode(ref, StandardCharsets.US_ASCII.name()));
            HttpURLConnection connection = (HttpURLConnection) refUrl.openConnection();
            Enumeration<String> headerNames = baseRequest.getHeaderNames();
            for (String headerName = headerNames.nextElement(); headerNames.hasMoreElements(); headerName = headerNames.nextElement()) {
                connection.setRequestProperty(headerName, baseRequest.getHeader(headerName));
            }
            try {
                if (connection.getResponseCode() / 100 == 2) { // Shorthand for 'is response successful?'
                    try (OutputStream out = new GZIPOutputStream(new FileOutputStream(cachedFile))) {
                        IOUtils.copy(connection.getInputStream(), out);
                    }
                    
                    try (Writer writer = new FileWriter(metadataFile)) {
                        for (String headerName : connection.getHeaderFields().keySet()) {
                            if (keepHeader(headerName)) {
                                writer.write(headerName + ":" + connection.getHeaderField(headerName) + '\n');
                            }
                        }
                        writer.write("Content-Encoding:gzip\n");
                        writer.write("Content-Length:" + cachedFile.length() + '\n');
                    }
                } else {
                    response.sendError(connection.getResponseCode(), connection.getResponseMessage());
                    return;
                }
            } finally {
                connection.disconnect();
            }
        }
        
        if (!cachedFile.exists()) {
            response.sendError(500, "Failed to retrieve cached file for resource " + ref);
            return;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(metadataFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] split = line.split(":", 2);
                response.setHeader(split[0], split[1]);
            }
        }
        
        try (InputStream fin = new FileInputStream(cachedFile)) {
            IOUtils.copy(fin, response.getOutputStream());
        }
        
        response.setStatus(200);
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
