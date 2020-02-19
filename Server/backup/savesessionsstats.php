<?php

$curusers = file_get_contents("http://s15757996.onlinehome-server.com:9090/plugins/onlineusers");

//$curusers = file_get_contents("http://localhost:9090/plugins/onlineusers");

// echo $curusers;

$dblink = mysql_pconnect("v42785.1blu.de","xxxxxxx","xxxxxxx");
if (!$dblink){
	echo "error:".mysql_error();	
}

$res = "INSERT INTO statistics.sessions (date,users) VALUES (now(), ".$curusers.");";

// echo $res;

$bla = mysql_query($res);

// echo "   rows:".mysql_affected_rows();

?> 
