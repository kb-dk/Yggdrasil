Yggdrasil
=========

Yggdrasil - SIFD Preservation Service



GIT checkout of repository
--------------------------

git clone https://gitusername@github.com/Det-Kongelige-Bibliotek/Yggdrasil.git

Replace 'gitusername' with your own gitusername.
This is necessary, if you want to push commits back to master branch 

Run "mvn clean -Dmaven.test.skip=true package" to download dependencies to local Maven repository.

Software requisites
-------------------

Eclipse (with git support) : Eclipse 4.2+ (Juno release and above)
Java Oracle JDK 1.7.0_17+ (currently also compiles with Java Oracle JDK 1.6.0_32+)

Source code level 1.6 (changed to 1.7 if needed)
Apache Maven 3.0.4+

RabbitMQ installed on your development machine. Note that rabbitmq depends on Erlang.

Eclipse setup for developers
----------------------------

You need a classpath variable in Eclipse named M2_REPO to point
at the repository folder inside your .m2 repository. 
Some Eclipse distributions have this by default, some don't.
Then perform the following steps:  
  - Add your local git repository to the list known by the Git Repository Exploring tab (use the Git icon with the green +). You should have now an Yggdrasil repository in the list. 
  - Go to File->Import and select Git->Projects from Git and click Next. 
  - Select as Repository Source "Local" and click Next.  
  - Select as Git Repository the Yggdrasil repository, and click next.  
  - Select as Wizard for importing projects "Import existing projects" 
  - Finally, on the Import projects page, select the directory of the local Yggdrasil repository, and optionally add the project to an existing workset. 

You can now edit or add files in eclipse  
It is probably still preferable to make the commits, pushes, and pull using the commandline git command 

Requirements for making the tests pass
--------------------------------------

The broker tests require a running rabbitMQ server running locally on port 5672.  
You can define another host than localhost by setting the environment variable RABBITMQ_HOSTNAME (e.g. export RABBITMQ_HOSTNAME=dia-prod-udv-01.kb.dk) and another port by setting the RABBITMQ_PORT. (e.g. export RABBITMQ_PORT=5673).

Also requires to know the location of the config-files (if the location is different from <user.home>/Yggdrasil/config),
use the YGGDRASIL_CONFIG_DIR to locate config-files.



