select count(*) from instasnap.ofuser 
where username in (select username from instasnap.ofpresence where offlineDate < 1357932524000);

select * from instasnap.ofvcard 
where username in (select username from instasnap.ofpresence where offlineDate < 1357932524000);

SELECT * FROM instasnap.ofroster
where username in (select username from instasnap.ofpresence where offlineDate < 1357932524000);

SELECT * FROM instasnap.ofoffline
where username in (select username from instasnap.ofpresence where offlineDate < 1357932524000);

select count(*) from instasnap.ofpresence where offlineDate < 1357932524000;