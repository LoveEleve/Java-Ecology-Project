package com.loveeleve.ecology.arthas.controller;

import com.loveeleve.ecology.arthas.service.DiagnosticService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/arthas")
public class DiagnosticController {

    private final DiagnosticService service;

    public DiagnosticController(DiagnosticService service) {
        this.service = service;
    }

    @GetMapping("/slow")
    public Map<String, String> slow(@RequestParam(defaultValue = "300") int millis) {
        return Map.of("result", service.slow(millis));
    }

    @GetMapping("/hot")
    public Map<String, Long> hot(@RequestParam(defaultValue = "1000000") int iterations) {
        return Map.of("result", service.hotLoop(iterations));
    }

    @GetMapping("/fail")
    public Map<String, String> fail(@RequestParam(defaultValue = "demo") String message) {
        return Map.of("result", service.fail(message));
    }

    @GetMapping("/nested")
    public Map<String, String> nested(@RequestParam(defaultValue = "arthas") String value) {
        return Map.of("result", service.nested(value));
    }
}
