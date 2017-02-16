# MBARI

This doc contains MBARI specific notes.

## Database

We are running on a SQL Server 2016 instance. Note that [jTDS](http://jtds.sourceforge.net/) drivers [do not work correctly with SQL Server 2016](https://sourceforge.net/p/jtds/bugs/769/). This requires us to use [Microsoft's JDBC driver](https://github.com/Microsoft/mssql-jdbc).

