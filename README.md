# iSnap

iSnap, formerly InstaSnap, is a photo sharing app for Android devices.

Users can send each other encrypted pictures that show up on recipient's phone for a few seconds and disappear forever.
Screenshots are disabled.

The backend is based on Openfire IM server with some additional code in PHP.
User data and pictures are stored in MySQL database. Messages and pictures are wiped from the server as soon recipient's "seen" confirmation is received.

Web interface and customer support are based on Drupal.


Check ANDROID for app code. Only essential code is included.

Check SERVER for additional PHP code on server side. Openfire and Drupal files are not included.
