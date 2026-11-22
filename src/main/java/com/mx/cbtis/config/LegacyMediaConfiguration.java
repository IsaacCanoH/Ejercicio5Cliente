package com.mx.cbtis.config;

import java.io.IOException;
import java.util.Properties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class LegacyMediaConfiguration implements WebMvcConfigurer {
    private final Properties aliases;

    public LegacyMediaConfiguration() throws IOException {
        aliases = PropertiesLoaderUtils.loadAllProperties("legacy-media.properties");
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        aliases.forEach((oldPath, newPath) -> registry
            .addRedirectViewController(oldPath.toString(), newPath.toString())
            .setStatusCode(HttpStatus.PERMANENT_REDIRECT));
    }
}
