<?php
header('Content-Type: application/json');
include '../db_connect.php';

if ($_SERVER['REQUEST_METHOD'] == 'POST') {
    $video_id = $_POST['video_id'];

    $stmt = $conn->prepare("DELETE FROM videos WHERE id = ?");
    $stmt->bind_param("i", $video_id);

    if ($stmt->execute()) {
        echo json_encode(["success" => true, "message" => "Video deleted successfully"]);
    } else {
        echo json_encode(["success" => false, "message" => "Delete failed: " . $conn->error]);
    }
    $stmt->close();
}
$conn->close();
?>