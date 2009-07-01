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
package org.apache.camel.component.bean;

import javax.naming.Context;

import org.apache.camel.ContextTestSupport;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.util.jndi.JndiContext;

/**
 * @version $Revision$
 */
public class BeanWithMethodHeaderTest extends ContextTestSupport {

    private MyBean bean;

    public void testEcho() throws Exception {
        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedBodiesReceived("echo Hello World");
        
        template.sendBody("direct:echo", "Hello World");

        assertMockEndpointsSatisfied();
        assertNull("There should no Bean_METHOD_NAME header",
                   mock.getExchanges().get(0).getIn().getHeader(BeanProcessor.METHOD_NAME));
    }
    
    public void testEchoWithMethodHeaderHi() throws Exception {
        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedBodiesReceived("echo Hello World");
        mock.expectedHeaderReceived(BeanProcessor.METHOD_NAME, "hi");

        template.sendBodyAndHeader("direct:echo", ExchangePattern.InOut, "Hello World", BeanProcessor.METHOD_NAME, "hi");

        assertMockEndpointsSatisfied();
    }
    
    public void testMixedBeanEndpoints() throws Exception {
        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedBodiesReceived("hi echo Hello World");
        mock.expectedHeaderReceived(BeanProcessor.METHOD_NAME, "hi");

        template.sendBodyAndHeader("direct:mixed", ExchangePattern.InOut, "Hello World", BeanProcessor.METHOD_NAME, "hi");

        assertMockEndpointsSatisfied();
    }

    public void testHi() throws Exception {
        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedBodiesReceived("hi Hello World");

        template.sendBody("direct:hi", "Hello World");

        assertMockEndpointsSatisfied();
    }

   
    protected Context createJndiContext() throws Exception {
        JndiContext answer = new JndiContext();
        bean = new MyBean();
        answer.bind("myBean", bean);
        return answer;
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:echo").beanRef("myBean", "echo").to("mock:result");

                from("direct:hi").beanRef("myBean", "hi").to("mock:result");
                
                from("direct:mixed").beanRef("myBean", "echo").beanRef("myBean", "hi").to("mock:result");
             
            }
        };
    }

    public static class MyBean {

        public String hi(String s) {
            return "hi " + s;
        }

        public String echo(String s) {
            return "echo " + s;
        }
    }

}