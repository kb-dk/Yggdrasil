
Yggdrasil scripts
=========

This describes the functions of the Yggdrasil helper scripts.


env
--------------------------
Sets environment variables for the simple run scripts (run.sh, run_debug.sh, run_debug_suspended.sh, shutdown.sh, transform.sh, and validate.sh). 


run
--------------------------
Performs a simple startup of Yggdrasil. 


shutdown
--------------------------
Performs a simple shutdown of Yggdrasil by sending a shutdown message to RabbitMQ. 


run_debug
--------------------------
Performs a simple startup of Yggdrasil enabling server-side debugging. 


run_debug_suspended
--------------------------
Performs a simple startup of Yggdrasil enabling server-side debugging and suspends the program until a debugger is connected.


transform
--------------------------
Run Yggdrasils XSLT transformations.


validate
--------------------------
Run Yggdrasils XML validations.


echo_functions.sh
--------------------------
Extra echo helper-functions for Debian systems.


daemon-settings.sh (daemon-settings.template.sh)
--------------------------
Sets environment variables for the init-like script (yggdrasil_ctl.sh).


yggdrasil_ctl.sh
--------------------------
An init-like script, to be called from the real init script.


yggdrasil-init-script
--------------------------
The real init script, so far specifically targeting Red Hat.


