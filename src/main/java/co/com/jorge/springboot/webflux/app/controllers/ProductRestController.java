package co.com.jorge.springboot.webflux.app.controllers;

import co.com.jorge.springboot.webflux.app.models.repository.ProductRepository;
import co.com.jorge.springboot.webflux.app.models.documents.Product;
import co.com.jorge.springboot.webflux.app.models.services.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/products")
public class ProductRestController {


    private ProductService productService;

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    public ProductRestController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/list")
    public Flux<Product> list(){
        return productService.findAllWithNameUpperCaseRepeat();
    }

    @GetMapping
    public Mono<ResponseEntity<Flux<Product>>>listResponseEntity(){
        return productService.findAllWithNameUpperCaseRepeat()
                .collectList()
                .map(products -> ResponseEntity.ok(Flux.fromIterable(products)))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<Product>>  findById(@PathVariable String id){
        return productService.findById(id)
                .map(prod -> ResponseEntity.ok(prod.setName(prod.getName().toUpperCase())))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
