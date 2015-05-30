## Done ##

  * Enforced garbage collection: (onStop, onDestroy).
  * Removed depluicate instances (MixContex created twice, DataView, DownloadManager)
  * Finalized one time use fields.
  * Added webview cancelation if clicked away.
  * Added activities' result passing among them.
  * Restructured field memebers and method for developer's readibility.

  * Despite the code enhancement, and lowering the processing power, I was unable to add enhancement on clustered images. It takes more processing power, more than what's already done in the Augmented reality. I tried these methods:
  1. Converting bitmap to drawable to make use of Android internal gallery viewer.(BitmapView class and use API demo gallery)
> 2- LayerDrawable
> 3- NinePatch drawable
> 4- PolyToPoly


## To do ##

  * Create pull request to merge project into Mixare. (see Metadata for more info)