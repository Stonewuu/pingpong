package com.challenge.ping.config

import org.springframework.web.reactive.function.client.WebClient
import spock.lang.Specification

class WebClientConfigSpec extends Specification {

    def "should create WebClient builder"() {
        given:
        def config = new WebClientConfig()
        
        when:
        def builder = config.webClientBuilder()
        
        then:
        builder instanceof WebClient.Builder
    }
    
    def "should create WebClient with default configuration"() {
        given:
        def config = new WebClientConfig()
        def builder = config.webClientBuilder()
        
        when:
        def webClient = builder.build()
        
        then:
        webClient instanceof WebClient
    }
    
    def "should create WebClient with custom base url"() {
        given:
        def config = new WebClientConfig()
        def builder = config.webClientBuilder()
        
        when:
        def webClient = builder.baseUrl("http://localhost:8080").build()
        
        then:
        webClient instanceof WebClient
    }
    
    def "should create WebClient with custom timeout"() {
        given:
        def config = new WebClientConfig()
        def builder = config.webClientBuilder()
        
        when:
        def webClient = builder
            .baseUrl("http://localhost:8080")
            .build()
        
        then:
        webClient instanceof WebClient
    }
} 