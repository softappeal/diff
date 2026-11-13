### diff

This tool tracks changes in a directory.

Precondition: a `JRE` must be installed.

The file `diff.jar` must be copied into a directory directly below the directory to be tracked.

The current content is tracked in `node.yass`.

The tool is started with `java -jar diff.jar`.

The output is Markdown.

You can add a double-clickable file:

- `diff.command` for Mac

    ```
    cd `dirname "$0"`
    java -jar diff.jar
    ```

- `diff.bat` for Windows

    ```
    cmd /c java -jar diff.jar
    pause
    ```

Directory layout:

```
    toBeTracked/
        diff/
            diff.jar
            diff.command
            node.yass
        ...
        .../
            ...
```
