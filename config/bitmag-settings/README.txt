This folder requires the settings for the Bitrepository, where you are preserving your data.
It requires the RepositorySettings.xml and the ReferenceSettings.xml (only the one for the commandline-client), 
and it also requires the certificate, if your bitrepository is configures to using certificates.
It you use certificates, then the certificate must be referenced to from the bitmag.yml in the parent folder (current it referes to a non-existing 'webclient.pem' in this folder).

If you do not have an existing Bitrepository installation, and you only want to test Yggdrasil, then you can use the Bitrepository Quickstart:
https://sbforge.org/display/BITMAG/Quickstart
Just install and start the Bitrepository Quickstart according to the guide, and then copy the settings for the commandline client into this folder:
cp {BITREPOSITORY_QUICKSTART_INSTALL_FOLDER}/conf/commandline/* .

It is recommended to install the Bitrepository Quickstart and Yggdrasil on the same machine, and it does not require more than a normal machine easily should be able to handle.
Though if you do put the Bitrepository Quickstart on a different machine, then you need to change the settings to point to the right ActiveMQ and WebDav (and ensure that the firewall is open).
