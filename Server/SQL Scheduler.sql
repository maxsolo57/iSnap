
SHOW PROCESSLIST;

SET GLOBAL event_scheduler = ON;


http://s15757996.onlinehome-server.com:9090/plugins/onlineusers


delimiter |
CREATE EVENT statistics.event_get_total_users ON SCHEDULE
EVERY 1 day starts '2013-02-27 00:01:00'
COMMENT 'get total users and daily registered users statistics'
DO BEGIN
	select count(*) into @total from instasnap.ofuser;
	SET @dt = now() - interval 1 day;
	SET @dayago = (cast(DATEDIFF(@dt, '1970-01-01 00:00:00') as signed) * 86400000 
		+ hour(@dt)*3600000 + 6*3600000
		+ minute(@dt)*60000
		+ second(@dt)*1000);
	SELECT count(*) into @daily FROM instasnap.ofuser where creationDate > @dayago;

	insert into statistics.users(date,total_users,daily_new_users)
	select date(now()), @total, @daily;
end |
delimiter ;