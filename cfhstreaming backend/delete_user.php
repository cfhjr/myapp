<?php
include '../db_connect.php';
$id = $_POST['id'];
$sql = "DELETE FROM users WHERE id=$id";
if(mysqli_query($conn, $sql)) echo json_encode(["success" => true]);
?>