echo "** This Script will generate keys for the server and client"
echo "** Please note that it will CLEAR ALL KEYS IN THE ./keys FOLDER!"

read -n 1 -s -p "~~ Press any key to continue"
echo ""
rm -rf ./keys
mkdir ./keys

openssl ecparam -genkey -name secp384r1 -noout -out ./keys/server-legacy.pem
openssl ec -in ./keys/server-legacy.pem -pubout -out ./keys/server-pub.pem
echo "| Generated Server keys"

openssl ecparam -genkey -name secp384r1 -noout -out ./keys/client.pem
openssl ec -in ./keys/client.pem -pubout -out ./keys/client-pub.pem
echo "| Generated Client keys"

openssl pkey -in ./keys/server-legacy.pem -out ./keys/server.pem
echo "| Converted server-legacy.pem to new format server.pem"

echo "| Done!"
