# CHANGELOG

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

### Fixes
    * Fixed silently ignoring wrong syntax in hostname
    * Fixed missing where params in request