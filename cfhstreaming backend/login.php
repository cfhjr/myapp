<?php
header('Content-Type: application/json');
include 'db_connect.php';

$email = $_POST['email'] ?? '';
$password = $_POST['password'] ?? ''; // This is the SHA-256 hash from Android

if (empty($email) || empty($password)) {
    echo json_encode(["success" => false, "message" => "Please enter email and password"]);
    exit;
}

$sql = "SELECT * FROM users WHERE email = ?";
$stmt = $conn->prepare($sql);
$stmt->bind_param("s", $email);
$stmt->execute();
$result = $stmt->get_result();

if ($result->num_rows > 0) {
    $user = $result->fetch_assoc();
    
    // Use password_verify to check the hash sent by the app
    // against the hash stored in the database
    if (password_verify($password, $user['password'])) {
        unset($user['password']); 
        echo json_encode([
            "success" => true, 
            "message" => "Login Successful",
            "user" => [
                "id" => (int)$user['id'],
                "name" => $user['name'],
                "email" => $user['email'],
                "phone" => $user['phone'],
                "user_type" => $user['user_type'],
                "birth_date" => $user['birth_date'],
                "created_at" => $user['created_at']
            ]
        ]);
    } else {
        echo json_encode(["success" => false, "message" => "Invalid credentials"]);
    }
} else {
    echo json_encode(["success" => false, "message" => "User not found"]);
}
?>