## Mantichor Corda Adapter

Adapter for [Mantichor](https://github.com/bptlab/mantichor-frontend/wiki/Architecture) to execute Process Choreographies on the Corda Platform. In the [wiki](https://github.com/bptlab/mantichor-corda/wiki) you can find the detailled architecture and design concept. In this Readme you will find a quick start to build and run the adapter.

- [Repository Structure](#repository-structure)
- [Deployment](#deployment)
  * [Starting](#starting)
  * [Stopping](#stopping)
  * [Execute tasks](#execute-tasks)
- [Development](#development)
  * [Requirements](#requirements)
  * [IntelliJ Settings](#intellij-settings)
  * [Build the project](#build-the-project)
  * [Node.js Server](#nodejs-server)
  * [Execute tasks in development](#execute-tasks-in-development)
  
## Repository Structure

* `adapterServer/` | Node.js server and pre-built jars
* `cordapp_template/` | intelliJ CorDapp project as template
* `initGen/` | src for the pre-built jar to calculate the participant list and the right address for task execution
* `tasksGen/` | src for the pre-built jar to calculate executable tasks based on corda state
* `xmlDebugging/` | src for the pre-built jar for bpmn parsing and project generation
* `.gitignore` | files ignored by Git
* `Dockerfile` | Docker configuration
* `README.md` | Readme about the repository
* `constants.properties` | Corda configuration

## Deployment

You can start a **pre-built version** of the adapter using docker. Make sure that the latest versions of [`docker`](https://docs.docker.com/install/) is installed.

### Starting

Open a terminal window and use `docker pull ferandal/cordaadapter` and then `docker run —rm -p 8080:8080 ferandal/cordaadapter`.

### Stopping

To stop the server just kill the docker container. `Docker kill (DockerID)`

### Execute tasks

With [Postman](https://www.getpostman.com/downloads/) you can then send requests to `localhost:8080` as described in the REST API for mantichor. The implemented interface can be found as Swagger Documentation in [Mantichor's Frontend Repository](https://github.com/bptlab/mantichor-frontend/blob/master/adapter-apidoc.yaml).


## Development

### Requirements
* **Java 8 JVM** - at least version 8u171, but not Java 9 or higher.
* **IntelliJ IDEA** - supported versions 2017.x, 2018.x and 2019.x (with Kotlin plugin version 1.2.71).
* **Gradle 4.10** - the gradlew script in the project will download it for you.

### IntelliJ Settings
1. Open IntelliJ
2. Open the folder `xmlDebugging\` as project in IntelliJ.
3. Make sure that the folder `src\` is marked as *Sources Root*.
4. Click `File`, then `Project Structure`. Under `Project SDK:`, set the project SDK by clicking `New...`, clicking `JDK`, and navigating to `C:\Program Files\Java\jdk1.8.0_XXX` on Windows or `Library/Java/JavaVirtualMachines/jdk1.8.XXX` on MacOSX (where XXX is the latest minor version number). Click `Apply` followed by `OK`.
5. Again under `File` then `Project Structure`, select `Modules`. Click `+`, then `Import Module`, then select the `cordapp-template` folder and click `Open`. Choose to `Import module from external model`, select `Gradle`, click `Next` then `Finish` (leaving the defaults) and `OK`.
```diff 
! Gradle will now download all the project dependencies and perform some indexing. 
! This usually takes a minute or so.
```

### Build the project
For developing the bpmn parser:
1. Inside of the main() function of the XmlReader.kt you have to set the path to the testing bpmn. The default value is choreo.bpmn as this is used by the adapter server
2. Run the `XmlReader.kt`, which will generate a CorDapp project for the BPMN under `mantichor-corda\cordapp__XXX\` (XXX = id of the BPMN)
3. Open a terminal window in the `cordapp_XXX` directory. 
```diff
! For Windows: The project is build for the Unix platform. 
! Therefore, you have to open the generated CorDapp as project in IntelliJ. 
! Then delete the folder `.gradle` and the file `gradle-wrapper.properties` 
! under `mantichor-corda\cordapp__XXX\gradle\wrapper`. 
! Then click on `Import changes`.
```
4. Run the `build` Gradle task to compile our CorDapp project:
        **Unix/Mac OSX:** `./gradlew build`
        **Windows:** `gradlew.bat build`
5. Run the `deployNodes` Gradle task to build four nodes with our CorDapp already installed on them:
        **Unix/Mac OSX:** `./gradlew deployNodes`
        **Windows:** `gradlew.bat deployNodes`
6. Start the nodes by running the following command:
    **Unix/Mac OSX:** workflows-kotlin/build/nodes/runnodes
    **Windows:** call workflows-kotlin/build/nodes/runnodes.bat
7. Each participant server needs to be started in its own terminal/command prompt, replace **participantID** with the specific participant id, e.g. *participant_a*:
    **Unix/Mac OSX:** ./gradlew run**participantID**Server
    **Windows:** gradlew.bat run**participantID**Server
```diff
+ Under `mantichor-corda\xmlDebugging\` you will find the file `deployServer.txt`, 
+ which the `XmlReader.kt` has also generated. There you have a list of the participants.
```

### Node.js Server
The server that implements the defined interface lives inside the `mantichor-corda\adapterServer\` folder. The `index.js` file contains the server. You need to have [Node.js](https://nodejs.org/en/download/) installed to run it. The server is completely written with libraries that [Node.js](https://nodejs.org/en/download/) provides. Therefore, additional installations are not required. To run it, open a terminal window and execute `node index.js`.

### Execute tasks in development
To test the Server refere to the defined [interface](https://github.com/bptlab/mantichor-frontend/blob/master/adapter-apidoc.yaml) and send the corresponding requests to `http://localhost:8080`. We recomment to use [Postman](https://www.getpostman.com/downloads/) to do that. For testing the projects, that are generated by the bpmn parser, you need to send the request to the corresponding corda node. For excuting a task corda implies a special structure:
`http://localhost:50005/api/generatedBPMNID/TASKNAME?partyName0=O=FIRSTPARTICIPANTID, L=London, C=GB&partyName1=O=SECONDPARTICIPANTID, L=London, C=GB&...` 
For example you could run: `http://localhost:50005/api/generatedchoreo/Task?partyName0=O=participant_a, L=London, C=GB&partyName1=O=participant_b, L=London, C=GB`

###### tags: `Corda Adapter` `Corda` `Process Choreography`
