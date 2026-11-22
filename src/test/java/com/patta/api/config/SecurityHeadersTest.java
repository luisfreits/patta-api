package com.patta.api.config;

import com.google.firebase.auth.FirebaseAuth;
import com.patta.api.service.PapelUsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.mock;

@WebMvcTest(controllers = SecurityHeadersTest.FrameTestController.class)
@Import({SecurityConfig.class, SecurityHeadersTest.TestBeans.class})
class SecurityHeadersTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void respostasDaApiNegamIncorporacaoEmFrames() throws Exception {
        mockMvc.perform(get("/frame-protection-test"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Content-Security-Policy",
                        containsString("frame-ancestors 'none'")));
    }

    @RestController
    static class FrameTestController {
        @GetMapping("/frame-protection-test")
        String response() {
            return "ok";
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestBeans {
        @Bean
        FirebaseTokenFilter firebaseTokenFilter(ObjectMapper objectMapper) {
            return new FirebaseTokenFilter(mock(FirebaseAuth.class), objectMapper, new PapelUsuarioService(""));
        }
    }
}
