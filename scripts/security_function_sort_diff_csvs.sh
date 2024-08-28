#!/usr/bin/env bash

echo "Renaming and sorting old CSV files"

files=$(ls old_xray)
for name in ${files}
do
  newname=$(echo ${name} | sed -e "s/^Docker_proj-am-releases-//" -e "s/[0-9.\-]*_Security_Export.csv/.csv/")
  head -n 1 old_xray/${name} > old_xray/${newname}
  tail -n +2 old_xray/${name} | sort >> old_xray/${newname}
  rm old_xray/${name}
  echo "Created old_xray/${newname}"
done

echo "Renaming and sorting new CSV files"

files=$(ls new_xray)
for name in ${files}
do
  newname=$(echo ${name} | sed -e "s/^Docker_proj-am-releases-//" -e "s/[0-9.\-]*_Security_Export.csv/.csv/")
  head -n 1 new_xray/${name} > new_xray/${newname}
  tail -n +2 new_xray/${name} | sort >> new_xray/${newname}
  rm new_xray/${name}
  echo "Created new_xray/${newname}"
done

echo "Running diff on old and new files"
rm -rf diff_xray || echo "Could not delete diff_xray folder"
mkdir diff_xray
result=0
files=$(ls new_xray)
for name in ${files}
do
  if [[ -f old_xray/${name} ]]; then
    diff old_xray/${name} new_xray/${name} | grep -v ^[0-9] > diff_xray/${name}
  else
    echo "New Xray report found which doesn't have a matching old reports"
    result=1
  fi
done