# typesafe-config-yaml-provider
A ConfigProvider implementation that allows for yaml files to be used with the typesafe config library

Simply include this library in your classpath if you are running the version of typesafe config that has the SPI feature.

Including a file within your yml can be done by tagging the value of an item with !include

### Known limitations:
Currently for a mulidoc-file things are a little wonky when it comes to using substitutions/placeholders to reference other values within the file. Also for multi-doc files, each stream with in the file is stored in a ConfigList with the key of "_"

### Example
Let's say you have a multi-doc yaml file named 'hello.yml' similar to the following:
``` yaml
item_a: hello
item_b:
    - is
    - it
    - me
---
item_a: you're
looking: 4
```
Loading it is exactly the same as loading any other typesafe config file (the SPI stuff takes care of everything for you)
``` java
Config helloConfig = ConfigFactory.load("hello");
```

Since this is a multi-doc yaml file there is a slight difference in retrieving values (for the time being) than what you would normally would do with a single-doc file (which behaves exactly the same as a standard Config object loaded from a conf,json, or properties file)

to retrieve item_a in the first document stream you'd use
``` java
helloConfig.getConfigList("_").get(0).getString("item_a");
//returns the string value hello
```
to retrieve item_a in the _second_ document stream you'd use
``` java
helloConfig.getConfigList("_").get(1).getString("item_a");
//returns the string value you're
```

