# Restore drill: prove the database backups work

A backup nobody has restored is a hope, not a backup. Run this once after the first deploy, before
a pilot studio puts real data in, and then each quarter. Allow about 30 minutes, mostly waiting
for Azure.

## What protects the data

| Layer | What it covers | Where it's set |
|---|---|---|
| Point-in-time restore | The whole server, to any second in the last 7 days (7–35 allowed) | `backup_retention_days`, `infra/modules/postgres` |
| `prevent_destroy` | Terraform refuses a plan that would replace or delete the server or database | `infra/modules/postgres/main.tf` |
| Per-studio export | One studio's data as CSV files, downloaded by its owner from Settings | `GET /studios/{id}/export` |

There is no geo-redundant backup: it doubles the backup cost, and a regional outage that lasts
longer than a pilot will tolerate is unlikely. Revisit before taking paying studios.

## The drill

You need the Azure CLI, signed in to the subscription, and membership of the
`studiomanager-dev-db-admins` group (from bootstrap).

1. **Note the time and a fact to check.** In Azure Cloud Shell, connect to the live server as
   yourself (as in step 12 of the [first deploy](first-deploy.md)) and run:

   ```sql
   select now(), count(*) from studio;
   select count(*) from piece;
   ```

   Keep the time and both counts.

2. **Restore to a new server**, from a few minutes after that time. Restores always create a new
   server; the live one is untouched.

   ```bash
   az postgres flexible-server restore \
     --resource-group rg-studiomanager-dev \
     --source-server <live server name, psql-...> \
     --name psql-restore-drill \
     --restore-time "<UTC time from step 1, plus 5 minutes, e.g. 2026-11-02T18:05:00Z>"
   ```

3. **Check it.** Connect to `psql-restore-drill` the same way and run the same queries. The
   counts should match step 1 (or be higher if studios were busy). Also check that Entra sign-in
   works and that `studiomanager_api` and the `app_rw` role exist (`\du`). The API's login has to
   survive a restore, or a real restore would need step 12 of the first deploy again. Write down
   what you found.

4. **Delete the restored server.** It bills like the live one.

   ```bash
   az postgres flexible-server delete --resource-group rg-studiomanager-dev --name psql-restore-drill --yes
   ```

5. **Record the drill** in the table below: the date, how long the restore took, and anything
   that didn't match.

## Drill log

| Date | Restore took | Counts matched | Notes |
|---|---|---|---|
| _not yet run_ | | | |
