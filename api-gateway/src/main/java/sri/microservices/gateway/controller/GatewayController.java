package sri.microservices.gateway.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sri.microservices.gateway.config.GatewayProperties;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

@RestController
public class GatewayController {

    private static final Set<String> HOP_BY_HOP_HEADERS = Set.of(
            "connection",
            "content-length",
            "expect",
            "host",
            "keep-alive",
            "proxy-authenticate",
            "proxy-authorization",
            "te",
            "trailer",
            "transfer-encoding",
            "upgrade"
    );

    private final GatewayProperties gatewayProperties;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public GatewayController(GatewayProperties gatewayProperties) {
        this.gatewayProperties = gatewayProperties;
    }

    @RequestMapping({"/auth", "/auth/**", "/api/auth", "/api/auth/**"})
    public ResponseEntity<byte[]> auth(HttpServletRequest request) throws IOException, InterruptedException {
        return proxy(request, gatewayProperties.authServiceUrl());
    }

    @RequestMapping({"/api/cultivos", "/api/cultivos/**"})
    public ResponseEntity<byte[]> cultivos(HttpServletRequest request) throws IOException, InterruptedException {
        return proxy(request, gatewayProperties.cultivosServiceUrl());
    }

    @RequestMapping({"/api/mqtt", "/api/mqtt/**", "/api/sensor", "/api/sensor/**", "/api/estado-vivo"})
    public ResponseEntity<byte[]> sensores(HttpServletRequest request) throws IOException, InterruptedException {
        return proxy(request, gatewayProperties.sensoresServiceUrl());
    }

    @RequestMapping({"/api/riego", "/api/riego/**"})
    public ResponseEntity<byte[]> riego(HttpServletRequest request) throws IOException, InterruptedException {
        return proxy(request, gatewayProperties.riegoServiceUrl());
    }

    @RequestMapping({"/api/reportes", "/api/reportes/**", "/api/estadisticas", "/api/estadisticas/**"})
    public ResponseEntity<byte[]> reportes(HttpServletRequest request) throws IOException, InterruptedException {
        return proxy(request, gatewayProperties.reportesServiceUrl());
    }

    @RequestMapping({"/api/chat", "/api/chat/**"})
    public ResponseEntity<byte[]> chat(HttpServletRequest request) throws IOException, InterruptedException {
        return proxy(request, gatewayProperties.chatServiceUrl());
    }

    @RequestMapping("/gateway/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("api-gateway activo");
    }

    private ResponseEntity<byte[]> proxy(HttpServletRequest request, String targetBaseUrl)
            throws IOException, InterruptedException {
        URI targetUri = buildTargetUri(request, targetBaseUrl);
        System.out.println("Gateway base URL: " + targetBaseUrl);
        System.out.println("Gateway path recibido: " + request.getRequestURI());
        System.out.println("Gateway URL destino: " + targetUri);


        byte[] requestBody = request.getInputStream().readAllBytes();

        HttpRequest.Builder outbound = HttpRequest.newBuilder(targetUri)
                .method(request.getMethod(), bodyPublisher(requestBody));

        copyRequestHeaders(request, outbound);

        HttpResponse<byte[]> response = httpClient.send(outbound.build(), HttpResponse.BodyHandlers.ofByteArray());

        System.out.println("AUTH STATUS: " + response.statusCode());
        System.out.println("AUTH BODY: " + new String(response.body()));

        HttpHeaders headers = copyResponseHeaders(response);


        return ResponseEntity.status(HttpStatus.valueOf(response.statusCode()))
                .headers(headers)
                .body(response.body());
    }

    private URI buildTargetUri(HttpServletRequest request, String targetBaseUrl) {
        String path = request.getRequestURI();
        String query = request.getQueryString();
        String normalizedBaseUrl = targetBaseUrl.endsWith("/")
                ? targetBaseUrl.substring(0, targetBaseUrl.length() - 1)
                : targetBaseUrl;

        return URI.create(normalizedBaseUrl + path + (query == null ? "" : "?" + query));
    }

    private HttpRequest.BodyPublisher bodyPublisher(byte[] requestBody) {
        if (requestBody.length == 0) {
            return HttpRequest.BodyPublishers.noBody();
        }
        return HttpRequest.BodyPublishers.ofByteArray(requestBody);
    }

    private void copyRequestHeaders(HttpServletRequest request, HttpRequest.Builder outbound) {
        Enumeration<String> headerNames = request.getHeaderNames();
        for (String headerName : Collections.list(headerNames)) {
            if (isBlockedHeader(headerName)) {
                continue;
            }

            Enumeration<String> values = request.getHeaders(headerName);
            for (String value : Collections.list(values)) {
                outbound.header(headerName, value);
            }
        }
    }

    private HttpHeaders copyResponseHeaders(HttpResponse<byte[]> response) {
        HttpHeaders headers = new HttpHeaders();
        Set<String> copiedHeaders = new HashSet<>();

        response.headers().map().forEach((name, values) -> {
            if (!isBlockedHeader(name) && copiedHeaders.add(name.toLowerCase())) {
                headers.addAll(name, values);
            }
        });

        return headers;
    }

    private boolean isBlockedHeader(String headerName) {
        return headerName == null || HOP_BY_HOP_HEADERS.contains(headerName.toLowerCase());
    }
}
