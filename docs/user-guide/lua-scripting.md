### Lua Scripting

[Lua](https://redis.io/topics/lua-api) is a powerful scripting language
that is supported at the core of Redis. Lua scripts can be invoked
dynamically by providing the script contents to Redis or used as stored
procedure by loading the script into Redis and using its digest to
invoke it.

<div class="informalexample">

``` java
String helloWorld = redis.eval("return ARGV[1]", STATUS, new String[0], "Hello World");
```

</div>

Using Lua scripts is straightforward. Consuming results in Java requires
additional details to consume the result through a matching type. As we
do not know what your script will return, the API uses call-site
generics for you to specify the result type. Additionally, you must
provide a `ScriptOutputType` hint to `EVAL` so that the driver uses the
appropriate output parser. See [Output Formats](redis-functions.md#output-formats) for
further details.

Lua scripts can be stored on the server for repeated execution.
Dynamically-generated scripts are an anti-pattern as each script is
stored in Redis' script cache. Generating scripts during the application
runtime may, and probably will, exhaust the host’s memory resources for
caching them. Instead, scripts should be as generic as possible and
provide customized execution via their arguments. You can register a
script through `SCRIPT LOAD` and use its SHA digest to invoke it later:

<div class="informalexample">

``` java
String digest = redis.scriptLoad("return ARGV[1]", STATUS, new String[0], "Hello World");

// later
String helloWorld = redis.evalsha(digest, STATUS, new String[0], "Hello World");
```

</div>