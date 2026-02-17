<?php
// dashboard.php
include 'db_connect.php';

// ONLY fetch videos where status is 'Approved'
$sql = "SELECT * FROM videos WHERE status = 'Approved' ORDER BY created_at DESC";
$result = mysqli_query($conn, $sql);

// Logic to display videos in HTML/CSS goes here...
while($row = mysqli_fetch_assoc($result)) {
    echo "<div>";
    echo "<h3>" . $row['title'] . "</h3>";
    echo "<video src='" . $row['video_path'] . "' controls></video>";
    echo "</div>";
}
?>