# CHANGELOG

## 0.9.2


### Features
    * Added support for initial acl on documents
    * Added getters and test methods to acl
    
### Bugs
    * Fixed #34 wrong change password url
    * Fixed #33 files throw null pointer exception when streaming
    
    
## 0.9.1

### Changes
    * Removed deprecated methods
    * Deprecated rest and restSync in favor of a rest interface accessible through
      BaasBox.rest()

### Features
    * Added support to all keys of the push api

### Bugs
    * Fixed #32


## 0.8.3

### Changes
    * Project layout conforms to Android Studio/gradle standards
    * Project now includes a samples directory that contains
      api usage samples.

### Features
    * Query api and Acl Api changes
    * First link api implementation
    * Ability to access files url to use with other libraries
    * Alpha OkHttp client implementation available as alternative rest client
    * RestClient implementation has a new init method called when the client is initalized
    * Added skip parameter to query api
    * Ability to initialize documents from json objects

### Fixes
    * Fixed BaasUser bug during fetch of current user (#25)
    * Added missing methods overloads
    * Fixed error with social signup
    * Fixed BaasDocument parcelable cast error
    * Fixed file streaming error on large file size

## 0.8.0

### Features
    * Simplified google cloud messaging integration
    * Ability to concatenate query builders
    * Renamed BaasFile image streaming methods to the more explicit streamImage*
    * Replaced Priority paramter with a more generic flags bitmask See RequestOptions constants.
    
### Fixes
    * Fixed silently ignoring wrong syntax in hostname
    * Fixed missing where params in request
    * Fixed file streaming bugs
