package com.legally.config;

/** Qualifiers for ordered LLM provider lists ({@code @Bean(name = …)}). */
public final class LlmChainQualifiers {

    public static final String LEGAL = "legalLlmChain";
    public static final String CONTACT = "contactLlmChain";

    private LlmChainQualifiers() {
    }
}
