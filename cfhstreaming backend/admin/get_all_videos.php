<?php
header('Content-Type: application/json');
include '../db_connect.php';

// We use LEFT JOIN to get the name of the user who uploaded the video
$sql = "SELECT v.*, u.name as uploader_name 
        FROM videos v 
        LEFT JOIN users u ON v.user_id = u.id 
        ORDER BY v.created_at DESC";

$result = $conn->query($sql);

$videos = array();
if ($result && $result->num_rows > 0) {
    while($row = $result->fetch_assoc()) {
        $videos[] = $row;
    }
}

echo json_encode($videos);
$conn->close();
?>
