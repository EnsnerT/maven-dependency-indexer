# Maven Dependency Indexer

This Project is indeded to fetch remote POMs and create an index between Artifacts and Dependencies by Version. <br/>
Example Problem:  
- Artifact `com.any:alpha:1.18.3` uses dependency `com.any:bravo:1.1.3`. 
- So if i want to upgrade "bravo" to the latest patch of generation "1.1.x" (for example: 1.1.99), you would need to search all version of "alpha" to find a matching reference.
  
My Solution - an scraper / scaner to index the versions. It should also be able to create matrixies of versions.


## Commands

| command              | arguments                         | Description                                                                          |
|----------------------|-----------------------------------|--------------------------------------------------------------------------------------|
| `test`               |                                   | Run the `main(...)` in `s.Test`                                                      |
| `index`              | {artifact+version}                | Index the specific artifact by version                                               |
| `version` `versions` | {artifact+?version}               | Display the Versions (same as `index -v`)                                            |
| `find`               | {A_artifact} {B_artifact+version} | Search the Database for Artifact_B and return the Artifact_A which is dependent on B |
| ~~`range`~~          | {artifact+version} {?version}     | Index all Artifact within a specific range                                           |

### Index

Indexing will only occure, if the version is <b>not</b> in the Database.csv present.

| parameter                       | Description                             |
|---------------------------------|-----------------------------------------|
| `--version` `-v`                | Only Display the Version Numbers.       |
| `--all` `-a`                    | Scan <b>not</b> only "Recommendations". |
| `--new`                         | Scan also Newer versions.               |
| `--old`                         | Scan also older Versions                |
| `--snapshot` `--snapshots` `-s` | Scan also Snapshots                     |

Example: 
- Current Version: 1.2.3
- There are versions: 1.0.0, 1.0.99 (latest), 1.1.3(current), 1.1.99(recommendation), 2.0.0 (latest)
- 1.0.0 will only index using `index --old --all {arifact+version}` or `index {artifact}:1.0.0`
- 1.0.99 will only index using `index --old {artifact+version}` if it is the last patch of "1.1.x" or `index {artifact}:1.0.99`
- 1.1.3 will index using `index {artifact}:1.1.3`
- 1.1.99 will index using `index --new {artifact+version}` or `index {artifact}:1.1.99`
- 2.0.0 will index using `index --new {artifact+version}` or `index {artifact}:2.0.0`

There is a max of 15 Versions to be scanned at once. To allow more, use the "--yes" parameter to allow for unlimited Scans.

### Version

This command can have the Version ommited.

| parameter                       | Description                             | Requires Version |
|---------------------------------|-----------------------------------------|------------------|
| `--all` `-a`                    | Show <b>not</b> only "Recommendations". | `false`          |
| `--new`                         | Scan also Newer versions.               | `true`           |
| `--old`                         | Scan also older Versions                | `true`           |
| `--snapshot` `--snapshots` `-s` | Scan also Snapshots                     |

If you are happy with the Displayed Versions to be scanned, you can just add the "index" at the end to index these versions.
`versions {artifact}` -> `versions {artifact} index`

### Find

### Range
- NOT IMPLEMENTED YET.

This command i indeded to support version ranges, that if you want to index a specific range of versions.

### Include
- NOT IMPLEMENTED YET.

This command should be a tool to help index all dependencies of a Project POM. <br/>
It could be also very possible that the indexer already works for local repository entries.

