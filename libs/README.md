# Local development jars

Drop local (non-committed) jars here for development tooling.

## PlayerEngine API introspection

1) Copy the PlayerEngine jar into this folder as:

- `libs/playerengine-dev.jar`

Example source path on Windows (your Gradle cache):

- `C:\Users\<YOU>\.gradle\caches\modules-2\files-2.1\curse.maven\playerengine-1322604\7371353\...\playerengine-1322604-7371353.jar`

2) Run the Gradle task:

- `./gradlew dumpPlayerEngineApi`

This prints the discovered command/task class names and helps map whistle "automaton" modes to the real PlayerEngine features for your exact version.
