# Releasing

Releases are published by a single manual button click in GitHub Actions. No manual upload is needed once
the token is configured.

## One-time setup

1. Create a repository secret named `CURSEFORGE_TOKEN` under **Settings → Secrets and variables → Actions**.
   It must be a CurseForge API token with upload access to **Locks neoforked** (project ID `1767458`).
2. `GITHUB_TOKEN` is provided automatically by GitHub; there is nothing to create for GitHub Releases.

The CurseForge project description is **not** updated by the workflow. When the player-facing text changes,
copy `publication/MOD_DESCRIPTION.md` into the project description on CurseForge manually.

## Publishing a version

1. Set the final version in `gradle.properties` (currently `mod_version=1.8.3`) and add a matching
   `## 1.8.3` section to `CHANGELOG.md`.
2. Commit and push to `main`. The **CI** workflow builds and tests the mod and uploads the JAR as an artifact.
3. In the repository, open **Actions → Release → Run workflow**, select `main`, and click **Run workflow**.

There is no version field to fill in. The workflow reads `mod_version` from `gradle.properties` and then:

- builds and tests with `./gradlew build`;
- extracts only the matching `## <version>` section of `CHANGELOG.md` as release notes;
- verifies `build/libs/locks_neoforked-<version>.jar` exists;
- publishes that JAR to GitHub Releases and CurseForge in one
  [`mc-publish`](https://github.com/Kira-NT/mc-publish) step.

If a GitHub tag for that version does not exist yet, `mc-publish` creates it from the exact commit that was
built. Existing tags are reused. Tags still follow the plain `1.8.3` convention, without a `v` prefix.

Avoid clicking **Run workflow** twice after a successful release: CurseForge would receive a duplicate file.
`mc-publish` can update an existing GitHub release, but it cannot remove an already uploaded CurseForge file.

If the first run fails because `CURSEFORGE_TOKEN` was missing, add the secret and click **Run workflow** again.

## Files

- `.github/workflows/ci.yml` — push/PR build and test
- `.github/workflows/release.yml` — manual publish
- `publication/MOD_DESCRIPTION.md` — source for the CurseForge description
