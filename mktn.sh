#!/usr/bin/bash

DIR="/mnt/backup/final/"

readarray FILES < <(find $DIR -type f -name '*.jpg' -o -name '*.jpeg')
declare -a FILES
echo ${#FILES[@]} files found.

for file in ${FILES[@]};
do
    # Extract the filename without the directory path
    filename=$(basename "$file")

    echo $filename

    # Extract the dir part before the first underscore
    path=$(dirname "$file")

    # make thumbnail
    tn_file="${file%.*}_tn.jpg"
    convert "$file" -auto-orient -thumbnail 150x150^ -gravity center -extent 150x150 "$tn_file"
done
