#!/bin/bash
# 确保已安装 GraalVM 和 native-image

# 设置 GRAALVM_HOME
GRAALVM_HOME=$graal

# 清理之前的构建
echo '清理之前的制品'
rm -rf target

# 编译
echo '编译'
clojure -T:build compile

# 创建 uberjar
echo '创建 uberjar'
clojure -T:build uber

# 使用完整路径调用 native-image
echo '构建 native-image'
$GRAALVM_HOME/bin/native-image \
  --no-fallback \
  --initialize-at-build-time \
  --report-unsupported-elements-at-runtime \
  -H:+ReportExceptionStackTraces \
  -H:IncludeResources='.*/.*' \
  -H:ConfigurationFileDirectories=resources/META-INF/native-image \
  -jar target/cissy.jar \
  target/cissy 