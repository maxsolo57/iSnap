<?php

$delay_days = 14;





$total = 0;
$folders = 0;




function is_dir_empty($dir) {
  if (!is_readable($dir)) return NULL; 
  return (count(scandir($dir)) == 2);
}
 


$countries = glob('../instasnap_data/*');
foreach($countries as $country)
{
//      echo $country."<br>";
	$users = glob($country.'/*');
	foreach($users as $user)
	{
//		echo "____".$user."<br>";
		$pix = glob($user.'/*');
		foreach($pix as $pic)
		{
			$diff = floor((time() - filectime($pic))/60/60/24);
//			echo "________".$pic."    ".$diff." days ago<br>";

			if($diff>$delay_days){
				$total = $total + 1;
				unlink($pic);				
			}			
		}
		if(is_dir_empty($user))
		{
			$folders = $folders + 1;
			rmdir($user);
		}
	}
}

echo $total." files deleted<br>".$folders." folders deleted";



?>