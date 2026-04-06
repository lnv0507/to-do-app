package vn.com.anhemsoftware.license_app.service;

/**
 * Abstraction layer for Spring AI inference.
 */
public interface AiInferenceService {

    String ask(String userMessage);

    String ask(String userMessage, String conversationId);

}
