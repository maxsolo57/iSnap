<?php




if (isset($_REQUEST['username']) && isset($_REQUEST['pass'])){

	if (!preg_match('/^\+[0-9]+$/', $_REQUEST['username'])) {
		echo 'error bad username';
    		return;
	} 


	$url = 'http://v42785.1blu.de:9090/plugins/userService/userservice?type=add&secret=0j2GoYCR&username='
		.urlencode($_REQUEST['username'])
		.'&password='.urlencode($_REQUEST['pass'])
		.'&name='.urlencode($_REQUEST['username']);



	$result = file_get_contents($url);

	if (strpos($result, 'UserAlreadyExistsException')){

		$url = 'http://v42785.1blu.de:9090/plugins/userService/userservice?type=update&secret=0j2GoYCR&username='
		.urlencode($_REQUEST['username'])
		.'&password='.urlencode($_REQUEST['pass'])
		.'&name='.urlencode($_REQUEST['username']);

		$result = file_get_contents($url);

	}


	if (strpos($result, 'result>ok</result')){
		echo 'OK';
	} else {
		echo 'error';
	}


}


?>