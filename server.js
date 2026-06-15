const http = require('http');
const fs = require('fs');
const path = require('path');
const { GoogleGenAI } = require('@google/genai');

/**
 * server.js for Osei TV
 * Robust, deployment-ready Node.js server with Gemini and Firebase verification.
 */

const PORT = 3000; // Hardcoded to port 3000 to comply with platform deployment standards

// Gemini Initialization - Lazy client setup
let aiInstance = null;
function getGemini() {
  if (!aiInstance) {
    const key = process.env.GEMINI_API_KEY || process.env.GOOGLE_GENERATIVE_AI_API_KEY;
    if (!key) {
      throw new Error('GEMINI_API_KEY environment variable is required');
    }
    aiInstance = new GoogleGenAI({
      apiKey: key,
      httpOptions: {
        headers: {
          'User-Agent': 'aistudio-build',
        },
      },
    });
  }
  return aiInstance;
}

const defaultChannels = [
  { id: "1", name: "News 24", url: "https://shm.97u.xyz/live/news24.m3u8", category: "News" },
  { id: "2", name: "Sports Live", url: "https://shm.97u.xyz/live/sports.m3u8", category: "Sports" },
  { id: "3", name: "Movies Now", url: "https://shm.97u.xyz/live/movies.m3u8", category: "Movies" },
  { id: "4", name: "Action Max", url: "https://shm.97u.xyz/live/action.m3u8", category: "Movies" },
  { id: "5", name: "Discovery Plus", url: "https://shm.97u.xyz/live/discovery.m3u8", category: "Documentary" },
  { id: "6", name: "Cartoon Network", url: "https://shm.97u.xyz/live/cartoons.m3u8", category: "Kids" }
];

function serveStaticFile(res, fileName, contentType) {
  const filePath = path.join(__dirname, 'public', fileName);
  if (fs.existsSync(filePath)) {
    const content = fs.readFileSync(filePath);
    res.writeHead(200, { 
      'Content-Type': contentType,
      'Content-Length': content.length,
      'Cache-Control': 'no-cache'
    });
    res.end(content);
    return true;
  }
  return false;
}

const server = http.createServer(async (req, res) => {
  const urlPath = req.url.split('?')[0];
  console.log(`[${new Date().toISOString()}] ${req.method} ${urlPath}`);

  // 1. Health Checks
  if (urlPath === '/healthz' || urlPath === '/ping') {
    res.writeHead(200, { 'Content-Type': 'text/plain' });
    res.end('OK');
    return;
  }

  // 2. APK Serving
  if (urlPath === '/app-debug.apk') {
    let apkFilePath = path.join(__dirname, 'app-debug.apk');
    
    // Fallback to build folder if not in root
    if (!fs.existsSync(apkFilePath)) {
      apkFilePath = path.join(__dirname, 'app', 'build', 'outputs', 'apk', 'debug', 'app-debug.apk');
    }

    if (fs.existsSync(apkFilePath)) {
      const stat = fs.statSync(apkFilePath);
      res.writeHead(200, {
        'Content-Type': 'application/vnd.android.package-archive',
        'Content-Length': stat.size,
        'Content-Disposition': 'attachment; filename="app-debug.apk"'
      });
      fs.createReadStream(apkFilePath).pipe(res);
    } else {
      res.writeHead(404, { 'Content-Type': 'text/plain' });
      res.end('APK not found. Please compile the app first.');
    }
    return;
  }

  // 3. Static Assets
  if (urlPath === '/hls-player.js') {
    if (serveStaticFile(res, 'hls-player.js', 'application/javascript')) return;
  }
  
  if (urlPath === '/manifest.json') {
    const manifest = {
      name: "Osei TV",
      short_name: "Osei TV",
      start_url: "/",
      display: "standalone",
      background_color: "#0b0c10",
      theme_color: "#df256a",
      icons: [{ src: "https://img.icons8.com/color/512/television.png", sizes: "512x512", type: "image/png" }]
    };
    const json = JSON.stringify(manifest);
    res.writeHead(200, { 
      'Content-Type': 'application/json', 
      'Content-Length': Buffer.byteLength(json) 
    });
    res.end(json);
    return;
  }

  // 4. API Endpoints
  if (urlPath === '/api/config') {
    let firebaseConfig = {};
    try {
      const configPath = path.join(__dirname, 'firebase-applet-config.json');
      if (fs.existsSync(configPath)) {
        firebaseConfig = JSON.parse(fs.readFileSync(configPath, 'utf8'));
      }
    } catch (e) {}

    const key = process.env.GEMINI_API_KEY || process.env.GOOGLE_GENERATIVE_AI_API_KEY;
    const config = JSON.stringify({
      channels: defaultChannels,
      firebase: firebaseConfig,
      hasGeminiKey: !!key
    });
    res.writeHead(200, { 'Content-Type': 'application/json', 'Content-Length': config.length });
    res.end(config);
    return;
  }

  // Gemini Verification Route
  if (urlPath === '/api/gemini/verify') {
    try {
      const ai = getGemini();
      const response = await ai.models.generateContent({
        model: "gemini-3.5-flash",
        contents: "You are Osei TV Assistant. Briefly say hello and confirm your system is online.",
      });
      
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ 
        status: 'online', 
        message: response.text,
        model: 'gemini-3.5-flash'
      }));
    } catch (e) {
      console.error('[GEMINI ERROR]', e);
      res.writeHead(500, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'Gemini Verification Failed', detail: e.message }));
    }
    return;
  }

  // AI Assistant with Thinking Mode
  if (urlPath === '/api/assistant' && req.method === 'POST') {
    let body = '';
    req.on('data', chunk => { body += chunk.toString(); });
    req.on('end', async () => {
      try {
        const { message } = JSON.parse(body);
        const ai = getGemini();
        
        // Using thinking mode for complex queries as requested
        const response = await ai.models.generateContent({
          model: "gemini-3.1-pro-preview",
          contents: message,
          config: {
            systemInstruction: "You are the Osei TV Intelligent Assistant. You help users with complex questions about the app, streaming technology, or content recommendations. Use your advanced reasoning carefully.",
            thinkingConfig: {
              thinkingLevel: 'HIGH'
            }
          }
        });

        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ response: response.text }));
      } catch (e) {
        console.error('[ASSISTANT ERROR]', e);
        res.writeHead(500, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'Assistant failed', detail: e.message }));
      }
    });
    return;
  }

  // 5. Index Logic with Hydration
  if (urlPath === '/' || urlPath === '/index.html') {
    const indexPath = path.join(__dirname, 'public', 'index.html');
    if (fs.existsSync(indexPath)) {
      let content = fs.readFileSync(indexPath, 'utf8');
      
      // Load Firebase config
      let firebaseConfig = "{}";
      try {
        const configPath = path.join(__dirname, 'firebase-applet-config.json');
        if (fs.existsSync(configPath)) {
          firebaseConfig = fs.readFileSync(configPath, 'utf8');
        }
      } catch (err) {}

      // Robust placeholders replacement
      content = content.replace(/{{CHANNELS}}/g, JSON.stringify(defaultChannels));
      content = content.replace(/{{FIREBASE_CONFIG}}/g, firebaseConfig);

      const buf = Buffer.from(content, 'utf8');
      res.writeHead(200, { 
        'Content-Type': 'text/html; charset=UTF-8',
        'Content-Length': buf.length
      });
      res.end(buf);
      return;
    }
  }

  // Default 404
  res.writeHead(404, { 'Content-Type': 'text/plain' });
  res.end('404 Not Found');
});

// Port logging for debugging
server.listen(PORT, '0.0.0.0', () => {
  console.log(`[OSEI TV] Server started on port ${PORT}`);
});

// Cleanup
process.on('SIGTERM', () => server.close(() => process.exit(0)));
process.on('SIGINT', () => server.close(() => process.exit(0)));
