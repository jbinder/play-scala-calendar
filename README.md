play-scala-calendar
===================

A simple calender using Scala/Play/Slick.

Work in progress!

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
 