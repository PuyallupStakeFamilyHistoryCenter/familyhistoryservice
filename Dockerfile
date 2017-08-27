FROM maven:3-jdk-8 AS build

RUN mkdir -p /familyhistorydiscoveryservice
WORKDIR /familyhistorydiscoveryservice

# Precache dependencies
COPY ./pom.xml /familyhistorydiscoveryservice/pom.xml
RUN mvn dependency:go-offline

# Compile project
COPY . /familyhistorydiscoveryservice
RUN mvn install -Dmaven.test.skip=true

# Run project
CMD mvn process-classes exec:exec -Dexec.args="-classpath %classpath org.puyallupfamilyhistorycenter.service.FamilyHistoryCacheServlet" -Dexec.executable=java

