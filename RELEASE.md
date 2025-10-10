# How to Release

This guide explains how to create a new release for PersistID.

## Release Process

### 1. Pre-Release Verification

Verify the build is stable:
```bash
# Execute test suite
./gradlew test

# Build release artifact
./gradlew :persistid:assembleRelease

# Perform full build verification
./gradlew clean build
```

### 2. Version Update

Update the version number in `persistid/build.gradle.kts`:
```kotlin
version = "0.1-alpha01"  // Update to target version
```

### 3. Commit Changes

```bash
git add .
git commit -m "Prepare for release v0.1-alpha01"
git push origin main
```

### 4. Tag Creation and Publication

```bash
# Create annotated tag (without 'v' prefix)
git tag -a 0.1-alpha01 -m "Release 0.1-alpha01"

# Publish tag to remote
git push origin 0.1-alpha01
```

### 5. Automated Release Workflow

Upon tag publication, GitHub Actions automatically executes:
- Test suite execution
- Release AAR build
- GitHub Release creation
- AAR artifact upload
- Pre-release marking (for alpha/beta/rc versions)
- JitPack build notification

### 6. Release Verification

1. **GitHub Release Validation**
   - Navigate to: https://github.com/shibaprasadsahu/PersistID/releases
   - Confirm release creation
   - Verify AAR attachment

2. **JitPack Build Verification**
   - Navigate to: https://jitpack.io/#shibaprasadsahu/PersistID
   - Monitor build completion (2-5 minutes)
   - Confirm version availability

3. **Installation Testing**
   ```kotlin
   dependencies {
       implementation("com.github.shibaprasadsahu:PersistID:0.1-alpha01")
   }
   ```

## Version Naming Convention

The project follows semantic versioning with pre-release identifiers:

### Alpha Releases (Current Phase)
```
0.1-alpha01
0.1-alpha02
0.2-alpha01
```

### Beta Releases (Future Phase)
```
0.9-beta01
1.0-beta01
```

### Release Candidates (Future Phase)
```
1.0-rc01
1.0-rc02
```

### Stable Releases (Future Phase)
```
1.0
1.1
2.0
```

## Pre-release vs Stable Classification

The release workflow automatically classifies releases as **pre-release** when the tag contains:
- `alpha`
- `beta`
- `rc`

Versions without these identifiers are classified as **stable releases**.

## Release Rollback

To remove a published release:

```bash
# Remove local tag
git tag -d 0.1-alpha01

# Remove remote tag
git push --delete origin 0.1-alpha01
```

Subsequently, manually delete the release from the GitHub Releases page.

## Troubleshooting

### JitPack Build Failure
- Review build logs: https://jitpack.io/com/github/shibaprasadsahu/PersistID/[version]/build.log
- Verify `jitpack.yml` configuration
- Confirm local Gradle build succeeds

### Release Creation Failure
- Inspect GitHub Actions: https://github.com/shibaprasadsahu/PersistID/actions
- Verify tag publication: `git ls-remote --tags origin`
- Review workflow execution logs

### AAR Attachment Failure
- Confirm build success in workflow logs
- Verify artifact path: `persistid/build/outputs/aar/persistid-release.aar`
- Retry by rebuilding and republishing tag

## Best Practices

- Perform comprehensive local testing before release
- Write descriptive commit messages
- Maintain consistent versioning
- Document breaking changes thoroughly
- Validate published artifacts before announcement
