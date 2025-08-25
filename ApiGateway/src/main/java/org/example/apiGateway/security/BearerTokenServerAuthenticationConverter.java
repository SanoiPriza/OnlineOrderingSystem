package org.example.apiGateway.security;

  import org.springframework.http.HttpHeaders;
  import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
  import org.springframework.security.core.Authentication;
  import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
  import org.springframework.util.StringUtils;
  import org.springframework.web.server.ServerWebExchange;
  import reactor.core.publisher.Mono;

  final class BearerTokenServerAuthenticationConverter implements ServerAuthenticationConverter {

      private static final String BEARER_PREFIX = "Bearer ";

      @Override
      public Mono<Authentication> convert(ServerWebExchange exchange) {
          String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
          if (!StringUtils.hasText(authHeader) || !authHeader.startsWith(BEARER_PREFIX)) {
              return Mono.empty();
          }
          String token = authHeader.substring(BEARER_PREFIX.length()).trim();
          if (!StringUtils.hasText(token)) {
              return Mono.empty();
          }
          return Mono.just(new UsernamePasswordAuthenticationToken(null, token));
      }
  }