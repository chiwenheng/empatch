#!/bin/bash

cp MANIFEST.MF empatchjar/build/classes/main/
mkdir -p empatchjar/build/classes/main/libs/
cp libs/* empatchjar/build/classes/main/libs/
cd empatchjar/build/classes/main

function pack {
    jar cvfm empatch.jar MANIFEST.MF\
        com/hyphenate/asm/*.class\
        com/hyphenate/asm/reflect/*.class
}

if [ $# -gt 0 ]; then
    if [ $1 = pack ]; then
        pack
        exit 0;
    fi
else
cat <<EOF
    asm.sh accept parameters:
        pack
EOF
fi

