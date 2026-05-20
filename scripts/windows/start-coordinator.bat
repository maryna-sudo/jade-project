@echo off
setlocal

set "ROOT_DIR=%~dp0..\.."
set "CLASSPATH=%ROOT_DIR%\lib\jade.jar;%ROOT_DIR%\bin"

java -cp "%CLASSPATH%" jade.Boot ^
  -container ^
  -host localhost ^
  -agents "coordinator:com.example.jade.projectmanagement.ProjectCoordinatorAgent"
