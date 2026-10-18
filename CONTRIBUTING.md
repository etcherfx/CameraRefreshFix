# Contributing to CameraRefreshFix

Bug reports, compatibility reports for other devices and frameworks, and focused fixes are welcome. Open an issue before a large change.

## Reporting issues

Search existing issues first. Include reproducible steps, expected and actual behavior, and the module version, device, Android version and Xposed framework. Review logs and screenshots before sharing; remove secrets and personal information.

Use the issue forms for bugs and feature requests. For a security problem, see [SECURITY.md](SECURITY.md).

## Development

Build and test commands will live in [AGENTS.md](AGENTS.md) once the implementation lands. Everything Android-related runs inside a Docker container that carries the Android SDK.

## Pull requests

- Keep each pull request to one focused change, and fill in the pull request template.
- Include relevant tests and update affected docs when behavior changes.
- Write in American English: code, comments, docs and user-facing text.
- Use Conventional Commits and sign off every commit (`git commit -s`).

## Releasing

Maintainers only. Write `.github/release-notes/vX.Y.Z.md`, commit it as `chore(release): vX.Y.Z`, then tag and push:

```sh
git tag -m vX.Y.Z vX.Y.Z
git push origin vX.Y.Z
```

The release workflow checks that the tag is valid SemVer, appends the Full Changelog link to the notes, and publishes the release.

## Licensing

Contributions are licensed under the project's [GPL-3.0](LICENSE) license.
