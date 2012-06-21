<?php

require_once 'Requests/library/Requests.php';
require_once 'Safe.php';
Requests::register_autoloader();
$safe = new Safe();

mb_internal_encoding("UTF-8");
mb_regex_encoding("UTF-8");

function mb_chr($value) {
	return mb_convert_encoding("&#".$value.';','UTF-8','HTML-ENTITIES');
}
function mb_ord($char) {
    $k = mb_convert_encoding($char,'UCS-2LE','UTF-8');
    $k1 = ord(substr($k,0,1));
    $k2 = ord(substr($k,1,1));
    return $k2*256+$k1;
}

$output = $safe->checkScript($_GET["code"], 1);
exit($output);