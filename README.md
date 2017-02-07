Requirements

- JRE 8
- JDK 1.8?
- MySQL
- Unlimited Strength JCE Policy for java 8

Installation
1. Create a configuration file for mysql at /etc/my.cnf:
~~~~
[mysqld]
sql\_mode = ONLY\_FULL\_GROUP\_BY,ERROR\_FOR\_DIVISION\_BY\_ZERO,NO\_AUTO\_CREATE\_USER,NO\_ENGINE\_SUBSTITUTION
~~~~
2. Run mysqld
3. Run the SQL script at SQL/MoopleDEV.sql
4. Extract the unlimited strength JCE policy and move the files to <java-home>/lib/security
5. Run the server (net.server.Server)
