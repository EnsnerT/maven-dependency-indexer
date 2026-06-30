# Maven Dependency Indexer
## The Project
This project is **<u>not</u>** affiliated with apache, their product maven and neither am I. <br/>

The project is indeded to fetch remote maven POMs and create an index between maven Artifacts and maven Dependencies by Version using apache's maven libraries. <br/>
If any legal issues arise, dont hesitate to create a Issue in GitHub for this project. I will try to stay up to date with it.<br/>

### Goal of the Project
Example Problem this project should solve:  
- Example Artifact `com.any:alpha:1.18.3` uses dependency `com.any:bravo:1.1.3`. 
- To Upgrade "bravo" to the latest patch of generation "1.1.x" (for example: 1.1.99) but not higher, you would need to search all version of "alpha" to find a matching version.

My Solution : a scraper (or scaner) to index the versions and its dependencies. <br/>
It should also be able to create matrixies of versions.

## Commands

| command     | arguments                         | &gt; 1 Artifact | Description                                                                          |
|-------------|-----------------------------------|:----------------|--------------------------------------------------------------------------------------|
| `help`      |                                   |                 | Show help for the current installed Analyser version.                                |
| `index`     | {artifact+version}                | Yes             | Index the specific artifact by version                                               |
| `version`   | {artifact+?version}               | No              | Display the Versions (same as `index -v`)                                            |
| `find`      | {A_artifact} {B_artifact+version} | Yes             | Search the Database for Artifact_B and return the Artifact_A which is dependent on B |
| ~~`range`~~ | {artifact+version} {?version}     |                 | Index all Artifact within a specific range                                           |
| ~~`test`~~  |                                   |                 | Run the `main(...)` in `s.Test`                                                      |



<caption>General Parameters</caption>

| parameter                       | Description                                                                                                                         |
|---------------------------------|-------------------------------------------------------------------------------------------------------------------------------------|
| `--tab` `-t`                    | when the output prints a table, the columns will be `\t` seperated and the rows will seperate with `\n`. <u>Good for Automation</u> |
| `--verbose` `-v`                | Sometimes there is additional debug data or interesting data related to the output. With Verbose mode, you will see these.          |
| `--no-color` `--no-colour` `-c` | Prevents the output to be colorfull. <u>Good for Automation</u>.                                                                    |

---

### Help
The help command works like `help` but also for `index help` or `find help` to show more details about each command.

### Index

Indexing will only occure, if the version is <b>not</b> in the Database.csv present.

| parameter                       | Description                                         |
|---------------------------------|-----------------------------------------------------|
| `--all` `-a`                    | Scan <b>not</b> only "Recommendations".             |
| `--new` `-n`                    | Scan also newer Versions.                           |
| `--old` `-o`                    | Scan also older Versions                            |
| `--snapshot` `--snapshots` `-s` | Scan also Snapshots                                 |
| `--release` `--releases` `-r`   | Scan also Releases (only in combinatzion with `-s`) |

Note: Shorthands expand internally, if you chain them. You can use `-sonar` to declare `-s -o -n -a -r` which translates to `--snapshot --old --new --all --release`.

Example: 
- Current Version: 1.2.3
- There are versions: 1.0.0, 1.0.99 (latest), 1.1.3(current), 1.1.99(recommendation), 2.0.0 (latest)
- 1.0.0 will only index using `index --old --all {arifact+version}` or `index {artifact}:1.0.0`
- 1.0.99 will only index using `index --old {artifact+version}` if it is the last patch of "1.1.x" or `index {artifact}:1.0.99`
- 1.1.3 will index using `index {artifact}:1.1.3`
- 1.1.99 will index using `index --new {artifact+version}` or `index {artifact}:1.1.99`
- 2.0.0 will index using `index --new {artifact+version}` or `index {artifact}:2.0.0`

### Version

This command can have the Version ommited.

| parameter                       | Description                                         | Requires Version |
|---------------------------------|-----------------------------------------------------|------------------|
| `--all` `-a`                    | Scan <b>not</b> only "Recommendations".             | `false`          |
| `--new` `-n`                    | Scan also newer Versions.                           | `true`           |
| `--old` `-o`                    | Scan also older Versions                            | `true`           |
| `--snapshot` `--snapshots` `-s` | Scan also Snapshots                                 | `false`          |
| `--release` `--releases` `-r`   | Scan also Releases (only in combinatzion with `-s`) | `false`          |

If you are happy with the Displayed Versions to be scanned, you can just add the "index" at the end to index these versions.
`versions {artifact} {?params}` -> `versions {artifact} {?params} index`

### Find
This command is the main Feature of this Application.

| parameter   | Description                                                                                              |
|-------------|----------------------------------------------------------------------------------------------------------|
| `-all` `-a` | show all versions of given artifacts (by default, it will only show intersections between ALL artifacts) |

| argument style | Description |
|----------------|-------------|
| `find *:alpha  | show        |



### Range
- NOT IMPLEMENTED YET.

This command i indeded to support version ranges, that if you want to index a specific range of versions.

### Include
- NOT IMPLEMENTED YET.

This command should be a tool to help index all dependencies of a Project POM. <br/>
It could be also very possible that the indexer already works for local repository entries.

---

## Expectable Issues
Running the App, you may encounter some common Issues. Here is what happend and how to fix it:

| What is shown                                                                                                                                                                                                                            | What happend                                                                                                                                           | How to fix it                                                                                           |
|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------|
| org.eclipse.aether.internal.impl.DefaultArtifactResolver - Artifact ... is present in the local repository, but cached from a remote repository ID that is unavailable in current build context, verifying that is downloadable from ... | Your local respository's `_remote.repositories` file for the Artifact contains an unknown Repository ID, which was not provided anywhere in the build. | Either remove the repository id not in the List of the Output OR add it to the ~/.m2/settings.xml file. |

## Unplaned Changes
- ! **currently the App can not handle 2 Artifact Versions pointing to the same dependency version**.
- maven has a m2.conf plexus loader. maybe i could add a similar one like maven.
- make a maven plugin out of the App.
- a config file for different settings provided for once. 
    - modify maven logging
    - add repositories
    - configure the db.csv
        - configure the database type -> JSON , YAML , EXCEL ...
- implement better logging
- implement process housekeeping (keep informations between runs). 
  - for example, if a user indexes something, the next logical step will be viewing relations between versions using the `find` command.
  - loading and storing a lot of data comes at a cost. Housekeeping the database file is mandatory!