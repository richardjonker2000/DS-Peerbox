# Peerbox
### General Instructions
To deploy the system in Docker, it is necessary to run both containers: the gossip-router and peerbox-host.
Peerbox-host is set to scale to 2 by default, this can be changed at will.

To launch the GUI, it is necessary to run the GUIMain file. This uses JavaFX, which requires additional
VM-options in intellij ([JavaFX-IntelliJ]( https://openjfx.io/openjfx-docs/)) - "module-path path-to-javafx-lib 
--add-modules=javafx.controls,javafx.fxml". 

The following Directories need to exist for the system to work:  Files/UploadedFiles,
Files/ReceivedFiles, Files/RetrievedFiles, Files/SentFiles, this is true for the Docker/ directory
aswell.

### Instructions regarding the GUI

1. It is necessary to refresh the members list to detect any changes to the hosts in the network.
2. A host must be selected for the functions to send instructions to the correct host.
3. All inputs are file names, for example test.pdf, not the full file path. The nodes manage their own file systems.
4. The retrieved files are stored in Files/retrievedFiles on the local system. In Docker this is mounted to the docker
   directory.
5. sometimes the inputs for the ScrollPanes are not ordered correctly, after a few attempts the inputs will become 
   ordered
   
   