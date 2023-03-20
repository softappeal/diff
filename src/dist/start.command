set -e
cd `dirname "$0"`
chmod +x bin/diff
bin/diff MD5 .. node.yass
cd ..
archiveFile=../`basename \`pwd\``_`date +%Y.%m.%d-%H.%M.%S`.zip
zip --quiet --recurse-paths $archiveFile . --exclude "*/.DS_Store" --exclude ".DS_Store"
echo archive \'$archiveFile\' written
