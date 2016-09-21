# How to deploy

## Customize for your Database

These are the deployment instructions for MBARI. To deploy on your own network and database you will need to do the following:

1. Run `sbt pack`
2. Find the JDBC driver for your database. For example, you can get the PostgreSQL driver at [https://jdbc.postgresql.org/download.html](https://jdbc.postgresql.org/download.html). Download the _.jar_ file for your database and copy it into `target\pack\lib`.
3. Customize configuration file at `target\pack\conf\application.conf`. You will need to change the `production` section to use your databases parameters. 
    - Refer to your databases JDBC documentation for the format of the URL and the drivers _class_ name. 
    - Your database administrator can provide you with a username (with write access) and password for your database
    - Annosaurus will generate the database schema automatically when it's first run. Make sure that the user account has permissions to create schemas.
4. Run the commands below. Substitute your docker username for _hohonuuli_.    


## Deployment Instructions

Build and deploy an new docker image. __Substitute your docker username for _hohonuuli_.__

```
sbt pack
docker build -t hohonuuli/annosaurus .
docker login --username=hohonuuli
docker push hohonuuli/annosaurus
```

## On Deployment Machine

```
docker pull hohonuuli/annosaurus
sudo /usr/bin/systemctl restart docker.annosaurus
```

## Setup for deployment

This has already been done, but these notes are for reference. If you are using a machine that supports systemctl you can follow the instructions below to setup annosaurus as a service.

1. Copy `docker.annosaurus.service` onto portal at `/etc/systemd/system`. I did it as myself (brian) and did not have to monkey with file permissions at all.
2. Run a test using `/usr/bin/systemctl start docker.annosaurus` and verify that it works.
3. Enable it with `/usr/bin/systemctl enable docker.annosaurus`. You can verify the status using `/usr/bin/systemctl status docker.annosaurus`