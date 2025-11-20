///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS io.undertow:undertow-core:2.3.12.Final
//DEPS io.undertow:undertow-servlet:2.3.12.Final

import java.net.URI;

import io.undertow.Undertow;
import io.undertow.attribute.ExchangeAttributes;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.proxy.LoadBalancingProxyClient;
import io.undertow.server.handlers.proxy.ProxyHandler;
import io.undertow.util.HttpString;

// simple proxy - can be used top test the --base-path cli feature
public class Proxy {

  public static final String TEST_PROXY_PREFIX = "/test_proxy";

  public static void main(String[] args) {
    startProxy(8888, 8080, false);
  }

  private static Undertow startProxy(final int proxyPort, final int backendPort, final boolean setForwardedPrefix) {
    try {
      final URI backend = new URI("http://localhost:" + backendPort);

      final LoadBalancingProxyClient client = new LoadBalancingProxyClient().addHost(backend);

      final HttpHandler handler = exchange -> {
        final String originalPath = exchange.getRequestPath();

        // Strip prefix from requestPath
        String newPath = originalPath.substring(TEST_PROXY_PREFIX.length());
        if (newPath.isEmpty()) {
          newPath = "/";
        }

        // Update all path fields to maintain consistent exchange state
        exchange.setRequestPath(newPath);
        exchange.setRelativePath(newPath);
        // exchange.setRequestURI(newPath + (query == null || query.isEmpty() ? "" : "?" + query));
        exchange.setRequestURI(newPath);

        var ph = ProxyHandler
            .builder()
            .setProxyClient(client)
            .setMaxRequestTime(30000);
        if (setForwardedPrefix) {
          ph.addRequestHeader(new HttpString("X-Forwarded-Prefix"), ExchangeAttributes.constant(TEST_PROXY_PREFIX));
        }
        ph.build().handleRequest(exchange);
      };

      final PathHandler root = new PathHandler().addPrefixPath(TEST_PROXY_PREFIX, handler);

      final Undertow proxyServer = Undertow.builder().addHttpListener(proxyPort, "0.0.0.0").setHandler(root).build();

      proxyServer.start();
      return proxyServer;
    } catch (Exception e) {
      throw new RuntimeException("Failed to start proxy server", e);
    }
  }
}
