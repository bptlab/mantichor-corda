java -jar XmlReader.jar
echo "moving to $1"
cd ../cordapp_$1
chmod +x gradlew
./gradlew build
./gradlew deployNodes
cd workflows-kotlin/build/nodes
screen -dm bash -c 'java -jar runnodes.jar; exec bash'