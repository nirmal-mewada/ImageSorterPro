@echo off
REM %1 is the argument passed from Explorer (the clicked folder path)
REM cd D:\master-drive\nirmal\Dev\PhotoSort\ImageSorterPro\target
REM java.exe -Xms1G -Xmx1G -jar --module-path "D:\n-temp\javafx-sdk-24.0.2\lib" --add-modules javafx.controls,javafx.fxml,javafx.swing ImageSorterPro-1.0-SNAPSHOT.jar

set "folderPath=%~1"

REM Navigate to your project directory
cd /d "D:\master-drive\nirmal\Dev\PhotoSort\ImageSorterPro\target"

REM Run your JAR with JavaFX
java.exe -Xms1G -Xmx1G -jar ^
  --module-path "D:\n-temp\javafx-sdk-24.0.2\lib" ^
  --add-modules javafx.controls,javafx.fxml,javafx.swing ^
  ImageSorterPro-1.0-SNAPSHOT.jar "%folderPath%"
  
pause