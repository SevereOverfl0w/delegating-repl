mvn deploy:deploy-file \
      -Dfile=delegate.jar \
      -DrepositoryId=clojars \
      -Durl=https://clojars.org/repo \
      -DpomFile=pom.xml
