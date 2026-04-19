# java-explore-with-me
Template repository for ExploreWithMe project.

## Stage 1

- Multi-module build is prepared:
  - `ewm-main-service`
  - `stats/stats-dto`
  - `stats/stats-client`
  - `stats/stats-server`
- Statistics API from `ewm-stats-service-spec.json` is implemented in `stats-server`.
- Reusable HTTP client for statistics service is implemented in `stats-client`.
- Docker files are added for `ewm-main-service` and `stats-server`.
- Root `docker-compose.yml` starts both services and both PostgreSQL instances.

## Additional functionality theme

Additional functionality for the final project is planned around **city leisure and local events discovery** (afisha-like scenario for searching and sharing city activities).
