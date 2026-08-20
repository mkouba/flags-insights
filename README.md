# insights-flags

A small **Quarkus** demo app built for a talk about the Quarkiverse
[**quarkus-flags**](https://docs.quarkiverse.io/quarkus-flags/dev/) extension.

It's a mock "Quarkus Insights" dashboard whose behaviour and appearance are driven entirely by feature
flags. The point of the demo is to show the same extension serving flags from **three different
sources** (config, database, in-memory), plus **custom** and **security** flag evaluators, all read
from both Java code and Qute templates.

## What it demonstrates

| Flag | Source | What it does | Evaluator |
|------|--------|--------------|-----------|
| `theme` | **Config** (`application.properties`, `quarkus.flags.build.*`) | Serves a dark or light CSS theme based on the **local time of the logged-in user** (dark from 21:00 to 06:00). | `ThemeFlagEvaluator` (custom `FlagEvaluator`) |
| `dashboard.announcement` | **Database** (`DbFlag`, `@FlagSource`) | A **kill switch** for the announcement banner - an admin can turn it on/off at runtime, no redeploy. | – |
| `dashboard.insights-panel` | **Database** (`DbFlag` + metadata) | A **gradual, per-user rollout** of the "Insights (Beta)" panel; an admin can raise the rollout percentage. | `UsernameRolloutFlagEvaluator` (from `quarkus-flags-security`) |
| `dashboard.tips-shown` | **In-memory** (`@RegisterFlag` `int` field) | How many random feature-flag tips to show. The value is **read and changed directly in code** via a static field. | – |

Cross-cutting pieces worth pointing out during the talk:

- **Custom evaluator + identity augmentor** — `ThemeFlagEvaluator` computes the theme without touching
  the database: the user's time zone is attached to the `SecurityIdentity` by
  `TimezoneIdentityAugmentor` and read during flag evaluation.
- **Username-based rollout** — `UsernameRolloutFlagEvaluator` hashes `username + feature`, so a given
  user consistently sees (or doesn't see) the Insights panel, and raising the percentage only ever
  *widens* the audience.
- **`@RegisterFlag` read & write** — reading the static field returns the current flag value (the field
  read is rewritten at build time); assigning the field changes the value at runtime. The in-memory
  source isn't cached, so the change is visible immediately.
- **Qute integration** — templates read flags directly with the `flag:` namespace, e.g.
  `{flag:enabled('dashboard.announcement', true)}` and `{flag:string('theme', 'light')}`.
- **Flag cache** — enabled with a TTL in production (`quarkus.flags.cache.*`) and disabled for tests;
  the database-backed admin actions invalidate the cache so changes take effect at once.

## Running in dev mode

```shell script
./mvnw quarkus:dev
```

Then open <http://localhost:8080/dashboard> - you'll be redirected to the login page.

### Try it out

Log in with any of the seeded users (**password = username**). Each lives in a different time zone,
which drives the theme flag:

| User | Password | Roles | Time zone |
|------|----------|-------|-----------|
| `admin` | `admin` | admin, user | system default |
| `alice` | `alice` | user | Europe/Prague |
| `bob` | `bob` | user | America/New_York |
| `carlos` | `carlos` | user | America/Sao_Paulo |
| `diana` | `diana` | user | Europe/London |
| `eiko` | `eiko` | user | Asia/Tokyo |
| `farah` | `farah` | user | Asia/Dubai |
| `giovanni` | `giovanni` | user | Europe/Rome |
| `hana` | `hana` | user | Asia/Seoul |
| `ivan` | `ivan` | user | Europe/Moscow |
| `julia` | `julia` | user | Europe/Berlin |
| `kiri` | `kiri` | user | Pacific/Auckland |

Things to try:

- **Theme by time zone** — log in as `alice` (Prague, daytime → **light**) and `kiri` (Auckland, +10h,
  night → **dark**) at the same moment to see both themes and the matching Quarkus logo variant.
- **As `admin`** — the dashboard shows an *Admin* panel where you can flip the announcement kill switch,
  change the Insights rollout percentage, and set how many tips are shown, plus a table of all users
  with their current local time.
- **Rollout** — bump the Insights rollout percentage and log in as different users to see who's now in
  the rollout.

The Quarkus **Dev UI** is available at <http://localhost:8080/q/dev/>.

## Run in JVM mode

Build the app first:

```shell script
./mvnw clean package
```

Next, make sure you have a PostgreSQL database running.
In production, Quarkus does not start a container for you like it does in Dev Mode.
To set up a PostgreSQL database with Docker:

```shell script
docker run -it --rm=true --name quarkus_test -e POSTGRES_USER=quarkus_test -e POSTGRES_PASSWORD=quarkus_test -e POSTGRES_DB=quarkus_test -p 5432:5432 postgres:18
```

Then run it:

```shell script
java -jar ./target/quarkus-app/quarkus-run.jar
```

## Run as a native application

```shell script
./mvnw clean package -Dnative
```

Or, without a local GraalVM:

```shell script
./mvnw clean package -Dnative -Dquarkus.native.container-build=true
```

Then again, make sure you have a PostgreSQL database running.

Then run `./target/insights-flags-1.0.0-SNAPSHOT-runner`. See
<https://quarkus.io/guides/maven-tooling> for details.

## Related guides

- **quarkus-flags** ([docs](https://docs.quarkiverse.io/quarkus-flags/dev/)): the feature-flags
  extension this demo is built around.
- **Qute Web** ([guide](https://docs.quarkiverse.io/quarkus-qute-web/dev/index.html)): serves Qute
  templates over HTTP.
- **Security JPA** ([guide](https://quarkus.io/guides/security-jpa)): username/password auth backed by
  a database.
- **Quarkus brand** ([reference](https://quarkus.io/brand/)): the colors and logos used for styling.
