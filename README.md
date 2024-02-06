# MPPEmailNotifier
Here is the source of a projet to allow users to send email notifications on demand based on a MPP file and based on the tasks status

# Requirement : 
- Having proper access to external libraries repository (WARN : some VPN restrict this access, you may have to deactivate it to build properly the project).
- jdk-17.0.8 or higher
- Maven : Installing Apache Maven (https://maven.apache.org/install.html)
- IDE (Visual studio or IntellijIDEA)

- You can take back the code locally by :  from the '<> Code' button on top of this page, then either :
-- Extracting the project using GitHub for desktop
-- Click on "Download ZIP" (extract the downloaded ZIP in a folder of your choice). 

# Simple install anywhere
## Check you got a JDK installed and defined as the default JAVA on the target server/computer. 
Check your environment variable JAVA_HOME is properly pointing to a JDK instance (not to a JRE instance, otherwise you'll not be able to compile the project).

## Compile the project to generate the JAR you'll use anywhere: 
- Using a command line window
- Go to the root directory of the project (where the pom.xml file is presents)
- Execute the following : mvn.exe package

The result should show you the following lines at the end
```log
[INFO] --- jar:3.3.0:jar (default-jar) @ ProjectPlanGUI ---
[INFO] Building jar: C:\Carrier\ATLAS\Projects\ProjectPlanGUI\target\ProjectPlanGUI-0.0.1-SNAPSHOT.jar
[INFO]
[INFO] --- spring-boot:3.0.6:repackage (repackage) @ ProjectPlanGUI ---
[INFO] Replacing main artifact with repackaged archive
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  16.732 s
[INFO] Finished at: 2024-02-06T15:54:41+01:00
[INFO] ------------------------------------------------------------------------
```

Create a .bat file to launch the java program containing the command bellow (change the values in brakets) : 
```cmd
"%JAVA_HOME%\bin\java.exe" -jar <JAR_FULLPATH> --spring.config.location=<APPLICATION_CONFIG_FILE_FULLPATH> >> <LOG_FILE_DESTINATION>
```


