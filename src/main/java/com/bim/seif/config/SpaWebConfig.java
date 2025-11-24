package com.bim.seif.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SpaWebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/{path:^(?!.*\\.).*$}")
                .setViewName("forward:/ext/index.html");
        registry.addViewController("/**/{path:^(?!.*\\.).*$}")
                .setViewName("forward:/ext/index.html");
    }
}
