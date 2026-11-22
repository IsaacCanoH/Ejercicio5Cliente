package com.mx.cbtis.config;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class LegacyMediaConfigurationTest {
    @Autowired MockMvc mvc;

    @Test
    void everyLegacyUrlRedirectsToAnExistingPackagedAsset() throws Exception {
        var aliases = PropertiesLoaderUtils.loadAllProperties("legacy-media.properties");
        assertTrue(aliases.containsKey("/Images/animacion1.gif"));
        for (String oldPath : aliases.stringPropertyNames()) {
            String target = aliases.getProperty(oldPath);
            assertTrue(new ClassPathResource("static" + target).exists(), target);
            mvc.perform(get(oldPath)).andExpect(status().isPermanentRedirect())
                .andExpect(redirectedUrl(target));
        }
    }

    @Test
    void videoSupportsByteRangesForPlaybackAndSeeking() throws Exception {
        mvc.perform(get("/Images/fondoLogin.mp4").header("Range", "bytes=0-127"))
            .andExpect(status().isPartialContent());
    }
}
