# AutoBackup

A mod that implements automatic server backup, adding `/backup`.

Made for servers! Clients don't have to install AutoBackup to connect to the server.

## Usage

`/backup enable <enable>` Enable or disable auto backup, which is enabled by default, only valid for this server
shutdown.

Automatically compress the folder `world` in zip format and backup it to the folder `backup` when the server is shut
down.
