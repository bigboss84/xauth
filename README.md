# xauth
X-Auth is a multi-tenant authentication service that allows user access to any application via Json Web Token.

Note that the software is experimental and it is under development, so this readme file is not yet updated with new features.
For now, if you want to use this software you should manually:
 - copy the script `script/keygen.sh` (requires `openssl`) to the path defined under `workspace.jwt.secretKey.path` property in `conf/application.conf` file;
 - build the `Dockerfile` under `docker/compose/images/mongodb-auth`;
 - start services defined in `docker/compose/docker-compose.yml` file with command `docker-compose up`;
 - compile and run;
 - initialize application via REST route `/v1/init/configuration` defined in `conf/routes`.

As soon as possible, this readme will be updated at latest software version and it will been documented fine.

Thanks for your patience!

# Index
1. [Architecture](#architecture)
2. [Requirements](#requirements)
3. [Build and Install](#build-and-install)
4. [Run](#run)
    - 4.1 [From IDE](#from-ide)
    - 4.2 [From SBT](#from-sbt)
5. [Configuration](#configuration)
    - 5.1 [Application initialization](#application-initialization)
    - 5.2 [First login](#first-login)
    - 5.3 [Clients configuration](#clients-configuration)
    - 5.4 [Users and roles configuration](#users-and-roles-configuration)
6. [REST APIs](#rest-apis)
    - 6.1 [Index](#index)
    - 6.2 [Initialization](#initialization)
    - 6.3 [Health](#health) ❤️
    - 6.4 [Administration](#administration)
    - 6.5 [Applications Administration](#application-administration)
    - 6.6 [Invitation](#invitation)
    - 6.7 [User](#user)
    - 6.8 [Authentication](#authentication)
    - 6.9 [Password Reset](#password-reset)
    - 6.10 [Contact Activation](#contact-activation)
    - 6.11 [Account Activation](#account-activation)
    - 6.12 [Account Deletion](#account-deletion) 😱
    - 6.13 [API JSON Schema](#api-json-schema)
6. [Tests](#tests)
7. [Packaging and Deployment](#packaging-and-deployment)

## 1. Architecture
![Application Architecture](docs/images/application-architecture.jpg?raw=true "Architecture")

## 2. Requirements
- 🧠💻😂
- [AWS Client](https://aws.amazon.com/it/cli/)
- [Docker](https://www.docker.com/) to run locally an instance of MongoDb (see `docker/compose/docker-compose.yml`), build, tag and push images on Amazon ECR repository
- [Java Development Kit 8+](https://openjdk.java.net/projects/jdk8/)
- [Scala 2.12.6](https://www.scala-lang.org/download/)
- [Scala Build Tool 1.1.6](https://www.scala-sbt.org/download.html)

## 3. Build and Install
Open a BASH terminal and clone project on your machine: 

````bash
$ git clone https://github.xauth.com/DigitalPlatform/central-auth-service.git
```` 

After cloning move into the project folder and compile source using `sbt`:

````bash
$ cd central-auth-service && sbt compile
````

## 4. Run

### 4.1 From IDE
To run application from your development environment such as your preferred IDE, you can simply import the project from file-system and add the "Play Framework Application" project facet and run it.

> Ensure to set the working directory at project root directory

### 4.2 From SBT
Run from a SBT is very simple, you can run application your project root directory by the following command:

```bash
$ sbt run
```

You can ensure that application is running opening the welcome page from your browser visiting at `http://localhost:9000`, you should to view the following welcome page:

![Welcome page](docs/images/application-index.jpg?raw=true "Welcome page")

## 5. Configuration
On first start, application needs to be configured to allow administration.

Supposing you are running application on your local machine you can perform the following
HTTP request to the application health route:

```bash
$ curl http://localhost:9000/v1/health
```

on first start, before application configuration you may have a response like as follows:

```json
{
  "status": "DOWN",
  "services": [
    {
      "name": "mongodb",
      "status": "DOWN"
    }
  ],
  "updatedAt": "2018-12-19T13:10:43.043Z"
}
```

you can see `$.status == "DOWN"` implied by `$.services[0].status = "DOWN"` that corresponds
to the application initialization status.

Application needs to be initialized!

### 5.1 Application initialization
Initialization is very simple and consists to create first trusted client for http basic authentication
and first user, the administrator.

Perform following request:

```bash
$ curl -X POST http://localhost:9000/v1/init/configuration -d '
  {
    "client": {
      "id": "trusted-client",
      "secret":"trusted-client"
    },
    "admin": {
      "username": "admin@fake.xauth.com",
      "password": "MyWonderfulS3cr37"
    }
  }
'
```

Now your application is initialized and it is ready to be configured with other basic clients, users and then you can manage the roles.

### 5.2 First log-in
To customize the configuration you need to interact with some `/v1/admin/*` rest routes that are restricted to admin users then you can obtain the admin access token performing a login with admin credentials.

Login route is `POST /v1/auth/token` that is protected by HTTP-Basic-Authentication and you can perform a request authenticating with just created `trusted-client` client.

You can obtain one access token making a login request as follows:

````bash
$ curl                                \
  -X POST                             \
  -u trusted-client:trusted-client    \
  -H 'Content-Type: application/json' \
  http://localhost:9000/v1/auth/token \
  -d '{"username":"admin@auth.xauth.com","password":"MyWonderfulS3cr37"}'
````

The login api will respond with a JSON containing your access token that you can use to perform requests protected by JWT.

```json
{
  "tokenType": "bearer",
  "accessToken": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJtYnAubG9jYWwiLCJleHAiOjE1NDQ3OTc2NDIsImlhdCI6MTU0NDc5NTg0MiwianRpIjoiNTNkNGE1YWEtY2YxYi00Yzk0LThkNDktMDE0N2YxMWEzNzg3Iiwicm9sZXMiOlsiVVNFUiIsIkFETUlOIl19.hIftyAyFINZyiGsdDmislNC58sGSoZuAxDHT9In802o",
  "expiresIn": 30,
  "refreshToken": "493f976d3ab3274632eb37ff8e141c80635b007a"
}
```

For additional information about JWT you can read [Json Web Token](https://tools.ietf.org/html/rfc7519) specifications.

> Note that access token contains information about user roles.

Now you can save token into a variable for future configuration requests:

```bash
$ token="eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJtYnAubG9jYWwiLCJleHAiOjE1NDQ3OTc2NDIsImlhdCI6MTU0NDc5NTg0MiwianRpIjoiNTNkNGE1YWEtY2YxYi00Yzk0LThkNDktMDE0N2YxMWEzNzg3Iiwicm9sZXMiOlsiVVNFUiIsIkFETUlOIl19.hIftyAyFINZyiGsdDmislNC58sGSoZuAxDHT9In802o"
```

### 5.3 Clients configuration
With the admin access token you can create new clients making a request like the following:

```bash
$ curl                                   \
  -X POST                                \
  -H "Content-Type: application/json"    \
  -H "Authorization: Bearer $token"      \
  http://localhost:9000/v1/admin/clients \
  -d '{"id":"trusted-client","secret":"trusted-client"}'
```

### 5.4 Users and Roles configuration
You can use admin access token for creating basic users and configure their main roles.

For registering users by `ADMIN` privilege you can perform requests like the following:

```bash
$ curl                                 \
  -X POST                              \
  -H "Content-Type: application/json"  \
  -H "Authorization: Bearer $token"    \
  http://localhost:9000/v1/admin/users \
  -d '
    {
      "password": "YourTemporaryS3cr37",
      "passwordCheck": "YourTemporaryS3cr37",
      "description": "default user for human resource role",
      "userInfo": {
        "firstName": "hr",
        "lastName": "hr",
        "contacts": [
          {
            "type": "EMAIL",
            "value": "fake@fake.xauth.com"
          }
        ]
      }
    }
  '
```

then the application returns just created user:

```json
{
  "id": "c808426d-1967-41a7-82f0-ef45383f426e",
  "username": "fake@fake.xauth.com",
  "roles": [
    "USER"
  ],
  "status": "DISABLED",
  "description": "default user for human resource role",
  "userInfo": {
    "firstName": "hr",
    "lastName": "hr",
    "contacts": [
      {
        "type": "EMAIL",
        "value": "fake@fake.xauth.com",
        "trusted": false
      }
    ]
  },
  "registeredAt": "2018-12-14T16:51:04.004Z",
  "updatedAt": "2018-12-14T16:51:04.004Z"
}
```

By default this route creates the a `DISABLED` user with only the `USER` role, the system will send the activation code to the supplied email and the user will able to autonomically enable its account.

Now we can assign the `HR` role performing a patch request like the following:

```bash
$ userId="c808426d-1967-41a7-82f0-ef45383f426e"
$ curl                                               \
  -X PATCH                                           \
  -H "Content-Type: application/json"                \
  -H "Authorization: Bearer $token"                  \
  http://localhost:9000/v1/admin/users/$userId/roles \
  -d '
    {
      "roles": [
        "USER",
        "HR"
      ]
    }
  '
```

## 6. REST APIs
Following REST APIs that follows are available also in the Postman collection that you can import from [here](docs/postman-collection.json "Postman APIs Collection").

### 6.1 Index
| Accessibility                          | Verb           | Path  | Description           |
| -------------------------------------- | -------------- | ----- | --------------------- |
| `ANON`, `BASIC`, `USER`, `HR`, `ADMIN` | `GET`          | `/`   | Application home page |

### 6.2 Initialization
| Accessibility | Verb   | Path                     | Description                             |
| ------------- | ------ | ------------------------ | --------------------------------------- |
| `ANON`        | `POST` | `/v1/init/configuration` | Initializes application (runnable once) |

### 6.3 Health ❤️
| Accessibility | Verb           | Path         | Description              |
| ------------- | -------------- | ------------ | ------------------------ |
| `ANON`        | `GET`          | `/v1/health` | Application health check |

### 6.4 Administration
| Accessibility | Verb           | Path                          | Description                                              |
| ------------- | -------------- | ----------------------------- | -------------------------------------------------------- |
| `ADMIN`       | `POST`         | `/v1/admin/users`             | Creates new user                                         |
| `ADMIN`       | `POST`         | `/v1/admin/users/:id/unblock` | Unblocks blocked user                                    |
| `ADMIN`       | `PATCH`        | `/v1/admin/users/:id/roles`   | Patches the user roles                                   |
| `ADMIN`       | `PATCH`        | `/v1/admin/users/:id/status`  | Patches the user status                                  |
| `ADMIN`       | `POST`         | `/v1/admin/account-trust`     | Forces dispatch of activation message to user            |
| `ADMIN`       | `POST`         | `/v1/admin/clients`           | Creates new trusted client for http-basic authentication |
| `ADMIN`       | `GET`          | `/v1/admin/clients`           | Gets all registered clients                              |
| `ADMIN`       | `GET`          | `/v1/admin/clients/:id`       | Gets client by id                                        |
| `ADMIN`       | `PUT`          | `/v1/admin/clients/:id`       | Updates client                                           |
| `ADMIN`       | `DELETE`       | `/v1/admin/clients/:id`       | Deletes client                                           |

### 6.5 Applications Administration
| Accessibility                         | Verb           | Path                               | Description                   |
| ------------------------------------- | -------------- | ---------------------------------- | ----------------------------- |
| `ADMIN`, `HD_OPERATOR`, `RESPONSIBLE` | `PATCH`        | `/v1/owner/users/:id/applications` | Patches the user applications |

### 6.6 Invitation
| Accessibility | Verb           | Path                       | Description                                           |
| ------------- | -------------- | -------------------------- | ----------------------------------------------------- |
| `HR`          |`POST`          | `/v1/invitations`          | Creates new invitation by user email                  |
| `HR`          |`POST`          | `/v1/invitations/:id/code` | Creates a registration code for the invitation        |
| `HR`          |`GET`           | `/v1/invitations`          | Searches an invitation by 'email' or 'invitationCode' |
| `HR`          |`GET`           | `/v1/invitations/:id`      | Gets invitation by id                                 |
| `HR`          |`DELETE`        | `/v1/invitations/:id`      | Deletes invitation by id                              |

### 6.7 User
| Accessibility | Verb           | Path            | Description                          |
| ------------- | -------------- | --------------- | ------------------------------------ |
| `ANON`        | `POST`         | `/v1/users`     | Regist new user                      |
| `ADMIN`       | `GET`          | `/v1/users/:id` | Gets a user by id                    |
| `ADMIN`       | `DELETE`       | `/v1/users/:id` | Deletes user by id                   |

### 6.8 Authentication
| Accessibility | Verb           | Path               | Description                                                    |
| ------------- | -------------- | ------------------ | -------------------------------------------------------------- |
| `BASIC`       | `GET`          | `/v1/auth/jwk`     | Retrieves the public key to verify the access tokens signature |
| `BASIC`       | `POST`         | `/v1/auth/token`   | Authenticates the user and returns access and refresh tokens   |
| `BASIC`       | `GET`          | `/v1/auth/check`   | Checks access token in request header                          |
| `BASIC`       | `POST`         | `/v1/auth/refresh` | Refreshes access token by refresh token                        |

### 6.9 Password Reset
| Accessibility | Verb           | Path                          | Description                                    |
| ------------- | -------------- | ----------------------------- | ---------------------------------------------- |
| `ANON`        | `POST`         | `/v1/auth/password-forgotten` | Sends to user a secret code for password reset |
| `ANON`        | `POST`         | `/v1/auth/password-reset`     | Sets new user password                         |

### 6.10 Contact Activation
| Accessibility         | Verb           | Path                          | Description                                        |
| --------------------- | -------------- | ----------------------------- | -------------------------------------------------- |
| `USER`, `HR`, `ADMIN` | `GET`          | `/v1/auth/user`               | Retrieves current user                             |
| `USER`, `HR`, `ADMIN` | `POST`         | `/v1/auth/contact-trust`      | Sends to user a secret code for contact activation |
| `USER`, `HR`, `ADMIN` | `POST`         | `/v1/auth/contact-activation` | Activate the user contact by secret code           |

### 6.11 Account Activation
| Accessibility | Verb           | Path                  | Description                             |
| ------------- | -------------- | --------------------- | --------------------------------------- |
| `ANON`        | `POST`         | `/v1/auth/activation` | Enables new user account by secret code |
| `ANON`        | `POST`         | `/test`               | Simple api test                         |

### 6.12 Account Deletion 😱
| Accessibility         | Verb           | Path                                   | Description                                      |
| --------------------- | -------------- | -------------------------------------- | ------------------------------------------------ |
| `USER`, `HR`, `ADMIN` | `POST`         | `/v1/auth/account-delete-request`      | Sends to user a secret code for account deletion |
| `USER`, `HR`, `ADMIN` | `POST`         | `/v1/auth/account-delete-confirmation` | Deletes definitively the user account            |

### 6.13 API JSON Schema
| Accessibility | Verb           | Path                       | Description                          |
| ------------- | -------------- | -------------------------- | ------------------------------------ |
| `ANON`        | `GET`          | `/public/schema/$path<.*>` | Gets the API json schema definitions |

## 7. Tests
TODO

## 8. Packaging and Deployment
Move into your directory folder and compile source code:

```bash
$ cd central-auth-service && sbt compile
```

then prepare your distribution typing following sbt command:

```bash
$ sbt dist
```

sbt now is preparing your distribution zip package under path `central-auth-service/target/universal`.

When sbt finishes packaging you can create the docker container and push it on the repository by the `install` sbt task:

```bash
$ sbt install
```

sbt `install` task runs the script `docker/build/build.sh` that builds the docker container (for development environment by default).
If you want to build a container for production you can specify the `environment` system property as follows:

```bash
$ sbt -Denvironment=production install
```

this implies that the production configuration will be used by the application, you can quickly check these information by the script output:

```bash
...

Building docker image

project directory: /dev/xauth/jwt-auth
    artifact name: jwt-auth
          version: 0.3.7
      environment: production
       repository: 256909349812.dkr.ecr.eu-west-1.amazonaws.com
            proxy: 'http://proxy-web.xauth.com'

...
```

The `build.sh` script do the following operations:

- Builds the docker image by `docker build` command
- Tags the container image by `docker tag` command
- Performs a login using `aws get-login` command
- Pushes the built image on Amazon ECR
- Removes all local docker images
- Removes the packaged application archive

When the script finishes all this operation the application image is ready to be deployed!

Enjoy your authenticated life! ◬😎
