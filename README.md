play-scala-calendar
===================

A simple calender using Scala/Play/Slick for creating and joining events.

:warning: Work in progress!

Overview
--------

Events are assigned to locations and can have multiple series and tags. Series define temporal properties, i.e. date, time, recurrence rules, etc. Each series occurrence is stored individually as well, and will contain booking information in the future.

Requirements
------------

* Java 8 JDK (http://www.oracle.com/technetwork/java/javase/downloads/index.html)
* Scala 2.12 (https://www.scala-lang.org)
* SBT 0.13 (https://www.scala-sbt.org/download.html)
* H2 (just the jar file is required for the server, see below, http://www.h2database.com/html/download.html)

Run
---

* Start a H2 server:

      java -cp h2-1.4.197.jar org.h2.tools.Server

* Run app:

      sbt run
 