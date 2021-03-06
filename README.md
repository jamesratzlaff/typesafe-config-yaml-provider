# typesafe-config-yaml-provider
A ConfigProvider implementation that allows for yaml files to be used with the typesafe config library

Simply include this library in your classpath if you are using the version of typesafe config that has the SPI feature and you should be able to load yml config files like you would any other config file.

You can add it to your maven project by inclduing the dependency
``` xml
<dependency>
  <groupId>com.jamesratzlaff</groupId>
  <artifactId>typesafe-config-yaml-provider</artifactId>
  <version>2019.09.16</version>
</dependency>
```
For other build systems go [here](https://search.maven.org/artifact/com.jamesratzlaff/typesafe-config-yaml-provider/2019.09.16/jar) to to get what you need to include this library

Including a file within your yml can be done by tagging the value of an item with !include



## This libaray is currently in a POC stage
This was developed to show the flexibility of the new SPI feature for typesafe config.  That said the code is not very pretty right now as well as javadoc and unit tests being pretty much non-existant. So throw some caution to the wind before you consider using this in a production environment.
______

### Known limitations
Currently for a mulidoc-file things are a little wonky when it comes to using substitutions/placeholders to reference other values within the file. Also for multi-doc files, each stream with in the file is stored in a ConfigList with the key of ```"---"``` (or you can use the String constant ```YAML_CONF.MULTI_DOC_KEY``` )

### Multi-doc file example
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

to retrieve item\_a in the first document stream you'd use

``` java
helloConfig.getConfigList("---").get(0).getString("item_a");
//returns the string value hello
```

to retrieve item\_a in the _second_ document stream you'd use

``` java
helloConfig.getConfigList("---").get(1).getString("item_a");
//returns the string value you're
```

