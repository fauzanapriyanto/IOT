
<?php
// ======================================================
// GAN SMART IRRIGATION IoT API
// PT GOLDEN AGIN NUSA
// ESP32 + PHP + MYSQL
// ======================================================

require_once __DIR__ . "/config.php";

date_default_timezone_set("Asia/Jakarta");

// Jangan tampilkan warning PHP ke respons API.
// Error tetap dicatat di PHP error log.
ini_set("display_errors", "0");
ini_set("log_errors", "1");

header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type");

// ======================================================
// RESPONSE JSON
// ======================================================

function responseJSON($success, $message, $data = [])
{
    if (!headers_sent()) {
        header("Content-Type: application/json; charset=UTF-8");
    }

    echo json_encode([
        "success" => (bool)$success,
        "message" => $message,
        "data" => $data
    ], JSON_UNESCAPED_UNICODE);

    exit;
}

// ======================================================
// DATABASE CHECK
// ======================================================

if (!isset($conn) || !($conn instanceof mysqli)) {
    http_response_code(500);
    responseJSON(false, "Database connection not available");
}

if ($conn->connect_error) {
    http_response_code(500);
    responseJSON(false, "Database connection failed");
}

// ======================================================
// REQUEST METHOD
// ======================================================

$method = $_SERVER["REQUEST_METHOD"] ?? "GET";

// Preflight request
if ($method === "OPTIONS") {
    http_response_code(200);
    exit;
}

$action = $_GET["action"] ?? "";

// ======================================================
// GET CONTROL
// ======================================================

if ($method === "GET" && $action === "control") {

    $device_id = $_GET["device_id"] ?? "GAN-IRRIGATION-001";

    $stmt = $conn->prepare("
        SELECT
            device_id,
            water_mode,
            soil_mode,
            water_pump,
            soil_pump,
            timer_enabled,
            timer_start,
            timer_end,
            water_on_at,
            water_off_at,
            soil_on_at,
            soil_off_at
        FROM smart_farming_control
        WHERE device_id = ?
        LIMIT 1
    ");

    if (!$stmt) {
        http_response_code(500);
        responseJSON(false, "Prepare control query failed: " . $conn->error);
    }

    $stmt->bind_param("s", $device_id);

    if (!$stmt->execute()) {
        http_response_code(500);
        responseJSON(false, "Execute control query failed: " . $stmt->error);
    }

    $result = $stmt->get_result();

    if ($result && $result->num_rows > 0) {

        $data = $result->fetch_assoc();

        $data["water_pump"] = (int)$data["water_pump"];
        $data["soil_pump"] = (int)$data["soil_pump"];
        $data["timer_enabled"] = (bool)$data["timer_enabled"];

        $data["water_on_at"] = (float)$data["water_on_at"];
        $data["water_off_at"] = (float)$data["water_off_at"];
        $data["soil_on_at"] = (float)$data["soil_on_at"];
        $data["soil_off_at"] = (float)$data["soil_off_at"];

    } else {

        $data = [
            "device_id" => $device_id,
            "water_mode" => "AUTO",
            "soil_mode" => "AUTO",
            "water_pump" => 0,
            "soil_pump" => 0,
            "timer_enabled" => false,
            "timer_start" => "06:00",
            "timer_end" => "06:30",
            "water_on_at" => 50,
            "water_off_at" => 80,
            "soil_on_at" => 70,
            "soil_off_at" => 85
        ];
    }

    $stmt->close();
    $conn->close();

    echo json_encode($data, JSON_UNESCAPED_UNICODE);
    exit;
}

// ======================================================
// GET LATEST SENSOR DATA
// ======================================================

if ($method === "GET" && $action === "latest") {

    $device_id = $_GET["device_id"] ?? "GAN-IRRIGATION-001";

    $stmt = $conn->prepare("
        SELECT *
        FROM smart_farming
        WHERE device_id = ?
        ORDER BY id DESC
        LIMIT 1
    ");

    if (!$stmt) {
        http_response_code(500);
        responseJSON(false, "Prepare latest query failed: " . $conn->error);
    }

    $stmt->bind_param("s", $device_id);

    if (!$stmt->execute()) {
        http_response_code(500);
        responseJSON(false, "Latest query failed: " . $stmt->error);
    }

    $result = $stmt->get_result();
    $data = $result ? $result->fetch_assoc() : [];

    $stmt->close();

    responseJSON(true, "Latest sensor data", $data ?: []);
}

// ======================================================
// GET HISTORY
// ======================================================

if ($method === "GET" && $action === "history") {

    $device_id = $_GET["device_id"] ?? "GAN-IRRIGATION-001";

    $limit = isset($_GET["limit"])
        ? (int)$_GET["limit"]
        : 100;

    $limit = max(1, min($limit, 1000));

    $stmt = $conn->prepare("
        SELECT
            id,
            device_id,
            water_level,
            soil_moisture,
            temperature,
            humidity,
            water_pump,
            soil_pump,
            sensor_status,
            created_at
        FROM smart_farming
        WHERE device_id = ?
        ORDER BY id DESC
        LIMIT ?
    ");

    if (!$stmt) {
        http_response_code(500);
        responseJSON(false, "Prepare history query failed: " . $conn->error);
    }

    $stmt->bind_param("si", $device_id, $limit);

    if (!$stmt->execute()) {
        http_response_code(500);
        responseJSON(false, "History query failed: " . $stmt->error);
    }

    $result = $stmt->get_result();

    $rows = [];

    while ($result && ($row = $result->fetch_assoc())) {
        $rows[] = $row;
    }

    $stmt->close();

    responseJSON(true, "History sensor data", $rows);
}

// ======================================================
// GET DASHBOARD SUMMARY
// ======================================================

if ($method === "GET" && $action === "summary") {

    $device_id = $_GET["device_id"] ?? "GAN-IRRIGATION-001";

    $stmt = $conn->prepare("
        SELECT
            COUNT(*) AS total_data,
            AVG(water_level) AS avg_water,
            AVG(soil_moisture) AS avg_soil,
            AVG(temperature) AS avg_temperature,
            AVG(humidity) AS avg_humidity,
            MAX(created_at) AS last_update
        FROM smart_farming
        WHERE device_id = ?
          AND created_at >= NOW() - INTERVAL 24 HOUR
    ");

    if (!$stmt) {
        http_response_code(500);
        responseJSON(false, "Prepare summary query failed: " . $conn->error);
    }

    $stmt->bind_param("s", $device_id);

    if (!$stmt->execute()) {
        http_response_code(500);
        responseJSON(false, "Summary query failed: " . $stmt->error);
    }

    $result = $stmt->get_result();
    $data = $result ? $result->fetch_assoc() : [];

    $stmt->close();

    responseJSON(true, "Dashboard summary", $data ?: []);
}

// ======================================================
// POST REQUEST
// ======================================================

if ($method === "POST") {

    $input = file_get_contents("php://input");
    $data = json_decode($input, true);

    if (!is_array($data)) {
        http_response_code(400);
        responseJSON(false, "Invalid JSON request");
    }

    $action = $data["action"] ?? "insert";

    // ==================================================
    // INSERT SENSOR DATA FROM ESP32
    // ==================================================

    if ($action === "insert") {

        $device_id = $data["device_id"] ?? "GAN-IRRIGATION-001";

        $water_level = (float)($data["water_level"] ?? 0);
        $soil_moisture = (float)($data["soil_moisture"] ?? 0);
        $temperature = (float)($data["temperature"] ?? 0);
        $humidity = (float)($data["humidity"] ?? 0);

        $raw_water = (int)($data["raw_water"] ?? 0);
        $raw_soil = (int)($data["raw_soil"] ?? 0);

        $water_sensor_status = $data["water_sensor_status"] ?? "UNKNOWN";
        $soil_sensor_status = $data["soil_sensor_status"] ?? "UNKNOWN";
        $dht_sensor_status = $data["dht_sensor_status"] ?? "UNKNOWN";

        $water_pump = (int)($data["water_pump"] ?? 0);
        $soil_pump = (int)($data["soil_pump"] ?? 0);

        $water_mode = strtoupper($data["water_mode"] ?? "AUTO");
        $soil_mode = strtoupper($data["soil_mode"] ?? "AUTO");

        // Validasi sensor
        if (
            $water_level < 0 || $water_level > 100 ||
            $soil_moisture < 0 || $soil_moisture > 100 ||
            $humidity < 0 || $humidity > 100 ||
            $temperature < -40 || $temperature > 100
        ) {
            http_response_code(422);
            responseJSON(false, "Sensor value out of range");
        }

        if (!in_array($water_mode, ["AUTO", "MANUAL", "TIMER"], true)) {
            $water_mode = "AUTO";
        }

        if (!in_array($soil_mode, ["AUTO", "MANUAL"], true)) {
            $soil_mode = "AUTO";
        }

        $sensor_status =
            "WATER:" . $water_sensor_status .
            ";SOIL:" . $soil_sensor_status .
            ";DHT:" . $dht_sensor_status;

        $stmt = $conn->prepare("
            INSERT INTO smart_farming (
                device_id,
                water_level,
                soil_moisture,
                temperature,
                humidity,
                raw_water,
                raw_soil,
                water_sensor_status,
                soil_sensor_status,
                dht_sensor_status,
                water_pump,
                soil_pump,
                water_mode,
                soil_mode,
                sensor_status,
                created_at
            ) VALUES (
                ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW()
            )
        ");

        if (!$stmt) {
            http_response_code(500);
            responseJSON(false, "Prepare insert failed: " . $conn->error);
        }

        // 15 parameter:
        // s d d d d i i s s s i i s s s
        $stmt->bind_param(
            "sddddiisssiisss",
            $device_id,
            $water_level,
            $soil_moisture,
            $temperature,
            $humidity,
            $raw_water,
            $raw_soil,
            $water_sensor_status,
            $soil_sensor_status,
            $dht_sensor_status,
            $water_pump,
            $soil_pump,
            $water_mode,
            $soil_mode,
            $sensor_status
        );

        if ($stmt->execute()) {

            $insert_id = $stmt->insert_id;
            $stmt->close();

            responseJSON(
                true,
                "Sensor data saved successfully",
                [
                    "id" => $insert_id,
                    "device_id" => $device_id,
                    "created_at" => date("Y-m-d H:i:s")
                ]
            );

        } else {

            $error = $stmt->error;
            $stmt->close();

            http_response_code(500);
            responseJSON(false, "Insert failed: " . $error);
        }
    }

    // ==================================================
    // UPDATE PUMP CONTROL
    // ==================================================

    if ($action === "update_control") {

        $device_id = $data["device_id"] ?? "GAN-IRRIGATION-001";

        $water_mode = strtoupper($data["water_mode"] ?? "AUTO");
        $soil_mode = strtoupper($data["soil_mode"] ?? "AUTO");

        $water_pump = (int)($data["water_pump"] ?? 0);
        $soil_pump = (int)($data["soil_pump"] ?? 0);
        $timer_enabled = (int)($data["timer_enabled"] ?? 0);

        $timer_start = $data["timer_start"] ?? "06:00";
        $timer_end = $data["timer_end"] ?? "06:30";

        $water_on_at = (float)($data["water_on_at"] ?? 50);
        $water_off_at = (float)($data["water_off_at"] ?? 80);
        $soil_on_at = (float)($data["soil_on_at"] ?? 70);
        $soil_off_at = (float)($data["soil_off_at"] ?? 85);

        // Validasi mode
        if (!in_array($water_mode, ["AUTO", "MANUAL", "TIMER"], true)) {
            http_response_code(422);
            responseJSON(false, "Invalid water mode");
        }

        if (!in_array($soil_mode, ["AUTO", "MANUAL"], true)) {
            http_response_code(422);
            responseJSON(false, "Invalid soil mode");
        }

        // Validasi nilai pompa
        if (
            !in_array($water_pump, [0, 1], true) ||
            !in_array($soil_pump, [0, 1], true) ||
            !in_array($timer_enabled, [0, 1], true)
        ) {
            http_response_code(422);
            responseJSON(false, "Invalid pump or timer value");
        }

        // Validasi timer
        if (
            !preg_match('/^(?:[01]\d|2[0-3]):[0-5]\d$/', $timer_start) ||
            !preg_match('/^(?:[01]\d|2[0-3]):[0-5]\d$/', $timer_end)
        ) {
            http_response_code(422);
            responseJSON(false, "Invalid timer format");
        }

        // Validasi threshold
        if (
            $water_on_at < 0 || $water_on_at > 100 ||
            $water_off_at < 0 || $water_off_at > 100 ||
            $water_on_at >= $water_off_at ||
            $soil_on_at < 0 || $soil_on_at > 100 ||
            $soil_off_at < 0 || $soil_off_at > 100 ||
            $soil_on_at >= $soil_off_at
        ) {
            http_response_code(422);
            responseJSON(false, "Invalid auto threshold");
        }

        // Simpan pengaturan kontrol
        $stmt = $conn->prepare("
            INSERT INTO smart_farming_control (
                device_id,
                water_mode,
                soil_mode,
                water_pump,
                soil_pump,
                timer_enabled,
                timer_start,
                timer_end,
                water_on_at,
                water_off_at,
                soil_on_at,
                soil_off_at,
                updated_at
            ) VALUES (
                ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW()
            )
            ON DUPLICATE KEY UPDATE
                water_mode = VALUES(water_mode),
                soil_mode = VALUES(soil_mode),
                water_pump = VALUES(water_pump),
                soil_pump = VALUES(soil_pump),
                timer_enabled = VALUES(timer_enabled),
                timer_start = VALUES(timer_start),
                timer_end = VALUES(timer_end),
                water_on_at = VALUES(water_on_at),
                water_off_at = VALUES(water_off_at),
                soil_on_at = VALUES(soil_on_at),
                soil_off_at = VALUES(soil_off_at),
                updated_at = NOW()
        ");

        if (!$stmt) {
            http_response_code(500);
            responseJSON(
                false,
                "Prepare update control failed: " . $conn->error
            );
        }

        // PENTING:
        // 12 tanda ? pada SQL
        // 12 karakter tipe data
        // 12 variabel
        //
        // s s s i i i s s d d d d
        $stmt->bind_param(
            "sssiiissdddd",
            $device_id,
            $water_mode,
            $soil_mode,
            $water_pump,
            $soil_pump,
            $timer_enabled,
            $timer_start,
            $timer_end,
            $water_on_at,
            $water_off_at,
            $soil_on_at,
            $soil_off_at
        );

        if ($stmt->execute()) {

            $stmt->close();

            responseJSON(
                true,
                "Control updated successfully",
                [
                    "device_id" => $device_id,
                    "water_mode" => $water_mode,
                    "soil_mode" => $soil_mode,
                    "water_pump" => $water_pump,
                    "soil_pump" => $soil_pump,
                    "timer_enabled" => $timer_enabled,
                    "timer_start" => $timer_start,
                    "timer_end" => $timer_end,
                    "water_on_at" => $water_on_at,
                    "water_off_at" => $water_off_at,
                    "soil_on_at" => $soil_on_at,
                    "soil_off_at" => $soil_off_at
                ]
            );

        } else {

            $error = $stmt->error;
            $stmt->close();

            http_response_code(500);

            responseJSON(
                false,
                "Update control failed: " . $error
            );
        }
    }

    responseJSON(false, "Unknown POST action");
}

// ======================================================
// UNKNOWN REQUEST
// ======================================================

http_response_code(400);
responseJSON(false, "Unknown API action");

?>
