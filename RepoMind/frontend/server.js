import express from 'express';
import { createProxyMiddleware } from 'http-proxy-middleware';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const app = express();
const PORT = process.env.PORT || 3000;
const BACKEND_URL = process.env.VITE_API_URL || 'http://localhost:8080';

console.log(`Starting Proxy Server...`);
console.log(`Backend URL: ${BACKEND_URL}`);

// Proxy API requests to Backend
// CRITICAL: Must include /auth for the success handler and /login/oauth2 for the callback
app.use(['/api', '/auth', '/oauth2', '/login/oauth2'], createProxyMiddleware({
    target: BACKEND_URL,
    changeOrigin: true,
    ws: true, // Proxy WebSockets too!
    cookieDomainRewrite: {
        "*": "" // Rewrite all cookies to be valid for the current domain
    },
    onProxyReq: (proxyReq, req, res) => {
        // Log proxy requests for debugging
        console.log(`Proxying ${req.method} ${req.path} -> ${BACKEND_URL}`);
    },
    onError: (err, req, res) => {
        console.error('Proxy Error:', err);
        res.status(500).send('Proxy Error');
    }
}));

// Serve Static Files (React App)
app.use(express.static(path.join(__dirname, 'dist')));

// Handle Client-Side Routing (SPA)
// Check if request is for API or static file; if not, serve index.html
app.get('*', (req, res) => {
    res.sendFile(path.join(__dirname, 'dist', 'index.html'));
});

app.listen(PORT, () => {
    console.log(`Frontend URL: http://localhost:${PORT}`);
});
