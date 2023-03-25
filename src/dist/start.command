set -e
cd `dirname "$0"`
chmod +x bin/diff
newNode=`mktemp`
bin/diff create MD5 .. >> $newNode
echo

# print duplicates
bin/diff duplicates < $newNode
echo

if [[ -f node.yass ]]; then
    bin/diff diff node.yass < $newNode
    echo
    printf "%s" "type <y> to overwrite node file (else abort): "
    read answer
    echo
    if [[ "$answer" != "y" ]]; then
        rm $newNode
        echo ABORTED
        exit 1
    fi
fi
mv $newNode node.yass

# write archive
cd ..
archiveFile=../`basename \`pwd\``_`date +%Y.%m.%d-%H.%M.%S`.zip
zip --quiet --recurse-paths $archiveFile . --exclude "*/.DS_Store" --exclude ".DS_Store"
echo archive \'$archiveFile\' written
echo

echo DONE
