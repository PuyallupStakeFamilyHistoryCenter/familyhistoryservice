/*
 * Copyright (c) 2016, tibbitts
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
package org.puyallupfamilyhistorycenter.service;

import com.google.common.collect.ImmutableSet;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.puyallupfamilyhistorycenter.service.utils.UserImageRegistry;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author tibbitts
 */
public class SaveImageHandler extends AbstractHandler {
    
    @Autowired
    UserImageRegistry imageRegistry;
    
    static final File saveImageDir = new File("/tmp/fhc/image-save");
    static final MultipartConfigElement mpce = new MultipartConfigElement(saveImageDir.getAbsolutePath(), Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
    static {
        saveImageDir.mkdirs();
        
        //Clean up saved filed from previous runs
        for (File child : saveImageDir.listFiles()) {
            child.delete();
        }
    }
    
    Set<String> acceptedMethods = ImmutableSet.<String>builder().add("POST", "PUT").build();

    @Override
    public void handle(String string, Request rqst, HttpServletRequest hsr, HttpServletResponse hsr1) throws IOException, ServletException {
        if (!acceptedMethods.contains(rqst.getMethod())) {
            throw new IllegalStateException("Unsupported method " + rqst.getMethod());
        }
        
        rqst.setAttribute(Request.__MULTIPART_CONFIG_ELEMENT, mpce);
        
        Part part = hsr.getPart("coloring-page");
        File file = File.createTempFile("saved-image-", ".png", saveImageDir);
        file.deleteOnExit();
        
        try (InputStream in = part.getInputStream();
                OutputStream out = new FileOutputStream(file)) {
            IOUtils.copy(in, out);
        }
        
        String userId = rqst.getParameter("user-id");
        imageRegistry.registerImage(userId, file.getPath());
        
        rqst.setHandled(true);
    }
    
}
