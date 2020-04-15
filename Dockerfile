# Just test compile the jar in an controlled environment (both JDK 8 and 11 should work)
#FROM maven:3-jdk-8
FROM maven:3-jdk-11

# Download many dependencies to cache some files
#COPY pom.xml /app/
#COPY KotlinUtils/pom.xml /app/KotlinUtils/pom.xml
#COPY Lib6502/pom.xml /app/Lib6502/pom.xml
#COPY Emulator/pom.xml /app/Emulator/pom.xml
WORKDIR /app
#RUN mvn dependency:go-offline

# Build and package war file
ADD . /app
RUN mvn package
