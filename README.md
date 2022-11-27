# email-to-paperless

email-to-paperless is a small useful application which scans all emails of a preset mailbox, converts them to PDF and
uploads them together with possible email attachments to a
preset [Paperless NGX](https://github.com/paperless-ngx/paperless-ngx) instance.
It's also possible to store the generated pdf files in a specific folder, for example the `consume` folder of the
paperless instance.

A [docker image](#2-using-docker) and a [docker-compose configuration](#3-using-docker-compose) are provided for easy
and fast use.

The whole thing was inspired by the fact that I would like to have all emails archived, and not just attachments, as
Paperless NGX offers out of the box.

## Table of Contents

* [Features](#features)
* [Requirements](#requirements)
* [Build](#build)
* [Usage](#usage)
* [Configuration](#configuration)
* [Authors](#authors--contributors)
* [License](#license)

## Features

1. Periodically scans the entire mailbox (individual folders can be excluded, see below)
2. Checks (against a database) whether an email has already been imported
3. If not, download them with possible attachments
4. Converts the e-mail to a PDF with wkhtmltopdf (e.g. date, sender, recipient, etc. are added to the PDF as headers)
5. Uploads the created PDF and all attachments, if any, to Paperless NGX (and tags
   them with the preset tags if configured) or stores them in a local folder.

## Requirements

* Java 17
* [wkhtmltopdf](https://github.com/wkhtmltopdf/wkhtmltopdf)
* An existing [mariadb database](https://mariadb.org/) (or the use of
  the [docker-compose configuration](#3-using-docker-compose))
* A [Paperless NGX](https://github.com/paperless-ngx/paperless-ngx) instance
* An email mailbox, which can be accessed through imap

## Build

The build process is optional, as the application is ready to be built. See the point [Usage](#usage).
Building the application can be done via the included maven wrapper.

```
git clone https://github.com/Skyleiger/email-to-paperless.git
cd email-to-paperless
./mvnw clean package
```

The JAR file of the application can then be found under `target/email-to-paperless.jar`.

<br>

After that, the Docker image can optionally be built as well. Docker is required for this.

```
docker build . --tag skyleiger/email-to-paperless
```

## Usage

There are three ways to use this application:

1. [Standalone](#1-standalone)
2. [Using docker](#2-using-docker)
3. [Using docker-compose](#3-using-docker-compose)

<br>

#### 1. Standalone

First you need to download the latest version of the application. This can be
done [here](https://github.com/Skyleiger/email-to-paperless/releases/download/latest/email-to-paperless.jar).
Afterwards the application can be started simply with `java -jar email-to-paperless.jar`.
Please note the [requirements](#requirements) and the [configuration](#configuration) via the environment variables,
which must be set beforehand.
Otherwise the application will stop with an error.

<br>

#### 2. Using docker

The easiest way to use email-to-paperless is via the existing docker image,
if an existing MariaDB database is already in place.
Otherwise, the use via docker-compose, with an integrated database, is the better choice.

In the following example, the application is created in a docker container named `email-to-paperless`.
Also, a volume named `email-to-paperless_logs` is created for the logs.
In addition, `--restart always` ensures that the container (re)starts automatically.
Please note that for productive use, the environment variables must be adapted and supplemented if necessary.

```
docker run \
  -d \
  --name email-to-paperless \
  --restart always
  -e DB_URL=jdbc:mariadb://db.example.com:3306/email-to-paperless-db \
  -e DB_USER=username \
  -e DB_PASSWORD=password \
  -e IMAP_URL=imap.dwienzek.de \
  -e IMAP_USER=username \
  -e IMAP_PASSWORD=password \
  -e PAPERLESS_URL=paperless.example.com \
  -e PAPERLESS_TOKEN=token \
  -v email-to-paperless_logs:/email-to-paperless/logs
  ghcr.io/skyleiger/email-to-paperless
```

<br>

#### 3. Using docker-compose

If a MariaDB database is not yet available, the provided docker-compose configuration can help out.
This consists of the container of the email-to-paperless instance and a MariaDB container.

To use this, a Docker Compose installation is required. Then the files from the "docker-compose" directory of this
repository have to be copied into a new folder and the environment variables in the ".env" file have to be adjusted.

After that, the docker-compose configuration can be started with `docker compose pull && docker-compose up -d`.

If you want to stop the containers again, you can use the command `docker compose down`.

## Configuration

The configuration is performed by environment variables.
For this reason, it makes sense to use it via a docker.

The following environment variables exist:

| Name                     | Description                                                                                                                                                                                                                                                                        | Required |                                                                                    Example                                                                                    | 
|--------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|:-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------:|
| `DB_URL`                 | The database URL in JDBC format.                                                                                                                                                                                                                                                   | Yes      | `jdbc:mariadb://db.example.com:3306/email-to-paperless-db` where `db.example.com` is the database url, `3306` the database port and `email-to-paperless-db` the database name |
| `DB_USER`                | The username of the database.                                                                                                                                                                                                                                                      | Yes      |                                      `username`                                                                                    <br/>                                      |
| `DB_PASSWORD`            | The password of the database.                                                                                                                                                                                                                                                      | Yes      |                                                                                  `password`                                                                                   |
| `IMAP_URL`               | The url of the imap server. A connection to this is established via SSL over port 993.                                                                                                                                                                                             | Yes      |                                                                              `imap.example.com`                                                                               |
| `IMAP_USERNAME`          | The username of the imap server.                                                                                                                                                                                                                                                   | Yes      |                                                                              `inbox@example.com`                                                                              |
| `IMAP_PASSWORD`          | The password of the imap server.                                                                                                                                                                                                                                                   | Yes      |                                                                                  `password`                                                                                   |
| `IMAP_EXCLUDED_FOLDERS`  | A list of mail folders which should be ignored. Several can be separated with a comma.                                                                                                                                                                                             | No       |                                                                               `Trash,Sent,Junk`                                                                               |
| `EMAIL_UPDATE_INTERVAL`  | The interval in minutes of the email update job, which searches for new emails. Defaults to `240` minutes.                                                                                                                                                                         | No       |                                                                                     `240`                                                                                     |
| `EMAIL_STORING_STRATEGY` | The storing strategy for the emails. Possible values are `PAPERLESS` or `FOLDER`, which results in an upload to Paperless or storage in a specific folder.                                                                                                                         | No       |                                                                            `PAPERLESS` or `FOLDER`                                                                            |
| `EMAIL_STORING_FOLDER`   | The folder to store the emails, if `EMAIL_STORING_STRATEGY` is set to folder. Defaults to `emails`.                                                                                                                                                                                | No       |                                                                            `/my-paperless/consume`                                                                            |
| `PAPERLESS_URL`          | The url of the paperless instance.                                                                                                                                                                                                                                                 | Yes      |                                                                            `paperless.example.com`                                                                            |
| `PAPERLESS_TOKEN`        | The auth token of the paperless instance. It can be created in the admin section of the paperless instance (or under the following url `<your-paperless-url>/admin/authtoken/tokenproxy/add/`).                                                                                    | Yes      |                                                                                  `password`                                                                                   |
| `PAPERLESS_TAGS`         | A list of internal IDs of tags that are automatically added to uploaded documents in Paperless. The IDs can be found out via a manual query to the API (`/api/tags`). Visit the [API documentation](https://paperless-ngx.readthedocs.io/en/latest/api.html) for more information. | No       |                                                                                  `13,26,49`                                                                                   |

## Authors & Contributors

* [Skyleiger](https://github.com/Skyleiger)

These are the most common authors and contributors.
See also the [github list of contributors](https://github.com/Skyleiger/email-to-paperless/contributors) who
participated in
this project.

## License

See [LICENSE](https://github.com/Skyleiger/email-to-paperless/blob/master/LICENSE) file for the license of this
project.