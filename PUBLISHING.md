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

For a local packaging check without publishing:

```sh
mvn -Prelease -Dgpg.skip=true clean verify
```

Maven Central releases are immutable; publish corrections under a new version.

