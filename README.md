# Fcrepo Bootstrap Data

This repository contains rdf sample datasets and code to generate sample datasets from umd fedora2 pid list. These datasets can be used to bootstrap a fedora 4 repsitory using the resourceimport profile of the [fcrepo-sample-dataset](https://github.com/mohideen/fcrepo-sample-dataset) project.

## Generating Datasets from PID List:
Use ```mvn compile``` and ```mvn exec:java``` to generate the dataset.

The following paramaters can be overriden, if necessary:

```pids.list``` (default: ```src/main/resources/pid-list.txt```)

```fedora2.url``` (default: ```http://fedora.lib.umd.edu/fedora```)

```out.dir``` (default: ```target/bootstrap-data```)