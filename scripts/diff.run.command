cd `dirname "$0"`
echo
java -jar diff.jar | tee diff.md
