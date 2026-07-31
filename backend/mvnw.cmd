@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    https://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@REM Maven Wrapper Startup Batch Script

@IF "%DEBUG%"=="" @ECHO OFF
@REM Find the project base dir
SET "MAVEN_PROJECTBASEDIR=%~dp0"

SET "MVNW_CMDOPTS="
SET "MVNW_VERBOSE="

@REM Resolve MAVEN_HOME from the registry or env
FOR /F "tokens=2*" %%A IN ('REG QUERY "HKLM\SOFTWARE\Apache\ Maven" /v MavenHome 2^>nul') DO SET "M2_HOME=%%B"
IF DEFINED M2_HOME GOTO runMaven

SET "M2_HOME=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.9"

:runMaven
SET "MAVEN_JAVA_EXE=%JAVA_HOME%\bin\java.exe"
IF NOT EXIST "%MAVEN_JAVA_EXE%" SET "MAVEN_JAVA_EXE=java"

"%MAVEN_JAVA_EXE%" ^
  -classpath "%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar" ^
  org.apache.maven.wrapper.MavenWrapperMain ^
  %MVNW_CMDOPTS% ^
  %*
