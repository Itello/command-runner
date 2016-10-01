# Command Runner

Command Runner is a GUI for the lazy who don't want to have to remember script parameters or in what order to run commands. 
Basically a command organizer and executor (like a script) in a JavaFX GUI.
Command Runner requires Java 8 or later.

![screenshot](http://i.imgur.com/1IjlDsL.png "screenshot")

##Some of the functionality
* Run selected commands (sequentially or in parallel)
* Run history log which saves output from each run
* Create a runnable group
  - Commands inherit directory from their group (recursively)
* Drag and drop a command
  - Hold <kbd>CTRL</kbd> to copy rows instead of moving
  - Drag to your file manager to create a file with commands to backup or share
* Run a saved command or group via command-line arguments
  - To run with variable parameters (from within command runner or via command line), use the reserved `#`-character together with `--run` and `--variables` 
  - Example: `--run=comment --variables=name=x,folder=y`, where `comment` is the command comment and `#name` will be replaced with `x`, `#folder` with `y`.
* Input field for when a command or script requires input

##Keyboard shortcuts (PC)
* <kbd>DEL</kbd>: remove selected commands
* <kbd>ENTER</kbd>: run selected commands sequentially
* <kbd>CTRL+ENTER</kbd>: run selected commands in parallel
* <kbd>PAGE_UP</kbd>: move selected rows up one step
* <kbd>PAGE_DOWN</kbd>: move selected rows down one step
* <kbd>CTRL+S</kbd>: save commands
* <kbd>CTRL+C</kbd>: copy selected commands (as file and as pasteable)
* <kbd>CTRL+V</kbd>: paste commands to selected row
* <kbd>CTRL+X</kbd>: cut selected commands
* <kbd>CTRL+N</kbd>: add new row
* <kbd>CTRL+G</kbd>: create group for selected commands
* <kbd>F2</kbd>: edit selected command (command name and arguments)
* <kbd>SHIFT+F2</kbd>: edit selected directory
* <kbd>CTRL+F2</kbd>: edit selected comment

## [Change Log](CHANGELOG.md)
