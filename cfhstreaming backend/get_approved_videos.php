<?php
header('Content-Type: application/json');
include 'db_connect.php';

$sql = "SELECT * FROM videos WHERE status = 'approved' ORDER BY created_at DESC";
$result = $conn->query($sql);

$videos = array();
if ($result->num_rows > 0) {
    while($row = $result->fetch_assoc()) {
        $videos[] = $row;
    }
}
echo json_encode($videos);
$conn->close();
?>