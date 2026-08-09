.PHONY: all build run dev test clean deps

all: build

clean:
	./gradlew clean
	rm -rf .db tower-backend/.db

test:
	./gradlew test

build:
	./gradlew assemble
	podman build -t tower-backend:latest tower-backend/ || docker build -t tower-backend:latest tower-backend/
	podman build -t tower-web:latest tower-web/ || docker build -t tower-web:latest tower-web/

run:
	podman-compose up || docker-compose up

dev:
	./start.sh

deps:
	./gradlew -q tower-backend:dependencies
