// sockjs-client (used by TelemetryWebsocketService) assumes a Node-like
// environment and references `global` / `process` directly. Angular's
// esbuild-based build (v17+) no longer polyfills these automatically the
// way the old Webpack builder did, so without this file the whole app
// crashes silently on bootstrap (blank screen, no console-visible error
// until you check the Console tab -- you'll see "global is not defined").
(window as any).global = window;
(window as any).process = { env: {} };
