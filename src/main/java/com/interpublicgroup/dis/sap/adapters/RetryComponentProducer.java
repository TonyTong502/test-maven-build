/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.interpublicgroup.dis.sap.adapters;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.impl.DefaultProducer;
import org.apache.camel.builder.ExchangeBuilder;
import org.apache.camel.builder.SimpleBuilder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The www.Sample.com producer.
 */
public class RetryComponentProducer extends DefaultProducer {
    private static final transient Logger LOG = LoggerFactory.getLogger(RetryComponentProducer.class);
    private RetryComponentEndpoint endpoint;

	public RetryComponentProducer(RetryComponentEndpoint endpoint) {
        super(endpoint);
        this.endpoint = endpoint;
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
    }

    public void process(final Exchange exchange) throws Exception {
        String address = getValue(endpoint.getAddress(), exchange);
        String url = getSAPAddress(address);
        int count = endpoint.getCount();
        long interval = (long) endpoint.getInterval() * 1000;
        AtomicInteger counter = new AtomicInteger(1);
        Exchange response;
        while(
                (response = process(exchange.getContext(), url, exchange.getIn())).isFailed()
                        && counter.get() < count
        ) {
            counter.addAndGet(1);
            Thread.sleep(interval);
        }
        if(response.isFailed())
            exchange.setException(response.getException());
    }

    private Exchange process(CamelContext context, String url, Message message) {
        return process(context, url, message.getBody(), message.getHeaders());
    }

    private Exchange process(CamelContext context, String url, Object body, Map<String, Object> headers){
        ExchangeBuilder builder = ExchangeBuilder.anExchange(context);
        if(body != null){
            builder.withBody(body);
        }
        if(headers!=null && !headers.isEmpty()){
            for(Map.Entry<String, Object> entry: headers.entrySet()){
                builder.withHeader(entry.getKey(), entry.getValue());
            }
        }
        Exchange request = builder.build();
        ProducerTemplate producer = context.createProducerTemplate();
        return producer.send(url, request);
    }

    private String getValue(String source, Exchange exchange){
        if(source == null || source.trim().isEmpty())
            return "";
        Pattern p = Pattern.compile("\\$\\{.*?}");
        Matcher m = p.matcher(source);
        while(m.find()){
            String expression = m.group();
            String evaluated = evaluateSimple(expression,exchange);
            if(evaluated == null){
                throw new RuntimeException("expression:" + expression + " cannot be null");
            }
            source = source.replace(expression, evaluated);
        }
        return source;
    }
    private String evaluateSimple(String expression, Exchange exchange){
        return SimpleBuilder.simple(expression).evaluate(exchange, String.class);
    }

    private String getSAPAddress(String address){
        if(StringUtils.isNotEmpty(address))
            return String.format("sap-processdirect:ipg?address=%s", address);
        else
            throw new RuntimeException("address is empty");
    }
}
