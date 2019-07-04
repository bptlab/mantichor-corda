java -jar XmlReader.jar
echo "moving to $1"
cd ../cordapp_$1
chmod +x gradlew
./gradlew build
./gradlew deployNodes 
./workflows-kotlin/build/nodes/runnodes
./workflows-kotlin/build/nodes/runnodes
./workflows-kotlin/build/nodes/runnodes