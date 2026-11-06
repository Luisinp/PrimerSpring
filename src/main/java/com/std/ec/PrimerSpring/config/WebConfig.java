package com.std.ec.PrimerSpring.config;
 
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
 
import com.std.ec.PrimerSpring.service.ImagenService;
 
@Configuration
public class WebConfig implements WebMvcConfigurer {
 
    private final ImagenService imagenService;
 
    public WebConfig(ImagenService imagenService) {
        this.imagenService = imagenService;
    }
 
    /** Publica la carpeta de fotos en la dirección /media/paquetes/nombre-del-archivo. */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String ubicacion = imagenService.getCarpeta().toUri().toString();
        if (!ubicacion.endsWith("/")) {
            ubicacion += "/";
        }
        registry.addResourceHandler("/media/paquetes/**").addResourceLocations(ubicacion);
    }
 
    /** Al entrar a http://localhost:8082/ se abre el catálogo. */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/", "/home.xhtml");
    }
}
 