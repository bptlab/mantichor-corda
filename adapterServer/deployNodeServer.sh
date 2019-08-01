echo "moving to $1"
cd ../cordapp_$1
echo "building $2"
screen -dmS $2 ./gradlew $2