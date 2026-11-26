
<?php
// ======================================================
// GAN SMART IRRIGATION
// DATABASE CONFIGURATION
// PT GOLDEN AGIN NUSA
// ======================================================

date_default_timezone_set("Asia/Jakarta");

$DB_HOST = "localhost";
$DB_USER = "root";
$DB_PASS = "IbNh3[0vaP1y/844";
$DB_NAME = "db_counter_inj";

// ======================================================
// MYSQL CONNECTION
// ======================================================

$conn = new mysqli(
    $DB_HOST,
    $DB_USER,
    $DB_PASS,
    $DB_NAME
);

if ($conn->connect_error) {
    error_log("GAN SMART IRRIGATION DB ERROR: " . $conn->connect_error);

    http_response_code(500);

    die("Database connection failed.");
}

$conn->set_charset("utf8mb4");

// ======================================================
// APPLICATION CONFIGURATION
// ======================================================

define("GAN_DEVICE_ID", "GAN-IRRIGATION-001");

define(
    "GAN_API_URL",
    "http://aginnusa.com:800/smart/api.php"
);
?>
