package com.xanophis.gradle.fatmanifest

// TODO:  This could support a lookup-by-name set of configurations for
//        multiple registries.  But not today...
class FatManifestExtension {
    String name;
    String url;
    String username;
    String password;
    
    String manifestDirectory = '.'
}
