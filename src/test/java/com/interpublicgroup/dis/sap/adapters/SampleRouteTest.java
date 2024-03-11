package com.interpublicgroup.dis.sap.adapters;

import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Assert;
import org.junit.Test;

public class SampleRouteTest extends CamelTestSupport {

    @Test
    public void testSample() throws Exception {
        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedMinimumMessageCount(1);

        MockEndpoint step1 = getMockEndpoint("mock:step1");
        step1.whenAnyExchangeReceived( exchange -> {
            assertEquals("haha", exchange.getIn().getBody(String.class));
            exchange.getIn().setBody("step1");
        });

        MockEndpoint step2 = getMockEndpoint("mock:step2");
        step2.whenAnyExchangeReceived( exchange -> {
            assertEquals("step1", exchange.getIn().getBody(String.class));
            exchange.getIn().setBody("step2");
        });

        template.sendBody("direct:start", "haha");
        String result = mock.getExchanges().get(0).getIn().getBody(String.class);
        Assert.assertEquals("step2",result);

        assertMockEndpointsSatisfied();
    }

    @Override
    protected RoutesBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            public void configure() {
                from("direct:start")
                        .to("mock:step1")
                        .to("mock:step2")
                        .to("mock:result");
            }
        };
    }
}
