package org.acme;

import io.quarkus.arc.Arc;
import io.smallrye.mutiny.TimeoutException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.groups.UniCreate;
import io.smallrye.stork.api.ServiceInstance;
import io.smallrye.stork.impl.CachingServiceDiscovery;
import io.smallrye.stork.impl.DefaultServiceInstance;
import io.smallrye.stork.utils.ServiceInstanceIds;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MyServiceDiscovery extends CachingServiceDiscovery {

    Logger log = Logger.getLogger(MyServiceDiscovery.class);

    MyServiceDiscoveryProviderConfiguration config;

    public MyServiceDiscovery(MyServiceDiscoveryProviderConfiguration config) {
        super("600S");
        this.config = config;
    }

    @Override
    public Uni<List<ServiceInstance>> fetchNewServiceInstances(List<ServiceInstance> previousInstances) {
        DiscoveryClient discoveryClient = Arc.container().instance(DiscoveryClientProvider.class).get().getClient();
        long timeout = previousInstances.isEmpty() ? 10000L : 100L;
        String addresses = discoveryClient.discovery(config.getApp()).await().atMost(Duration.ofMillis(timeout));
        UniCreate uni = Uni.createFrom();
        try {
            return uni.item(map(addresses));
        } catch (TimeoutException e) {
            return uni.failure(e);
        }
    }

    private List<ServiceInstance> map(String list) {
        return Arrays.stream(list.split(",")).map(this::createInstance).collect(Collectors.toList());
    }

    private ServiceInstance createInstance(String hostPort) {
        String[] split = hostPort.split(":");
        String host = split[0];
        int port = Integer.parseInt(split[1]);
        return new DefaultServiceInstance(ServiceInstanceIds.next(), host, port, false);
    }

    // @Override
    public Uni<List<ServiceInstance>> __fetchNewServiceInstances(List<ServiceInstance> previousInstances) {
        DiscoveryClient discoveryClient = Arc.container().instance(DiscoveryClientProvider.class).get().getClient();
        long timeout = previousInstances.isEmpty() ? 10000L : 100L;
        // how do I set the timeout??
        return discoveryClient.discovery(config.getApp()).onItem().transform(this::map);
    }

    // @Override
//    public Uni<List<ServiceInstance>> fetchNewServiceInstances(List<ServiceInstance> previousInstances) {
//
//        log.info("fetching new instances");
//        log.info("old instances: " + previousInstances);
//
//        DiscoveryClient discoveryClient = Arc.container().instance(DiscoveryClientProvider.class).get().getClient();
//        DownDiscoveryClient downDiscoveryClient = Arc.container().instance(DownDiscoveryClientProvider.class).get().getClient();
//        DiscoveryParams discoveryParams = Arc.container().instance(DiscoveryParams.class).get();
//
//        boolean awaitStyle = true;
//
//        if (awaitStyle) {
//            long timeout = previousInstances.isEmpty() ? 10000L : 100L;
//            String addresses;
//            try {
//                if (discoveryParams.down) {
//                    addresses = downDiscoveryClient.discovery(discoveryParams.delay, discoveryParams.error, discoveryParams.addresses)
//                            .await().atMost(Duration.ofMillis(timeout));
//                } else {
//                    addresses = discoveryClient.discovery(discoveryParams.delay, discoveryParams.error, discoveryParams.addresses)
//                            .await().atMost(Duration.ofMillis(timeout));
//                }
//                return Uni.createFrom().item(map(addresses));
//            } catch (TimeoutException e) {
//                return Uni.createFrom().failure(e);
//            }
//        } else {
//            Uni<String> discovery = discoveryParams.down
//                    ? downDiscoveryClient.discovery(discoveryParams.delay, discoveryParams.error, discoveryParams.addresses)
//                    : discoveryClient.discovery(discoveryParams.delay, discoveryParams.error, discoveryParams.addresses);
//            Uni<List<ServiceInstance>> result = discovery.onItem().transform(this::map);
//            return result;
//        }
//    }


}
