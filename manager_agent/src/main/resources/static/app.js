const summaryEl = document.getElementById("summary");
const resultEl = document.getElementById("result-text");
const rawEl = document.getElementById("raw-output");
const tableBody = document.getElementById("task-table-body");
const submitBtn = document.getElementById("submit-btn");
const toggleRawBtn = document.getElementById("toggle-raw-btn");
const workflowIdInput = document.getElementById("workflowIdInput");
const liveIndicator = document.getElementById("live-indicator");
const copyResultBtn = document.getElementById("copy-result-btn");
const dagBoard = document.getElementById("dag-board");
const apiBaseUrlInput = document.getElementById("apiBaseUrl");
const detectBackendBtn = document.getElementById("detect-backend-btn");

let pollingTimer = null;

function escapeHtml(text) {
  return String(text ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll("\"", "&quot;")
    .replaceAll("'", "&#39;");
}

function getApiBase() {
  const value = (apiBaseUrlInput?.value || "").trim();
  return value || window.location.origin;
}

function buildUrl(path) {
  return `${getApiBase()}${path}`;
}

async function fetchWithTimeout(url, options = {}, timeoutMs = 180000) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);
  try {
    return await fetch(url, { ...options, signal: controller.signal });
  } finally {
    clearTimeout(timer);
  }
}

function setIndicator(state, text) {
  liveIndicator.className = `live-indicator ${state}`;
  liveIndicator.textContent = text;
}

function renderSummary(data) {
  const success = data?.success ? "SUCCESS" : "FAILED";
  const degraded = data?.degraded ? "YES" : "NO";
  const idempotent = data?.idempotentHit ? "YES" : "NO";
  summaryEl.innerHTML = `
    <div class="summary-item"><span>状态</span><strong>${escapeHtml(success)}</strong></div>
    <div class="summary-item"><span>降级</span><strong>${escapeHtml(degraded)}</strong></div>
    <div class="summary-item"><span>幂等命中</span><strong>${escapeHtml(idempotent)}</strong></div>
    <div class="summary-item"><span>RequestId</span><strong>${escapeHtml(data?.requestId || "-")}</strong></div>
    <div class="summary-item"><span>WorkflowId</span><strong>${escapeHtml(data?.workflowId || "-")}</strong></div>
    <div class="summary-item"><span>消息</span><strong>${escapeHtml(data?.message || "-")}</strong></div>
  `;
}

function renderTasks(taskRecords) {
  const tasks = Array.isArray(taskRecords) ? taskRecords : [];
  if (tasks.length === 0) {
    tableBody.innerHTML = `<tr><td colspan="6" class="empty">暂无任务记录</td></tr>`;
    return;
  }

  tableBody.innerHTML = tasks.map((t) => `
    <tr>
      <td>${escapeHtml(t.taskId || "-")}</td>
      <td>${escapeHtml(t.role || "-")}</td>
      <td><span class="status-badge status-${String(t.status || "").toLowerCase()}">${escapeHtml(t.status || "-")}</span></td>
      <td>${escapeHtml(t.attempts ?? 0)}</td>
      <td title="${escapeHtml(t.error || "")}">${escapeHtml(t.error || "-")}</td>
      <td title="${escapeHtml(t.outputPreview || "")}">${escapeHtml(t.outputPreview || "-")}</td>
    </tr>
  `).join("");
}

function toStatusClass(status) {
  const s = String(status || "PENDING").toLowerCase();
  if (["pending", "running", "success", "failed", "compensated", "skipped"].includes(s)) {
    return s;
  }
  return "pending";
}

function renderDag(taskRecords) {
  const tasks = Array.isArray(taskRecords) ? taskRecords : [];
  const byId = {};
  tasks.forEach((t) => {
    byId[t.taskId] = t;
  });

  const nodes = [
    { id: "plan_trip", title: "plan_trip", x: 60, y: 40 },
    { id: "plan_route", title: "plan_route", x: 60, y: 120 },
    { id: "review_trip", title: "review_trip", x: 320, y: 40 },
    { id: "review_route", title: "review_route", x: 320, y: 120 },
    { id: "coordinate_final", title: "coordinate_final", x: 600, y: 80 }
  ];

  const edges = [
    ["plan_trip", "review_trip"],
    ["plan_route", "review_route"],
    ["review_trip", "coordinate_final"],
    ["review_route", "coordinate_final"]
  ];

  const edgeSvg = edges.map(([from, to]) => {
    const a = nodes.find((n) => n.id === from);
    const b = nodes.find((n) => n.id === to);
    const x1 = a.x + 200;
    const y1 = a.y + 26;
    const x2 = b.x;
    const y2 = b.y + 26;
    return `<path class="dag-link" d="M${x1},${y1} C ${x1 + 40},${y1} ${x2 - 40},${y2} ${x2},${y2}" />`;
  }).join("");

  const nodeSvg = nodes.map((n) => {
    const task = byId[n.id];
    const status = toStatusClass(task?.status);
    const attempts = task?.attempts ?? 0;
    const role = task?.role || "-";
    return `
      <g transform="translate(${n.x},${n.y})">
        <rect class="dag-node dag-node-${status}" rx="10" ry="10" width="200" height="56"></rect>
        <text class="dag-node-label" x="10" y="20">${escapeHtml(n.title)}</text>
        <text class="dag-node-sub" x="10" y="40">status: ${escapeHtml(String(task?.status || "PENDING"))} | role: ${escapeHtml(role)} | attempts: ${escapeHtml(attempts)}</text>
      </g>
    `;
  }).join("");

  dagBoard.innerHTML = `
    <svg class="dag-svg" viewBox="0 0 840 200" preserveAspectRatio="xMidYMid meet">
      <defs>
        <marker id="arrow" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse">
          <path d="M 0 0 L 10 5 L 0 10 z" fill="#9aabbe"></path>
        </marker>
      </defs>
      ${edgeSvg}
      ${nodeSvg}
    </svg>
  `;
}

function renderResponse(data) {
  renderSummary(data);
  renderTasks(data?.taskRecords);
  renderDag(data?.taskRecords);
  resultEl.textContent = data?.result || "无结果文本";
  rawEl.textContent = JSON.stringify(data, null, 2);
  if (data?.workflowId) {
    workflowIdInput.value = data.workflowId;
  }
}

function isTerminalStatus(status) {
  const s = String(status || "").toUpperCase();
  return s === "SUCCESS" || s === "FAILED" || s === "COMPENSATED" || s === "SKIPPED";
}

function isWorkflowFinished(data) {
  const tasks = Array.isArray(data?.taskRecords) ? data.taskRecords : [];
  if (tasks.length === 0) {
    return !!data?.workflowId;
  }
  return tasks.every((t) => isTerminalStatus(t.status));
}

async function fetchWorkflow(workflowId) {
  const res = await fetchWithTimeout(buildUrl(`/app/workflow/${encodeURIComponent(workflowId)}`), {}, 30000);
  if (!res.ok) {
    throw new Error(`HTTP ${res.status}`);
  }
  return res.json();
}

function stopPolling() {
  if (pollingTimer) {
    clearInterval(pollingTimer);
    pollingTimer = null;
  }
}

function startPolling(workflowId) {
  stopPolling();
  if (!workflowId) {
    return;
  }
  setIndicator("running", "执行中（自动刷新）");
  pollingTimer = setInterval(async () => {
    try {
      const data = await fetchWorkflow(workflowId);
      renderResponse(data);
      if (isWorkflowFinished(data)) {
        stopPolling();
        setIndicator(data?.success ? "done" : "failed", data?.success ? "已完成" : "已结束（含失败）");
      }
    } catch (_) {
      stopPolling();
      setIndicator("failed", "自动刷新失败");
    }
  }, 2000);
}

function setLoading(loading, text = "") {
  submitBtn.disabled = loading;
  submitBtn.textContent = loading ? text || "执行中..." : "开始规划";
}

function renderError(message) {
  const data = {
    success: false,
    degraded: true,
    idempotentHit: false,
    message,
    requestId: "-",
    workflowId: "-",
    result: "请求失败，请检查后端服务与日志。",
    taskRecords: []
  };
  renderResponse(data);
  setIndicator("failed", "请求失败");
}

document.getElementById("plan-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  const body = {
    requestId: document.getElementById("requestId").value || undefined,
    origin: document.getElementById("origin").value,
    destination: document.getElementById("destination").value,
    travelDate: document.getElementById("travelDate").value,
    preferences: document.getElementById("preferences").value,
    budget: document.getElementById("budget").value,
    transportMode: document.getElementById("transportMode").value,
    extraRequirements: document.getElementById("extraRequirements").value
  };

  if (!body.requestId) {
    body.requestId = `req-${Date.now()}`;
    document.getElementById("requestId").value = body.requestId;
  }

  setLoading(true, "规划中...");
  setIndicator("running", "已提交，执行中");
  resultEl.textContent = "执行中，请稍候...";

  try {
    const res = await fetchWithTimeout(buildUrl("/app"), {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body)
    }, 180000);

    if (!res.ok) {
      throw new Error(`HTTP ${res.status}`);
    }

    const data = await res.json();
    renderResponse(data);
    if (data?.workflowId) {
      startPolling(data.workflowId);
    } else {
      setIndicator(data?.success ? "done" : "failed", data?.success ? "已完成" : "执行失败");
    }
  } catch (error) {
    renderError(`调用 /app 失败: ${error.message}`);
  } finally {
    setLoading(false);
  }
});

document.getElementById("ping-btn").addEventListener("click", async () => {
  try {
    const res = await fetchWithTimeout(buildUrl("/app/ping"), {}, 8000);
    if (!res.ok) {
      throw new Error(`HTTP ${res.status}`);
    }
    const text = await res.text();
    alert(`后端连通性: ${text}`);
  } catch (error) {
    alert(`连通性检测失败: ${error.message}`);
  }
});

document.getElementById("query-btn").addEventListener("click", async () => {
  const workflowId = workflowIdInput.value?.trim();
  if (!workflowId) {
    alert("请先输入 workflowId");
    return;
  }
  try {
    const data = await fetchWorkflow(workflowId);
    renderResponse(data);
    setIndicator("done", "已加载历史流程");
  } catch (error) {
    renderError(`查询 workflow 失败: ${error.message}`);
  }
});

toggleRawBtn.addEventListener("click", () => {
  rawEl.classList.toggle("collapsed");
  toggleRawBtn.textContent = rawEl.classList.contains("collapsed") ? "展开" : "折叠";
});

copyResultBtn.addEventListener("click", async () => {
  try {
    await navigator.clipboard.writeText(resultEl.textContent || "");
    copyResultBtn.textContent = "已复制";
    setTimeout(() => {
      copyResultBtn.textContent = "复制结果";
    }, 1200);
  } catch (error) {
    alert(`复制失败: ${error.message}`);
  }
});

document.querySelectorAll(".preset-btn").forEach((btn) => {
  btn.addEventListener("click", () => {
    const type = btn.dataset.preset;
    if (type === "weekend") {
      document.getElementById("preferences").value = "nature, food, easy walk";
      document.getElementById("budget").value = "2000";
      document.getElementById("transportMode").value = "self-driving";
      document.getElementById("extraRequirements").value = "two-day weekend, avoid intense schedule";
    } else if (type === "family") {
      document.getElementById("preferences").value = "family attractions, kid-friendly food";
      document.getElementById("budget").value = "3000";
      document.getElementById("transportMode").value = "self-driving";
      document.getElementById("extraRequirements").value = "children age 6 and 10, stroller friendly";
    } else if (type === "budget") {
      document.getElementById("preferences").value = "local food, free attractions";
      document.getElementById("budget").value = "1000";
      document.getElementById("transportMode").value = "public transport";
      document.getElementById("extraRequirements").value = "strict cost control, practical itinerary";
    }
  });
});

async function detectBackend() {
  const candidates = [
    window.location.origin,
    `${window.location.protocol}//${window.location.hostname}:8081`,
    `${window.location.protocol}//${window.location.hostname}:18081`,
    `${window.location.protocol}//${window.location.hostname}:18082`
  ];

  for (const base of candidates) {
    try {
      const res = await fetchWithTimeout(`${base}/app/ping`, {}, 3000);
      if (res.ok) {
        apiBaseUrlInput.value = base;
        setIndicator("done", `后端已连接: ${base}`);
        return;
      }
    } catch (_) {
      // try next
    }
  }
  setIndicator("failed", "未探测到可用后端，请手动填写后端地址");
}

detectBackendBtn.addEventListener("click", detectBackend);

renderDag([]);
detectBackend();
