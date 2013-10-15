Yggdrasil
=========

Yggdrasil - SIFD Preservation Service



GIT checkout of repository
==========================

git clone https://gitusername@github.com/Det-Kongelige-Bibliotek/Yggdrasil.git

Replace 'gitusername' with your own gitusername.
This is necessary, if you want to push commits back to master branch 


Run "mvn clean install" to download dependencies to local Maven repository.


Eclipse setup for developers
============================

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

