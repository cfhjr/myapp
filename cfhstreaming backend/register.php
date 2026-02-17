<?php
header('Content-Type: application/json');
include 'db_connect.php';

$name = $_POST['name'] ?? '';
$phone = $_POST['phone'] ?? '';
$user_type = $_POST['user_type'] ?? '';
$birth_date = $_POST['birth_date'] ?? '';
$email = $_POST['email'] ?? '';
$password = $_POST['password'] ?? ''; // This is the SHA-256 hash from Android
$created_at = $_POST['created_at'] ?? date('Y-m-d H:i:s');

if (empty($name) || empty($email) || empty($password)) {
    echo json_encode(["success" => false, "message" => "Required fields missing"]);
    exit;
}

// 1. Check if email exists
$checkSql = "SELECT id FROM users WHERE email = ?";
$checkStmt = $conn->prepare($checkSql);
$checkStmt->bind_param("s", $email);
$checkStmt->execute();
if ($checkStmt->get_result()->num_rows > 0) {
    echo json_encode(["success" => false, "message" => "Email already registered"]);
    exit;
}

// 2. Hash the password for secure storage
// We hash the SHA-256 string again using bcrypt (standard PHP behavior)
$secure_password = password_hash($password, PASSWORD_DEFAULT);

// 3. Insert user into Database
$sql = "INSERT INTO users (name, phone, user_type, birth_date, email, password, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
$stmt = $conn->prepare($sql);
$stmt->bind_param("sssssss", $name, $phone, $user_type, $birth_date, $email, $secure_password, $created_at);

if ($stmt->execute()) {
    echo json_encode(["success" => true, "message" => "Registration Successful"]);
} else {
    echo json_encode(["success" => false, "message" => "Database error: " . $conn->error]);
}
?>