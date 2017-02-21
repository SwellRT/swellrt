#!/bin/bash

if [ $TRAVIS_BRANCH = "master" ]; then
  echo "Deploying..."
  openssl aes-256-cbc -K $encrypted_dd5995a51c0a_key -iv $encrypted_dd5995a51c0a_iv -in .travis/secrets.tar.enc -out .travis/secrets.tar -d
  tar xvf .travis/secrets.tar --directory .travis 
  chmod 600 .travis/id_rsa
  chmod +x .travis/deploy-demo.sh
  cd .travis
  ./deploy-demo.sh	
fi

