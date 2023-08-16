# User Service
This service manages users for The Cabinet Office's Apply for a Grant service

## Getting started
### Prerequisites
- IntelliJ (Community or Ultimate)
- Java 17
- Homebrew
- Postgres
  - pgadmin also recommended
- Docker

### Database Setup with Docker
- Install Colima using the instructions here: [GitHub - abiosoft/colima: Container runtimes on macOS (and Linux) with minimal setup](https://github.com/abiosoft/colima)
- You’ll also need Docker command line so make sure to read that part of the Colima instructions
- run `colima start`
- run `docker pull postgres`
- run `docker run -itd -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=mysecretpassword -p 5432:5432 --name postgres-local postgres`
- NOTE: to start this container AFTER the first time you run this command, just use `docker start postgres-local`
Install PGAdmin either through docker or directly onto your system then connect to the instance of Postgres you’ve just started:
  - host: `localhost` or `127.0.0.1` (or `host.docker.internal` if you also run PGAdmin with docker)
  - port: `5432`
  - username: `postgres`
  - password: `mysecretpassword`

### Database Setup without Docker
- Install postgres and pgadmin
- run `brew services start postgresql`
- open pgadmin and create a database called `gapuserlocaldb`
- update your application.properties database username and password. Default user/pass for the `postgres` user is `postgres`/`postgres`
  - spring.datasource.username
  - spring.datasource.password
  
### Wiremock

Wiremock can be used in local development to stub responses from OneLogin API.

To start wiremock server on http://localhost:8888

```
cd mockOneLogin
docker compose up -d
```

Ensure One Login base URL points to Wiremock

`onelogin.base-url=http://localhost:8888`

Request Mappings are found in `mockOneLogin/wiremock`

New mappings should be added to `mockOneLogin/wiremock/mappings` ensuring file name conforms to using UUID

Responses that return JSON should be added to `mockOneLogin/wiremock/__files`


### Instructions
1. Configure IntelliJ to use the Java 17 SDK
2. Set up the database as above
3. Install your dependencies with Maven, using the Maven tab in IntelliJ:
   1. Download Sources and/or Documentation
   2. Generate Sources and Update Folders For All Projects
   3. Reload All Maven Projects
4. Ensure you have the following in your application.properties:
   - feature.onelogin.enabled=true
   - feature.onelogin.migration.enabled=true
5. Run GapUserServiceApplication.java
6. If successful, you should see a message at the bottom of the terminal stating:
    `g.c.g.GapUserServiceApplication          : Started GapUserServiceApplication in X.xxx seconds`


## Troubleshooting
- ensure that the email and sub in the gaplocaluserdb>gap_users table match what's returned in wiremock
  - you must restart the wiremock server if you make any changes
- ensure that ONE_LOGIN_ENABLED is true in all your projects
- ensure that the ports match up between your FE and BE
- if you're logging in as an admin, ensure that admin has a department
- if you're having trouble running the migration scripts, run `mvn flyway:clean` and rerun the service
  `
