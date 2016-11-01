/*
 * Copyright 2014-2016 Groupon, Inc
 * Copyright 2014-2016 The Billing Project, LLC
 *
 * The Billing Project licenses this file to you under the Apache License, version 2.0
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

package org.killbill.billing.payment.core;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.killbill.automaton.MissingEntryException;
import org.killbill.automaton.State;
import org.killbill.billing.account.api.Account;
import org.killbill.billing.account.api.AccountApiException;
import org.killbill.billing.account.api.AccountInternalApi;
import org.killbill.billing.callcontext.InternalCallContext;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.invoice.api.InvoiceInternalApi;
import org.killbill.billing.osgi.api.OSGIServiceRegistration;
import org.killbill.billing.payment.api.Payment;
import org.killbill.billing.payment.api.PaymentApiException;
import org.killbill.billing.payment.api.PaymentTransaction;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.payment.api.TransactionType;
import org.killbill.billing.payment.core.sm.PaymentControlStateMachineHelper;
import org.killbill.billing.payment.core.sm.PluginControlPaymentAutomatonRunner;
import org.killbill.billing.payment.core.sm.PluginControlPaymentAutomatonRunner.ControlOperation;
import org.killbill.billing.payment.dao.PaymentAttemptModelDao;
import org.killbill.billing.payment.dao.PaymentDao;
import org.killbill.billing.payment.dao.PaymentModelDao;
import org.killbill.billing.payment.dao.PluginPropertySerializer;
import org.killbill.billing.payment.dao.PluginPropertySerializer.PluginPropertySerializerException;
import org.killbill.billing.payment.plugin.api.PaymentPluginApi;
import org.killbill.billing.tag.TagInternalApi;
import org.killbill.billing.util.callcontext.CallContext;
import org.killbill.billing.util.callcontext.InternalCallContextFactory;
import org.killbill.clock.Clock;
import org.killbill.commons.locker.GlobalLocker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import static org.killbill.billing.payment.logging.PaymentLoggingHelper.logEnterAPICall;
import static org.killbill.billing.payment.logging.PaymentLoggingHelper.logExitAPICall;

public class PluginControlPaymentProcessor extends ProcessorBase {

    private static final Logger log = LoggerFactory.getLogger(PluginControlPaymentProcessor.class);

    private static final Joiner JOINER = Joiner.on(", ");

    private final PluginControlPaymentAutomatonRunner pluginControlledPaymentAutomatonRunner;
    private final PaymentControlStateMachineHelper paymentControlStateMachineHelper;

    @Inject
    public PluginControlPaymentProcessor(final OSGIServiceRegistration<PaymentPluginApi> pluginRegistry,
                                         final AccountInternalApi accountInternalApi,
                                         final InvoiceInternalApi invoiceApi,
                                         final TagInternalApi tagUserApi,
                                         final PaymentDao paymentDao,
                                         final GlobalLocker locker,
                                         final InternalCallContextFactory internalCallContextFactory,
                                         final PluginControlPaymentAutomatonRunner pluginControlledPaymentAutomatonRunner,
                                         final PaymentControlStateMachineHelper paymentControlStateMachineHelper,
                                         final Clock clock) {
        super(pluginRegistry, accountInternalApi, paymentDao, tagUserApi, locker, internalCallContextFactory, invoiceApi, clock);
        this.paymentControlStateMachineHelper = paymentControlStateMachineHelper;
        this.pluginControlledPaymentAutomatonRunner = pluginControlledPaymentAutomatonRunner;
    }

    public Payment createAuthorization(final boolean isApiPayment, final Account account, final UUID paymentMethodId, @Nullable final UUID paymentId, final BigDecimal amount, final Currency currency, final String paymentExternalKey, final String transactionExternalKey,
                                       final Iterable<PluginProperty> properties, final List<String> paymentControlPluginNames, final CallContext callContext, final InternalCallContext internalCallContext) throws PaymentApiException {
        return pluginControlledPaymentAutomatonRunner.run(isApiPayment,
                                                          TransactionType.AUTHORIZE,
                                                          ControlOperation.AUTHORIZE,
                                                          account,
                                                          paymentMethodId,
                                                          paymentId,
                                                          paymentExternalKey,
                                                          transactionExternalKey,
                                                          amount,
                                                          currency,
                                                          properties,
                                                          paymentControlPluginNames,
                                                          callContext, internalCallContext);
    }

    public Payment createCapture(final boolean isApiPayment, final Account account, final UUID paymentId, final BigDecimal amount, final Currency currency,
                                 final String transactionExternalKey,
                                 final Iterable<PluginProperty> properties, final List<String> paymentControlPluginNames,
                                 final CallContext callContext, final InternalCallContext internalCallContext) throws PaymentApiException {
        return pluginControlledPaymentAutomatonRunner.run(isApiPayment,
                                                          TransactionType.CAPTURE,
                                                          ControlOperation.CAPTURE,
                                                          account,
                                                          null,
                                                          paymentId,
                                                          null,
                                                          transactionExternalKey,
                                                          amount,
                                                          currency,
                                                          properties,
                                                          paymentControlPluginNames,
                                                          callContext, internalCallContext);
    }

    public Payment createPurchase(final boolean isApiPayment, final Account account, final UUID paymentMethodId, final UUID paymentId, final BigDecimal amount, final Currency currency,
                                  final String paymentExternalKey, final String transactionExternalKey, final Iterable<PluginProperty> properties,
                                  final List<String> paymentControlPluginNames, final CallContext callContext, final InternalCallContext internalCallContext) throws PaymentApiException {
        return pluginControlledPaymentAutomatonRunner.run(isApiPayment,
                                                          TransactionType.PURCHASE,
                                                          ControlOperation.PURCHASE,
                                                          account,
                                                          paymentMethodId,
                                                          paymentId,
                                                          paymentExternalKey,
                                                          transactionExternalKey,
                                                          amount,
                                                          currency,
                                                          properties,
                                                          paymentControlPluginNames,
                                                          callContext, internalCallContext);
    }

    public Payment createVoid(final boolean isApiPayment, final Account account, final UUID paymentId, final String transactionExternalKey,
                              final Iterable<PluginProperty> properties, final List<String> paymentControlPluginNames, final CallContext callContext, final InternalCallContext internalCallContext) throws PaymentApiException {
        return pluginControlledPaymentAutomatonRunner.run(isApiPayment,
                                                          TransactionType.VOID,
                                                          ControlOperation.VOID,
                                                          account,
                                                          null,
                                                          paymentId,
                                                          null,
                                                          transactionExternalKey,
                                                          null,
                                                          null,
                                                          properties,
                                                          paymentControlPluginNames,
                                                          callContext, internalCallContext);
    }

    public Payment createRefund(final boolean isApiPayment, final Account account, final UUID paymentId, final BigDecimal amount, final Currency currency, final String transactionExternalKey,
                                final Iterable<PluginProperty> properties, final List<String> paymentControlPluginNames, final CallContext callContext, final InternalCallContext internalCallContext) throws PaymentApiException {
        return pluginControlledPaymentAutomatonRunner.run(isApiPayment,
                                                          TransactionType.REFUND,
                                                          ControlOperation.REFUND,
                                                          account,
                                                          null,
                                                          paymentId,
                                                          null,
                                                          transactionExternalKey,
                                                          amount,
                                                          currency,
                                                          properties,
                                                          paymentControlPluginNames,
                                                          callContext, internalCallContext);
    }

    public Payment createCredit(final boolean isApiPayment, final Account account, final UUID paymentMethodId, final UUID paymentId, final BigDecimal amount, final Currency currency, final String paymentExternalKey,
                                final String transactionExternalKey, final Iterable<PluginProperty> properties, final List<String> paymentControlPluginNames, final CallContext callContext, final InternalCallContext internalCallContext) throws PaymentApiException {

        return pluginControlledPaymentAutomatonRunner.run(isApiPayment,
                                                          TransactionType.CREDIT,
                                                          ControlOperation.CREDIT,
                                                          account,
                                                          paymentMethodId,
                                                          paymentId,
                                                          paymentExternalKey,
                                                          transactionExternalKey,
                                                          amount,
                                                          currency,
                                                          properties,
                                                          paymentControlPluginNames,
                                                          callContext, internalCallContext);
    }

    public Payment createChargeback(final boolean isApiPayment, final Account account, final UUID paymentId, final String transactionExternalKey, final BigDecimal amount, final Currency currency,
                                    final List<String> paymentControlPluginNames, final CallContext callContext, final InternalCallContext internalCallContext) throws PaymentApiException {
        return pluginControlledPaymentAutomatonRunner.run(isApiPayment,
                                                          TransactionType.CHARGEBACK,
                                                          ControlOperation.CHARGEBACK,
                                                          account,
                                                          null,
                                                          paymentId,
                                                          null,
                                                          transactionExternalKey,
                                                          amount,
                                                          currency,
                                                          ImmutableList.<PluginProperty>of(),
                                                          paymentControlPluginNames,
                                                          callContext, internalCallContext);
    }

    public Payment createChargebackReversal(final boolean isApiPayment, final Account account, final UUID paymentId, final String transactionExternalKey,
                                            final List<String> paymentControlPluginNames, final CallContext callContext, final InternalCallContext internalCallContext) throws PaymentApiException {
        return pluginControlledPaymentAutomatonRunner.run(isApiPayment,
                                                          TransactionType.CHARGEBACK,
                                                          ControlOperation.CHARGEBACK_REVERSAL,
                                                          account,
                                                          null,
                                                          paymentId,
                                                          null,
                                                          transactionExternalKey,
                                                          null,
                                                          null,
                                                          ImmutableList.<PluginProperty>of(),
                                                          paymentControlPluginNames,
                                                          callContext,
                                                          internalCallContext);
    }

    public void retryPaymentTransaction(final UUID attemptId, final List<String> paymentControlPluginNames, final InternalCallContext internalCallContext) {
        final PaymentAttemptModelDao attempt = paymentDao.getPaymentAttempt(attemptId, internalCallContext);
        log.info("Retrying attemptId='{}', paymentExternalKey='{}', transactionExternalKey='{}'. paymentControlPluginNames='{}'",
                 attemptId, attempt.getPaymentExternalKey(), attempt.getTransactionExternalKey(), paymentControlPluginNames);

        final PaymentModelDao paymentModelDao = paymentDao.getPaymentByExternalKey(attempt.getPaymentExternalKey(), internalCallContext);
        final UUID paymentId = paymentModelDao != null ? paymentModelDao.getId() : null;

        final CallContext callContext = buildCallContext(internalCallContext);

        final String transactionType = TransactionType.PURCHASE.name();
        Account account = null;
        Payment payment = null;
        PaymentTransaction paymentTransaction = null;
        try {
            account = accountInternalApi.getAccountById(attempt.getAccountId(), internalCallContext);
            final State state = paymentControlStateMachineHelper.getState(attempt.getStateName());
            final Iterable<PluginProperty> pluginProperties = PluginPropertySerializer.deserialize(attempt.getPluginProperties());

            logEnterAPICall(log,
                            transactionType,
                            account,
                            attempt.getPaymentMethodId(),
                            paymentId,
                            null,
                            attempt.getAmount(),
                            attempt.getCurrency(),
                            attempt.getPaymentExternalKey(),
                            attempt.getTransactionExternalKey(),
                            null,
                            paymentControlPluginNames);

            payment = pluginControlledPaymentAutomatonRunner.run(state,
                                                                 false,
                                                                 attempt.getTransactionType(),
                                                                 ControlOperation.valueOf(attempt.getTransactionType().toString()),
                                                                 account,
                                                                 attempt.getPaymentMethodId(),
                                                                 paymentId,
                                                                 attempt.getPaymentExternalKey(),
                                                                 attempt.getTransactionExternalKey(),
                                                                 attempt.getAmount(),
                                                                 attempt.getCurrency(),
                                                                 pluginProperties,
                                                                 paymentControlPluginNames,
                                                                 callContext,
                                                                 internalCallContext);

            paymentTransaction = Iterables.<PaymentTransaction>find(Lists.<PaymentTransaction>reverse(payment.getTransactions()),
                                                                    new Predicate<PaymentTransaction>() {
                                                                        @Override
                                                                        public boolean apply(final PaymentTransaction input) {
                                                                            return attempt.getTransactionExternalKey().equals(input.getExternalKey());
                                                                        }
                                                                    });
        } catch (final AccountApiException e) {
            log.warn("Failed to retry attemptId='{}', paymentControlPlugins='{}'", attemptId, toPluginNamesOnError(paymentControlPluginNames), e);
        } catch (final PaymentApiException e) {
            log.warn("Failed to retry attemptId='{}', paymentControlPlugins='{}'", attemptId, toPluginNamesOnError(paymentControlPluginNames), e);
        } catch (final PluginPropertySerializerException e) {
            log.warn("Failed to retry attemptId='{}', paymentControlPlugins='{}'", attemptId, toPluginNamesOnError(paymentControlPluginNames), e);
        } catch (final MissingEntryException e) {
            log.warn("Failed to retry attemptId='{}', paymentControlPlugins='{}'", attemptId, toPluginNamesOnError(paymentControlPluginNames), e);
        } finally {
            logExitAPICall(log,
                           transactionType,
                           account,
                           payment != null ? payment.getPaymentMethodId() : null,
                           payment != null ? payment.getId() : null,
                           paymentTransaction != null ? paymentTransaction.getId() : null,
                           paymentTransaction != null ? paymentTransaction.getProcessedAmount() : null,
                           paymentTransaction != null ? paymentTransaction.getProcessedCurrency() : null,
                           payment != null ? payment.getExternalKey() : null,
                           paymentTransaction != null ? paymentTransaction.getExternalKey() : null,
                           paymentTransaction != null ? paymentTransaction.getTransactionStatus() : null,
                           paymentControlPluginNames,
                           null);
        }
    }

    private String toPluginNamesOnError(final Collection<String> paymentControlPluginNames) {
        if (paymentControlPluginNames == null || paymentControlPluginNames.isEmpty()) {
            return "";
        }
        return JOINER.join(paymentControlPluginNames);
    }
}
