# List of Done Changes and Contributions #

  * Change the android minim API level to 8, targeting API 10 and
higher. The API change was done because android SDK 9, which is
obsolete API. I have added the [missing functionality to lower the API compatibility to API 8](https://github.com/DevBinnooh/mixare/commit/a3378e8d49a895a9506cbb09e697f7338bba68ea).    http://developer.android.com/resources/dashboard/platform-versions.html

  * Correct Open Street displayed arrow connection

  * Add Thumbnil display type

  * Added Panoramio Photo support

  * Shrinking displayed titles (currently for Panoramio)

  * Abstracted data from operations (decoupled Mixview MixContext, for
better caching)

  * Reorganized classes (moved AugmentedView and camera surface to
reality package)

  * Optimize app's lifecycle for faster transition between Activities (garbug collection: (onStop, onDestroy), dataFlow, removed depluicate instances, removed used fields, finalized one time use fields)

  * Enhance construction and destruction of markers for better caches use. (adopted Abstract tree structure for markers,

  * Bug Fixes in DownloadManager (thread deadlock)

  * Added webview cancelation if clicked away

  * Added activities' result passing among them

  * Restructured field memebers and method for developer's readibility.

  * [Added Arabic language support to Mixare](https://github.com/DevBinnooh/mixare/commit/aee3b63f0a840e9d09ba9728baa23c023def359e)

  * [UML Diagrams for both Open Album and Mixare](https://github.com/DevBinnooh/mixare/commit/a3378e8d49a895a9506cbb09e697f7338bba68ea).

  * [Fixed Issue 113 on Mixare](http://code.google.com/p/mixare/issues/detail?id=113&colspec=ID%20Type%20Status%20Priority%20Stars%20Version%20Owner%20Summary).

  * [work around Issue 98](http://code.google.com/p/mixare/issues/detail?id=98&colspec=ID%20Type%20Status%20Priority%20Stars%20Version%20Owner%20Summary).

  * [Added Web View Cache](https://github.com/DevBinnooh/mixare/commit/350d595ecf489e5769b599adf2954942d6354a7b), an Approach that is in [progress](http://code.google.com/p/mixare/issues/detail?id=117&colspec=ID%20Type%20Status%20Priority%20Stars%20Version%20Owner%20Summary).

  * [decoupled tied functionality](https://github.com/DevBinnooh/mixare/commit/e13a6ec88851a3dd90e8cff192e585e23b059d02).
