package com.superdoc.api.controller;

import jakarta.servlet.ServletContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

public class TestAuthHelper {

   
    public static RequestPostProcessor withEmail(String email) {
        return mockRequest -> {
            var auth = new UsernamePasswordAuthenticationToken(
                    email, 
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_USER"))
            );
            
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(auth);
            
            if (mockRequest instanceof MockHttpServletRequest) {
                MockHttpServletRequest mock = (MockHttpServletRequest) mockRequest;
                mock.getSession(true).setAttribute(
                        HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                        securityContext
                );
            }
            
            return SecurityMockMvcRequestPostProcessors.authentication(auth).postProcessRequest(mockRequest);
        };
    }
}

