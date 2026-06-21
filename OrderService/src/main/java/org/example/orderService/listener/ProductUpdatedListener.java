package org.example.orderService.listener;

import org.example.common.event.ProductUpdatedEvent;
import org.example.orderService.config.RabbitMQConfig;
import org.example.orderService.model.ProductPriceCache;
import org.example.orderService.repository.ProductPriceCacheRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ProductUpdatedListener {

    private static final Logger log = LoggerFactory.getLogger(ProductUpdatedListener.class);
    private final ProductPriceCacheRepository productPriceCacheRepository;

    public ProductUpdatedListener(ProductPriceCacheRepository productPriceCacheRepository) {
        this.productPriceCacheRepository = productPriceCacheRepository;
    }

    @RabbitListener(queues = RabbitMQConfig.PRODUCT_UPDATED_QUEUE)
    public void handleProductUpdated(ProductUpdatedEvent event) {
        log.info("Received ProductUpdatedEvent for product id={}", event.getId());
        ProductPriceCache cache = productPriceCacheRepository.findById(event.getId())
                .orElse(new ProductPriceCache());
        cache.setProductId(event.getId());
        cache.setName(event.getName());
        cache.setPrice(event.getPrice());
        cache.setUpdatedAt(java.time.LocalDateTime.now());
        productPriceCacheRepository.save(cache);
    }
}
