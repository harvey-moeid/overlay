#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
export JAVA_HOME="${BYGA_JAVA_HOME:-/workspace/toolchains/jdk-17.0.15+6}"
export ANDROID_HOME="${BYGA_ANDROID_SDK_ROOT:-/workspace/toolchains/android-sdk}"
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-/workspace/toolchains/gradle-home}"
test -x "$JAVA_HOME/bin/javac" || { echo 'JDK lengkap belum tersedia; lihat README'; exit 1; }
test -f "$ANDROID_HOME/platforms/android-36/android.jar" || { echo 'Android SDK 36 belum tersedia; lihat README'; exit 1; }
# JVM does not automatically use HTTPS_PROXY. Never disable certificate validation.
if [[ -n "${HTTPS_PROXY:-}" ]]; then
  read -r byga_proxy_host byga_proxy_port < <(python3 - <<'PYTHON'
import os, urllib.parse
u=urllib.parse.urlparse(os.environ['HTTPS_PROXY'])
assert u.hostname and not u.username and not u.password
print(u.hostname, u.port or 80)
PYTHON
)
  export JAVA_OPTS="${JAVA_OPTS:-} -Dhttps.proxyHost=$byga_proxy_host -Dhttps.proxyPort=$byga_proxy_port -Dhttp.proxyHost=$byga_proxy_host -Dhttp.proxyPort=$byga_proxy_port -Dhttp.nonProxyHosts=localhost|127.* -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts"
fi
if [[ $# -eq 0 ]]; then set -- testDebugUnitTest assembleDebug; fi
exec ./gradlew "$@" --no-daemon --console=plain
