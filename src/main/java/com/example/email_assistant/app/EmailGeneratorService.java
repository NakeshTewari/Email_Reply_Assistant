package com.example.email_assistant.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class EmailGeneratorService {

    private final WebClient webClient;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    public EmailGeneratorService(WebClient.Builder webClient) {
        this.webClient = WebClient.builder().build();
    }

    public String generateEmailReply(EmailRequest emailRequest){
        String fullUrl="https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key="+geminiApiKey;

        //build the prompt
        String prompt= buildPrompt(emailRequest);
        //craft a request

        Map<String, Object> requestBody = Map.of(
                "contents", new Object[]{
                        Map.of( "parts", new Object[]{
                                Map.of("text", prompt)
                        })
                }
        );

        //Extract response and Return response
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT); // Optional for pretty print

        try {
            String json = mapper.writeValueAsString(requestBody);
            System.out.println("Request JSON:\n" + json);
        } catch (Exception e) {
            System.out.println("Error while printing JSON: " + e.getMessage());
        }

        String response="";

        try {
            fullUrl=fullUrl.substring(0, fullUrl.length()-1);
            System.out.println(fullUrl);
            response = webClient.post()
                    .uri(fullUrl)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            System.out.println(response);
        } catch (Exception e) {
            System.out.println("WEBCLIENT ERROR: " +e.getMessage());
        }
        return extractResponseContent(response);

    }

    private String extractResponseContent(String response) {
        try{
            ObjectMapper mapper=new ObjectMapper();
            JsonNode rootNode= mapper.readTree(response);


            return rootNode.path("candidates")
                    .get(0)
                    .get("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();

        }catch (Exception e){
            return "Error processing request: "+ e.getMessage();
        }
    }

    public String buildPrompt(EmailRequest emailRequest){
        StringBuilder prompt= new StringBuilder();
        prompt.append("Generate a professional email in detail and reply for the following email content.Please don't generate a subject line. ");

        if(emailRequest.getTone()!=null && !emailRequest.getTone().isEmpty()){
            prompt.append("Use a ").append(emailRequest.getTone()).append(" tone.");
        }

        prompt.append("\nOriginal email: \n").append(emailRequest.getEmailContent());
        return prompt.toString();
    }
}
