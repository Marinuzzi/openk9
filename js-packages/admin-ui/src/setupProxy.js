const { createProxyMiddleware } = require("http-proxy-middleware");

module.exports = function (app) {
  app.use(
    "/api/datasource/graphql",
    createProxyMiddleware({
      target: "https://k9-frontend.openk9.io",
      changeOrigin: true,
    })
  );
  app.use(
    "/api/datasource/v1",
    createProxyMiddleware({
      target: "https://k9-frontend.openk9.io",
      changeOrigin: true,
    })
  );
  app.use(
    "/api/datasource/oauth2",
    createProxyMiddleware({
      target: "https://k9-frontend.openk9.io",
      changeOrigin: true,
    })
  );
  app.use(
    "/api/k8s-client",
    createProxyMiddleware({
      target: "https://k9-frontend.openk9.io",
      changeOrigin: true,
    })
  );
};
