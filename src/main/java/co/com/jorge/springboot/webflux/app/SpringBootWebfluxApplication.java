package co.com.jorge.springboot.webflux.app;

import co.com.jorge.springboot.webflux.app.models.documents.Category;
import co.com.jorge.springboot.webflux.app.models.documents.Product;
import co.com.jorge.springboot.webflux.app.models.services.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import reactor.core.publisher.Flux;

import java.util.Date;

@SpringBootApplication
public class SpringBootWebfluxApplication implements CommandLineRunner {

	private ProductService service;

	private static final Logger log = LoggerFactory.getLogger(SpringBootWebfluxApplication.class);

	private ReactiveMongoTemplate mongoTemplate;

	public SpringBootWebfluxApplication(ProductService service, ReactiveMongoTemplate mongoTemplate) {
		this.service = service;
		this.mongoTemplate = mongoTemplate;
	}

	public static void main(String[] args) {
		SpringApplication.run(SpringBootWebfluxApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {

		mongoTemplate.dropCollection("productos").subscribe();
		mongoTemplate.dropCollection("categorias").subscribe();
		Category electronica = new Category("Electronica");
		Category computacion = new Category("Computación");
		Category deporte = new Category("Deporte");
		Category muebles = new Category("Muebles");

		Flux.just(electronica, computacion, muebles, deporte)
				.flatMap( service::saveCategory)
				.doOnNext(category -> log.info(category.toString()))
				.thenMany(Flux.just(
								new Product("TV Sony LCD 43", 500.0, electronica),
								new Product("TV LG LCD 43", 482.0, electronica),
								new Product("TV Kalley LCD 43", 398.0, electronica),
								new Product("Monitor Gamer LG 24", 425.0, computacion),
								new Product("Silla Gamer Kangu", 212.0, muebles),
								new Product("Teclado Gamer K", 120.0, computacion),
								new Product("Mouse Gamer G", 113.0, computacion)
						)
						.flatMap( product -> {
							product.setCreateAt(new Date());
							return service.save(product);
						}))
				.subscribe(product -> log.info(product.toString()));


	}
}
