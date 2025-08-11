#!/usr/bin/bash

DIR="/mnt/backup/final/"

readarray FILES < <(find $DIR -type f ! -name '*_tn.jpg' -name '*.jpg' -o -name '*.jpeg')
declare -a FILES
echo ${#FILES[@]} files found.

for file in ${FILES[@]};
do
    echo "----------------------------------------------------------"
    # Extract the filename without the directory path
    filename=$(basename "$file")

    # Extract the dir part before the first underscore
    path=$(dirname "$file")

    # extract info
    file_sz=$(stat -c%s "$file")
    xRes=$(magick identify "$file" | grep -Po '\d+x\d+ ' | tr 'x' ' ' | awk '{print $1}')
    yRes=$(magick identify "$file" | grep -Po '\d+x\d+ ' | tr 'x' ' ' | awk '{print $2}')
    d_str=$(magick identify -verbose "$file" | grep 'date:modify' | awk '{print $2}')

    # make thumbnail
    #tn_file="${file%.*}_tn.png"
    #convert "$file" -auto-orient -thumbnail 200x200^ -gravity center -extent 200x200 "$tn_file"

    # build SQL insert statement
    read -r -d '' sql << EOM
insert into thumbnails(timeStr, path, filename, size, xRes, yRes, rating)
values("$d_str", "$path", "$filename", $file_sz, $xRes, $yRes, NULL);
EOM

    # insert
    echo $sql
    sqlite3 /home/soren/Linux/clojure/picweb/pictures.sqlite3 "$sql";
done
