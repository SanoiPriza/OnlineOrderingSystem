package org.example.apiGateway.security;

  import org.example.common.security.jwt.JwtTokenUtil;
  import org.springframework.security.authentication.BadCredentialsException;
  import org.springframework.security.authentication.ReactiveAuthenticationManager;
  import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
  import org.springframework.security.core.Authentication;
  import org.springframework.security.core.GrantedAuthority;
  import org.springframework.security.core.authority.SimpleGrantedAuthority;
  import reactor.core.publisher.Mono;

  import java.util.Collection;
  import java.util.Collections;
  import java.util.Date;
  import java.util.List;

  final class JwtReactiveAuthenticationManager implements ReactiveAuthenticationManager {

      private final JwtTokenUtil jwtTokenUtil;

      JwtReactiveAuthenticationManager(JwtTokenUtil jwtTokenUtil) {
          this.jwtTokenUtil = jwtTokenUtil;
      }

      @Override
      public Mono<Authentication> authenticate(Authentication authentication) {
          if (authentication == null || authentication.getCredentials() == null) {
              return Mono.error(new BadCredentialsException("Missing credentials"));
          }
          String token = authentication.getCredentials().toString();

          return Mono.fromCallable(() -> {
              try {
                  String username = jwtTokenUtil.extractUsername(token);
                  if (username == null) {
                      throw new BadCredentialsException("Invalid token: no subject");
                  }
                  Date exp = jwtTokenUtil.extractExpiration(token);
                  if (exp != null && exp.before(new Date())) {
                      throw new BadCredentialsException("Token expired");
                  }
                  Collection<? extends GrantedAuthority> authorities = extractAuthorities(token);
                  return new UsernamePasswordAuthenticationToken(username, token, authorities);
              } catch (BadCredentialsException e) {
                  throw e;
              } catch (Exception ex) {
                  throw new BadCredentialsException("Invalid token", ex);
              }
          });
      }

      @SuppressWarnings("unchecked")
      private Collection<? extends GrantedAuthority> extractAuthorities(String token) {
          try {
              Object rolesObj = jwtTokenUtil.extractClaim(token, claims -> claims.get("roles"));
              if (rolesObj instanceof List<?> rawList) {
                  return rawList.stream()
                          .filter(String.class::isInstance)
                          .map(String.class::cast)
                          .map(SimpleGrantedAuthority::new)
                          .toList();
              }
          } catch (Exception ignored) {}
          return Collections.emptyList();
      }
  }