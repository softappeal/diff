diff="java -jar build/tasks/_diff_executableJarJvm/diff-jvm-executable.jar"

$diff createNode SHA3-256 testFiles | \
$diff printNode
