package io.github.jython234.nectar.server.config;

import io.github.jython234.nectar.server.NectarServerConfiguration;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Controller;

@Controller
public class ServletConfig {
    @Bean
    public EmbeddedServletContainerCustomizer containerCustomizer() {
        return (container -> container.setPort(NectarServerConfiguration.getInstance().getBindPort()));
    }
}
