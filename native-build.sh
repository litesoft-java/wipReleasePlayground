[ "$GRAALVM_HOME" == "" ] && source setUpNative-Local_Dir.sh

jenv local "$JENV_VERSION"
java -version

mvn -Pnative -DskipTests clean package
