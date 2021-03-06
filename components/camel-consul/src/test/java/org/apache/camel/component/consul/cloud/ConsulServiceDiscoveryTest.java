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
package org.apache.camel.component.consul.cloud;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import com.orbitz.consul.AgentClient;
import com.orbitz.consul.model.agent.ImmutableRegCheck;
import com.orbitz.consul.model.agent.ImmutableRegistration;
import com.orbitz.consul.model.agent.Registration;
import org.apache.camel.cloud.ServiceDefinition;
import org.apache.camel.cloud.ServiceDiscovery;
import org.apache.camel.component.consul.ConsulConfiguration;
import org.apache.camel.component.consul.support.ConsulTestSupport;
import org.junit.Test;
import org.springframework.util.SocketUtils;

public class ConsulServiceDiscoveryTest extends ConsulTestSupport {
    private AgentClient client;
    private List<Registration> registrations;

    @Override
    public boolean isUseRouteBuilder() {
        return false;
    }

    @Override
    protected void doPreSetup() throws Exception {
        client = getConsul().agentClient();
        registrations = new ArrayList<>(3);

        for (int i = 0; i < 6; i++) {
            final boolean healty = ThreadLocalRandom.current().nextBoolean();
            final int port = SocketUtils.findAvailableTcpPort();

            Registration.RegCheck c = ImmutableRegCheck.builder()
                .ttl("1m")
                .status(healty ? "passing" : "critical")
                .build();

            Registration r = ImmutableRegistration.builder()
                .id("service-" + i)
                .name("my-service")
                .address("127.0.0.1")
                .addTags("a-tag")
                .addTags("key1=value1")
                .addTags("key2=value2")
                .addTags("healthy=" + healty)
                .port(port)
                .check(c)
                .build();

            client.register(r);
            registrations.add(r);
        }
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();

        registrations.forEach(r -> client.deregister(r.getId()));
    }

    // *************************************************************************
    // Test
    // *************************************************************************

    @Test
    public void testServiceDiscovery() throws Exception {
        ConsulConfiguration configuration = new ConsulConfiguration();
        configuration.setUrl(consulUrl());

        ServiceDiscovery discovery = new ConsulServiceDiscovery(configuration);

        List<ServiceDefinition> services = discovery.getServices("my-service");
        assertNotNull(services);
        assertEquals(6, services.size());

        for (ServiceDefinition service : services) {
            assertFalse(service.getMetadata().isEmpty());
            assertTrue(service.getMetadata().containsKey("service_name"));
            assertTrue(service.getMetadata().containsKey("service_id"));
            assertTrue(service.getMetadata().containsKey("a-tag"));
            assertTrue(service.getMetadata().containsKey("key1"));
            assertTrue(service.getMetadata().containsKey("key2"));
            assertEquals("" + service.getHealth().isHealthy() , service.getMetadata().get("healthy"));
        }
    }
}
