// ==================== Importaciones ====================
const net = require("net");
const express = require("express");
const cors = require("cors");
const path = require("path");

// ==================== Configuración ====================
const JAVA_HOST = process.env.JAVA_HOST || "127.0.0.1";
const JAVA_PORT = parseInt(process.env.JAVA_PORT || "6789", 10);
const HTTP_PORT = parseInt(process.env.HTTP_PORT || "5000", 10);

const app = express();
app.use(express.json());
app.use(cors());

// ==================== Servir frontend (opcional) ====================
app.use(express.static(path.join(__dirname, "public")));
app.get("/", (req, res) => {
  res.sendFile(path.join(__dirname, "public", "index.html"));
});

// ==================== Sesiones TCP ====================
const sessions = new Map();

function ensureSession(username) {
  if (!username || !username.trim()) throw new Error("username requerido");

  if (sessions.has(username)) return sessions.get(username);

  const socket = new net.Socket();
  socket.setEncoding("utf8");

  let buf = "";
  socket.on("data", (chunk) => {
    buf += chunk.toString();
  });
  socket.on("error", (e) => console.error(`[TCP][${username}]`, e.message));
  socket.on("close", () => {
    sessions.delete(username);
    console.log(`[TCP][${username}] cerrado`);
  });

  socket.connect(JAVA_PORT, JAVA_HOST, () => {
    console.log(`[TCP][${username}] conectado a ${JAVA_HOST}:${JAVA_PORT}`);
    socket.write(username + "\n"); // login automático
  });

  const writeLines = async (lines) => {
    for (const ln of lines) {
      socket.write(ln + "\n");
    }
  };

  const readUntil = (predicate, quietMs = 400, maxMs = 8000) =>
    new Promise((resolve) => {
      let lastLen = buf.length;
      const start = Date.now();
      const tick = () => {
        const now = Date.now();
        const grown = buf.length !== lastLen;
        lastLen = buf.length;
        if (predicate && predicate(buf)) {
          const out = buf;
          buf = "";
          return resolve(out);
        }
        if (!grown && now - start > quietMs) {
          const out = buf;
          buf = "";
          return resolve(out);
        }
        if (now - start > maxMs) {
          const out = buf;
          buf = "";
          return resolve(out);
        }
        setTimeout(tick, 50);
      };
      setTimeout(tick, 50);
    });

  const sess = { socket, writeLines, readUntil };
  sessions.set(username, sess);
  return sess;
}

// ==================== Endpoints HTTP ====================

// 🟩 LOGIN
app.post("/api/login", async (req, res) => {
  try {
    const { username } = req.body;
    const sess = ensureSession(username);
    const out = await sess.readUntil(
      (t) => t.includes("MENU") || t.includes("¡Hola") || t.includes("Bienvenido")
    );
    res.json({ ok: true, output: out });
  } catch (e) {
    res.status(400).json({ ok: false, error: e.message });
  }
});

// 🟩 MENSAJE PRIVADO
app.post("/api/message/private", async (req, res) => {
  try {
    const { username, to, message } = req.body;
    const sender = ensureSession(username);

    // Esperar menú antes de interactuar
    await sender.readUntil((t) => t.includes("MENU") || t.includes("Hola"));

    // Enviar flujo al servidor Java
    await sender.writeLines(["1", to, message]);

    // Esperar respuesta final
    const out = await sender.readUntil(
      (t) =>
        t.includes("Mensaje enviado correctamente") ||
        t.includes("Usuario no encontrado") ||
        t.includes("MENU")
    );

    res.json({ ok: true, output: out });
  } catch (e) {
    console.error("Error en /api/message/private:", e);
    res.status(400).json({ ok: false, error: e.message });
  }
});

// 🟩 CREAR GRUPO
app.post("/api/group", async (req, res) => {
  try {
    const { username, group, members = [] } = req.body;
    const s = ensureSession(username);
    const membersCSV = members.join(", ");

    await s.readUntil((t) => t.includes("MENU") || t.includes("Hola"));
    await s.writeLines(["2", group, membersCSV]);

    const out = await s.readUntil(
      (t) =>
        t.includes("Miembros actuales") ||
        t.includes("Grupo") ||
        t.includes("MENU")
    );

    res.json({ ok: true, output: out });
  } catch (e) {
    res.status(400).json({ ok: false, error: e.message });
  }
});

// 🟩 MENSAJE A GRUPO
app.post("/api/message/group", async (req, res) => {
  try {
    const { username, group, message } = req.body;
    const s = ensureSession(username);
    await s.readUntil((t) => t.includes("MENU") || t.includes("Hola"));

    await s.writeLines(["3", group, message]);
    const out = await s.readUntil(
      (t) =>
        t.includes("Mensaje enviado al grupo correctamente") ||
        t.includes("Grupo no encontrado") ||
        t.includes("MENU")
    );

    res.json({ ok: true, output: out });
  } catch (e) {
    res.status(400).json({ ok: false, error: e.message });
  }
});

// 🟩 HISTORIAL PRIVADO
app.get("/api/history/private", async (req, res) => {
  try {
    const { username, with: other } = req.query;
    const s = ensureSession(username);

    await s.writeLines(["7", other]);
    const out = await s.readUntil(
      (t) =>
        t.includes("=== FIN DEL HISTORIAL ===") ||
        t.includes("No hay historial") ||
        t.includes("MENU"),
      400,
      8000
    );

    res.json({ ok: true, output: out });
  } catch (e) {
    console.error("Error en /api/history/private:", e);
    res.status(400).json({ ok: false, error: e.message });
  }
});

// 🟩 HISTORIAL DE GRUPO
app.get("/api/history/group", async (req, res) => {
  try {
    const { username, group } = req.query;
    const s = ensureSession(username);

    await s.writeLines(["8", group]);
    const out = await s.readUntil(
      (t) =>
        t.includes("=== FIN DEL HISTORIAL ===") ||
        t.includes("No hay historial") ||
        t.includes("MENU"),
      400,
      8000
    );

    res.json({ ok: true, output: out });
  } catch (e) {
    console.error("Error en /api/history/group:", e);
    res.status(400).json({ ok: false, error: e.message });
  }
});

// ==================== Inicialización ====================
app.listen(HTTP_PORT, () => {
  console.log(`Proxy HTTP escuchando en http://localhost:${HTTP_PORT}`);
});
