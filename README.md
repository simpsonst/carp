# Purpose

This software defines an IDL (not CORBA), and consists of an annotation processor to convert IDL files into Java, and run-time support to use such classes.
[More details are available.](https://www.lancaster.ac.uk/~simpsons/software/pkg-carp)

# Installation

You need the following to build from source:

* [Jardeps](https://github.com/simpsonst/jardeps)

* [LuSyn](https://www.lancaster.ac.uk/~simpsons/software/pkg-lusyn)

* some Apache HTTP libraries

* Java 10

Create `config.mk` adjacent to `Makefile`:

```
## In ./config.mk

PREFIX=$(HOME)/.local
CLASSPATH += /usr/share/java/junit4.jar
CLASSPATH += /usr/share/java/javax.json.jar
CLASSPATH += /usr/share/java/httpcore.jar
CLASSPATH += /usr/share/java/httpclient.jar
CLASSPATH += $(HOME)/.local/share/java/lusyn.jar
```

The you can run:

```
make
make install
```

Variations:

* Adjust `PREFIX` and the path of `lusyn.jar` as required.
  The default for `PREFIX` is `/usr/local`.
  You might need `sudo make install` if installing to a protected location.

* You can alternatively place configuration in `carp-env.mk`, which can be anywhere in `make`'s search path, as set by `make -I`.

# Use

## Compilation

To compile, in your classpath, you need:

* `httpcore.jar`
* `httpclient.jar`
* `carp_core.jar`

In your processor classpath, you need:

* `javax.json.jar`
* `lusyn.jar`
* `carp_aproc.jar`

You can use these with NetBeans to generate the classes as you edit.

[See the Javadoc.](https://www.lancaster.ac.uk/~simpsons/javadoc/carp/overview-summary)

## Execution

At run time, you need:

* `javax.json.jar`
* `carp_rt.jar`
