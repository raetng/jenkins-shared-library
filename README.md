# Jenkins Shared Library

Reusable pipeline functions for the DevOps e-commerce microservices project.

## Setup

Add to Jenkins under **Manage Jenkins > System > Global Pipeline Libraries**:

| Field | Value |
|-------|-------|
| Name | `devops-shared-library` |
| Default version | `main` |
| Load implicitly | checked |
| Source | Path to this repo / directory |

In your Jenkinsfile:

```groovy
@Library('devops-shared-library') _
```

## Functions

### `dockerBuildAndTag`

Builds a Docker image and tags it using the project's tagging strategy.

```groovy
def tag = dockerBuildAndTag(
    imageName: 'raetng/product',
    dockerfilePath: '.',       // optional, defaults to '.'
    tag: 'custom-tag'          // optional, auto-resolved from branch if omitted
)
```

**Auto-resolved tags:**
| Branch | Tag format | Example |
|--------|-----------|---------|
| `develop` (and feature branches) | `dev-<SHA>` | `dev-a1b2c3d` |
| `release/*` | `rc-<SHA>` | `rc-a1b2c3d` |
| `main` | `v<VERSION>` | `v1.2.0` (reads `VERSION` file) |

---

### `dockerPush`

Pushes a Docker image to Docker Hub.

```groovy
dockerPush(
    imageName: 'raetng/product',
    tag: 'dev-a1b2c3d',
    credentialsId: 'dockerhub-credentials'  // optional, this is the default
)
```

Also pushes a `latest` tag alongside the specified tag.

---

### `kubernetesDeploy`

Applies Kubernetes manifests and updates image tags on all deployments in the namespace.

```groovy
kubernetesDeploy(
    namespace: 'dev',
    manifestPath: 'k8s/product/',
    imageTag: 'dev-a1b2c3d'
)
```

Waits for rollout to complete (300s timeout).

---

### `securityScan`

Runs a Trivy vulnerability scan, archives reports (JSON + table), and marks the build unstable if the severity threshold is exceeded.

```groovy
securityScan(
    imageName: 'raetng/product',
    tag: 'dev-a1b2c3d',
    severityThreshold: 'CRITICAL'  // optional, defaults to 'CRITICAL'
)
```

**Recommended thresholds:**
| Environment | Threshold |
|-------------|-----------|
| Dev | `CRITICAL` |
| Staging | `HIGH` |
| Prod | `HIGH` |

---

### `runTests`

Runs a test command, archives results, and publishes JUnit reports.

```groovy
runTests(
    testCommand: 'npm test',
    resultPath: 'test-results/'  // optional, defaults to 'test-results/'
)
```

---

### `gitleaksScan`

Runs Gitleaks for secret detection and fails the build if secrets are found.

```groovy
gitleaksScan(
    reportFile: 'gitleaks-report.json',  // optional
    configFile: '.gitleaks.toml'          // optional, uses default rules if omitted
)
```

## Directory Structure

```
shared-library/
├── vars/
│   ├── dockerBuildAndTag.groovy
│   ├── dockerPush.groovy
│   ├── kubernetesDeploy.groovy
│   ├── securityScan.groovy
│   ├── runTests.groovy
│   └── gitleaksScan.groovy
└── README.md
```
