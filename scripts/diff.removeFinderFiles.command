cd `dirname "$0"`
echo
echo "'.DS_Store' files to be deleted:"
echo
find . -name ".DS_Store" -type f -print
echo
read -p "Really delete? [y/N] " answer
if [[ "$answer" =~ ^[Yy]$ ]]; then
    find . -name ".DS_Store" -type f -delete
    echo "Deleted."
else
    echo "Aborted."
    exit 1
fi
