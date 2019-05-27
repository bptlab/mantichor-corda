if [[ "$OSTYPE" == "darwin"* ]]; then
    ./gradlew build
    ./gradlew deployNodes
    workflows-kotlin/build/nodes/runnodes
else
    gradlew.bat build
    gradlew.bat deployNodes
    workflows-kotlin/build/nodes/runnodes.bat
fi
