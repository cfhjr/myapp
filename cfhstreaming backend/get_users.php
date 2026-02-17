<?php
include '../db_connect.php';
$sql = "SELECT id, name, phone, email, user_type FROM users";
$result = mysqli_query($conn, $sql);
$users = mysqli_fetch_all($result, MYSQLI_ASSOC);
echo json_encode($users);
?>