
<?php
require_once __DIR__ . "/config.php";

$deviceId = GAN_DEVICE_ID;
$apiUrl = GAN_API_URL;
?>
<!DOCTYPE html>
<html lang="id">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">

<title>GAN SMART IRRIGATION IoT</title>

<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
<link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css" rel="stylesheet">

<link href="https://cdn.datatables.net/2.1.8/css/dataTables.bootstrap5.min.css" rel="stylesheet">

<script src="https://code.jquery.com/jquery-3.7.1.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.4/dist/chart.umd.min.js"></script>
<script src="https://cdn.datatables.net/2.1.8/js/dataTables.min.js"></script>
<script src="https://cdn.datatables.net/2.1.8/js/dataTables.bootstrap5.min.js"></script>

<style>
:root {
    --gan-green: #087f5b;
    --gan-dark: #123b32;
    --gan-orange: #f59e0b;
    --gan-bg: #f4f7f8;
}

body {
    background: var(--gan-bg);
    font-family: Arial, sans-serif;
    color: #253238;
}

.navbar {
    background: linear-gradient(110deg, #075e46, #11966c);
}

.brand-title {
    font-size: 19px;
    font-weight: 800;
    letter-spacing: .5px;
}

.brand-subtitle {
    font-size: 11px;
    opacity: .85;
}

.dashboard-container {
    max-width: 1500px;
    margin: auto;
    padding: 22px;
}

.section-title {
    font-size: 17px;
    font-weight: 800;
    margin-bottom: 15px;
    color: var(--gan-dark);
}

.sensor-card {
    border: 0;
    border-radius: 15px;
    background: #fff;
    box-shadow: 0 4px 15px rgba(20,50,40,.06);
    height: 100%;
    overflow: hidden;
}

.sensor-card .card-body {
    padding: 20px;
}

.sensor-icon {
    width: 46px;
    height: 46px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 22px;
}

.sensor-label {
    color: #718096;
    font-size: 13px;
    font-weight: 600;
}

.sensor-value {
    font-size: 30px;
    font-weight: 800;
    margin-top: 9px;
    color: #243b35;
}

.sensor-unit {
    font-size: 15px;
    color: #718096;
    font-weight: 600;
}

.sensor-footer {
    font-size: 11px;
    color: #84918d;
    margin-top: 8px;
}

.sensor-card { transition: transform .2s ease, box-shadow .2s ease; }
.sensor-card:hover { transform: translateY(-3px); box-shadow: 0 10px 25px rgba(20,50,40,.11); }
.sensor-state { display:inline-flex; align-items:center; border-radius:999px; padding:5px 10px; margin-top:10px; font-size:11px; font-weight:800; letter-spacing:.35px; }
.state-red { background:#fee2e2; color:#b91c1c; }
.state-yellow { background:#fef3c7; color:#92400e; }
.state-green { background:#dcfce7; color:#166534; }
.state-blue { background:#dbeafe; color:#1d4ed8; }
.sensor-card.tone-red { border-top:4px solid #dc2626; }
.sensor-card.tone-yellow { border-top:4px solid #eab308; }
.sensor-card.tone-green { border-top:4px solid #16a34a; }
.sensor-card.tone-blue { border-top:4px solid #2563eb; }
.tank-wrap { display:flex; align-items:center; justify-content:center; gap:14px; margin-top:12px; }
.tank { position:relative; width:76px; height:112px; border:4px solid #64748b; border-radius:13px 13px 17px 17px; overflow:hidden; background:linear-gradient(90deg,#f8fafc,#e2e8f0); box-shadow:inset 0 0 8px #94a3b855; }
.tank:before { content:""; position:absolute; left:-5px; right:-5px; top:-7px; height:10px; border:4px solid #64748b; border-radius:8px; background:#cbd5e1; z-index:2; }
.tank-water { position:absolute; bottom:0; left:0; right:0; height:0%; background:linear-gradient(180deg,#38bdf8,#0284c7); transition:height .5s ease,background .3s ease; }
.tank-water:after { content:""; position:absolute; left:-10%; top:-5px; width:120%; height:10px; border-radius:50%; background:rgba(255,255,255,.35); }
.tank-scale { display:flex; flex-direction:column; justify-content:space-between; height:105px; color:#94a3b8; font-size:10px; font-weight:700; }
.tank-reading { font-size:25px; font-weight:900; line-height:1; }
.sensor-description { color:#64748b; font-size:11px; margin-top:5px; }

.water-bg { background: #e0f2fe; color: #0284c7; }
.soil-bg { background: #dcfce7; color: #15803d; }
.temp-bg { background: #ffedd5; color: #ea580c; }
.humidity-bg { background: #ede9fe; color: #7c3aed; }

.panel {
    background: white;
    border-radius: 15px;
    padding: 20px;
    box-shadow: 0 4px 15px rgba(20,50,40,.06);
    margin-bottom: 20px;
}

.chart-container {
    position: relative;
    height: 280px;
}

.pump-panel {
    border: 1px solid #e5ece8;
    border-radius: 13px;
    padding: 17px;
    height: 100%;
}

.pump-status {
    font-size: 25px;
    font-weight: 800;
}

.status-on { color: #16a34a; }
.status-off { color: #dc2626; }

.form-label {
    font-size: 13px;
    font-weight: 700;
}

.form-select, .form-control {
    border-radius: 9px;
}

.btn-gan {
    background: var(--gan-green);
    color: white;
    border-radius: 9px;
    font-weight: 700;
}

.btn-gan:hover {
    background: #056747;
    color: white;
}

.table thead th {
    background: #edf5f1;
    color: #315348;
    font-size: 12px;
    white-space: nowrap;
}

.table tbody td {
    font-size: 12px;
    vertical-align: middle;
}

.connection-dot {
    display: inline-block;
    width: 9px;
    height: 9px;
    border-radius: 50%;
    margin-right: 6px;
    background: #9ca3af;
}

.connection-online { background: #16a34a; }
.connection-offline { background: #dc2626; }

footer {
    color: #84918d;
    font-size: 12px;
    padding: 20px;
    text-align: center;
}

@media(max-width:576px) {
    .dashboard-container { padding: 12px; }
    .sensor-value { font-size: 25px; }
    .panel { padding: 14px; }
}
</style>
</head>

<body>

<!-- ==================================================
     HEADER
================================================== -->

<nav class="navbar navbar-dark shadow-sm">
<div class="container-fluid px-4 py-2">

    <div class="d-flex align-items-center gap-3">
        <div class="bg-white rounded-3 p-2 text-success">
            <i class="bi bi-flower3 fs-3"></i>
        </div>

        <div class="text-white">
            <div class="brand-title">GAN Smart Irrigation</div>
            <div class="brand-subtitle">PT GOLDEN AGIN NUSA | IoT MONITORING SYSTEM</div>
        </div>
    </div>

    <div class="text-white text-end">
        <div class="small">
            <span id="connectionDot" class="connection-dot"></span>
            <span id="connectionText">Menghubungkan...</span>
        </div>
        <div class="small" id="clock">--:--:--</div>
    </div>

</div>
</nav>

<div class="dashboard-container">

<!-- ==================================================
     DEVICE INFORMATION
================================================== -->

<div class="d-flex flex-wrap justify-content-between align-items-center mb-4">

    <div>
        <h5 class="fw-bold mb-1">Dashboard Monitoring</h5>
        <div class="text-secondary small">
            Device ID:
            <strong><?php echo htmlspecialchars($deviceId); ?></strong>
        </div>
    </div>

    <div class="mt-2 mt-md-0">
        <span class="badge text-bg-success px-3 py-2">
            <i class="bi bi-cpu"></i> Device IoT
        </span>
    </div>

</div>

<!-- ==================================================
     SENSOR CARDS
================================================== -->

<div class="section-title">
    <i class="bi bi-speedometer2 me-2"></i>MONITORING SENSOR
</div>

<div class="row g-3 mb-4">

    <!-- WATER LEVEL -->
    <div class="col-6 col-lg-3"><div class="card sensor-card tone-blue" id="waterCard"><div class="card-body">
        <div class="d-flex justify-content-between align-items-center"><div class="sensor-label">WATER LEVEL</div><div class="sensor-icon water-bg"><i class="bi bi-water"></i></div></div>
        <div class="tank-wrap"><div class="tank"><div class="tank-water" id="tankWater"></div></div><div>
            <div class="tank-scale"><span>100%</span><span>75%</span><span>50%</span><span>25%</span><span>0%</span></div>
            <div class="tank-reading mt-2"><span id="waterLevel">--</span><span class="sensor-unit">%</span></div>
            <div id="waterState" class="sensor-state state-blue">MEMUAT...</div>
        </div></div>
        <div class="progress mt-3" style="height:6px"><div id="waterProgress" class="progress-bar bg-info" style="width:0%"></div></div>
        <div class="sensor-description">Ketinggian air tandon</div>
    </div></div></div>

    <!-- SOIL MOISTURE -->
    <div class="col-6 col-lg-3"><div class="card sensor-card" id="soilCard"><div class="card-body">
        <div class="d-flex justify-content-between align-items-center"><div class="sensor-label">SOIL MOISTURE</div><div class="sensor-icon soil-bg"><i class="bi bi-flower1"></i></div></div>
        <div class="text-center py-2"><i id="soilBigIcon" class="bi bi-moisture display-3 text-success"></i>
            <div class="sensor-value"><span id="soilMoisture">--</span><span class="sensor-unit">%</span></div>
            <div id="soilState" class="sensor-state state-green">MEMUAT...</div></div>
        <div class="progress mt-2" style="height:7px"><div id="soilProgress" class="progress-bar bg-success" style="width:0%"></div></div>
        <div class="sensor-description" id="soilDescription">Kondisi kelembaban tanah</div>
    </div></div></div>

    <!-- TEMPERATURE -->
    <div class="col-6 col-lg-3"><div class="card sensor-card" id="temperatureCard"><div class="card-body">
        <div class="d-flex justify-content-between align-items-center"><div class="sensor-label">TEMPERATURE</div><div class="sensor-icon temp-bg"><i class="bi bi-thermometer-half"></i></div></div>
        <div class="text-center py-2"><i id="temperatureIcon" class="bi bi-thermometer-half display-3 text-success"></i>
            <div class="sensor-value"><span id="temperature">--</span><span class="sensor-unit">°C</span></div>
            <div id="temperatureState" class="sensor-state state-green">MEMUAT...</div></div>
        <div class="sensor-description" id="temperatureDescription">Temperatur lingkungan</div>
    </div></div></div>

    <!-- HUMIDITY -->
    <div class="col-6 col-lg-3"><div class="card sensor-card" id="humidityCard"><div class="card-body">
        <div class="d-flex justify-content-between align-items-center"><div class="sensor-label">HUMIDITY</div><div class="sensor-icon humidity-bg"><i class="bi bi-cloud-drizzle"></i></div></div>
        <div class="text-center py-2"><i id="humidityIcon" class="bi bi-cloud display-3 text-success"></i>
            <div class="sensor-value"><span id="humidity">--</span><span class="sensor-unit">%</span></div>
            <div id="humidityState" class="sensor-state state-green">MEMUAT...</div></div>
        <div class="sensor-description" id="humidityDescription">Kelembaban udara</div>
    </div></div></div>
</div>

<div class="small text-secondary mb-4">
    <i class="bi bi-clock-history"></i>
    Update sensor terakhir:
    <strong id="lastUpdate">Belum tersedia</strong>
</div>

<!-- ==================================================
     CHARTS
================================================== -->

<div class="section-title">
    <i class="bi bi-graph-up me-2"></i>GRAFIK MONITORING
</div>

<div class="row g-3 mb-4">

    <div class="col-lg-6">
        <div class="panel">
            <h6 class="fw-bold">Water Level & Soil Moisture</h6>
            <div class="chart-container">
                <canvas id="chartMoisture"></canvas>
            </div>
        </div>
    </div>

    <div class="col-lg-6">
        <div class="panel">
            <h6 class="fw-bold">Temperature & Humidity</h6>
            <div class="chart-container">
                <canvas id="chartClimate"></canvas>
            </div>
        </div>
    </div>

</div>

<!-- ==================================================
     PUMP CONTROL
================================================== -->

<div class="section-title">
    <i class="bi bi-sliders me-2"></i>CONTROL PUMP
</div>

<div class="panel">

    <div class="row g-3">

        <!-- WATER PUMP -->
        <div class="col-lg-6">

            <div class="pump-panel">

                <div class="d-flex justify-content-between align-items-center mb-3">

                    <div>
                        <h6 class="fw-bold mb-1">
                            <i class="bi bi-water text-primary"></i>
                            WATER PUMP
                        </h6>
                        <div class="small text-secondary">
                            Pompa pengisian tandon air
                        </div>
                    </div>

                    <span id="waterPumpBadge" class="badge text-bg-secondary">
                        OFF
                    </span>

                </div>

                <div class="pump-status mb-3" id="waterPumpStatus">
                    OFF
                </div>

                <label class="form-label">Mode Operasional</label>

                <select id="waterMode" class="form-select mb-3">
                    <option value="AUTO">AUTO</option>
                    <option value="MANUAL">MANUAL</option>
                    <option value="TIMER">TIMER</option>
                </select>

                <div id="waterManualControl" class="mb-3" style="display:none">
                    <label class="form-label">Manual Switch</label>

                    <div class="form-check form-switch">
                        <input class="form-check-input"
                               type="checkbox"
                               id="waterPumpSwitch"
                               style="width:45px;height:23px">

                        <label class="form-check-label ms-2"
                               for="waterPumpSwitch">
                            Pompa Air ON/OFF
                        </label>
                    </div>
                </div>

                <div class="row g-2">

                    <div class="col-6">
                        <label class="form-label">ON jika level ≤ (%)</label>
                        <input type="number" id="waterOnAt"
                               class="form-control"
                               min="0" max="100" value="50">
                    </div>

                    <div class="col-6">
                        <label class="form-label">OFF jika level ≥ (%)</label>
                        <input type="number" id="waterOffAt"
                               class="form-control"
                               min="0" max="100" value="80">
                    </div>

                </div>

            </div>

        </div>

        <!-- SOIL PUMP -->
        <div class="col-lg-6">

            <div class="pump-panel">

                <div class="d-flex justify-content-between align-items-center mb-3">

                    <div>
                        <h6 class="fw-bold mb-1">
                            <i class="bi bi-flower1 text-success"></i>
                            SOIL IRRIGATION PUMP
                        </h6>

                        <div class="small text-secondary">
                            Pompa penyiraman tanaman
                        </div>
                    </div>

                    <span id="soilPumpBadge" class="badge text-bg-secondary">
                        OFF
                    </span>

                </div>

                <div class="pump-status mb-3" id="soilPumpStatus">
                    OFF
                </div>

                <label class="form-label">Mode Operasional</label>

                <select id="soilMode" class="form-select mb-3">
                    <option value="AUTO">AUTO</option>
                    <option value="MANUAL">MANUAL</option>
                </select>

                <div id="soilManualControl" class="mb-3" style="display:none">
                    <label class="form-label">Manual Switch</label>

                    <div class="form-check form-switch">
                        <input class="form-check-input"
                               type="checkbox"
                               id="soilPumpSwitch"
                               style="width:45px;height:23px">

                        <label class="form-check-label ms-2"
                               for="soilPumpSwitch">
                            Pompa Penyiraman ON/OFF
                        </label>
                    </div>
                </div>

                <div class="row g-2">

                    <div class="col-6">
                        <label class="form-label">ON jika tanah ≤ (%)</label>
                        <input type="number" id="soilOnAt"
                               class="form-control"
                               min="0" max="100" value="70">
                    </div>

                    <div class="col-6">
                        <label class="form-label">OFF jika tanah ≥ (%)</label>
                        <input type="number" id="soilOffAt"
                               class="form-control"
                               min="0" max="100" value="85">
                    </div>

                </div>

            </div>

        </div>

    </div>

    <!-- TIMER -->
    <div class="border rounded-3 p-3 mt-3">

        <h6 class="fw-bold">
            <i class="bi bi-clock text-warning"></i>
            TIMER PENYIRAMAN POMPA AIR
        </h6>

        <div class="form-check form-switch mb-3">
            <input class="form-check-input"
                   type="checkbox"
                   id="timerEnabled"
                   style="width:45px;height:23px">

            <label class="form-check-label ms-2"
                   for="timerEnabled">
                Aktifkan Timer
            </label>
        </div>

        <div class="row g-3">

            <div class="col-md-4">
                <label class="form-label">Jam Mulai</label>
                <input type="time" id="timerStart"
                       class="form-control" value="06:00">
            </div>

            <div class="col-md-4">
                <label class="form-label">Jam Selesai</label>
                <input type="time" id="timerEnd"
                       class="form-control" value="06:30">
            </div>

            <div class="col-md-4 d-flex align-items-end">
                <div class="small text-secondary">
                    Timer berjalan sesuai waktu WIB ESP32.
                </div>
            </div>

        </div>

    </div>

    <div class="d-flex flex-wrap gap-2 mt-4">

        <button class="btn btn-gan px-4" id="saveControl">
            <i class="bi bi-save"></i>
            SIMPAN PENGATURAN
        </button>

        <button class="btn btn-outline-secondary px-4" id="refreshButton">
            <i class="bi bi-arrow-clockwise"></i>
            REFRESH DATA
        </button>

    </div>

    <div id="controlMessage" class="mt-3 small"></div>

</div>

<!-- ==================================================
     HISTORY TABLE
================================================== -->

<div class="section-title">
    <i class="bi bi-table me-2"></i>HISTORI DATA SENSOR
</div>

<div class="panel">

    <div class="d-flex justify-content-between flex-wrap gap-2 mb-3">

        <h6 class="fw-bold mb-0">Data Monitoring IoT</h6>

        <button class="btn btn-sm btn-outline-success"
                id="refreshHistory">
            <i class="bi bi-arrow-clockwise"></i>
            Refresh Tabel
        </button>

    </div>

    <div class="table-responsive">

        <table id="historyTable"
               class="table table-striped table-hover w-100">

            <thead>
                <tr>
                    <th>No</th>
                    <th>Waktu</th>
                    <th>Water Level</th>
                    <th>Soil Moisture</th>
                    <th>Temperature</th>
                    <th>Humidity</th>
                    <th>Water Pump</th>
                    <th>Soil Pump</th>
                    <th>Status Sensor</th>
                </tr>
            </thead>

            <tbody></tbody>

        </table>

    </div>

</div>

</div>

<footer>
    GAN SMART IRRIGATION IoT<br>
    PT GOLDEN AGIN NUSA | Digital Transformation Department
</footer>

<script>

// ======================================================
// CONFIGURATION
// ======================================================

const API_URL = <?php echo json_encode($apiUrl); ?>;
const DEVICE_ID = <?php echo json_encode($deviceId); ?>;

let historyTable = null;
let chartMoisture = null;
let chartClimate = null;

let lastSensorId = null;
let controlLoaded = false;

// ======================================================
// FORMAT
// ======================================================

function numberValue(value, decimals = 1) {

    const number = Number(value);

    return Number.isFinite(number)
        ? number.toFixed(decimals)
        : "--";
}

function setConnection(online) {

    $("#connectionDot")
        .removeClass("connection-online connection-offline")
        .addClass(online ? "connection-online" : "connection-offline");

    $("#connectionText").text(
        online ? "SERVER ONLINE" : "SERVER OFFLINE"
    );
}

function showPumpStatus(id, badgeId, state) {

    const isOn = Number(state) === 1 || state === true;

    $(id)
        .text(isOn ? "ON" : "OFF")
        .removeClass("status-on status-off")
        .addClass(isOn ? "status-on" : "status-off");

    $(badgeId)
        .text(isOn ? "ON" : "OFF")
        .removeClass("text-bg-success text-bg-secondary")
        .addClass(isOn ? "text-bg-success" : "text-bg-secondary");
}

// ======================================================
// UPDATE SENSOR CARDS
// ======================================================

function setSensorState(cardId, stateId, tone, label, descriptionId, description) {
    $(cardId).removeClass("tone-red tone-yellow tone-green tone-blue").addClass("tone-" + tone);
    $(stateId).removeClass("state-red state-yellow state-green state-blue").addClass("state-" + tone).text(label);
    if (descriptionId && description) $(descriptionId).text(description);
}

function updateCards(d) {
    const water = Math.max(0, Math.min(100, Number(d.water_level) || 0));
    const soil = Math.max(0, Math.min(100, Number(d.soil_moisture) || 0));
    const temp = Number(d.temperature);
    const humidity = Number(d.humidity);

    $("#waterLevel").text(numberValue(water));
    $("#soilMoisture").text(numberValue(soil));
    $("#temperature").text(numberValue(temp));
    $("#humidity").text(numberValue(humidity));

    let waterTone, waterColor, waterLabel;
    if (water <= 25) { waterTone="red"; waterColor="linear-gradient(180deg,#f87171,#dc2626)"; waterLabel="AIR RENDAH"; }
    else if (water <= 50) { waterTone="yellow"; waterColor="linear-gradient(180deg,#fde047,#eab308)"; waterLabel="AIR SEDANG"; }
    else if (water < 95) { waterTone="green"; waterColor="linear-gradient(180deg,#4ade80,#16a34a)"; waterLabel="AIR CUKUP"; }
    else { waterTone="blue"; waterColor="linear-gradient(180deg,#38bdf8,#2563eb)"; waterLabel="TANGKI PENUH"; }
    $("#tankWater").css({height:water+"%",background:waterColor});
    $("#waterProgress").css({width:water+"%",background:waterTone==="yellow"?"#eab308":waterTone==="red"?"#dc2626":waterTone==="green"?"#16a34a":"#2563eb"});
    setSensorState("#waterCard","#waterState",waterTone,waterLabel);

    let soilTone, soilLabel, soilDesc, soilIcon;
    if (soil < 30) { soilTone="red"; soilLabel="KERING"; soilDesc="Tanah kering — tanaman membutuhkan penyiraman"; soilIcon="bi bi-sun"; }
    else if (soil <= 70) { soilTone="green"; soilLabel="LEMBAB"; soilDesc="Kelembaban tanah cukup dan terjaga"; soilIcon="bi bi-flower1"; }
    else { soilTone="blue"; soilLabel="BASAH"; soilDesc="Tanah sangat basah — penyiraman dapat dihentikan"; soilIcon="bi bi-droplet-fill"; }
    $("#soilBigIcon").attr("class",soilIcon+" display-3 "+(soilTone==="red"?"text-danger":soilTone==="blue"?"text-primary":"text-success"));
    $("#soilProgress").css({width:soil+"%",background:soilTone==="red"?"#dc2626":soilTone==="blue"?"#2563eb":"#16a34a"});
    setSensorState("#soilCard","#soilState",soilTone,soilLabel,"#soilDescription",soilDesc);

    let tempTone,tempLabel,tempDesc,tempIcon;
    if (!Number.isFinite(temp)) { tempTone="blue"; tempLabel="TIDAK TERSEDIA"; tempDesc="Data temperatur belum tersedia"; tempIcon="bi bi-thermometer"; }
    else if (temp < 20) { tempTone="blue"; tempLabel="DINGIN"; tempDesc="Suhu lingkungan rendah"; tempIcon="bi bi-thermometer-low"; }
    else if (temp <= 30) { tempTone="green"; tempLabel="NORMAL"; tempDesc="Suhu lingkungan dalam kondisi normal"; tempIcon="bi bi-thermometer-half"; }
    else { tempTone="red"; tempLabel="PANAS"; tempDesc="Suhu lingkungan tinggi"; tempIcon="bi bi-thermometer-high"; }
    $("#temperatureIcon").attr("class",tempIcon+" display-3 "+(tempTone==="red"?"text-danger":tempTone==="blue"?"text-primary":"text-success"));
    setSensorState("#temperatureCard","#temperatureState",tempTone,tempLabel,"#temperatureDescription",tempDesc);

    let humTone,humLabel,humDesc,humIcon;
    if (!Number.isFinite(humidity)) { humTone="blue"; humLabel="TIDAK TERSEDIA"; humDesc="Data kelembaban belum tersedia"; humIcon="bi bi-cloud"; }
    else if (humidity < 40) { humTone="blue"; humLabel="RENDAH"; humDesc="Udara relatif kering"; humIcon="bi bi-cloud-sun"; }
    else if (humidity <= 70) { humTone="green"; humLabel="NORMAL"; humDesc="Kelembaban udara normal"; humIcon="bi bi-cloud-check"; }
    else { humTone="red"; humLabel="TINGGI"; humDesc="Kelembaban udara tinggi"; humIcon="bi bi-cloud-rain-heavy"; }
    $("#humidityIcon").attr("class",humIcon+" display-3 "+(humTone==="red"?"text-danger":humTone==="blue"?"text-primary":"text-success"));
    setSensorState("#humidityCard","#humidityState",humTone,humLabel,"#humidityDescription",humDesc);

    $("#lastUpdate").text(d.created_at || "-");
    showPumpStatus("#waterPumpStatus","#waterPumpBadge",d.water_pump);
    showPumpStatus("#soilPumpStatus","#soilPumpBadge",d.soil_pump);
}

// ======================================================
// CREATE CHART
// ======================================================

function createCharts() {

    const commonOptions = {
        responsive: true,
        maintainAspectRatio: false,
        interaction: {
            mode: "index",
            intersect: false
        },
        plugins: {
            legend: {
                position: "bottom"
            }
        },
        scales: {
            x: {
                ticks: {
                    maxTicksLimit: 8
                }
            }
        }
    };

    chartMoisture = new Chart(
        document.getElementById("chartMoisture"),
        {
            type: "line",
            data: {
                labels: [],
                datasets: [
                    {
                        label: "Water Level (%)",
                        data: [],
                        borderColor: "#0284c7",
                        backgroundColor: "rgba(2,132,199,.10)",
                        tension: .35,
                        fill: true
                    },
                    {
                        label: "Soil Moisture (%)",
                        data: [],
                        borderColor: "#16a34a",
                        backgroundColor: "rgba(22,163,74,.10)",
                        tension: .35,
                        fill: true
                    }
                ]
            },
            options: {
                ...commonOptions,
                scales: {
                    ...commonOptions.scales,
                    y: {
                        min: 0,
                        max: 100,
                        title: {
                            display: true,
                            text: "Persentase (%)"
                        }
                    }
                }
            }
        }
    );

    chartClimate = new Chart(
        document.getElementById("chartClimate"),
        {
            type: "line",
            data: {
                labels: [],
                datasets: [
                    {
                        label: "Temperature (°C)",
                        data: [],
                        borderColor: "#ea580c",
                        backgroundColor: "rgba(234,88,12,.10)",
                        tension: .35,
                        yAxisID: "y"
                    },
                    {
                        label: "Humidity (%)",
                        data: [],
                        borderColor: "#7c3aed",
                        backgroundColor: "rgba(124,58,237,.10)",
                        tension: .35,
                        yAxisID: "y1"
                    }
                ]
            },
            options: {
                ...commonOptions,
                scales: {
                    x: commonOptions.scales.x,
                    y: {
                        position: "left",
                        title: {
                            display: true,
                            text: "Temperature (°C)"
                        }
                    },
                    y1: {
                        position: "right",
                        min: 0,
                        max: 100,
                        grid: {
                            drawOnChartArea: false
                        },
                        title: {
                            display: true,
                            text: "Humidity (%)"
                        }
                    }
                }
            }
        }
    );
}

// ======================================================
// LOAD LATEST DATA
// ======================================================

async function loadLatest() {

    try {

        const response = await fetch(
            API_URL +
            "?action=latest&device_id=" +
            encodeURIComponent(DEVICE_ID) +
            "&_=" + Date.now()
        );

        if (!response.ok) {
            throw new Error("HTTP " + response.status);
        }

        const result = await readApiResponse(response);

        if (!result.success || !result.data || !result.data.id) {
            setConnection(false);
            return;
        }

        setConnection(true);

        const d = result.data;

        updateCards(d);

        if (String(d.id) !== String(lastSensorId)) {
            lastSensorId = d.id;
            loadHistory();
        }

    } catch (error) {

        console.error("Latest API:", error);

        setConnection(false);
    }
}

// ======================================================
// LOAD CONTROL CONFIGURATION
// ======================================================

async function loadControl() {

    try {

        const response = await fetch(
            API_URL +
            "?action=control&device_id=" +
            encodeURIComponent(DEVICE_ID) +
            "&_=" + Date.now()
        );

        if (!response.ok) {
            throw new Error("HTTP " + response.status);
        }

        const d = await readApiResponse(response);

        if (!controlLoaded) {

            $("#waterMode").val(d.water_mode || "AUTO");
            $("#soilMode").val(d.soil_mode || "AUTO");

            $("#waterPumpSwitch").prop(
                "checked",
                Number(d.water_pump) === 1
            );

            $("#soilPumpSwitch").prop(
                "checked",
                Number(d.soil_pump) === 1
            );

            $("#timerEnabled").prop(
                "checked",
                d.timer_enabled === true ||
                Number(d.timer_enabled) === 1
            );

            $("#timerStart").val(
                String(d.timer_start || "06:00").substring(0, 5)
            );

            $("#timerEnd").val(
                String(d.timer_end || "06:30").substring(0, 5)
            );

            $("#waterOnAt").val(d.water_on_at ?? 50);
            $("#waterOffAt").val(d.water_off_at ?? 80);

            $("#soilOnAt").val(d.soil_on_at ?? 70);
            $("#soilOffAt").val(d.soil_off_at ?? 85);

            controlLoaded = true;

            updateModeFields();
        }

    } catch (error) {

        console.error("Control API:", error);
    }
}

// ======================================================
// MODE FIELD
// ======================================================

function updateModeFields() {

    const waterMode = $("#waterMode").val();
    const soilMode = $("#soilMode").val();

    $("#waterManualControl").toggle(
        waterMode === "MANUAL"
    );

    $("#soilManualControl").toggle(
        soilMode === "MANUAL"
    );
}

$("#waterMode, #soilMode").on("change", updateModeFields);

// Parser respons API aman: mendeteksi HTML/PHP warning dan menampilkan pesan yang terbaca.
async function readApiResponse(response) {
    const raw = await response.text();
    try {
        const result = JSON.parse(raw.replace(/^\uFEFF/, "").trim());
        if (!response.ok) throw new Error(result.message || ("HTTP " + response.status));
        return result;
    } catch (error) {
        if (error instanceof SyntaxError) {
            const preview = raw.replace(/<[^>]*>/g, " ").replace(/\s+/g, " ").trim().slice(0, 240);
            throw new Error("Respons API bukan JSON (HTTP " + response.status + "). " +
                (preview || "Periksa error PHP pada endpoint API.") +
                " Periksa API_URL, config.php, dan log PHP.");
        }
        throw error;
    }
}

// ======================================================
// SAVE CONTROL
// ======================================================

async function saveControl() {

    const waterMode = $("#waterMode").val();
    const soilMode = $("#soilMode").val();

    const waterOn = Number($("#waterOnAt").val());
    const waterOff = Number($("#waterOffAt").val());

    const soilOn = Number($("#soilOnAt").val());
    const soilOff = Number($("#soilOffAt").val());

    if (
        !Number.isFinite(waterOn) ||
        !Number.isFinite(waterOff) ||
        !Number.isFinite(soilOn) ||
        !Number.isFinite(soilOff) ||
        waterOn < 0 || waterOn > 100 ||
        waterOff < 0 || waterOff > 100 ||
        soilOn < 0 || soilOn > 100 ||
        soilOff < 0 || soilOff > 100 ||
        waterOn >= waterOff ||
        soilOn >= soilOff
    ) {

        alert("Periksa kembali nilai ambang batas otomatis.");

        return;
    }

    const payload = {

        action: "update_control",

        device_id: DEVICE_ID,

        water_mode: waterMode,
        soil_mode: soilMode,

        water_pump:
            $("#waterPumpSwitch").is(":checked") ? 1 : 0,

        soil_pump:
            $("#soilPumpSwitch").is(":checked") ? 1 : 0,

        timer_enabled:
            $("#timerEnabled").is(":checked") ? 1 : 0,

        timer_start: $("#timerStart").val(),
        timer_end: $("#timerEnd").val(),

        water_on_at: waterOn,
        water_off_at: waterOff,

        soil_on_at: soilOn,
        soil_off_at: soilOff
    };

    $("#saveControl")
        .prop("disabled", true)
        .html('<span class="spinner-border spinner-border-sm"></span> Menyimpan...');

    try {

        const response = await fetch(API_URL, {

            method: "POST",

            headers: {
                "Content-Type": "application/json"
            },

            body: JSON.stringify(payload)
        });

        const result = await readApiResponse(response);

        if (!result.success) {
            throw new Error(result.message || "Gagal menyimpan kontrol");
        }

        $("#controlMessage")
            .removeClass("text-danger")
            .addClass("text-success")
            .text("Pengaturan berhasil disimpan. ESP32 akan mengambil perintah berikutnya.");

        controlLoaded = false;

        await loadControl();

    } catch (error) {

        console.error("Save control error:", error);
        $("#controlMessage")
            .removeClass("text-success")
            .addClass("text-danger")
            .text("Gagal menyimpan: " + error.message);

    } finally {

        $("#saveControl")
            .prop("disabled", false)
            .html('<i class="bi bi-save"></i> SIMPAN PENGATURAN');
    }
}

$("#saveControl").on("click", saveControl);

// ======================================================
// LOAD HISTORY
// ======================================================

async function loadHistory() {

    try {

        const response = await fetch(
            API_URL +
            "?action=history&device_id=" +
            encodeURIComponent(DEVICE_ID) +
            "&limit=100&_=" + Date.now()
        );

        if (!response.ok) {
            throw new Error("HTTP " + response.status);
        }

        const result = await readApiResponse(response);

        if (!result.success || !Array.isArray(result.data)) {
            throw new Error(result.message || "Invalid history response");
        }

        const rows = result.data.slice().reverse();

        if (!chartMoisture || !chartClimate) {
            return;
        }

        const labels = rows.map(row => {

            if (!row.created_at) return "";

            const date = new Date(
                String(row.created_at).replace(" ", "T")
            );

            return Number.isNaN(date.getTime())
                ? row.created_at
                : date.toLocaleTimeString("id-ID", {
                    hour: "2-digit",
                    minute: "2-digit"
                });
        });

        chartMoisture.data.labels = labels;

        chartMoisture.data.datasets[0].data =
            rows.map(row => Number(row.water_level));

        chartMoisture.data.datasets[1].data =
            rows.map(row => Number(row.soil_moisture));

        chartMoisture.update();

        chartClimate.data.labels = labels;

        chartClimate.data.datasets[0].data =
            rows.map(row => Number(row.temperature));

        chartClimate.data.datasets[1].data =
            rows.map(row => Number(row.humidity));

        chartClimate.update();

        renderHistoryTable(result.data);

    } catch (error) {

        console.error("History API:", error);
    }
}

// ======================================================
// DATATABLE
// ======================================================

function renderHistoryTable(rows) {

    const tbody = $("#historyTable tbody");

    tbody.empty();

    rows.forEach((row, index) => {

        const waterPump = Number(row.water_pump) === 1;
        const soilPump = Number(row.soil_pump) === 1;

        const status = row.sensor_status || "-";

        const tr = $("<tr>");

        const values = [
            index + 1,
            row.created_at || "-",
            numberValue(row.water_level) + "%",
            numberValue(row.soil_moisture) + "%",
            numberValue(row.temperature) + " °C",
            numberValue(row.humidity) + "%",
            waterPump ? "ON" : "OFF",
            soilPump ? "ON" : "OFF",
            status
        ];

        values.forEach((value, i) => {

            const td = $("<td>");

            if (i === 6 || i === 7) {

                td.append(
                    $("<span>")
                        .addClass(
                            "badge " +
                            (value === "ON"
                                ? "text-bg-success"
                                : "text-bg-secondary")
                        )
                        .text(value)
                );

            } else {

                td.text(value);
            }

            tr.append(td);
        });

        tbody.append(tr);
    });

    if (historyTable) {
        historyTable.destroy();
    }

    historyTable = new DataTable("#historyTable", {

        pageLength: 10,

        order: [[1, "desc"]],

        language: {
            search: "Cari:",
            lengthMenu: "Tampilkan _MENU_ data",
            info: "Menampilkan _START_ - _END_ dari _TOTAL_ data",
            infoEmpty: "Tidak ada data",
            zeroRecords: "Data tidak ditemukan",
            paginate: {
                first: "Awal",
                last: "Akhir",
                next: "Berikutnya",
                previous: "Sebelumnya"
            }
        }
    });
}

// ======================================================
// CLOCK
// ======================================================

function updateClock() {

    $("#clock").text(
        new Date().toLocaleString("id-ID", {
            timeZone: "Asia/Jakarta",
            dateStyle: "medium",
            timeStyle: "medium"
        })
    );
}

// ======================================================
// REFRESH BUTTONS
// ======================================================

$("#refreshButton").on("click", async function() {

    await loadLatest();
    await loadControl();
    await loadHistory();

});

$("#refreshHistory").on("click", loadHistory);

// ======================================================
// INITIALIZATION
// ======================================================

$(document).ready(function() {

    createCharts();

    updateClock();

    loadLatest();

    loadControl();

    loadHistory();

    setInterval(updateClock, 1000);

    // Latest sensor
    setInterval(loadLatest, 5000);

    // Refresh history and charts
    setInterval(loadHistory, 30000);

    // Do not overwrite unsaved form values
    // Controls are loaded once during initialization.
});

</script>

</body>
</html>
