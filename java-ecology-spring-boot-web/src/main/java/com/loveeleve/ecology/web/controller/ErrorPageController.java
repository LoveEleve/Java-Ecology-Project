package com.loveeleve.ecology.web.controller;

import com.loveeleve.ecology.web.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.ServletWebRequest;

import java.util.Map;

@RestController
public class ErrorPageController implements ErrorController {

    private final ErrorAttributes errorAttributes;

    public ErrorPageController(ErrorAttributes errorAttributes) {
        this.errorAttributes = errorAttributes;
    }

    @RequestMapping("${server.error.path:${error.path:/error}}")
    public ResponseEntity<ApiResponse<Void>> error(HttpServletRequest request) {
        Map<String, Object> attributes = errorAttributes.getErrorAttributes(
                new ServletWebRequest(request), ErrorAttributeOptions.defaults());
        int status = attributes.get("status") instanceof Integer code ? code : HttpStatus.INTERNAL_SERVER_ERROR.value();
        String message = String.valueOf(attributes.getOrDefault("error", "request failed"));
        return ResponseEntity.status(status).body(ApiResponse.error("HTTP_" + status, message));
    }
}
