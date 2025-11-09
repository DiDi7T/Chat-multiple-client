document.getElementById("login").addEventListener("click", async () => {
  const username = document.getElementById("username").value;
  const output = document.getElementById("output");

  const res = await fetch("/api/login", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username })
  });

  const data = await res.json();
  output.textContent = data.output || "Error de conexión";
});
