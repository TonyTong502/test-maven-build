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
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockComponent;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

public class RetryComponentComponentTest extends CamelTestSupport {
    @Test
    public void testSuccessCase() throws Exception {
        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedMinimumMessageCount(1);

        MockEndpoint step1 = getMockEndpoint("sap-processdirect:ipg?address=url1");
        AtomicInteger count = new AtomicInteger();
        step1.whenAnyExchangeReceived(exchange -> {
            count.addAndGet(1);
        });
        template.sendBody("direct:start", "haha");

        assertMockEndpointsSatisfied();
        Assert.assertEquals(1, count.get());
    }
    @Test
    public void testErrorCase() throws Exception {
        Exchange start = getMandatoryEndpoint("direct:start").createExchange();
        start.getIn().setBody("haha");

        MockEndpoint step1 = getMockEndpoint("sap-processdirect:ipg?address=url1");
        AtomicInteger count = new AtomicInteger();
        step1.whenAnyExchangeReceived(exchange -> {
            count.addAndGet(1);
            throw new Exception("dummy exception");
        });

        Exchange result = template.send("direct:start", start);
        assertTrue(result.isFailed());
        assertMockEndpointsSatisfied();
        Assert.assertEquals(5, count.get());
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            public void configure() {
                from("direct:start")
                  .to("ipg-retry://ipg?address=url1&count=5&interval=5")
                  .to("mock:result");
            }
        };
    }

    @Override
    protected CamelContext createCamelContext() throws Exception {
        CamelContext camelContext = super.createCamelContext();
        camelContext.addComponent("sap-processdirect", new MockComponent());
        return camelContext;
    }
}
