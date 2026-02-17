<?php
// logout.php

// Hakikisha session imeanza (ili tuweze kui-destroy)
session_start();

// 1. Ondoa session variables zote (best practice)
$_SESSION = array();  // au session_unset(); (zote zinafanya kazi)

// 2. Haribu session kabisa
session_destroy();

// 3. Ondoa session cookie ili browser isikumbuke session ID (muhimu sana)
if (ini_get("session.use_cookies")) {
    $params = session_get_cookie_params();
    setcookie(session_name(), '', time() - 42000,
        $params["path"], $params["domain"],
        $params["secure"], $params["httponly"]
    );
}

// 4. Redirect user kwenda login page au home (badilisha URL kulingana na yako)
header("Location: login.php");  // au "index.php" au "http://192.168.137.1/streaming platform/login.php"
exit();  // Muhimu ili kuzuia code inayoendelea baada ya redirect
?>