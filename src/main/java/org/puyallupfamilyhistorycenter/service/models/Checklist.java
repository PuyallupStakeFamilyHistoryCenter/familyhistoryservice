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
package org.puyallupfamilyhistorycenter.service.models;

import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * @author tibbitts
 */
public class Checklist {
    private final String OPEN_KEY = "open";
    private final String CLOSE_KEY = "close";
    private final String SESSION_KEY = "session";
    
    public final String responseType = "checklist";
    public final Table<String, String, ChecklistItem> items;

    public Checklist() {
        this.items = Tables.newCustomTable(new LinkedHashMap<String, Map<String, ChecklistItem>>(), () -> {
            return new LinkedHashMap<String, ChecklistItem>();
        });
    }
    
    public void addOpenItem(ChecklistItem item) {
        items.put(OPEN_KEY, item.id, item);
    }
    
    public void addCloseItem(ChecklistItem item) {
        items.put(CLOSE_KEY, item.id, item);
    }
    
    public void addSessionItem(ChecklistItem item) {
        items.put(SESSION_KEY, item.id, item);
    }
    
    public void setChecked(String id, boolean checked) {
        if (items.contains(OPEN_KEY, id)) {
            items.get(OPEN_KEY, id).setChecked(checked);
        }
            
        if (items.contains(CLOSE_KEY, id)) {
            items.get(CLOSE_KEY, id).setChecked(checked);
        }
            
        if (items.contains(SESSION_KEY, id)) {
            items.get(SESSION_KEY, id).setChecked(checked);
        }
    }
}
