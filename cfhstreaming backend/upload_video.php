<?php
// 1. Force JSON output and hide HTML errors
header('Content-Type: application/json');
error_reporting(E_ALL);
ini_set('display_errors', 0); 

// 2. Database Connection - CRITICAL: Ensure this file name is correct
include 'db_connect.php'; 

// 3. Catch crashes and return as JSON
function handleError($errno, $errstr, $errfile, $errline) {
    echo json_encode(["success" => false, "message" => "PHP Error: [$errno] $errstr in $errfile:$errline"]);
    exit;
}
set_error_handler("handleError");

// 4. Get Data from App
$user_id = $_POST['user_id'] ?? null;
$category_id = $_POST['category_id'] ?? 1;
$title = $_POST['title'] ?? null;
$description = $_POST['description'] ?? "";

// 5. Basic Validation
if (!$user_id || !$title || !isset($_FILES['video'])) {
    echo json_encode(["success" => false, "message" => "Missing data: user_id, title, or video file"]);
    exit;
}

// 6. Handle Directory (Create if missing)
$upload_dir = "uploads/videos/";
if (!is_dir($upload_dir)) {
    mkdir($upload_dir, 0777, true);
}

// 7. Save the Video
$file_ext = pathinfo($_FILES['video']['name'], PATHINFO_EXTENSION);
$new_filename = uniqid() . "." . ($file_ext ?: "mp4");
$target_path = $upload_dir . $new_filename;

if (move_uploaded_file($_FILES['video']['tmp_name'], $target_path)) {
    // 8. SQL Insert - Ensure your table columns match exactly
    $sql = "INSERT INTO videos (user_id, category_id, title, description, video_path, status, created_at) 
            VALUES (?, ?, ?, ?, ?, 'pending', NOW())";
    
    $stmt = $conn->prepare($sql);
    if (!$stmt) {
        echo json_encode(["success" => false, "message" => "SQL Prepare Failed: " . $conn->error]);
        exit;
    }

    $stmt->bind_param("iisss", $user_id, $category_id, $title, $description, $target_path);
    
    if ($stmt->execute()) {
        echo json_encode(["success" => true, "message" => "Upload Success"]);
    } else {
        echo json_encode(["success" => false, "message" => "DB Execute Failed: " . $stmt->error]);
    }
} else {
    echo json_encode(["success" => false, "message" => "Could not save file. Check folder permissions."]);
}
?>