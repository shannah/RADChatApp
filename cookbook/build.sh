#!/bin/bash
mkdir ../docs/cookbook
mkdir ../docs/cookbook/images
asciidoctor -D ../docs/cookbook/ index.adoc 
cp -r images/* ../docs/cookbook/images/