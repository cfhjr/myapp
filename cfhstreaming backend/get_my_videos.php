<?php
header('Content-Type: application/json');
include 'db_connect.php';

$user_id = $_GET['user_id'] ?? null;
$status = $_GET['status'] ?? 'approved';

if (!$user_id) {
    echo json_encode([]);
    exit;
}

// SQL query to join videos with likes and comments counts
$sql = "SELECT 
            v.*, 
            (SELECT COUNT(*) FROM likes WHERE video_id = v.id) as like_count,
            (SELECT COUNT(*) FROM comments WHERE video_id = v.id) as comment_count
        FROM videos v 
        WHERE v.user_id = ? AND v.status = ? 
        ORDER BY v.created_at DESC";

$stmt = $conn->prepare($sql);
$stmt->bind_param("is", $user_id, $status);
$stmt->execute();
$result = $stmt->get_result();

$videos = [];
while ($row = $result->fetch_assoc()) {
    $videos[] = [
        "id" => (int)$row['id'],
        "title" => $row['title'],
        "description" => $row['description'],
        "video_path" => $row['video_path'],
        "thumbnail_path" => $row['thumbnail_path'],
        "user_id" => (int)$row['user_id'],
        "status" => $row['status'],
        "created_at" => $row['created_at'],
        "like_count" => (int)$row['like_count'],      // Add like count
        "comment_count" => (int)$row['comment_count'] // Add comment count
    ];
}

echo json_encode($videos);
?>