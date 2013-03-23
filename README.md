Cloudie
=======

Cloudie is a simple user interface to browse Open Stack Storage (Swift) and to manipulate it.

(C) 2012-2013 E.Hooijmeijer and 42 B.V., [Apache 2 licensed](https://www.apache.org/licenses/LICENSE-2.0.html).

Uses the CC-BY licensed Silk Iconset by [Mark James](http://www.famfamfam.com/)

Features
--------

Based on the [JOSS](https://github.com/javaswift/joss) library, it has the the following features:
- creation and deletion of containers.
- creation and deletion of stored objects.
- down and uploading of files.
- viewing meta-data.
- previewing text and images.
- opening a stored object in a public container in your browser.
- filename based search.
- storage of connection profiles.

Usage
-----
```
Usage: java -jar cloudie-0.7.1-full.jar [options]
    Options:
      -help, -?, --?
         Brief help.
      -login
         connects to the cloud. Takes 4 arguments: [AuthURL] [Tenant] [Username] [Password]
      -profile
         connects to the cloud using an previously stored profile.
```

A prebuilt binary can be [downloaded here](http://www.ctrl-alt-dev.nl/Projects/Cloudie-OpenStack/Cloudie-OpenStack.html).

Screenshots
-----------

![Cloudie](http://www.ctrl-alt-dev.nl/Projects/Cloudie-OpenStack/resources/Cloudie-OpenStack/cloudie-full.png "Main GUI")

History
=======

V0.7.1 16-Mrt-2013
------------------
- Rename to org.javaswift

V0.7.0 11-Feb-2013
------------------
- First Public Release

Sponsor
-------
This component was graciously donated by [42 BV](http://www.42.nl) ![42 logo](http://www.42.nl/images/42-54x59.png "42")