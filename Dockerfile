# To change this license header, choose License Headers in Project Properties.
# To change this template file, choose Tools | Templates
# and open the template in the editor.
FROM openjdk:latest

COPY ./dist /usr/src/app
WORKDIR /usr/src/app
CMD ["java", "-jar", "RelogioUDP.jar"]
