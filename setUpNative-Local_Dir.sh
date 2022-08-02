source Graalvm-Versions.sh
graalvmRoot="/Library/Java/JavaVirtualMachines/graalvm-ce-java$dirGraalvmCE/Contents/Home"
if [ ! -d "$graalvmRoot" ]
then
	echo "No graalvm-ce for version '$dirGraalvmCE' at: $graalvmRoot"
	exit 1
fi
if [ ! -e "$graalvmRoot/bin/javac" ]
then
	echo "Graalvm dir, but no 'bin/javac' at: $graalvmRoot"
	exit 1
fi
if [ ! -e "$graalvmRoot/bin/gu" ]
then
	echo "Graalvm JVM, but no 'bin/gu' at: $graalvmRoot"
	exit 1
fi

export PATH="$PATH:$graalvmRoot/bin"

if [ ! -e "$graalvmRoot/bin/native-image" ]
then
	echo "Graalvm JVM, but 'native-image' not installed; run: gu install native-image"
	exit 1
fi

export JENV_VERSION="graalvm64-$jenvGraalvm64"
export JAVA_HOME="$graalvmRoot"
export GRAALVM_HOME="$JAVA_HOME"

echo "***************************************************************************************************"
echo "* setUpNative-Local_Dir.sh was run -- to eliminate this, you can: source setUpNative-Local_Dir.sh"
echo "***************************************************************************************************"
