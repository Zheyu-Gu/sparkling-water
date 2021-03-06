#!/usr/bin/env bash

# Current dir
TOPDIR=$(cd "$(dirname "$0")/.."; pwd)

source "$TOPDIR/bin/sparkling-env.sh"
# Java check
checkJava
# Verify there is Spark installation
checkSparkHome
# Verify if correct Spark version is used
checkSparkVersion
# Check sparkling water assembly Jar exists
checkFatJarExists
DRIVER_CLASS=ai.h2o.sparkling.SparklingWaterDriver

DRIVER_MEMORY=${DRIVER_MEMORY:-$DEFAULT_DRIVER_MEMORY}
MASTER=${MASTER:-"$DEFAULT_MASTER"}
VERBOSE=--verbose
VERBOSE=
if [ -f "$SPARK_HOME"/conf/spark-defaults.conf ]; then
    EXTRA_DRIVER_PROPS=$(grep "^spark.driver.extraJavaOptions" "$SPARK_HOME"/conf/spark-defaults.conf 2>/dev/null | sed -e 's/spark.driver.extraJavaOptions//' )
fi

# Show banner
banner

spark-submit "$@" $VERBOSE --driver-memory "$DRIVER_MEMORY" --master "$MASTER" --conf spark.driver.extraJavaOptions="$EXTRA_DRIVER_PROPS" --class "$DRIVER_CLASS" "$FAT_JAR_FILE"
