# MPPEmailNotifier
Here is the source of a projet to allow users to send email notifications on demand based on a MPP file and based on the tasks status.

To use it, feel free to download it (using the green <>Code button on top right, then click on Download ZIP).

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
You can define your own application.properties file to setup parameters bellow.

# Parameters

```properties 
server.host=localhost 
server.port=8081
```
These values are used to define the port and host where you want to make the application run locally
```properties 
spring.application.name=ATLAS MPP NOTIFIER
```
Name of the application (not any impact)
```properties 
mppFilePath=\\\\ServerXXXX\\sharedFolder\\Transfer\\MyMPPFile.mpp
```
Default full MPP path to use
```properties 
outlineFilter=
```
Allow to have a default filter on email reminders (to be able to send reminders only to parts of the plan)
```properties 
notif.email.FROM=TOWER_CONTROL_EMAILS@mail.com
```
Email address used as emitter to send the reminder emails
```properties 
notif.email.BCC=TOWER_CONTROL_EMAILS@mail.com
```
EMail to define as BCC (if several email address used, use a coma as separator without space between as the following : TOWER_CONTROL_EMAILS@mail.com,PMO@mail.com)
```properties 
notif.email.TEST=john.doe@mail.com
```
Default email used as recipient for test purpose (if several email address used, use a coma as separator without space between as the following : TOWER_CONTROL_EMAILS@mail.com,PMO@mail.com)
```properties 
mail.smtp.host=smtp.mail.com
```
SMTP server to use to send Emails
```properties 
mail.smtp.port=25
```
SMTP port to use to send Emails
```properties 
spring.thymeleaf.prefix=file:C:\\temp\\templates\\
```
Path where the application and email templates
```properties 
#Allow to use a specific webContext as in http://localhost:8081/webContext/
web.context=
```
That parameter allow you to defined a specific web context (in case you have several applications running at the same place)
```properties 
spring.thymeleaf.prefix=file:C:\\temp\\templates\\
```

To install your bat file in a windows server as a Service use the nssm.exe tool presents in the tools directory. That will allow you to setup any command as a Service.
(Don't forget to launch the service with an account having access to the MPP file you want to use).

# Code change

##  Change email template to send: 
If you decide to edit the default template, then:
- Define a directory on same server as the application where your templates will be. 
- Copy all template files from here : [templates](src/main/resources/templates)
- Define the following properties in your application.properties file (adapt your path with your own directory):
```properties 
spring.thymeleaf.prefix=file:C:\\temp\\templates\\
```
- Restart the app (that will load the new template at restart only).  

you'll have then to update the following file : [reminderContent.html](src/main/resources/templates/reminderContent.html)

This file contains several parts: (you'll find each part in the file starting with a comment as bellow to make it easier for you to find out)

```html
<!-- PART1 : Here is the text on top of tasks lists. You can change the text if needed -->
```
```html
<!-- PART2 : Here is a reminder of the task part addressed by the email send (parent task full path and assigned resources to that parent task -->
```
```html
<!-- PART3 : Here is the list of late tasks -->
```
```html
<!-- PART4 : Here is the list of current tasks -->
```
```html
<!-- PART5 : Here is the list of next tasks -->
```
```html
<!-- PART6 : Here is the text added at the end of the email in case of test (to allow to see who will be the recipients of the email-->
```

## Change the Java code of the app
Most of the application behavior is in [TaskController.java class](src/main/java/com/graphikcorp/msproject/ProjectPlanGUI/controller/TaskController.java)

you'll find there all methods you may be interested in and related comments. 
