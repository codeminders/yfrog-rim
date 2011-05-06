#!/bin/bash

outdir="web_install"
modules="YFrog YFrogLib"
outjad="$outdir/YFrog.jad"

res="codeminders/yfrog/res/YFrog.rrc"
jdw="YFrog.jdw"

if [ ! -d $outdir ] ; then
    /bin/mkdir $outdir
else
    rm -f $outdir/* > /dev/null 2> /dev/null
fi

for n in $modules ; do
    unzip $n.cod -d $outdir > /dev/null 2> /dev/null
    if [ ! -f $outdir/$n.cod ] ; then
        cp $n.cod $outdir/
    fi
done

app_vendor=`cat $jdw | d2u | grep "VendorOverride=" | cut -d = -f 2`
app_version=`cat $jdw | d2u | grep "VersionOverride=" | cut -d = -f 2`
app_name=`cat $res | d2u | grep "APP_TITLE#0=\"" | cut -d \" -f 2`
app_desc=`cat $res | d2u | grep "APP_DESCRIPTION#0=\"" | cut -d \" -f 2`

echo "Manifest-Version: 1.0" >> $outjad
echo "RIM-COD-Module-Name: $app_name" >> $outjad
echo "MIDlet-Name: $app_name" >> $outjad
echo "MIDlet-Vendor: $app_vendor" >> $outjad
echo "MIDlet-Description: $app_desc" >> $outjad
echo "MIDlet-Version: $app_version" >> $outjad
echo >> $outjad

codnum=1

print_cod() {
    size=`ls -l $outdir/$1 | cut -d " " -f 5`
    echo "RIM-COD-URL-${codnum}: $1" >> $outjad
    echo "RIM-COD-Size-${codnum}: $size" >> $outjad
    codnum=$(($codnum+1))
}

for n in $modules ; do
    print_cod $n.cod
    ni=1
    while [ -f $outdir/$n-$ni.cod ] ; do
        print_cod $n-$ni.cod
        ni=$(($ni+1))
    done
done

echo "$app_vendor $app_name v$app_version"
