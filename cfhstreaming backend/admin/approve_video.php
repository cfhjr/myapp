<?php
header('Content-Type: application/json');include '../db_connect.php';

if ($_SERVER['REQUEST_METHOD'] == 'POST') {
    $video_id = $_POST['video_id'];
    $status = $_POST['status']; // Should be 'approved' or 'denied'

    // Simple validation to ensure we save 'denied' if app sends 'deny'
    if ($status == 'deny' || $status == 'denny') {
        $status = 'denied';
    }

    $stmt = $conn->prepare("UPDATE videos SET status = ? WHERE id = ?");
    $stmt->bind_param("si", $status, $video_id);

    if ($stmt->execute()) {
        echo json_encode(["success" => true, "message" => "Status updated to $status"]);
    } else {
        echo json_encode(["success" => false, "message" => "Error: " . $conn->error]);
    }
    $stmt->close();
}
$conn->close();
?>