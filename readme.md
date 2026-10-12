### diff

This tool tracks changes in a directory.

Precondition: a `JRE` must be installed.

The file `diff.jar` must be copied into a directory directly below the directory to be tracked.

The current content is tracked in `node.yass`.

The tool is started with `java -jar diff.jar`. See [Command Line Interface](src/Main.kt).

You can add a double-clickable file:

- `start.command` for Mac

    ```
    cd `dirname "$0"`
    java -jar diff.jar script SHA3-256
    ```

- `start.bat` for Windows

    ```
    cmd /c java -jar diff.jar script SHA3-256
    pause
    ```

Directory layout:

```
  directoryToBeTracked/
      diffToolDir/
          diff.jar
          start.command
          node.yass
      mySubDir1/
          ...
      mySubDir2/
          ...
      ...   
      myFile1
      myFile2
      ... 
```
