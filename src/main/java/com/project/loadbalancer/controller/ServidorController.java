package com.project.loadbalancer.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ServidorController {

    @Value("${INSTANCE_ID:app-local}")
    private String instanceId;

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> paginaInicial() {

        String cor = switch (instanceId) {
            case "app-01" -> "#dcecff";
            case "app-02" -> "#d9f4e3";
            case "app-03" -> "#ffe8cf";
            case "app-04" -> "#f0ddff";
            default -> "#eeeeee";
        };

        String html = """
            <!DOCTYPE html>
            <html lang="pt-BR">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport"
                      content="width=device-width, initial-scale=1">
                <title>Load Balancer Demo</title>
            </head>
            <body style="background-color: %s;
                         font-family: Arial;
                         text-align: center;
                         padding: 60px;">
                <h1>Load Balancer Demo</h1>
                <h2>Respondido por: %s</h2>
                <p>Esta requisição foi atendida por esta instância.</p>
                <p>Horário da resposta: %s</p>
            </body>
            </html>
            """.formatted(cor, instanceId,
                    java.time.LocalDateTime.now());

        return ResponseEntity.ok()
                .header("X-Served-By", instanceId)
                .body(html);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("UP");
    }
}
