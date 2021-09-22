package cnj.kubernetescontroller;

import io.kubernetes.client.extended.controller.Controller;
import io.kubernetes.client.extended.controller.builder.ControllerBuilder;
import io.kubernetes.client.extended.controller.reconciler.Reconciler;
import io.kubernetes.client.extended.controller.reconciler.Result;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.informer.cache.Lister;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1NodeList;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.util.generic.GenericKubernetesApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.Objects;

@SpringBootApplication
public class KubernetesControllerApplication {

    public static void main(String[] args) {
        SpringApplication.run(KubernetesControllerApplication.class, args);
    }

    @Bean
    SharedIndexInformer<V1Node> nodeSharedIndexInformer(ApiClient apiClient, SharedInformerFactory sharedInformerFactory) {
        var genericKubernetesApi =
                new GenericKubernetesApi<>(V1Node.class, V1NodeList.class, "", "v1", "nodes", apiClient);
        return sharedInformerFactory
                .sharedIndexInformerFor(genericKubernetesApi, V1Node.class, 0);
    }


    @Bean
    SharedIndexInformer<V1Pod> podSharedIndexInformer(ApiClient apiClient, SharedInformerFactory sharedInformerFactory) {
        var genericKubernetesApi =
                new GenericKubernetesApi<>(V1Pod.class, V1PodList.class, "", "v1", "pods", apiClient);
        return sharedInformerFactory
                .sharedIndexInformerFor(genericKubernetesApi, V1Pod.class, 0);
    }

    @Bean
    Lister<V1Node> nodeLister(SharedIndexInformer<V1Node> informer) {
        return new Lister<>(informer.getIndexer());
    }

    @Bean
    Lister<V1Pod> podLister(SharedIndexInformer<V1Pod> informer) {
        return new Lister<>(informer.getIndexer());
    }

    @Bean
    Reconciler reconciler(
            @Value("${cnj.controller.namespace:cnj}") String namespace,
            Lister<V1Node> nodeLister, Lister<V1Pod> podLister) {
        return request -> {
            var node = nodeLister.get(request.getName());
            System.out.println("node: " + node.getMetadata().getName());
            podLister
                    .namespace(namespace)
                    .list()
                    .stream()
                    .map(pod -> Objects.requireNonNull(pod.getMetadata()).getName())
                    .forEach(podName -> System.out.println("pod name: " + podName));

            return new Result(false);
        };
    }

    @Bean
    Controller controller(
            SharedIndexInformer<V1Pod> podSharedIndexInformer,
            SharedIndexInformer<V1Node> nodeSharedIndexInformer,
            Reconciler reconciler,
            SharedInformerFactory sif) {

        return ControllerBuilder
                .defaultBuilder(sif)
                .watch(q -> ControllerBuilder.controllerWatchBuilder(V1Node.class, q).build())
                .withReadyFunc(() -> podSharedIndexInformer.hasSynced() &&
                                     nodeSharedIndexInformer.hasSynced())
                .withReconciler(reconciler)
                .withName("booternetesController")
                .build();
    }

    @Bean
    CommandLineRunner go(SharedInformerFactory sif, Controller controller) {
        return args -> {
            sif.startAllRegisteredInformers();
            controller.run();
        };
    }

}
