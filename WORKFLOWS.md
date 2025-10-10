# GitHub Actions Workflows Explained

This document explains the CI/CD workflows used in PersistID.

## Overview

This project uses **2 automated workflows**:

1. **CI Workflow** (`.github/workflows/ci.yml`) - Executes on every push to main branch
2. **Release Workflow** (`.github/workflows/release.yml`) - Executes when a git tag is created

---

## 1. CI Workflow (Continuous Integration)

### When It Runs

```yaml
on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]
```

**Triggers:**
- Commits pushed to `main` branch
- Pull requests targeting `main` branch

**Does not execute on:**
- Commits to feature branches
- Tag creation

### Workflow Jobs

The CI workflow executes **3 jobs** sequentially:

---

#### **Job 1: Lint Check**

**Purpose:** Validates code quality and style

**Steps:**
```bash
1. Checkout repository
2. Configure JDK 17
3. Execute: ./gradlew lint
4. Upload lint reports (if issues detected)
```

**Duration:** ~2-3 minutes

**Validation checks:**
- Unused resource detection
- Hardcoded string identification
- API compatibility verification
- Code style enforcement
- Security vulnerability scanning

**Output:**
- Success indicator if no issues found
- Failure indicator if problems detected
- HTML report with detailed findings

---

#### **Job 2: Unit Tests**

**Purpose:** Executes all 46 unit tests

**Steps:**
```bash
1. Checkout repository
2. Configure JDK 17
3. Execute: ./gradlew test --stacktrace
4. Upload test reports
5. Publish test results
```

**Duration:** ~3-5 minutes

**Test coverage:**
- PersistId API (10 tests)
- DataStore storage (12 tests)
- Repository logic (14 tests)
- Identifier generation (10 tests)

**Output:**
- Success indicator if all tests pass
- Failure indicator if any test fails
- Detailed test report with pass/fail status for each test

---

#### **Job 3: Build Library**

**Purpose:** Builds release AAR file

**Execution condition:** Only if lint and tests both pass

**Steps:**
```bash
1. Checkout repository
2. Configure JDK 17
3. Execute: ./gradlew :persistid:assembleRelease
4. Upload AAR artifact
```

**Duration:** ~1-2 minutes

**Output:**
- Success indicator if build completes
- AAR artifact available for download (persistid-release.aar)

---

### Visual Flow Diagram

```
Push to main
    ↓
─────────────────────────────────
│  Job 1: Lint   │  Job 2: Test │  (Run in parallel)
─────────────────────────────────
    ↓                   ↓
    └───────┬───────────┘
            ↓
     Both pass? ─── No ──→ Stop (build fails)
            ↓
           Yes
            ↓
   Job 3: Build Library
            ↓
      Upload AAR
            ↓
       Success!
```

### How to View Results

1. Go to: `https://github.com/shibaprasadsahu/PersistID/actions`
2. Click on latest workflow run
3. See status of each job
4. Download artifacts (reports, AAR) if needed

---

## 2. Release Workflow (Automated Publishing)

### When It Runs

```yaml
on:
  push:
    tags:
      - '[0-9]*'
```

**Triggers:**
- Git tags starting with version numbers
- Examples: `0.1-alpha01`, `1.0`, `2.5-beta03`

**Does not execute on:**
- Regular commits to main branch
- Pull requests
- Tags with `v` prefix

### Workflow Execution

The release workflow contains **1 job** with multiple steps:

---

#### **Release Job Steps**

**1. Environment Setup**
```bash
- Checkout repository
- Configure JDK 17
- Set gradlew execute permissions
```

**2. Quality Verification**
```bash
- Execute all tests: ./gradlew test
- Halt release if tests fail
```

**3. Release Build**
```bash
- Build release AAR: ./gradlew :persistid:assembleRelease
- Output: persistid-release.aar
```

**4. Version Extraction**
```bash
- Parse tag: 0.1-alpha01
- Extract version: 0.1-alpha01
- Apply to release name
```

**5. Release Notes Generation**
```bash
Generates markdown document containing:
- Installation instructions
- Experimental API notice
- Documentation references
- JitPack build status link
```

**6. GitHub Release Creation**
```bash
- Publish release on GitHub
- Attach AAR artifact
- Include generated release notes
- Mark as pre-release for alpha/beta/rc versions
```

**7. JitPack Notification**
```bash
- Send API request to JitPack
- Initiate JitPack build process
- Publish library to JitPack repository
```

---

### Visual Flow Diagram

```
Push tag 0.1-alpha01
        ↓
    Checkout code
        ↓
    Setup JDK 17
        ↓
     Run tests
        ↓
  Tests pass? ─── No ──→ Stop (no release)
        ↓
       Yes
        ↓
  Build release AAR
        ↓
Extract version (0.1-alpha01)
        ↓
Generate release notes
        ↓
Create GitHub Release
        ↓
Upload AAR file
        ↓
  Mark as pre-release? (if alpha/beta/rc)
        ↓
   Notify JitPack
        ↓
JitPack builds library
        ↓
    Published!
```

**Total Time:** ~5-7 minutes

---

### Release Notes Content

The workflow auto-generates release notes containing:

- **Installation Instructions**: Dependency declaration with correct JitPack coordinates
- **Experimental API Notice**: Opt-in annotation requirement (`@OptIn(ExperimentalPersistIdApi::class)`)
- **Documentation Links**: References to README and architecture guide
- **JitPack Build Status**: Link to verify build completion
- **Release Notes**: Disclaimer for alpha/beta releases regarding API stability

---

## Pre-release Detection

The workflow automatically detects if it's a pre-release:

```yaml
prerelease: ${{ contains(github.ref, 'alpha') || contains(github.ref, 'beta') || contains(github.ref, 'rc') }}
```

**Classification Logic:**
- If tag contains `alpha` → Classified as pre-release
- If tag contains `beta` → Classified as pre-release
- If tag contains `rc` → Classified as pre-release
- Otherwise → Classified as stable release

**Examples:**
| Tag | Type |
|-----|------|
| 0.1-alpha01 | Pre-release |
| 0.9-beta02 | Pre-release |
| 1.0-rc01 | Pre-release |
| 1.0 | Stable |
| 2.5 | Stable |

---

## How to Trigger Each Workflow

### Trigger CI Workflow

```bash
# Method 1: Push to main
git add .
git commit -m "Update library"
git push origin main

# Method 2: Create pull request
git checkout -b feature/new-feature
git commit -m "Add new feature"
git push origin feature/new-feature
# Then create PR on GitHub
```

### Trigger Release Workflow

```bash
# Only triggered by pushing a tag (without 'v' prefix)
git tag -a 0.1-alpha01 -m "Release 0.1-alpha01"
git push origin 0.1-alpha01
```

---

## Workflow Artifacts

Both workflows create downloadable artifacts:

### CI Workflow Artifacts

1. **Lint Reports** (HTML)
   - Location: `persistid/build/reports/lint-results*.html`
   - When: Always (even if lint fails)

2. **Test Reports** (HTML + XML)
   - Location: `persistid/build/reports/tests/` and `persistid/build/test-results/`
   - When: Always (even if tests fail)

3. **Release AAR** (binary)
   - Location: `persistid/build/outputs/aar/*.aar`
   - When: Only if lint + tests pass

### Release Workflow Artifacts

1. **Release AAR** (attached to GitHub Release)
   - Filename: `persistid-release.aar`
   - Can be downloaded directly from release page

---

## Viewing Workflow Results

### On GitHub

1. **Status Badges** (in README)
   - Shows real-time status
   - Click badge → Go to Actions page

2. **Actions Tab**
   - URL: `https://github.com/shibaprasadsahu/PersistID/actions`
   - See all workflow runs
   - View logs for each step
   - Download artifacts

3. **Commit Status**
   - Green checkmark → All workflows passed
   - Red X → At least one workflow failed
   - Yellow circle → Workflow running

4. **Pull Request Checks**
   - Shows CI status on each PR
   - Must pass before merging

### Download Artifacts

```bash
# From GitHub UI:
1. Go to Actions tab
2. Click on workflow run
3. Scroll to "Artifacts" section
4. Click to download

# Files you can download:
- lint-report.zip
- test-reports.zip
- library-aar.zip (contains persistid-release.aar)
```

---

## Troubleshooting

### CI Workflow Failed

**Lint Failed:**
- Download lint report
- Fix issues shown in report
- Commit and push again

**Tests Failed:**
- Check test report
- See which test failed
- Fix the code
- Run locally: `./gradlew test`
- Commit and push again

**Build Failed:**
- Check build logs
- Usually dependency or Gradle issue
- Fix and push again

### Release Workflow Failed

**Tests Failed:**
- Run tests locally first: `./gradlew test`
- Fix any failing tests
- Delete tag: `git tag -d 0.1-alpha01`
- Delete remote tag: `git push --delete origin 0.1-alpha01`
- Push new tag after fixing

**Build Failed:**
- Check build logs
- Fix issue
- Delete and recreate tag

**JitPack Failed:**
- Check: https://jitpack.io/com/github/shibaprasadsahu/PersistID/[version]/build.log
- Usually means Gradle build issue
- Fix jitpack.yml or build.gradle.kts
- Delete release and tag
- Push new tag

---

## Best Practices

1. **Always wait for CI to pass** before creating a release
2. **Run tests locally** before pushing
3. **Create meaningful commit messages** for better CI logs
4. **Check Actions tab** after pushing to ensure workflow ran
5. **Download artifacts** if you need to inspect reports
6. **Test release locally** with `./gradlew publishToMavenLocal` before tagging

---

## Workflow Files

Both workflow files are in: `.github/workflows/`

- `ci.yml` - CI workflow
- `release.yml` - Release workflow

You can view them in your repository to see exact configuration.
