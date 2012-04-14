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

package com.ning.billing.entitlement.api.overdue;

import com.google.inject.Inject;
import com.ning.billing.ErrorCode;
import com.ning.billing.catalog.api.CatalogService;
import com.ning.billing.catalog.api.overdue.OverdueState;
import com.ning.billing.entitlement.api.user.EntitlementUserApiException;
import com.ning.billing.entitlement.api.user.Subscription;
import com.ning.billing.entitlement.api.user.SubscriptionBundle;
import com.ning.billing.entitlement.engine.dao.EntitlementDao;
import com.ning.billing.util.overdue.OverdueAccessApi;

public class DefaultOverdueChecker implements OverdueChecker {

    private final EntitlementDao dao;
    private final CatalogService catalogService;
    private final OverdueAccessApi overdueApi;

    @Inject
    public DefaultOverdueChecker(EntitlementDao dao, CatalogService catalogService, OverdueAccessApi overdueApi) {
        this.catalogService = catalogService;
        this.dao = dao;
        this.overdueApi = overdueApi;
    }

    @Override
    public void checkBlocked(Subscription subscription) throws EntitlementUserApiException {
        if(subscription.getBundleId() != null) {
            SubscriptionBundle bundle = dao.getSubscriptionBundleFromId(subscription.getBundleId());
            checkBlocked(bundle);
        }
    }

    @Override
    public void checkBlocked(SubscriptionBundle bundle) throws EntitlementUserApiException {
        OverdueState<SubscriptionBundle> bundleState = bundle.getOverdueState();
        if(bundleState != null && bundleState.blockChanges()) {
            throw new EntitlementUserApiException(ErrorCode.ENT_BUNDLE_IS_OVERDUE_BLOCKED, bundle.getId(), bundle.getKey());
        }
    }

 
}
