import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { spawn } from 'node:child_process';

const root = path.dirname(fileURLToPath(import.meta.url));
const publicDir = path.join(root, 'public');
const ADB = process.env.ADB || 'adb';
const HOST = process.env.HOST || '127.0.0.1';
const PORT = Number(process.env.PORT || 3456);

let state = {
  running: false,
  stopped: false,
  total: 0,
  current: 0,
  x: 540,
  y: 900,
  interval: 150,
  duration: 0,
  status: 'ready',
  logs: []
};

function addLog(text) {
  const line = `[${new Date().toLocaleTimeString()}] ${text}`;
  state.logs.push(line);
  if (state.logs.length > 200) state.logs.shift();
}

function runAdb(args, timeout = 3000) {
  return new Promise((resolve) => {
    const cp = spawn(ADB, args, { shell: false });
    let stdout = '';
    let stderr = '';
    const timer = setTimeout(() => {
      cp.kill('SIGKILL');
      stderr += '\nCommand timed out.';
    }, timeout);
    cp.stdout.on('data', (chunk) => stdout += chunk.toString());
    cp.stderr.on('data', (chunk) => stderr += chunk.toString());
    cp.on('close', (code) => {
      clearTimeout(timer);
      resolve({ code, stdout: stdout.trim(), stderr: stderr.trim(), killed: false });
    });
  });
}

async function listDevices() {
  const res = await runAdb(['devices']);
  const devices = res.stdout
    .split(/\r?\n/)
    .slice(1)
    .map((line) => line.split(/\s+/))
    .filter((parts) => parts[0] && parts[1] === 'device')
    .map((parts) => ({ serial: parts[0], status: parts[1] }));
  return devices;
}

async function runOnDevice(serial, cmd, timeout) {
  return runAdb([serial ? `-s` : null, serial || '', 'shell', ...cmd].filter(Boolean), timeout);
}

async function startRun(config) {
  if (state.running) return { ok: false, error: 'Already running' };
  state = { ...state, running: true, stopped: false, total: config.total, current: 0, x: config.x, y: config.y, interval: config.interval, duration: 0, status: 'running', logs: [] };
  addLog(`Starting auto tap at ${config.x},${config.y}, interval ${config.interval}ms, total ${config.total} taps.`);
  const startTime = Date.now();
  let i = 0;
  try {
    while (i < config.total && !state.stopped) {
      const res = await runOnDevice(config.serial, ['input', 'tap', String(config.x), String(config.y)], 2500);
      if (res.code === 0 || /wrote/i.test(res.stderr + res.stdout)) {
        i += 1;
        state.current = i;
      } else {
        addLog(`Tap failed at attempt ${i + 1}: ${res.stderr || res.stdout}`);
        break;
      }
      state.duration = Date.now() - startTime;
      if (i < config.total && !state.stopped) {
        await new Promise((r) => setTimeout(r, Math.max(0, config.interval - 30)));
      }
    }
    if (state.stopped) {
      state.status = 'stopped';
      addLog(`Stopped by user after ${state.current}/${config.total} taps.`);
    } else {
      state.status = 'done';
      addLog(`Finished ${config.total}/${config.total} taps in ${state.duration}ms.`);
    }
  } catch (err) {
    state.status = 'error';
    addLog(`Error: ${err.message}`);
  } finally {
    state.running = false;
  }
  return { ok: true };
}

function stopRun() {
  state.stopped = true;
  return { ok: true };
}

function parseJson(chunk) {
  try { return JSON.parse(chunk); } catch { return null; }
}

function jsonResponse(res, statusCode, data) {
  res.writeHead(statusCode, { 'content-type': 'application/json; charset=utf-8' });
  res.end(JSON.stringify(data));
}

function staticFile(res, file) {
  const filePath = path.join(publicDir, file);
  const stat = fs.statSync(filePath);
  const ext = path.extname(filePath);
  const mime = ext === '.html' ? 'text/html' : ext === '.css' ? 'text/css' : ext === '.js' ? 'application/javascript' : 'application/octet-stream';
  res.writeHead(200, { 'content-type': `${mime}; charset=utf-8`, 'content-length': stat.size });
  const stream = fs.createReadStream(filePath);
  stream.pipe(res);
}

async function readBody(req) {
  const chunks = [];
  for await (const chunk of req) chunks.push(chunk);
  const text = Buffer.concat(chunks).toString();
  return text ? parseJson(text) : null;
}

const server = http.createServer(async (req, res) => {
  try {
    const url = req.url.split('?')[0];
    if (url === '/' || url === '/index.html') {
      return staticFile(res, '/index.html');
    }
    if (url === '/status') {
      return jsonResponse(res, 200, state);
    }
    if (url === '/devices' && req.method === 'GET') {
      return jsonResponse(res, 200, await listDevices());
    }
    if (url === '/stop' && req.method === 'POST') {
      return jsonResponse(res, 200, stopRun());
    }
    if (url === '/start' && req.method === 'POST') {
      const config = await readBody(req);
      if (!config || config.total < 1 || config.interval < 40) {
        return jsonResponse(res, 400, { ok: false, error: 'Invalid config' });
      }
      return jsonResponse(res, 200, await startRun(config));
    }
    return jsonResponse(res, 404, { ok: false, error: 'not found' });
  } catch (err) {
    return jsonResponse(res, 500, { ok: false, error: err.message });
  }
});

server.listen(PORT, HOST, () => {
  addLog(`Server started at http://${HOST}:${PORT}`);
  console.log(`Auto Tapper running: http://${HOST}:${PORT}`);
});

process.on('uncaughtException', (err) => {
  console.error(err);
});
