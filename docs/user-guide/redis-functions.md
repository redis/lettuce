## Redis Functions

[Redis Functions](https://redis.io/topics/functions-intro) is an
evolution of the scripting API to provide extensibility beyond Lua.
Functions can leverage different engines and follow a model where a
function library registers functionality to be invoked later with the
`FCALL` command.

<div class="informalexample">

``` java
redis.functionLoad("FUNCTION LOAD "#!lua name=mylib\nredis.register_function('knockknock', function() return 'Who\\'s there?' end)");

String response = redis.fcall("knockknock", STATUS);
```

</div>

Using Functions is straightforward. Consuming results in Java requires
additional details to consume the result through a matching type. As we
do not know what your function will return, the API uses call-site
generics for you to specify the result type. Additionally, you must
provide a `ScriptOutputType` hint to `EVAL` so that the driver uses the
appropriate output parser. See [Output Formats](#output-formats) for
further details.

### Output Formats

You can choose from one of the following:

- `BOOLEAN`: Boolean output, expects a number `0` or `1` to be converted
  to a boolean value.

- `INTEGER`: 64-bit Integer output, represented as Java `Long`.

- `MULTI`: List of flat arrays.

- `STATUS`: Simple status value such as `OK`. The Redis response is
  parsed as ASCII.

- `VALUE`: Value return type decoded through `RedisCodec`.

- `OBJECT`: RESP3-defined object output supporting all Redis response
  structures.

### Leveraging Scripting and Functions through Command Interfaces

Using dynamic functionality without a documented response structure can
impose quite some complexity on your application. If you consider using
scripting or functions, then you can use [Command
Interfaces](../redis-command-interfaces.md) to declare
an interface along with methods that represent your scripting or
function landscape. Declaring a method with input arguments and a
response type not only makes it obvious how the script or function is
supposed to be called, but also how the response structure should look
like.

Let’s take a look at a simple function call first:

<div class="informalexample">

``` lua
local function my_hlastmodified(keys, args)
  local hash = keys[1]
  return redis.call('HGET', hash, '_last_modified_')
end
```

</div>

<div class="informalexample">

``` java
Long lastModified = redis.fcall("my_hlastmodified", INTEGER, "my_hash");
```

</div>

This example calls the `my_hlastmodified` function expecting some `Long`
response an input argument. Calling a function from a single place in
your code isn’t an issue on its own. The arrangement becomes problematic
once the number of functions grows or you start calling the functions
with different arguments from various places in your code. Without the
function code, it becomes impossible to investigate how the response
mechanics work or determine the argument semantics, as there is no
single place to document the function behavior.

Let’s apply the Command Interface pattern to see how the the declaration
and call sites change:

<div class="informalexample">

``` java
interface MyCustomCommands extends Commands {

    /**
     * Retrieve the last modified value from the hash key.
     * @param hashKey the key of the hash.
     * @return the last modified timestamp, can be {@code null}.
     */
    @Command("FCALL my_hlastmodified 1 :hashKey")
    Long getLastModified(@Param("my_hash") String hashKey);

}

MyCustomCommands myCommands = …;
Long lastModified = myCommands.getLastModified("my_hash");
```

</div>

By declaring a command method, you create a place that allows for
storing additional documentation. The method declaration makes clear
what the function call expects and what you get in return.