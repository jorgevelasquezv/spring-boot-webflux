package co.com.jorge.springboot.webflux.app.controllers;


import co.com.jorge.springboot.webflux.app.models.documents.Category;
import co.com.jorge.springboot.webflux.app.models.repository.ProductRepository;
import co.com.jorge.springboot.webflux.app.models.documents.Product;
import co.com.jorge.springboot.webflux.app.models.services.ProductService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;
import org.thymeleaf.spring6.context.webflux.ReactiveDataDriverContextVariable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

@Controller
@SessionAttributes("product")
public class ProductController {

    private ProductService productService;

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    @Value("${config.uploads.path}")
    private String path;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @ModelAttribute("categories")
    public Flux<Category> categories() {
        return productService.findAllCategory();
    }

    @GetMapping("/list-chunked")
    public String listChunked(Model model) {
        Flux<Product> products = productService.findAllWithNameUpperCaseRepeat();
        model.addAttribute("title", "Listado de productos");
        model.addAttribute("products", new ReactiveDataDriverContextVariable(products, 10));
        return "list-chunked";
    }

    @GetMapping("/list-full")
    public String listFull(Model model) {
        Flux<Product> products = productService.findAllWithNameUpperCaseRepeat();
        model.addAttribute("title", "Listado de productos");
        model.addAttribute("products", new ReactiveDataDriverContextVariable(products, 10));
        return "list";
    }

    @GetMapping("/list-data-driver")
    public String listDataDriver(Model model) {
        Flux<Product> products = productService.findAllWithNameUpperCaseRepeat().delayElements(Duration.ofSeconds(1));

        products.subscribe(product -> log.info(product.toString()));

        model.addAttribute("title", "Listado de productos");
        model.addAttribute("products", new ReactiveDataDriverContextVariable(products, 2));
        return "list";
    }

    @GetMapping({"/list", "/"})
    public String list(Model model) {
        Flux<Product> products = productService.findAllWithNameUpperCase();

        products.subscribe(product -> log.info(product.toString()));

        model.addAttribute("title", "Listado de productos");
        model.addAttribute("products", products);
        return "list";
    }

    @GetMapping("/form")
    public Mono<String> create(Model model) {
        model.addAttribute("title", "Formulario de productos");
        model.addAttribute("textButton", "Guardar");
        model.addAttribute("product", new Product());
        return Mono.just("form");
    }

    @GetMapping("/form/{id}")
    public Mono<String> edit(@PathVariable String id, Model model) {
        Mono<Product> productMono = productService.findById(id)
                .doOnNext(product -> log.info(product.getName()))
                .defaultIfEmpty(new Product());
        model.addAttribute("title", "Editar Producto");
        model.addAttribute("textButton", "Guardar");
        model.addAttribute("product", productMono);
        return Mono.just("/form");
    }

    @GetMapping("/form-2/{id}")
    public Mono<String> editTwo(@PathVariable String id, Model model) {
        return productService.findById(id)
                .doOnNext(product -> {
                    model.addAttribute("title", "Editar Producto");
                    model.addAttribute("textButton", "Guardar");
                    model.addAttribute("product", product);
                })
                .defaultIfEmpty(new Product())
                .flatMap(product -> {
                    if (product.getId() == null) {
                        return Mono.error(new InterruptedException("No existe el producto"));
                    } else {
                        return Mono.just(product);
                    }
                })
                .then(Mono.just("/form"))
                .onErrorResume(error -> Mono.just("redirect:/list?error=No+existe+el+producto"));
    }

    @PostMapping(value = "/form", consumes = {"multipart/form-data"})
    public Mono<String> save(@Valid Product product, BindingResult result, SessionStatus status, Model model, @RequestPart FilePart file) {
        log.info(file.filename());
        if (result.hasErrors()) {
            if (product.getId() == null) {
                model.addAttribute("title", "Formulario de productos");
            } else {
                model.addAttribute("title", "Editar Producto");
            }
            model.addAttribute("product", product);
            model.addAttribute("textButton", "Guardar");
            return Mono.just("/form");
        }
        status.setComplete();
        if (product.getCreateAt() == null) product.setCreateAt(new Date());
        if (!file.filename().isEmpty()) product.setPhoto(UUID.randomUUID() + "-" +file.filename()
                .replace(" ", "")
                .replace(":", "")
                .replace("\\", ""));


        Mono<Category> category = productService.findCategoryById(product.getCategory().getId());

        return category.flatMap(catego -> {
                    product.setCategory(catego);
                    return productService.save(product);
                })
                .doOnNext(prod -> log.info(product.toString()))
                .flatMap(prod -> {
                    if (!file.filename().isEmpty()) return file
                            .transferTo(
                                    new File(path + product.getPhoto())
                            );
                    return Mono.empty();
                })
                .thenReturn("redirect:/list?success=Producto+guardado+con+exito");
    }

    @GetMapping("/delete/{id}")
    public Mono<String> delete(@PathVariable String id) {
        return productService.findById(id)
                .defaultIfEmpty(new Product())
                .flatMap(product -> {
                    if (product.getId() == null) {
                        return Mono.error(new InterruptedException("No existe el producto"));
                    } else {
                        return productService.delete(product);
                    }
                })
                .then(Mono.just("redirect:/list?success=Producto+eliminado+con+exito"))
                .onErrorResume(error -> Mono.just("redirect:/list?error=No+existe+el+producto"));
    }

    @GetMapping("/see/{id}")
    public Mono<String> see(Model model, @PathVariable String id){
        return productService.findById(id)
                .doOnNext(product -> {
                    model.addAttribute("product", product);
                    model.addAttribute("title", "Detalle de Producto");
                }).switchIfEmpty(Mono.just(new Product()))
                .flatMap(product -> {
                    if (product.getId() == null) {
                        return Mono.error(new InterruptedException("No existe el producto"));
                    } else {
                        return Mono.just(product);
                    }
                })
                .then(Mono.just("see"))
                .onErrorResume(error -> Mono.just("redirect:/list?error=No+existe+el+producto"));
    }

    @GetMapping("/uploads/img/{photoName:.+}")
    public Mono<ResponseEntity<Resource>> seePhoto(@PathVariable String photoName) throws MalformedURLException {
        Path pathPhoto = Paths.get(path).resolve(photoName).toAbsolutePath();
        Resource image = new UrlResource(pathPhoto.toUri());

        return Mono.just(
                ResponseEntity
                        .ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename\"" + image.getFilename() + "\"")
                        .body(image));
    }

}
