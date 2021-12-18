@ECHO OFF
:: 修改编码为utf8un
:: CHCP 65001
TITLE PROTO-JAVA-GEN
REM 设置接口文档路径
SET CURR_PATH=%cd%
CD ..\..\..\..\..
SET PROTO_PATH=%cd%\cm4j-hotswap\src\main\resources\config\resource\proto
SET PROTO_PATH_GRPC=%PROTO_PATH%\grpc
SET JAVA_OUT=%cd%\cm4j-hotswap\src\main\java
CD %CURR_PATH%
SET PROTOC_PATH=%CURR_PATH%\protoc.exe

rd /s /q %JAVA_OUT%\com\cm4j\grpc\proto\

ECHO=
ECHO ====================================
ECHO 1.编译proto文件
ECHO   PROTO_PATH      =%PROTO_PATH%
ECHO   JAVA_OUT        =%JAVA_OUT%
ECHO   PROTOC_PATH     =%PROTOC_PATH%
ECHO ====================================
FOR /R %PROTO_PATH_GRPC% %%f IN (*.proto) DO ( 
    ECHO %%f
    %PROTOC_PATH% -I=%PROTO_PATH% --proto_path=%PROTO_PATH% --java_out=%JAVA_OUT% %%f
    %PROTOC_PATH% -I=%PROTO_PATH% --plugin=protoc-gen-grpc-java=%CURR_PATH%\protoc-gen-grpc-java-1.34.1-windows-x86_64.exe --grpc-java_out=%JAVA_OUT% --java_out=%JAVA_OUT% %%f
)

PAUSE & EXIT /b