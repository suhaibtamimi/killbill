/*
 * Copyright 2010-2011 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.ning.billing.util.overdue;

import java.util.SortedSet;
import java.util.UUID;

import com.google.inject.Inject;
import com.ning.billing.catalog.api.overdue.Overdueable;
import com.ning.billing.catalog.api.overdue.Overdueable.Type;
import com.ning.billing.util.overdue.dao.OverdueAccessDao;

public class DefaultOverdueAcessApi implements OverdueAccessApi {
    private OverdueAccessDao dao;

    @Inject
    public DefaultOverdueAcessApi(OverdueAccessDao dao) {
        this.dao = dao;
    }
    
    @Override
    public String getOverdueStateNameFor(Overdueable overdueable) {
        return dao.getOverdueStateFor(overdueable);
    }

    @Override
    public String getOverdueStateNameFor(UUID overdueableId, Type type) {
        return dao.getOverdueStateForIdAndType(overdueableId, type);
    }

    @Override
    public SortedSet<OverdueEvent> getOverdueHistory(Overdueable overdueable) {
        return dao.getOverdueHistoryFor(overdueable); 
    }

    @Override
    public SortedSet<OverdueEvent> getOverdueHistory(UUID overdueableId,
            Type type) {
        return dao.getOverdueHistoryForIdAndType(overdueableId, type);
    }

}
