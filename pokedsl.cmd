@echo off
rem Executa a CLI da DSL no Windows (cmd e PowerShell).
rem Requer "mvnw.cmd package" (gera o jar) e "npm ci" em motor-smogon\.
setlocal
set "RAIZ=%~dp0"
set "JAR=%RAIZ%target\pokemon-dsl-0.1.0-SNAPSHOT.jar"

if not exist "%JAR%" (
    echo Jar nao encontrado: %JAR%. Execute "mvnw.cmd package" na raiz do projeto. 1>&2
    exit /b 2
)

java -Dpokedsl.adaptador="%RAIZ%motor-smogon\calcular.js" -jar "%JAR%" %*
exit /b %ERRORLEVEL%
