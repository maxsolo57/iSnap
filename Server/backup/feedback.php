<?php

if (isset($_REQUEST['msg'])){
	$from = "";
	if (isset($_REQUEST['from'])){
		$from = $_REQUEST['from'];
	}

	$subject = 'InstaSnap Feedback';
	$to      = 'support@apparitionmobile.com';
	$headers = 'From: '.$from."\r\n";


	if (isset($_REQUEST['sbj'])){
		$subject = $_REQUEST['sbj'];
	}	

	mail($to, $subject, $_REQUEST['msg'], $headers);

}

?> 