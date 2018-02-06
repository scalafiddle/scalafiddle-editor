# Contributing

ScalaFiddle is open-source software and relies on individual contributions to keep it up-to-date. Please read through this (short) guide to make sure your
contributions can be easily integrated into the project.

## Setting up dev env locally

ScalaFiddle has two separate repositories, the editor (this one) and the [core](https://github.com/scalafiddle/scalafiddle-core). To get started, clone the two
repos locally.

```bash
$ git clone https://github.com/scalafiddle/scalafiddle-core.git

$ git clone https://github.com/scalafiddle/scalafiddle-editor.git
```

Next start two separate `sbt` sessions in two terminal windows, one for each project.

You can now start the editor with
```
[server] run
```
which will launch the Play framework, waiting for a connection on 0.0.0.0:9000. At this point you can already navigate to http://localhost:9000 and use the
editor.

Before you can compile anything, you must also start the `router` and `compilerServer` in the other `sbt` session. These are built on Akka-HTTP instead of Play,
so you can run them in the background with

```
> router/reStart

> compilerServer/reStart
```

The `router` will load library information from the same place as `editor` does. `compilerServer` connects to the `router` over a web-socket and
received commands from the `router`. On the first run the compiler server will download all libraries and build a large _flat file_ containing classes and sjsir
files from those libraries. This will take some time, but is only done once. Afterwards the _flat file_ is reused and new libraries are only appended to it.

Now your dev env should be running and you should be able to create, edit, compile and run fiddles! Note that saved fiddles will disappear when the `editor` is
stopped, as by default they are stored in an in-memory H2 database.

## Supporting new libraries

By far the most common contribution is adding support for a new library (or a version of a library). To do this you simply need to edit the 
[libraries.json](https://github.com/scalafiddle/scalafiddle-io/blob/master/libraries.json) file under the 
[scalafiddle-io](https://github.com/scalafiddle/scalafiddle-io) repo.

To test new libraries locally, override the default library URL configuration via environment variable `SCALAFIDDLE_LIBRARIES_URL` 
to point to a local file using `file:/path/to/local/libraries.json` syntax.

Please follow these rules for adding a new library:

1. Never remove old versions of libraries
1. When adding a new version, make it the first in the list of versions
1. A new library should be added under an appropriate group. If no group seems to match, contact the maintainers.
1. Remember to fill in all fields, including the doc link (which can be a Github project name or a full URL)
1. Test the newly added library in your local ScalaFiddle editor, using the library and its features.

When everything works locally, you can submit a PR with your changes to the main repo.
