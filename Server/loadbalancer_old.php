<?php
$homepage = file_get_contents('http://localhost:9090/plugins/sessionsinfo');

$defaultServerPort = "5222";
$cmLoadLimit = 30;

$port = "";
$address = "";
$users = -1;

$allservers = explode(';', $homepage);

foreach ($allservers as &$serv) {
    	$serv = trim($serv);
	if (strlen($serv) != 0)	{
		$serv = explode(',', $serv);
//		echo $serv[0]." ".$serv[1]." ".$serv[2];

//		if ($users < 0) {
//			$port = $serv[0];
//			$address = $serv[1];
//			$users = $serv[2];
//		}

		if ($serv[2] < $users || $users < 0){
			if ($serv[0] != $defaultServerPort && $serv[2] >= intval($cmLoadLimit)) {
			}
			else {	
				$port = $serv[0];
				$address = $serv[1];
				$users = $serv[2];
			}
		}
	}
//	echo "<br>";
}


echo "".$address.":".$port;


?>