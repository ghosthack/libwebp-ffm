# Publishing to Maven Central

Publishing is gated by a non-prerelease GitHub Release. Pushes and pull
requests build and test but never publish.

## Release procedure

1. Replace the SNAPSHOT version in the parent and module poms with the release
   version and update versioned documentation.
2. Merge the release commit to `main` and wait for CI.
3. Publish a GitHub Release tagged `v<version>` at that exact commit.
4. The release workflow validates the tag, rebuilds and tests natives on all
   three platforms, and deploys the resulting classifier jars and binding jar.

Repository secrets required by the workflow are
`MAVEN_CENTRAL_USERNAME`, `MAVEN_CENTRAL_PASSWORD`, `GPG_PRIVATE_KEY`, and
`GPG_PASSPHRASE`. The Central token supplies the first two values.

Builds use the Maven Wrapper (`./mvnw`), which pins Maven 3.9.16. Do not
switch the release to the runner's `mvn`: under Maven 3.10 the
central-publishing plugin (0.11.0) stages resolver files such as
`_remote.repositories` and `maven-metadata-local.xml` into the bundle, and
Central rejects it with "Bundle has content that does NOT have a .pom file".

For a local packaging check without publishing:

```sh
./mvnw -Prelease -Dgpg.skip=true clean verify
```

To inspect the signed bundle CI would upload, run the Release workflow
manually (optionally with a throwaway `version`). It rebuilds the natives on
all three platforms, builds against an unreachable Central URL with
placeholder credentials, fails if the bundle contains local repository
metadata, and attaches `central-bundle.zip` as a workflow artifact; nothing
is published.

Maven Central releases are immutable; publish corrections under a new version.

